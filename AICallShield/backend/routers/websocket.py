"""
WebSocket route for real-time call screening communication.

Enables live bidirectional communication between Android app and backend
for real-time transcription, AI responses, and call control.
"""

import json
import logging
from datetime import datetime
from typing import Optional

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from models.schemas import CallStatus, SenderType
from services.ai_engine import generate_ai_reply
from services.spam_detection import analyze_spam, detect_sentiment
from services.speech_to_text import transcribe_audio

logger = logging.getLogger(__name__)

router = APIRouter(tags=["WebSocket"])


class CallSession:
    """Manages state for an active call screening session."""

    def __init__(self, call_id: str, websocket: WebSocket):
        self.call_id = call_id
        self.websocket = websocket
        self.status = CallStatus.SCREENING
        self.conversation_history = []
        self.user_joined = False

    async def send_json(self, data: dict):
        await self.websocket.send_json(data)

    async def send_error(self, message: str):
        await self.send_json({"type": "error", "message": message})


# Active sessions
active_sessions: dict[str, CallSession] = {}


@router.websocket("/ws/call/{call_id}")
async def call_screening_websocket(websocket: WebSocket, call_id: str):
    """
    WebSocket endpoint for real-time call screening.

    Message types (client → server):
    - {"type": "audio", "data": "<base64 audio>", "format": "wav"}
    - {"type": "text", "sender": "caller", "text": "..."}
    - {"type": "user_join"}
    - {"type": "user_message", "text": "..."}
    - {"type": "end_call"}
    - {"type": "block_caller"}

    Message types (server → client):
    - {"type": "transcription", "text": "...", "confidence": 0.95}
    - {"type": "ai_reply", "text": "...", "spam_score": 0.1, "risk_level": "low"}
    - {"type": "spam_alert", "score": 0.85, "keywords": [...], "message": "..."}
    - {"type": "sentiment", "sentiment": "...", "is_aggressive": false}
    - {"type": "status", "status": "screening|user_joined|ended"}
    - {"type": "error", "message": "..."}
    """
    await websocket.accept()
    session = CallSession(call_id, websocket)
    active_sessions[call_id] = session

    logger.info(f"WebSocket connected for call: {call_id}")

    # Send initial status
    await session.send_json({
        "type": "status",
        "status": "screening",
        "message": "AI screening in progress...",
        "timestamp": datetime.utcnow().isoformat(),
    })

    try:
        while True:
            data = await websocket.receive_text()
            message = json.loads(data)
            msg_type = message.get("type", "")

            if msg_type == "audio":
                await _handle_audio_message(session, message)

            elif msg_type == "text":
                await _handle_text_message(session, message)

            elif msg_type == "user_join":
                await _handle_user_join(session)

            elif msg_type == "user_message":
                await _handle_user_message(session, message)

            elif msg_type == "end_call":
                await _handle_end_call(session)
                break

            elif msg_type == "block_caller":
                await _handle_block_caller(session)
                break

            else:
                await session.send_error(f"Unknown message type: {msg_type}")

    except WebSocketDisconnect:
        logger.info(f"WebSocket disconnected for call: {call_id}")
    except json.JSONDecodeError:
        logger.error(f"Invalid JSON received for call: {call_id}")
    except Exception as e:
        logger.error(f"WebSocket error for call {call_id}: {e}")
    finally:
        active_sessions.pop(call_id, None)


