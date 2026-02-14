"""
Spam Detection & Sentiment Analysis Service.

Provides keyword-based and heuristic spam detection,
sentiment analysis, and risk scoring.
"""

import logging
import re
from typing import Optional

from config import settings
from models.schemas import (
    RiskLevel,
    SentimentResponse,
    SpamAnalysisResponse,
)

logger = logging.getLogger(__name__)

# Try importing TextBlob for sentiment analysis
try:
    from textblob import TextBlob
    HAS_TEXTBLOB = True
except ImportError:
    HAS_TEXTBLOB = False
    logger.warning("TextBlob not installed. Sentiment analysis will use fallback.")


# ── Spam Detection ────────────────────────────────────────────────────

def analyze_spam(
    text: str,
    caller_number: Optional[str] = None,
) -> SpamAnalysisResponse:
    """
    Analyze text for spam/scam indicators.

    Uses keyword matching, pattern detection, and heuristics.
    """
    text_lower = text.lower().strip()

    if not text_lower:
        return SpamAnalysisResponse(
            spam_score=0.0,
            risk_level=RiskLevel.LOW,
            scam_keywords_found=[],
            is_spam=False,
            explanation="Empty text provided.",
        )

    # 1. Keyword matching
    found_keywords = []
    keyword_score = 0.0
    for keyword in settings.SCAM_KEYWORDS:
        if keyword.lower() in text_lower:
            found_keywords.append(keyword)
            keyword_score += 0.15  # Each keyword adds 15%

    # 2. Pattern detection
    pattern_score = 0.0
    patterns = [
        (r"\b\d{4,6}\b.*(?:otp|code|verify)", 0.3, "OTP/verification code request"),
        (r"(?:send|share|tell).*(?:otp|password|pin)", 0.35, "Credential request"),
        (r"(?:won|winner|prize|lottery|jackpot)", 0.25, "Prize/lottery scam"),
        (r"(?:urgent|immediately|right now|act fast)", 0.15, "Urgency pressure"),
        (r"(?:bank|account).*(?:blocked|suspended|frozen)", 0.3, "Account threat"),
        (r"(?:arrest|warrant|police|legal action)", 0.3, "Legal threat"),
        (r"(?:click|visit|go to).*(?:link|url|website)", 0.2, "Phishing attempt"),
        (r"(?:transfer|send|wire).*(?:money|cash|amount)", 0.25, "Money transfer request"),
        (r"(?:gift card|itunes|google play|amazon).*(?:buy|purchase|redeem)", 0.3, "Gift card scam"),
        (r"(?:crypto|bitcoin|investment).*(?:guaranteed|returns|profit)", 0.3, "Investment scam"),
    ]

    pattern_explanations = []
    for pattern, score, explanation in patterns:
        if re.search(pattern, text_lower):
            pattern_score += score
            pattern_explanations.append(explanation)

    # 3. Number analysis (if provided)
    number_score = 0.0
    if caller_number:
        # International numbers or very short numbers are suspicious
        if caller_number.startswith("+") and not caller_number.startswith("+91"):
            number_score = 0.1
        if len(caller_number.replace("+", "").replace("-", "")) < 7:
            number_score += 0.1

    # 4. Calculate final score
    raw_score = keyword_score + pattern_score + number_score
    spam_score = min(raw_score, 1.0)  # Cap at 1.0

    # 5. Determine risk level
    if spam_score >= 0.8:
        risk_level = RiskLevel.CRITICAL
    elif spam_score >= 0.6:
        risk_level = RiskLevel.HIGH
    elif spam_score >= 0.35:
        risk_level = RiskLevel.MEDIUM
    else:
        risk_level = RiskLevel.LOW

    # 6. Build explanation
    is_spam = spam_score >= settings.SPAM_THRESHOLD
    explanations = []
    if found_keywords:
        explanations.append(f"Scam keywords detected: {', '.join(found_keywords)}")
    if pattern_explanations:
        explanations.append(f"Suspicious patterns: {', '.join(pattern_explanations)}")
    if number_score > 0:
        explanations.append("Suspicious caller number pattern")
    if not explanations:
        explanations.append("No significant spam indicators found.")

    explanation = " | ".join(explanations)

    logger.info(f"Spam analysis: score={spam_score:.2f}, risk={risk_level.value}, keywords={found_keywords}")

    return SpamAnalysisResponse(
        spam_score=round(spam_score, 3),
        risk_level=risk_level,
        scam_keywords_found=found_keywords,
        is_spam=is_spam,
        explanation=explanation,
    )


# ── Sentiment Analysis ────────────────────────────────────────────────

def detect_sentiment(text: str) -> SentimentResponse:
    """
    Analyze the sentiment of text using TextBlob.

    Returns polarity (-1 to 1), subjectivity (0 to 1), and aggressiveness.
    """
    if not text.strip():
        return SentimentResponse(
            sentiment="neutral",
            polarity=0.0,
            subjectivity=0.0,
            is_aggressive=False,
        )

    if HAS_TEXTBLOB:
        blob = TextBlob(text)
        polarity = blob.sentiment.polarity
        subjectivity = blob.sentiment.subjectivity
    else:
        # Simple fallback
        polarity = 0.0
        subjectivity = 0.5

    # Determine sentiment label
    if polarity > 0.1:
        sentiment = "positive"
    elif polarity < -0.1:
        sentiment = "negative"
    else:
        sentiment = "neutral"

    # Detect aggressive tone
    aggressive_patterns = [
        r"(?:shut up|stupid|idiot|fool|damn|hell|threat)",
        r"(?:kill|hurt|destroy|sue|attack)",
        r"(?:you will pay|regret|consequence)",
        r"[A-Z]{5,}",  # Excessive caps
        r"!{3,}",  # Excessive exclamation
    ]

    is_aggressive = any(re.search(p, text) for p in aggressive_patterns)
    if polarity < -0.5:
        is_aggressive = True

    return SentimentResponse(
        sentiment=sentiment,
        polarity=round(polarity, 3),
        subjectivity=round(subjectivity, 3),
        is_aggressive=is_aggressive,
    )


# ── Risk Score Calculator ─────────────────────────────────────────────

def calculate_risk_score(
    spam_score: float,
    sentiment: SentimentResponse,
    call_duration_seconds: int = 0,
) -> tuple[float, RiskLevel]:
    """
    Calculate overall risk score combining spam and sentiment.
    """
    base_score = spam_score * 0.7  # Spam is 70% of risk

    # Sentiment adjustment
    if sentiment.is_aggressive:
        base_score += 0.15
    if sentiment.polarity < -0.3:
        base_score += 0.1

    # Very short calls that seem urgent are riskier
    if call_duration_seconds < 30 and spam_score > 0.3:
        base_score += 0.05

    final_score = min(base_score, 1.0)

    if final_score >= 0.8:
        risk_level = RiskLevel.CRITICAL
    elif final_score >= 0.6:
        risk_level = RiskLevel.HIGH
    elif final_score >= 0.35:
        risk_level = RiskLevel.MEDIUM
    else:
        risk_level = RiskLevel.LOW

    return round(final_score, 3), risk_level
