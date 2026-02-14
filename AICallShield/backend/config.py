"""
AICallShield Backend Configuration
"""

import os
from pydantic_settings import BaseSettings
from dotenv import load_dotenv

load_dotenv()


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # App
    APP_NAME: str = "AICallShield Backend"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True

    # Server
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # OpenAI
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    OPENAI_MODEL: str = "gpt-4o-mini"
    WHISPER_MODEL: str = "whisper-1"

    # Google Gemini (alternative to OpenAI)
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")
    GEMINI_MODEL: str = "gemini-1.5-flash"

    # Firebase
    FIREBASE_CREDENTIALS: str = os.getenv("FIREBASE_CREDENTIALS", "serviceAccount.json")
    FIREBASE_STORAGE_BUCKET: str = os.getenv("FIREBASE_STORAGE_BUCKET", "aicallshield.appspot.com")

    # AI Screening
    SCREENING_SYSTEM_PROMPT: str = (
        "You are an AI call screening assistant named AICallShield. "
        "You answer phone calls on behalf of the user. Be polite, professional, "
        "and concise. Ask the caller their name, purpose of the call, and if it's urgent. "
        "If you detect spam or scam patterns, politely end the conversation. "
        "Keep responses under 2 sentences. Never share any personal information about the user."
    )

    # Spam Detection
    SCAM_KEYWORDS: list[str] = [
        "otp", "share otp", "verify otp", "bank account", "credit card",
        "social security", "aadhaar", "pan card", "lottery", "prize",
        "won", "winner", "urgent payment", "transfer money", "gift card",
        "arrest warrant", "irs", "tax fraud", "suspended", "blocked account",
        "insurance claim", "free offer", "limited time", "act now",
        "wire transfer", "bitcoin", "crypto investment", "guaranteed returns",
    ]

    SPAM_THRESHOLD: float = 0.65  # Probability threshold for spam classification

    # Audio
    MAX_AUDIO_SIZE_MB: int = 25
    SUPPORTED_AUDIO_FORMATS: list[str] = ["wav", "mp3", "m4a", "ogg", "webm"]

    # Rate Limiting
    MAX_CALLS_PER_MINUTE: int = 10

    class Config:
        env_file = ".env"
        extra = "allow"


settings = Settings()
