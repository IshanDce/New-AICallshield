"""
Text-to-Speech Service.

Primary provider : ElevenLabs
  - VOICE_ASSISTANT  (male)  : IY8nsD2RIP5N4FFQLaT3
  - VOICE_ALERT      (female): 1Z7Y8o9cvUeWq8oLKgMY

Fallback provider : OpenAI TTS (used automatically if ElevenLabs key is absent).

Set TTS_PROVIDER=openai in .env to force the OpenAI fallback.
"""

import logging
from typing import AsyncIterator, Optional

import httpx
from openai import AsyncOpenAI

from config import settings

logger = logging.getLogger(__name__)

# ── Voice constants ────────────────────────────────────────────────────────────
# These are the ElevenLabs Voice IDs chosen for AICallShield.
# Override via ELEVENLABS_VOICE_ASSISTANT / ELEVENLABS_VOICE_ALERT in .env.
VOICE_ASSISTANT_ID: str = settings.ELEVENLABS_VOICE_ASSISTANT or "IY8nsD2RIP5N4FFQLaT3"  # male
VOICE_ALERT_ID: str = settings.ELEVENLABS_VOICE_ALERT or "1Z7Y8o9cvUeWq8oLKgMY"          # female

ELEVENLABS_BASE_URL = "https://api.elevenlabs.io/v1"

# OpenAI fallback client
_openai_client = (
    AsyncOpenAI(api_key=settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else None
)


# ── Internal helpers ───────────────────────────────────────────────────────────

def _elevenlabs_headers() -> dict:
    return {
        "xi-api-key": settings.ELEVENLABS_API_KEY,
        "Content-Type": "application/json",
        "Accept": "audio/mpeg",
    }


async def _elevenlabs_tts(text: str, voice_id: str) -> Optional[bytes]:
    """Call ElevenLabs /text-to-speech and return raw MP3 bytes."""
    url = f"{ELEVENLABS_BASE_URL}/text-to-speech/{voice_id}"
    payload = {
        "text": text,
        "model_id": settings.ELEVENLABS_MODEL,
        "voice_settings": {
            "stability": 0.5,
            "similarity_boost": 0.75,
            "style": 0.0,
            "use_speaker_boost": True,
        },
    }
    try:
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(url, json=payload, headers=_elevenlabs_headers())
            response.raise_for_status()
            logger.info(
                f"ElevenLabs TTS: {len(response.content)} bytes, voice_id={voice_id}"
            )
            return response.content
    except httpx.HTTPStatusError as e:
        logger.error(f"ElevenLabs TTS HTTP error {e.response.status_code}: {e.response.text}")
    except Exception as e:
        logger.error(f"ElevenLabs TTS failed: {e}")
    return None


async def _elevenlabs_stream(text: str, voice_id: str) -> AsyncIterator[bytes]:
    """Stream ElevenLabs TTS audio chunks."""
    url = f"{ELEVENLABS_BASE_URL}/text-to-speech/{voice_id}/stream"
    payload = {
        "text": text,
        "model_id": settings.ELEVENLABS_MODEL,
        "voice_settings": {
            "stability": 0.5,
            "similarity_boost": 0.75,
        },
    }
    try:
        async with httpx.AsyncClient(timeout=60) as client:
            async with client.stream(
                "POST", url, json=payload, headers=_elevenlabs_headers()
            ) as response:
                response.raise_for_status()
                async for chunk in response.aiter_bytes(chunk_size=4096):
                    yield chunk
    except Exception as e:
        logger.error(f"ElevenLabs TTS stream failed: {e}")


async def _openai_tts(text: str, voice: str = "alloy") -> Optional[bytes]:
    """OpenAI TTS fallback."""
    if not _openai_client:
        logger.warning("OpenAI client not initialised. TTS unavailable.")
        return None
    try:
        response = await _openai_client.audio.speech.create(
            model="tts-1",
            voice=voice,
            input=text,
            response_format="mp3",
        )
        audio_bytes = b""
        async for chunk in response.iter_bytes():
            audio_bytes += chunk
        logger.info(f"OpenAI TTS: {len(audio_bytes)} bytes, voice={voice}")
        return audio_bytes
    except Exception as e:
        logger.error(f"OpenAI TTS failed: {e}")
        return None


# ── Public API ─────────────────────────────────────────────────────────────────

async def text_to_speech(
    text: str,
    voice_role: str = "assistant",
) -> Optional[bytes]:
    """
    Convert text to speech and return MP3 bytes.

    Args:
        text:       Text to speak.
        voice_role: ``"assistant"`` → male screening voice (IY8nsD2RIP5N4FFQLaT3)
                    ``"alert"``     → female alert voice   (1Z7Y8o9cvUeWq8oLKgMY)

    Returns:
        MP3 audio bytes, or None on failure.
    """
    use_elevenlabs = (
        settings.TTS_PROVIDER.lower() == "elevenlabs"
        and bool(settings.ELEVENLABS_API_KEY)
    )

    if use_elevenlabs:
        voice_id = VOICE_ALERT_ID if voice_role == "alert" else VOICE_ASSISTANT_ID
        audio = await _elevenlabs_tts(text, voice_id)
        if audio:
            return audio
        logger.warning("ElevenLabs TTS failed — falling back to OpenAI TTS")

    # OpenAI fallback
    openai_voice = "nova" if voice_role == "alert" else "onyx"
    return await _openai_tts(text, openai_voice)


async def text_to_speech_stream(
    text: str,
    voice_role: str = "assistant",
):
    """
    Stream TTS audio chunks for real-time playback.

    Args:
        text:       Text to speak.
        voice_role: ``"assistant"`` (male) or ``"alert"`` (female).
    """
    use_elevenlabs = (
        settings.TTS_PROVIDER.lower() == "elevenlabs"
        and bool(settings.ELEVENLABS_API_KEY)
    )

    if use_elevenlabs:
        voice_id = VOICE_ALERT_ID if voice_role == "alert" else VOICE_ASSISTANT_ID
        async for chunk in _elevenlabs_stream(text, voice_id):
            yield chunk
        return

    # OpenAI PCM stream fallback
    if not _openai_client:
        return
    try:
        openai_voice = "nova" if voice_role == "alert" else "onyx"
        response = await _openai_client.audio.speech.create(
            model="tts-1",
            voice=openai_voice,
            input=text,
            response_format="pcm",
        )
        async for chunk in response.iter_bytes(chunk_size=4096):
            yield chunk
    except Exception as e:
        logger.error(f"OpenAI TTS stream failed: {e}")
