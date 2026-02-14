"""
AI Processing API Routes.

Handles AI reply generation, call summaries, spam analysis,
and sentiment analysis.
"""

import logging
from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from fastapi.responses import StreamingResponse

from models.schemas import (
    AIReplyRequest,
    AIReplyResponse,
    CallSummaryRequest,
    CallSummaryResponse,
    SpamAnalysisRequest,
    SpamAnalysisResponse,
    SentimentResponse,
    TTSRequest,
    TranscriptionResponse,
)
from services.ai_engine import generate_ai_reply, generate_call_summary
from services.spam_detection import analyze_spam, detect_sentiment
from services.speech_to_text import transcribe_audio
from services.text_to_speech import text_to_speech
from config import settings

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/ai", tags=["AI Processing"])


# ── AI Reply Generation ──────────────────────────────────────────────

@router.post("/reply", response_model=AIReplyResponse)
async def get_ai_reply(request: AIReplyRequest):
    """
    Generate an AI screening reply for a caller's message.

    Also returns spam analysis and sentiment data.
    """
    try:
        result = await generate_ai_reply(
            caller_message=request.caller_message,
            conversation_history=request.conversation_history,
            call_id=request.call_id,
        )
        return result
    except Exception as e:
        logger.error(f"AI reply failed: {e}")
        raise HTTPException(status_code=500, detail=f"AI reply generation failed: {str(e)}")


# ── Speech-to-Text ───────────────────────────────────────────────────

@router.post("/transcribe", response_model=TranscriptionResponse)
async def transcribe(
    audio: UploadFile = File(...),
    language: str = Form(default=None),
):
    """
    Transcribe audio file to text using Whisper.

    Supported formats: wav, mp3, m4a, ogg, webm
    """
    # Validate file size
    content = await audio.read()
    max_size = settings.MAX_AUDIO_SIZE_MB * 1024 * 1024
    if len(content) > max_size:
        raise HTTPException(
            status_code=413,
            detail=f"Audio file too large. Max size: {settings.MAX_AUDIO_SIZE_MB}MB",
        )

    # Validate format
    file_ext = audio.filename.rsplit(".", 1)[-1].lower() if audio.filename else "wav"
    if file_ext not in settings.SUPPORTED_AUDIO_FORMATS:
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported format: {file_ext}. Supported: {settings.SUPPORTED_AUDIO_FORMATS}",
        )

    try:
        result = await transcribe_audio(content, file_ext, language)
        return result
    except Exception as e:
        logger.error(f"Transcription failed: {e}")
        raise HTTPException(status_code=500, detail=f"Transcription failed: {str(e)}")


# ── Text-to-Speech ───────────────────────────────────────────────────

@router.post("/tts")
async def generate_tts(request: TTSRequest):
    """
    Convert text to speech audio.

    Returns audio file (MP3) for playback.
    """
    audio_bytes = await text_to_speech(
        text=request.text,
        voice=request.voice,
        speed=request.speed,
    )

    if audio_bytes is None:
        raise HTTPException(
            status_code=503,
            detail="TTS service unavailable. Set OPENAI_API_KEY.",
        )

    return StreamingResponse(
        iter([audio_bytes]),
        media_type="audio/mpeg",
        headers={"Content-Disposition": "attachment; filename=response.mp3"},
    )


# ── Call Summary ─────────────────────────────────────────────────────

@router.post("/summary", response_model=CallSummaryResponse)
async def get_call_summary(request: CallSummaryRequest):
    """
    Generate an AI summary for a completed call.
    """
    try:
        result = await generate_call_summary(
            transcript=request.transcript,
            call_id=request.call_id,
        )
        return result
    except Exception as e:
        logger.error(f"Summary generation failed: {e}")
        raise HTTPException(status_code=500, detail=f"Summary generation failed: {str(e)}")


# ── Spam Analysis ────────────────────────────────────────────────────

@router.post("/spam-check", response_model=SpamAnalysisResponse)
async def check_spam(request: SpamAnalysisRequest):
    """
    Analyze text for spam/scam indicators.
    """
    result = analyze_spam(request.text, request.caller_number)
    return result


# ── Sentiment Analysis ──────────────────────────────────────────────

@router.post("/sentiment", response_model=SentimentResponse)
async def check_sentiment(text: str = Form(...)):
    """
    Analyze sentiment of text.
    """
    result = detect_sentiment(text)
    return result
