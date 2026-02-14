"""
Call Management API Routes.

Handles call records: create, read, update, delete, search.
"""

from datetime import datetime
from typing import Optional

from fastapi import APIRouter, HTTPException, Query

from database.firebase_client import (
    create_call_record,
    delete_call_record,
    get_call_history,
    get_call_record,
    search_calls,
    update_call_record,
)
from models.schemas import (
    CallRecord,
    CallRecordCreate,
    CallRecordUpdate,
    CallStatus,
    ChatMessage,
    ChatMessageCreate,
    SenderType,
)

router = APIRouter(prefix="/calls", tags=["Calls"])


@router.post("/", response_model=dict)
async def create_new_call(call: CallRecordCreate):
    """Start a new call screening session."""
    call_data = {
        "caller_number": call.caller_number,
        "caller_name": call.caller_name,
        "status": CallStatus.SCREENING.value,
        "start_time": datetime.utcnow().isoformat(),
        "transcript": [],
        "spam_score": 0.0,
        "risk_level": "low",
        "scam_keywords_found": [],
        "is_blocked": False,
    }

    user_id = call.user_id or "default_user"
    call_id = await create_call_record(user_id, call_data)

    return {
        "call_id": call_id,
        "status": "screening",
        "message": "Call screening session created.",
    }


@router.get("/history")
async def get_history(
    user_id: str = Query(default="default_user"),
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
):
    """Get call history for a user."""
    calls = await get_call_history(user_id, limit=limit, offset=offset)
    return {"calls": calls, "count": len(calls)}


@router.get("/search")
async def search_call_history(
    q: str = Query(..., min_length=1, description="Search query"),
    user_id: str = Query(default="default_user"),
):
    """Search call records by transcript, summary, or number."""
    results = await search_calls(user_id, q)
    return {"results": results, "count": len(results)}


@router.get("/{call_id}")
async def get_call(call_id: str, user_id: str = Query(default="default_user")):
    """Get a specific call record."""
    call = await get_call_record(user_id, call_id)
    if not call:
        raise HTTPException(status_code=404, detail="Call not found")
    return call


@router.put("/{call_id}")
async def update_call(
    call_id: str,
    updates: CallRecordUpdate,
    user_id: str = Query(default="default_user"),
):
    """Update a call record."""
    update_data = updates.model_dump(exclude_none=True)
    if "status" in update_data:
        update_data["status"] = update_data["status"].value
    if "end_time" in update_data:
        update_data["end_time"] = update_data["end_time"].isoformat()

    success = await update_call_record(user_id, call_id, update_data)
    if not success:
        raise HTTPException(status_code=404, detail="Call not found")

    return {"message": "Call updated", "call_id": call_id}


@router.post("/{call_id}/messages")
async def add_message(call_id: str, msg: ChatMessageCreate, user_id: str = Query(default="default_user")):
    """Add a chat message to a call's transcript."""
    call = await get_call_record(user_id, call_id)
    if not call:
        raise HTTPException(status_code=404, detail="Call not found")

    message = {
        "sender": msg.sender.value,
        "text": msg.text,
        "timestamp": datetime.utcnow().isoformat(),
        "audio_url": msg.audio_url,
    }

    transcript = call.get("transcript", [])
    transcript.append(message)

    await update_call_record(user_id, call_id, {"transcript": transcript})

    return {"message": "Message added", "msg": message}


@router.post("/{call_id}/end")
async def end_call(call_id: str, user_id: str = Query(default="default_user")):
    """End a call screening session."""
    call = await get_call_record(user_id, call_id)
    if not call:
        raise HTTPException(status_code=404, detail="Call not found")

    end_time = datetime.utcnow()
    start_time = datetime.fromisoformat(call.get("start_time", end_time.isoformat()))
    duration = int((end_time - start_time).total_seconds())

    await update_call_record(user_id, call_id, {
        "status": CallStatus.ENDED.value,
        "end_time": end_time.isoformat(),
        "duration_seconds": duration,
    })

    return {
        "message": "Call ended",
        "call_id": call_id,
        "duration_seconds": duration,
    }


@router.post("/{call_id}/block")
async def block_caller(call_id: str, user_id: str = Query(default="default_user")):
    """Block the caller associated with this call."""
    success = await update_call_record(user_id, call_id, {
        "status": CallStatus.BLOCKED.value,
        "is_blocked": True,
    })

    if not success:
        raise HTTPException(status_code=404, detail="Call not found")

    return {"message": "Caller blocked", "call_id": call_id}


@router.delete("/{call_id}")
async def delete_call(call_id: str, user_id: str = Query(default="default_user")):
    """Delete a call record."""
    success = await delete_call_record(user_id, call_id)
    if not success:
        raise HTTPException(status_code=404, detail="Call not found")

    return {"message": "Call deleted", "call_id": call_id}
