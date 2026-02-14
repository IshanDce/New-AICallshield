package com.aicallshield.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallshield.data.model.*
import com.aicallshield.data.repository.CallRepository
import com.aicallshield.service.WebSocketClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for the Call Screening screen.
 *
 * Manages real-time call state, WebSocket communication,
 * chat messages, and spam detection during an active call.
 */
class CallScreeningViewModel : ViewModel() {

    companion object {
        private const val TAG = "CallScreeningVM"
    }

    private val repository = CallRepository()
    private val webSocketClient = WebSocketClient()

    // ── State ────────────────────────────────────────────────────────

    private val _callStatus = MutableStateFlow(CallStatus.SCREENING)
    val callStatus: StateFlow<CallStatus> = _callStatus.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentSpamScore = MutableStateFlow(0f)
    val currentSpamScore: StateFlow<Float> = _currentSpamScore.asStateFlow()

    private val _currentRiskLevel = MutableStateFlow(RiskLevel.LOW)
    val currentRiskLevel: StateFlow<RiskLevel> = _currentRiskLevel.asStateFlow()

    private val _isAIProcessing = MutableStateFlow(false)
    val isAIProcessing: StateFlow<Boolean> = _isAIProcessing.asStateFlow()

    private val _callerNumber = MutableStateFlow("")
    val callerNumber: StateFlow<String> = _callerNumber.asStateFlow()

    private val _callId = MutableStateFlow("")
    val callId: StateFlow<String> = _callId.asStateFlow()

    val connectionState = webSocketClient.connectionState

    // ── Initialization ───────────────────────────────────────────────

    fun startScreening(callerNumber: String) {
        _callerNumber.value = callerNumber

        // Add system message
        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = SenderType.SYSTEM,
                text = "🛡️ AI Screening started for $callerNumber",
                timestamp = System.currentTimeMillis()
            )
        )

        // Create call record on backend
        viewModelScope.launch {
            val result = repository.createCall(callerNumber)
            result.onSuccess { callId ->
                _callId.value = callId
                connectWebSocket(callId)
                Log.i(TAG, "Call created: $callId")
            }.onFailure { error ->
                Log.e(TAG, "Failed to create call", error)
                // Continue with local-only mode
                val localId = "local_${System.currentTimeMillis()}"
                _callId.value = localId
            }
        }
    }

    private fun connectWebSocket(callId: String) {
        webSocketClient.connect(callId)

        // Listen for WebSocket messages
        viewModelScope.launch {
            webSocketClient.messages.collect { wsMessage ->
                handleWebSocketMessage(wsMessage)
            }
        }
    }

    // ── WebSocket Message Handling ───────────────────────────────────

    private fun handleWebSocketMessage(wsMessage: WebSocketMessage) {
        when (wsMessage.type) {
            "transcription" -> {
                val sender = when (wsMessage.sender) {
                    "caller" -> SenderType.CALLER
                    "user" -> SenderType.USER
                    else -> SenderType.SYSTEM
                }
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = sender,
                        text = wsMessage.text ?: "",
                        timestamp = System.currentTimeMillis(),
                        confidence = wsMessage.confidence
                    )
                )
                _isAIProcessing.value = true
            }

            "ai_reply" -> {
                _isAIProcessing.value = false
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = SenderType.AI,
                        text = wsMessage.text ?: "",
                        timestamp = System.currentTimeMillis(),
                        spamScore = wsMessage.spamScore,
                        riskLevel = parseRiskLevel(wsMessage.riskLevel)
                    )
                )
                // Update spam score
                wsMessage.spamScore?.let { _currentSpamScore.value = it }
                parseRiskLevel(wsMessage.riskLevel)?.let { _currentRiskLevel.value = it }
            }

            "spam_alert" -> {
                val alertMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = SenderType.SYSTEM,
                    text = wsMessage.message ?: "⚠ Spam detected",
                    timestamp = System.currentTimeMillis(),
                    isAlert = true,
                    alertMessage = wsMessage.message,
                    spamScore = wsMessage.score ?: wsMessage.spamScore
                )
                addMessage(alertMsg)
                wsMessage.score?.let { _currentSpamScore.value = it }
            }

            "status" -> {
                val status = when (wsMessage.status) {
                    "screening" -> CallStatus.SCREENING
                    "user_joined" -> CallStatus.USER_JOINED
                    "ended" -> CallStatus.ENDED
                    "blocked" -> CallStatus.BLOCKED
                    else -> _callStatus.value
                }
                _callStatus.value = status

                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = SenderType.SYSTEM,
                        text = wsMessage.message ?: "Status: ${wsMessage.status}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            "error" -> {
                Log.e(TAG, "WebSocket error: ${wsMessage.message}")
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = SenderType.SYSTEM,
                        text = "⚠ Error: ${wsMessage.message}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // ── User Actions ─────────────────────────────────────────────────

    fun joinCall() {
        _callStatus.value = CallStatus.USER_JOINED
        webSocketClient.sendUserJoin()

        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = SenderType.SYSTEM,
                text = "👤 You have joined the call. AI screening stopped.",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        if (_callStatus.value == CallStatus.USER_JOINED) {
            webSocketClient.sendUserMessage(text)
        } else {
            // Simulate caller message for testing
            webSocketClient.sendCallerText(text)
        }

        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = if (_callStatus.value == CallStatus.USER_JOINED) SenderType.USER else SenderType.CALLER,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun sendSimulatedCallerMessage(text: String) {
        webSocketClient.sendCallerText(text)
    }

    fun blockCaller() {
        _callStatus.value = CallStatus.BLOCKED
        webSocketClient.sendBlockCaller()

        viewModelScope.launch {
            repository.blockCaller(_callId.value)
        }

        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = SenderType.SYSTEM,
                text = "🚫 Caller has been blocked.",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun endCall() {
        _callStatus.value = CallStatus.ENDED
        webSocketClient.sendEndCall()

        viewModelScope.launch {
            repository.endCall(_callId.value)
            // Generate summary
            val result = repository.getCallSummary(_callId.value, _messages.value)
            result.onSuccess { summary ->
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = SenderType.SYSTEM,
                        text = "📋 AI Summary: ${summary.summary}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Send audio chunk from AudioProcessingService to backend.
     */
    fun sendAudioChunk(audioBytes: ByteArray) {
        webSocketClient.sendAudio(audioBytes)
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun parseRiskLevel(level: String?): RiskLevel? {
        return when (level?.lowercase()) {
            "low" -> RiskLevel.LOW
            "medium" -> RiskLevel.MEDIUM
            "high" -> RiskLevel.HIGH
            "critical" -> RiskLevel.CRITICAL
            else -> null
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketClient.destroy()
    }
}