async def _handle_audio_message(session: CallSession, message: dict):
    """Process incoming audio and generate AI response."""
    import base64

    try:
        audio_b64 = message.get("data", "")
        audio_format = message.get("format", "wav")
        audio_bytes = base64.b64decode(audio_b64)

        # Transcribe
        transcription = await transcribe_audio(audio_bytes, audio_format)

        await session.send_json({
            "type": "transcription",
            "sender": "caller",
            "text": transcription.text,
            "confidence": transcription.confidence,
            "timestamp": datetime.utcnow().isoformat(),
        })

        # Generate AI reply if still screening
        if not session.user_joined:
            from models.schemas import ChatMessage, SenderType
            caller_msg = ChatMessage(sender=SenderType.CALLER, text=transcription.text)
            session.conversation_history.append(caller_msg)

            ai_response = await generate_ai_reply(
                caller_message=transcription.text,
                conversation_history=session.conversation_history,
                call_id=session.call_id,
            )

            ai_msg = ChatMessage(sender=SenderType.AI, text=ai_response.reply_text)
            session.conversation_history.append(ai_msg)

            await session.send_json({
                "type": "ai_reply",
                "text": ai_response.reply_text,
                "spam_score": ai_response.spam_score,
                "risk_level": ai_response.risk_level.value,
                "sentiment": ai_response.sentiment,
                "timestamp": datetime.utcnow().isoformat(),
            })

            # Send spam alert if needed
            if ai_response.should_alert_user:
                await session.send_json({
                    "type": "spam_alert",
                    "score": ai_response.spam_score,
                    "keywords": ai_response.scam_keywords_found,
                    "message": ai_response.alert_message,
                    "risk_level": ai_response.risk_level.value,
                })

    except Exception as e:
        logger.error(f"Audio processing error: {e}")
        await session.send_error(f"Audio processing failed: {str(e)}")


async def _handle_text_message(session: CallSession, message: dict):
    """Process text message from caller (simulated or real)."""
    from models.schemas import ChatMessage

    caller_text = message.get("text", "")
    sender = message.get("sender", "caller")

    caller_msg = ChatMessage(sender=SenderType.CALLER, text=caller_text)
    session.conversation_history.append(caller_msg)

    await session.send_json({
        "type": "transcription",
        "sender": sender,
        "text": caller_text,
        "confidence": 1.0,
        "timestamp": datetime.utcnow().isoformat(),
    })

    # Generate AI reply if still screening
    if not session.user_joined:
        ai_response = await generate_ai_reply(
            caller_message=caller_text,
            conversation_history=session.conversation_history,
            call_id=session.call_id,
        )

        ai_msg = ChatMessage(sender=SenderType.AI, text=ai_response.reply_text)
        session.conversation_history.append(ai_msg)

        await session.send_json({
            "type": "ai_reply",
            "text": ai_response.reply_text,
            "spam_score": ai_response.spam_score,
            "risk_level": ai_response.risk_level.value,
            "sentiment": ai_response.sentiment,
            "timestamp": datetime.utcnow().isoformat(),
        })

        if ai_response.should_alert_user:
            await session.send_json({
                "type": "spam_alert",
                "score": ai_response.spam_score,
                "keywords": ai_response.scam_keywords_found,
                "message": ai_response.alert_message,
                "risk_level": ai_response.risk_level.value,
            })


async def _handle_user_join(session: CallSession):
    """Handle user taking over the call."""
    session.user_joined = True
    session.status = CallStatus.USER_JOINED

    await session.send_json({
        "type": "status",
        "status": "user_joined",
        "message": "User has joined the call. AI screening stopped.",
        "timestamp": datetime.utcnow().isoformat(),
    })

    logger.info(f"User joined call: {session.call_id}")


async def _handle_user_message(session: CallSession, message: dict):
    """Handle message from user after they've joined."""
    from models.schemas import ChatMessage

    user_text = message.get("text", "")
    user_msg = ChatMessage(sender=SenderType.USER, text=user_text)
    session.conversation_history.append(user_msg)

    await session.send_json({
        "type": "transcription",
        "sender": "user",
        "text": user_text,
        "confidence": 1.0,
        "timestamp": datetime.utcnow().isoformat(),
    })


async def _handle_end_call(session: CallSession):
    """Handle call ending."""
    session.status = CallStatus.ENDED

    await session.send_json({
        "type": "status",
        "status": "ended",
        "message": "Call ended.",
        "timestamp": datetime.utcnow().isoformat(),
    })

    logger.info(f"Call ended: {session.call_id}")


async def _handle_block_caller(session: CallSession):
    """Handle caller being blocked."""
    session.status = CallStatus.BLOCKED

    await session.send_json({
        "type": "status",
        "status": "blocked",
        "message": "Caller blocked.",
        "timestamp": datetime.utcnow().isoformat(),
    })

    logger.info(f"Caller blocked for call: {session.call_id}")
