"""
Local storage client for AICallShield.

Stores call records and audio recordings on the local filesystem.
"""

import json
import logging
import shutil
from copy import deepcopy
from datetime import datetime
from enum import Enum
from pathlib import Path
from threading import Lock
from typing import Optional
from uuid import uuid4

from config import settings

logger = logging.getLogger(__name__)


def _resolve_data_dir() -> Path:
    data_dir = Path(settings.LOCAL_STORAGE_DIR)
    if data_dir.is_absolute():
        return data_dir
    # Resolve relative paths from backend root.
    return Path(__file__).resolve().parents[1] / data_dir


_DATA_DIR = _resolve_data_dir()
_CALLS_DIR = _DATA_DIR / "calls"
_RECORDINGS_DIR = _DATA_DIR / "recordings"
_LOCK = Lock()

_CALLS_DIR.mkdir(parents=True, exist_ok=True)
_RECORDINGS_DIR.mkdir(parents=True, exist_ok=True)


def _safe_segment(value: str) -> str:
    cleaned = "".join(ch if ch.isalnum() or ch in ("-", "_") else "_" for ch in value)
    return cleaned or "default_user"


def _user_calls_file(user_id: str) -> Path:
    return _CALLS_DIR / f"{_safe_segment(user_id)}.json"


def _json_default(value):
    if isinstance(value, datetime):
        return value.isoformat()
    if isinstance(value, Enum):
        return value.value
    return str(value)


def _to_json_compatible(data: dict) -> dict:
    return json.loads(json.dumps(data, default=_json_default))


def _read_user_calls(user_id: str) -> dict[str, dict]:
    file_path = _user_calls_file(user_id)
    if not file_path.exists():
        return {}

    try:
        parsed = json.loads(file_path.read_text(encoding="utf-8"))
        if isinstance(parsed, dict):
            return {
                str(key): value
                for key, value in parsed.items()
                if isinstance(value, dict)
            }
        logger.warning("Unexpected calls format in %s. Reinitializing file.", file_path)
    except json.JSONDecodeError:
        logger.warning("Corrupted calls file %s. Reinitializing file.", file_path)
    except Exception as exc:
        logger.error("Failed reading calls file %s: %s", file_path, exc)

    return {}


def _write_user_calls(user_id: str, calls: dict[str, dict]) -> None:
    file_path = _user_calls_file(user_id)
    temp_path = file_path.with_suffix(".json.tmp")
    temp_path.write_text(
        json.dumps(calls, ensure_ascii=True, indent=2),
        encoding="utf-8",
    )
    temp_path.replace(file_path)


def _generate_call_id() -> str:
    return f"call_{datetime.utcnow().strftime('%Y%m%d_%H%M%S')}_{uuid4().hex[:8]}"


def _content_type_to_suffix(content_type: str) -> str:
    normalized = content_type.split(";", 1)[0].strip().lower()
    mapping = {
        "audio/wav": ".wav",
        "audio/x-wav": ".wav",
        "audio/mpeg": ".mp3",
        "audio/mp3": ".mp3",
        "audio/ogg": ".ogg",
        "audio/webm": ".webm",
        "audio/mp4": ".m4a",
        "audio/x-m4a": ".m4a",
    }
    return mapping.get(normalized, ".wav")


async def create_call_record(user_id: str, call_data: dict) -> str:
    """Create a new call record. Returns the call ID."""
    call_id = _generate_call_id()
    record = _to_json_compatible(dict(call_data))
    record["id"] = call_id
    record["created_at"] = datetime.utcnow().isoformat()

    with _LOCK:
        user_calls = _read_user_calls(user_id)
        user_calls[call_id] = record
        _write_user_calls(user_id, user_calls)

    logger.info("Call record created in local storage: %s", call_id)
    return call_id


async def get_call_record(user_id: str, call_id: str) -> Optional[dict]:
    """Retrieve a call record by ID."""
    with _LOCK:
        call = _read_user_calls(user_id).get(call_id)
    return deepcopy(call) if call else None


async def update_call_record(user_id: str, call_id: str, updates: dict) -> bool:
    """Update an existing call record."""
    normalized_updates = _to_json_compatible(dict(updates))
    normalized_updates["updated_at"] = datetime.utcnow().isoformat()

    with _LOCK:
        user_calls = _read_user_calls(user_id)
        if call_id not in user_calls:
            return False
        user_calls[call_id].update(normalized_updates)
        _write_user_calls(user_id, user_calls)

    logger.info("Call record updated in local storage: %s", call_id)
    return True


async def get_call_history(
    user_id: str,
    limit: int = 50,
    offset: int = 0,
) -> list[dict]:
    """Get call history for a user, ordered by most recent."""
    with _LOCK:
        calls = list(_read_user_calls(user_id).values())

    calls.sort(key=lambda item: item.get("created_at", ""), reverse=True)
    return [deepcopy(call) for call in calls[offset:offset + limit]]


async def delete_call_record(user_id: str, call_id: str) -> bool:
    """Delete a call record and local recording files if they exist."""
    with _LOCK:
        user_calls = _read_user_calls(user_id)
        if call_id not in user_calls:
            return False
        user_calls.pop(call_id, None)
        _write_user_calls(user_id, user_calls)

    recording_dir = _RECORDINGS_DIR / _safe_segment(user_id) / _safe_segment(call_id)
    if recording_dir.exists():
        shutil.rmtree(recording_dir, ignore_errors=True)

    logger.info("Call record deleted from local storage: %s", call_id)
    return True


async def search_calls(user_id: str, query: str) -> list[dict]:
    """Search call records by transcript, summary, and caller number."""
    all_calls = await get_call_history(user_id, limit=200)

    results = []
    query_lower = query.lower()

    for call in all_calls:
        transcript = call.get("transcript", [])
        if isinstance(transcript, list):
            for msg in transcript:
                if isinstance(msg, dict) and query_lower in str(msg.get("text", "")).lower():
                    results.append(call)
                    break

        summary = str(call.get("ai_summary", ""))
        if query_lower in summary.lower() and call not in results:
            results.append(call)

        number = str(call.get("caller_number", ""))
        if query_lower in number and call not in results:
            results.append(call)

    return results


async def upload_audio(
    user_id: str,
    call_id: str,
    audio_data: bytes,
    content_type: str = "audio/wav",
) -> Optional[str]:
    """Save audio recording in local storage and return a relative file path."""
    file_ext = _content_type_to_suffix(content_type)
    recording_dir = _RECORDINGS_DIR / _safe_segment(user_id) / _safe_segment(call_id)
    recording_dir.mkdir(parents=True, exist_ok=True)

    audio_path = recording_dir / f"recording{file_ext}"

    try:
        audio_path.write_bytes(audio_data)
        relative_path = audio_path.relative_to(_DATA_DIR).as_posix()
        logger.info("Audio saved in local storage: %s", relative_path)
        return relative_path
    except Exception as exc:
        logger.error("Audio save failed: %s", exc)
        return None
