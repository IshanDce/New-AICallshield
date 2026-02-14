"""
Text-to-Speech Service using OpenAI TTS API.
"""

import io
import logging
from typing import Optional

from openai import AsyncOpenAI

from config import settings

logger = logging.getLogger(__name__)

client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else None

# Available voices
VOICES = ["alloy", "echo", "fable", "onyx", "nova", "shimmer"]


async def text_to_speech(
    text: str,
    voice: str = "alloy",
    speed: float = 1.0,
    response_format: str = "mp3",
) -> Optional[bytes]:
    """
    Convert text to speech audio using OpenAI TTS API.

    Args:
        text: Text to convert to speech
        voice: Voice to use (alloy, echo, fable, onyx, nova, shimmer)
        speed: Speech speed (0.25 to 4.0)
        response_format: Audio format (mp3, opus, aac, flac, wav, pcm)

    Returns:
        Audio bytes or None if unavailable
    """
    if not client:
        logger.warning("OpenAI client not initialized. TTS unavailable.")
        return None

    if voice not in VOICES:
        voice = "alloy"

    speed = max(0.25, min(4.0, speed))

    try:
        response = await client.audio.speech.create(
            model="tts-1",
            voice=voice,
            input=text,
            speed=speed,
            response_format=response_format,
        )

        # Read the audio bytes
        audio_bytes = b""
        async for chunk in response.iter_bytes():
            audio_bytes += chunk

        logger.info(f"TTS generated: {len(audio_bytes)} bytes, voice={voice}")
        return audio_bytes

    except Exception as e:
        logger.error(f"TTS generation failed: {e}")
        return None


async def text_to_speech_stream(
    text: str,
    voice: str = "alloy",
):
    """
    Stream TTS audio chunks for real-time playback.
    """
    if not client:
        return

    try:
        response = await client.audio.speech.create(
            model="tts-1",
            voice=voice,
            input=text,
            response_format="pcm",  # Raw PCM for streaming
        )

        async for chunk in response.iter_bytes(chunk_size=4096):
            yield chunk

    except Exception as e:
        logger.error(f"TTS streaming failed: {e}")
