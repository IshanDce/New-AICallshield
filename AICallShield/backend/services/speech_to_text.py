"""
Speech-to-Text Service using OpenAI Whisper API.
"""

import io
import logging
from typing import Optional

from openai import AsyncOpenAI

from config import settings
from models.schemas import TranscriptionResponse

logger = logging.getLogger(__name__)

# Initialize OpenAI client
client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else None


async def transcribe_audio(
    audio_data: bytes,
    audio_format: str = "wav",
    language: Optional[str] = None,
) -> TranscriptionResponse:
    """
    Convert speech audio to text using OpenAI Whisper API.

    Args:
        audio_data: Raw audio bytes
        audio_format: Audio format (wav, mp3, m4a, etc.)
        language: Optional language code (e.g., 'en', 'hi')

    Returns:
        TranscriptionResponse with text and metadata
    """
    if not client:
        logger.warning("OpenAI client not initialized. Using mock transcription.")
        return TranscriptionResponse(
            text="[Mock transcription - Set OPENAI_API_KEY to enable]",
            confidence=0.0,
            language=language or "en",
        )

    try:
        # Prepare audio file for API
        audio_file = io.BytesIO(audio_data)
        audio_file.name = f"audio.{audio_format}"

        # Call Whisper API
        kwargs = {
            "model": settings.WHISPER_MODEL,
            "file": audio_file,
            "response_format": "verbose_json",
        }
        if language:
            kwargs["language"] = language

        transcript = await client.audio.transcriptions.create(**kwargs)

        # Extract results
        text = transcript.text if hasattr(transcript, "text") else str(transcript)
        duration = transcript.duration if hasattr(transcript, "duration") else 0.0
        detected_lang = transcript.language if hasattr(transcript, "language") else (language or "en")

        logger.info(f"Transcription complete: {len(text)} chars, lang={detected_lang}")

        return TranscriptionResponse(
            text=text,
            confidence=0.95,  # Whisper doesn't return confidence per-segment easily
            language=detected_lang,
            duration_seconds=duration,
        )

    except Exception as e:
        logger.error(f"Transcription failed: {e}")
        raise RuntimeError(f"Speech-to-text failed: {str(e)}")


async def transcribe_audio_stream(audio_chunks: list[bytes], audio_format: str = "wav") -> TranscriptionResponse:
    """
    Transcribe a stream of audio chunks by concatenating them.

    For real-time streaming, consider using WebSocket-based approach.
    """
    combined_audio = b"".join(audio_chunks)
    return await transcribe_audio(combined_audio, audio_format)
