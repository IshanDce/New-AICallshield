"""
Firebase Database Client for AICallShield.

Handles Firestore operations for call records and user data.
"""

import logging
from datetime import datetime
from typing import Optional

from config import settings

logger = logging.getLogger(__name__)

# Firebase initialization
_firebase_initialized = False
_db = None
_storage = None


def _init_firebase():
    """Initialize Firebase Admin SDK."""
    global _firebase_initialized, _db, _storage

    if _firebase_initialized:
        return

    try:
        import firebase_admin
        from firebase_admin import credentials, firestore, storage

        cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS)
        firebase_admin.initialize_app(cred, {
            "storageBucket": settings.FIREBASE_STORAGE_BUCKET,
        })

        _db = firestore.client()
        _storage = storage.bucket()
        _firebase_initialized = True
        logger.info("Firebase initialized successfully.")

    except FileNotFoundError:
        logger.warning(
            f"Firebase credentials file not found: {settings.FIREBASE_CREDENTIALS}. "
            "Using in-memory storage instead."
        )
    except Exception as e:
        logger.warning(f"Firebase initialization failed: {e}. Using in-memory storage.")


# ── In-Memory Fallback Storage ────────────────────────────────────────

_memory_calls: dict[str, dict] = {}
_call_counter = 0


# ── Call Record Operations ────────────────────────────────────────────

async def create_call_record(user_id: str, call_data: dict) -> str:
    """Create a new call record. Returns the call ID."""
    global _call_counter

    _init_firebase()

    call_id = f"call_{datetime.utcnow().strftime('%Y%m%d_%H%M%S')}_{_call_counter}"
    _call_counter += 1

    call_data["id"] = call_id
    call_data["created_at"] = datetime.utcnow().isoformat()

    if _db:
        try:
            doc_ref = _db.collection("users").document(user_id).collection("calls").document(call_id)
            doc_ref.set(call_data)
            logger.info(f"Call record created in Firestore: {call_id}")
        except Exception as e:
            logger.error(f"Firestore write failed: {e}")
            _memory_calls[call_id] = call_data
    else:
        _memory_calls[call_id] = call_data
        logger.info(f"Call record created in memory: {call_id}")

    return call_id


async def get_call_record(user_id: str, call_id: str) -> Optional[dict]:
    """Retrieve a call record by ID."""
    _init_firebase()

    if _db:
        try:
            doc = _db.collection("users").document(user_id).collection("calls").document(call_id).get()
            if doc.exists:
                return doc.to_dict()
        except Exception as e:
            logger.error(f"Firestore read failed: {e}")

    return _memory_calls.get(call_id)


async def update_call_record(user_id: str, call_id: str, updates: dict) -> bool:
    """Update an existing call record."""
    _init_firebase()

    updates["updated_at"] = datetime.utcnow().isoformat()

    if _db:
        try:
            doc_ref = _db.collection("users").document(user_id).collection("calls").document(call_id)
            doc_ref.update(updates)
            logger.info(f"Call record updated in Firestore: {call_id}")
            return True
        except Exception as e:
            logger.error(f"Firestore update failed: {e}")

    if call_id in _memory_calls:
        _memory_calls[call_id].update(updates)
        return True

    return False


async def get_call_history(
    user_id: str,
    limit: int = 50,
    offset: int = 0,
) -> list[dict]:
    """Get call history for a user, ordered by most recent."""
    _init_firebase()

    if _db:
        try:
            query = (
                _db.collection("users")
                .document(user_id)
                .collection("calls")
                .order_by("created_at", direction="DESCENDING")
                .limit(limit)
                .offset(offset)
            )
            docs = query.stream()
            return [doc.to_dict() for doc in docs]
        except Exception as e:
            logger.error(f"Firestore query failed: {e}")

    # Fallback to memory
    calls = list(_memory_calls.values())
    calls.sort(key=lambda x: x.get("created_at", ""), reverse=True)
    return calls[offset:offset + limit]


async def delete_call_record(user_id: str, call_id: str) -> bool:
    """Delete a call record."""
    _init_firebase()

    if _db:
        try:
            _db.collection("users").document(user_id).collection("calls").document(call_id).delete()
            return True
        except Exception as e:
            logger.error(f"Firestore delete failed: {e}")

    if call_id in _memory_calls:
        del _memory_calls[call_id]
        return True

    return False


async def search_calls(user_id: str, query: str) -> list[dict]:
    """
    Search call records by transcript content.

    Note: For production, use Algolia or Elasticsearch for full-text search.
    """
    all_calls = await get_call_history(user_id, limit=200)

    results = []
    query_lower = query.lower()

    for call in all_calls:
        # Search in transcript
        transcript = call.get("transcript", [])
        if isinstance(transcript, list):
            for msg in transcript:
                if isinstance(msg, dict) and query_lower in msg.get("text", "").lower():
                    results.append(call)
                    break

        # Search in summary
        summary = call.get("ai_summary", "")
        if query_lower in summary.lower():
            if call not in results:
                results.append(call)

        # Search in caller number
        number = call.get("caller_number", "")
        if query_lower in number:
            if call not in results:
                results.append(call)

    return results


# ── Audio Storage ─────────────────────────────────────────────────────

async def upload_audio(user_id: str, call_id: str, audio_data: bytes, content_type: str = "audio/wav") -> Optional[str]:
    """Upload audio recording to Firebase Storage."""
    _init_firebase()

    if not _storage:
        logger.warning("Firebase Storage not available. Audio not uploaded.")
        return None

    try:
        blob_path = f"recordings/{user_id}/{call_id}/recording.wav"
        blob = _storage.blob(blob_path)
        blob.upload_from_string(audio_data, content_type=content_type)
        blob.make_public()
        url = blob.public_url
        logger.info(f"Audio uploaded: {url}")
        return url
    except Exception as e:
        logger.error(f"Audio upload failed: {e}")
        return None
