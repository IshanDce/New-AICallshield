package com.aicallshield.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents who sent a chat message.
 */
enum class SenderType {
    @SerializedName("caller") CALLER,
    @SerializedName("ai") AI,
    @SerializedName("user") USER,
    @SerializedName("system") SYSTEM
}

/**
 * Current status of a call.
 */
enum class CallStatus {
    @SerializedName("screening") SCREENING,
    @SerializedName("user_joined") USER_JOINED,
    @SerializedName("ended") ENDED,
    @SerializedName("blocked") BLOCKED
}

/**
 * Risk level classification.
 */
enum class RiskLevel {
    @SerializedName("low") LOW,
    @SerializedName("medium") MEDIUM,
    @SerializedName("high") HIGH,
    @SerializedName("critical") CRITICAL
}

/**
 * A single chat message in the call conversation.
 */
data class ChatMessage(
    val id: String = "",
    val sender: SenderType = SenderType.SYSTEM,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val audioUrl: String? = null,
    val confidence: Float? = null,
    val spamScore: Float? = null,
    val riskLevel: RiskLevel? = null,
    val isAlert: Boolean = false,
    val alertMessage: String? = null
)

/**
 * Complete call record.
 */
data class CallRecord(
    val id: String = "",
    val callerNumber: String = "",
    val callerName: String? = null,
    val status: CallStatus = CallStatus.SCREENING,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val durationSeconds: Int? = null,
    val transcript: List<ChatMessage> = emptyList(),
    val aiSummary: String? = null,
    val spamScore: Float = 0f,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val scamKeywordsFound: List<String> = emptyList(),
    val sentiment: String? = null,
    val recordingUrl: String? = null,
    val isBlocked: Boolean = false
)

/**
 * AI reply from the backend.
 */
data class AIReplyResponse(
    @SerializedName("reply_text") val replyText: String = "",
    @SerializedName("spam_score") val spamScore: Float = 0f,
    @SerializedName("risk_level") val riskLevel: RiskLevel = RiskLevel.LOW,
    @SerializedName("scam_keywords_found") val scamKeywordsFound: List<String> = emptyList(),
    val sentiment: String = "neutral",
    @SerializedName("should_alert_user") val shouldAlertUser: Boolean = false,
    @SerializedName("alert_message") val alertMessage: String? = null
)

/**
 * Call summary from the backend.
 */
data class CallSummaryResponse(
    val summary: String = "",
    @SerializedName("key_points") val keyPoints: List<String> = emptyList(),
    @SerializedName("caller_intent") val callerIntent: String = "unknown",
    @SerializedName("recommended_action") val recommendedAction: String = "ignore",
    @SerializedName("spam_score") val spamScore: Float = 0f,
    @SerializedName("risk_level") val riskLevel: RiskLevel = RiskLevel.LOW
)

/**
 * Spam analysis result.
 */
data class SpamAnalysisResult(
    @SerializedName("spam_score") val spamScore: Float = 0f,
    @SerializedName("risk_level") val riskLevel: RiskLevel = RiskLevel.LOW,
    @SerializedName("scam_keywords_found") val scamKeywordsFound: List<String> = emptyList(),
    @SerializedName("is_spam") val isSpam: Boolean = false,
    val explanation: String = ""
)

/**
 * WebSocket message structure for communication with backend.
 */
data class WebSocketMessage(
    val type: String,
    val data: String? = null,
    val text: String? = null,
    val sender: String? = null,
    val format: String? = null,
    val status: String? = null,
    val message: String? = null,
    val confidence: Float? = null,
    @SerializedName("spam_score") val spamScore: Float? = null,
    @SerializedName("risk_level") val riskLevel: String? = null,
    val sentiment: String? = null,
    val keywords: List<String>? = null,
    val score: Float? = null,
    val timestamp: String? = null
)
