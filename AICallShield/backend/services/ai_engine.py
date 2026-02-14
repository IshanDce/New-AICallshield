"""
AI Response Generation Service using OpenAI GPT or Google Gemini.
"""

import logging
from typing import Optional

from openai import AsyncOpenAI

from config import settings
from models.schemas import (
    AIReplyResponse,
    CallSummaryResponse,
    ChatMessage,
    RiskLevel,
    SenderType,
)
from services.spam_detection import analyze_spam, detect_sentiment

logger = logging.getLogger(__name__)

# Initialize OpenAI client
openai_client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else None


def _build_conversation_messages(
    conversation_history: list[ChatMessage],
    new_caller_message: str,
) -> list[dict]:
    """Build the message list for the LLM API."""
    messages = [{"role": "system", "content": settings.SCREENING_SYSTEM_PROMPT}]

    for msg in conversation_history[-10:]:  # Last 10 messages for context window
        if msg.sender == SenderType.CALLER:
            messages.append({"role": "user", "content": f"[Caller]: {msg.text}"})
        elif msg.sender == SenderType.AI:
            messages.append({"role": "assistant", "content": msg.text})
        elif msg.sender == SenderType.USER:
            messages.append({"role": "user", "content": f"[User joined]: {msg.text}"})

    messages.append({"role": "user", "content": f"[Caller]: {new_caller_message}"})
    return messages


async def generate_ai_reply(
    caller_message: str,
    conversation_history: list[ChatMessage] = [],
    call_id: Optional[str] = None,
) -> AIReplyResponse:
    """
    Generate an AI screening reply to the caller's message.

    Also performs spam analysis and sentiment detection.
    """
    # Run spam analysis
    spam_result = analyze_spam(caller_message)
    sentiment_result = detect_sentiment(caller_message)

    # Determine if user should be alerted
    should_alert = spam_result.is_spam or sentiment_result.is_aggressive
    alert_message = None
    if spam_result.is_spam:
        keywords_str = ", ".join(spam_result.scam_keywords_found)
        alert_message = f"⚠ Scam probability: {spam_result.spam_score * 100:.0f}% — Keywords: {keywords_str}"
    elif sentiment_result.is_aggressive:
        alert_message = "⚠ Caller appears aggressive or threatening"

    # Generate AI reply
    reply_text = await _generate_reply_text(caller_message, conversation_history, spam_result.is_spam)

    return AIReplyResponse(
        reply_text=reply_text,
        spam_score=spam_result.spam_score,
        risk_level=spam_result.risk_level,
        scam_keywords_found=spam_result.scam_keywords_found,
        sentiment=sentiment_result.sentiment,
        should_alert_user=should_alert,
        alert_message=alert_message,
    )


async def _generate_reply_text(
    caller_message: str,
    conversation_history: list[ChatMessage],
    is_spam: bool,
) -> str:
    """Generate the actual reply text using LLM."""
    if not openai_client:
        # Mock response when API key is not set
        if is_spam:
            return "I appreciate your call, but I'm not able to share any personal information or make any transactions. Is there something else I can help with?"
        if not conversation_history:
            return "Hello! This is an AI assistant screening calls. May I know who's calling and the purpose of your call?"
        return "Thank you for that information. Let me check if the person you're trying to reach is available. Is there anything else you'd like me to pass along?"

    try:
        messages = _build_conversation_messages(conversation_history, caller_message)

        # If spam detected, add extra instruction
        if is_spam:
            messages.append({
                "role": "system",
                "content": (
                    "WARNING: This message contains potential scam/spam indicators. "
                    "Do NOT share any personal information. Politely deflect and suggest "
                    "the caller contact through official channels."
                ),
            })

        response = await openai_client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=messages,
            max_tokens=150,
            temperature=0.7,
        )

        return response.choices[0].message.content.strip()

    except Exception as e:
        logger.error(f"AI reply generation failed: {e}")
        return "I apologize, I'm having trouble processing right now. Could you please call back in a moment?"


async def generate_call_summary(
    transcript: list[ChatMessage],
    call_id: Optional[str] = None,
) -> CallSummaryResponse:
    """
    Generate a post-call AI summary analyzing the entire conversation.
    """
    if not transcript:
        return CallSummaryResponse(
            summary="No conversation recorded.",
            key_points=[],
            caller_intent="unknown",
            recommended_action="ignore",
            spam_score=0.0,
            risk_level=RiskLevel.LOW,
        )

    # Build transcript text
    transcript_text = "\n".join(
        f"[{msg.sender.value}] {msg.text}" for msg in transcript
    )

    # Combine all caller messages for spam analysis
    caller_texts = " ".join(msg.text for msg in transcript if msg.sender == SenderType.CALLER)
    spam_result = analyze_spam(caller_texts)

    if not openai_client:
        # Mock summary
        return CallSummaryResponse(
            summary=f"Call with {len(transcript)} messages. {'Potential spam detected.' if spam_result.is_spam else 'Appears legitimate.'}",
            key_points=["AI screened the call", f"Total messages: {len(transcript)}"],
            caller_intent="unknown" if spam_result.is_spam else "inquiry",
            recommended_action="block" if spam_result.is_spam else "callback",
            spam_score=spam_result.spam_score,
            risk_level=spam_result.risk_level,
        )

    try:
        response = await openai_client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are analyzing a phone call transcript. Provide:\n"
                        "1. A brief summary (2-3 sentences)\n"
                        "2. Key points (bullet list)\n"
                        "3. Caller's intent (one word: inquiry, complaint, spam, scam, sales, personal, emergency, unknown)\n"
                        "4. Recommended action (callback, block, ignore)\n"
                        "Respond in JSON format with keys: summary, key_points, caller_intent, recommended_action"
                    ),
                },
                {"role": "user", "content": f"Transcript:\n{transcript_text}"},
            ],
            max_tokens=500,
            temperature=0.3,
            response_format={"type": "json_object"},
        )

        import json
        result = json.loads(response.choices[0].message.content)

        return CallSummaryResponse(
            summary=result.get("summary", "Summary unavailable."),
            key_points=result.get("key_points", []),
            caller_intent=result.get("caller_intent", "unknown"),
            recommended_action=result.get("recommended_action", "ignore"),
            spam_score=spam_result.spam_score,
            risk_level=spam_result.risk_level,
        )

    except Exception as e:
        logger.error(f"Summary generation failed: {e}")
        return CallSummaryResponse(
            summary="Summary generation failed.",
            key_points=[],
            caller_intent="unknown",
            recommended_action="ignore",
            spam_score=spam_result.spam_score,
            risk_level=spam_result.risk_level,
        )
