"""
Pydantic models/schemas for request and response objects.
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


# ── Enums ──────────────────────────────────────────────────────────────

class SenderType(str, Enum):
    CALLER = "caller"
    AI = "ai"
    USER = "user"
    SYSTEM = "system"


class CallStatus(str, Enum):
    SCREENING = "screening"
    USER_JOINED = "user_joined"
    ENDED = "ended"
    BLOCKED = "blocked"


class RiskLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


# ── Chat Message ───────────────────────────────────────────────────────

class ChatMessage(BaseModel):
    """A single message in the call conversation."""
    id: Optional[str] = None
    sender: SenderType
    text: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    audio_url: Optional[str] = None
    confidence: Optional[float] = None  # STT confidence


class ChatMessageCreate(BaseModel):
    sender: SenderType
    text: str
    audio_url: Optional[str] = None


# ── Call Record ────────────────────────────────────────────────────────

class CallRecord(BaseModel):
    """Complete call record stored in database."""
    id: Optional[str] = None
    caller_number: str
    caller_name: Optional[str] = None
    status: CallStatus = CallStatus.SCREENING
    start_time: datetime = Field(default_factory=datetime.utcnow)
    end_time: Optional[datetime] = None
    duration_seconds: Optional[int] = None
    transcript: list[ChatMessage] = []
    ai_summary: Optional[str] = None
    spam_score: float = 0.0
    risk_level: RiskLevel = RiskLevel.LOW
    scam_keywords_found: list[str] = []
    sentiment: Optional[str] = None
    recording_url: Optional[str] = None
    is_blocked: bool = False
    user_id: Optional[str] = None


class CallRecordCreate(BaseModel):
    caller_number: str
    caller_name: Optional[str] = None
    user_id: Optional[str] = None


class CallRecordUpdate(BaseModel):
    status: Optional[CallStatus] = None
    end_time: Optional[datetime] = None
    ai_summary: Optional[str] = None
    spam_score: Optional[float] = None
    risk_level: Optional[RiskLevel] = None
    is_blocked: Optional[bool] = None


# ── Speech Processing ─────────────────────────────────────────────────

class TranscriptionRequest(BaseModel):
    """Request for speech-to-text processing."""
    call_id: str
    audio_format: str = "wav"


class TranscriptionResponse(BaseModel):
    text: str
    confidence: float
    language: str = "en"
    duration_seconds: float = 0.0


# ── AI Processing ─────────────────────────────────────────────────────

class AIReplyRequest(BaseModel):
    """Request for AI to generate a reply."""
    call_id: str
    caller_message: str
    conversation_history: list[ChatMessage] = []


class AIReplyResponse(BaseModel):
    reply_text: str
    spam_score: float = 0.0
    risk_level: RiskLevel = RiskLevel.LOW
    scam_keywords_found: list[str] = []
    sentiment: str = "neutral"
    should_alert_user: bool = False
    alert_message: Optional[str] = None


# ── Spam Analysis ─────────────────────────────────────────────────────

class SpamAnalysisRequest(BaseModel):
    text: str
    caller_number: Optional[str] = None


class SpamAnalysisResponse(BaseModel):
    spam_score: float
    risk_level: RiskLevel
    scam_keywords_found: list[str]
    is_spam: bool
    explanation: str


# ── Sentiment Analysis ────────────────────────────────────────────────

class SentimentResponse(BaseModel):
    sentiment: str  # positive, negative, neutral
    polarity: float  # -1.0 to 1.0
    subjectivity: float  # 0.0 to 1.0
    is_aggressive: bool = False


# ── Call Summary ──────────────────────────────────────────────────────

class CallSummaryRequest(BaseModel):
    call_id: str
    transcript: list[ChatMessage]


class CallSummaryResponse(BaseModel):
    summary: str
    key_points: list[str]
    caller_intent: str
    recommended_action: str  # callback, block, ignore
    spam_score: float
    risk_level: RiskLevel


# ── TTS ───────────────────────────────────────────────────────────────

class TTSRequest(BaseModel):
    text: str
    voice_role: str = "assistant"  # "assistant" = male (IY8nsD2RIP5N4FFQLaT3)
                                   # "alert"     = female (1Z7Y8o9cvUeWq8oLKgMY)


# ── API Responses ─────────────────────────────────────────────────────

class HealthResponse(BaseModel):
    status: str = "healthy"
    version: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class ErrorResponse(BaseModel):
    error: str
    detail: Optional[str] = None
    status_code: int = 500
