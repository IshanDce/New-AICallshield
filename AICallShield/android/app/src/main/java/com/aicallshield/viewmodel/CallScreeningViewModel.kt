package com.aicallshield.viewmodel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.aicallshield.AICallShieldApp
import com.aicallshield.data.local.LocalCallStore
import com.aicallshield.data.model.*
import com.aicallshield.data.repository.CallRepository
import com.aicallshield.service.AudioProcessingService
import com.aicallshield.service.CallSpeechEngine
import com.aicallshield.service.LocalScreeningEngine
import com.aicallshield.service.WebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
        private const val ASSISTANT_GREETING = "HI, I AM ASSISTANT. I will screen this call and protect your privacy."
    }

    private val repository = CallRepository()
    private val webSocketClient = WebSocketClient()
    private val localScreeningEngine = LocalScreeningEngine()
    private var webSocketMessagesJob: Job? = null
    private var callStartTimeMs: Long = 0L
    private var isAudioCaptureRunning: Boolean = false

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

    private val _isLocalMode = MutableStateFlow(false)
    val isLocalMode: StateFlow<Boolean> = _isLocalMode.asStateFlow()

    val connectionState = webSocketClient.connectionState

    init {
        observeConnectionState()
    }

    // ── Initialization ───────────────────────────────────────────────

    fun startScreening(callerNumber: String) {
        resetSession(callerNumber)

        // Add system message
        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = SenderType.SYSTEM,
                text = "🛡️ AI Screening started for $callerNumber",
                timestamp = System.currentTimeMillis()
            )
        )

        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = SenderType.AI,
                text = ASSISTANT_GREETING,
                timestamp = System.currentTimeMillis(),
                spamScore = _currentSpamScore.value,
                riskLevel = _currentRiskLevel.value,
            )
        )
        CallSpeechEngine.speak(AICallShieldApp.appContext, ASSISTANT_GREETING)

        callStartTimeMs = System.currentTimeMillis()
        _isLocalMode.value = true
        if (_callId.value.isBlank()) {
            _callId.value = "local_$callStartTimeMs"
        }

        // Create call record on backend
        viewModelScope.launch {
            val result = repository.createCall(callerNumber)
            result.onSuccess { callId ->
                _callId.value = callId
                connectWebSocket(callId)
                awaitConnectionOrFallback()
                Log.i(TAG, "Call created: $callId")
            }.onFailure { error ->
                Log.e(TAG, "Failed to create call", error)
                enableLocalMode("Cloud backend unavailable. Switched to local screening mode.")
            }
        }
    }

    private fun resetSession(callerNumber: String) {
        _callerNumber.value = callerNumber
        _callStatus.value = CallStatus.SCREENING
        _messages.value = emptyList()
        _currentSpamScore.value = 0f
        _currentRiskLevel.value = RiskLevel.LOW
        _isAIProcessing.value = false
        _isLocalMode.value = false
        _callId.value = ""
    }

    private fun connectWebSocket(callId: String) {
        webSocketClient.connect(callId)

        // Listen for WebSocket messages
        webSocketMessagesJob?.cancel()
        webSocketMessagesJob = viewModelScope.launch {
            webSocketClient.messages.collect { wsMessage ->
                handleWebSocketMessage(wsMessage)
            }
        }
    }

    private suspend fun awaitConnectionOrFallback() {
        val state = withTimeoutOrNull(4000) {
            webSocketClient.connectionState
                .filter {
                    it == WebSocketClient.ConnectionState.CONNECTED ||
                        it == WebSocketClient.ConnectionState.ERROR
                }
                .first()
        }

        if (state != WebSocketClient.ConnectionState.CONNECTED) {
            enableLocalMode("Cloud connection timed out. Using local protection mode.")
        } else {
            _isLocalMode.value = false
            addMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = SenderType.SYSTEM,
                    text = "☁ Connected to cloud AI screening.",
                    timestamp = System.currentTimeMillis()
                )
            )
            if (_callId.value.isNotBlank()) {
                startAudioCapture(_callId.value)
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                if (
                    state == WebSocketClient.ConnectionState.ERROR &&
                    _callStatus.value == CallStatus.SCREENING &&
                    !_isLocalMode.value
                ) {
                    enableLocalMode("Connection issue detected. Switched to local screening mode.")
                }
            }
        }
    }

    private fun enableLocalMode(reason: String, announce: Boolean = true) {
        val wasLocalMode = _isLocalMode.value
        _isLocalMode.value = true
        stopAudioCapture()
        if (_callId.value.isBlank()) {
            _callId.value = "local_${System.currentTimeMillis()}"
        }

        if (announce) {
            addMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = SenderType.SYSTEM,
                    text = "📱 $reason",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        if (!wasLocalMode) {
            addMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = SenderType.SYSTEM,
                    text = "📱 Local mode active. Assistant continues screening with on-device rules.",
                    timestamp = System.currentTimeMillis(),
                    spamScore = _currentSpamScore.value,
                    riskLevel = _currentRiskLevel.value
                )
            )
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
                val aiText = wsMessage.text ?: ""
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = SenderType.AI,
                        text = aiText,
                        timestamp = System.currentTimeMillis(),
                        spamScore = wsMessage.spamScore,
                        riskLevel = parseRiskLevel(wsMessage.riskLevel)
                    )
                )
                if (aiText.isNotBlank()) {
                    CallSpeechEngine.speak(AICallShieldApp.appContext, aiText)
                }
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
                enableLocalMode("Cloud AI error detected. Local screening remains active.")
            }
        }
    }

    // ── User Actions ─────────────────────────────────────────────────

    fun joinCall() {
        _callStatus.value = CallStatus.USER_JOINED
        if (!_isLocalMode.value) {
            webSocketClient.sendUserJoin()
        }

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
            if (!_isLocalMode.value) {
                webSocketClient.sendUserMessage(text)
            }

            addMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = SenderType.USER,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
            )
            return
        }

        if (_isLocalMode.value) {
            processLocalCallerMessage(text)
            return
        }

        // Simulated caller text path when cloud mode is active.
        webSocketClient.sendCallerText(text)
        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = SenderType.CALLER,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun sendSimulatedCallerMessage(text: String) {
        if (_isLocalMode.value) {
            processLocalCallerMessage(text)
        } else {
            webSocketClient.sendCallerText(text)
        }
    }

    private fun processLocalCallerMessage(text: String) {
        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = SenderType.CALLER,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )

        _isAIProcessing.value = true
        viewModelScope.launch {
            delay(220)
            val result = localScreeningEngine.evaluate(text, _callerNumber.value)

            _currentSpamScore.value = maxOf(_currentSpamScore.value, result.spamScore)
            if (result.riskLevel.ordinal > _currentRiskLevel.value.ordinal) {
                _currentRiskLevel.value = result.riskLevel
            }

            if (result.shouldAlert) {
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = SenderType.SYSTEM,
                        text = result.alertMessage ?: "⚠ Suspicious call pattern detected.",
                        timestamp = System.currentTimeMillis(),
                        isAlert = true,
                        alertMessage = result.alertMessage,
                        spamScore = result.spamScore,
                        riskLevel = result.riskLevel,
                    )
                )
            }

            addMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = SenderType.AI,
                    text = result.replyText,
                    timestamp = System.currentTimeMillis(),
                    spamScore = result.spamScore,
                    riskLevel = result.riskLevel,
                )
            )
            CallSpeechEngine.speak(AICallShieldApp.appContext, result.replyText)
            _isAIProcessing.value = false
        }
    }

    fun blockCaller() {
        _callStatus.value = CallStatus.BLOCKED
        stopAudioCapture()
        CallSpeechEngine.stop()
        if (!_isLocalMode.value) {
            webSocketClient.sendBlockCaller()
        }

        val summary = generateLocalSummary()
        persistCallRecord(CallStatus.BLOCKED, summary)

        viewModelScope.launch {
            if (!_isLocalMode.value && !isLocalCallId()) {
                repository.blockCaller(_callId.value)
            }
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
        stopAudioCapture()
        CallSpeechEngine.stop()
        if (!_isLocalMode.value) {
            webSocketClient.sendEndCall()
        }

        val localSummary = generateLocalSummary()
        persistCallRecord(CallStatus.ENDED, localSummary)

        if (_isLocalMode.value) {
            addMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = SenderType.SYSTEM,
                    text = "📋 Summary: $localSummary",
                    timestamp = System.currentTimeMillis()
                )
            )
            return
        }

        viewModelScope.launch {
            if (!isLocalCallId()) {
                repository.endCall(_callId.value)
            }

            val result = repository.getCallSummary(_callId.value, _messages.value)
            result.onSuccess { summary ->
                val cloudSummaryText = summary.summary.ifBlank { localSummary }
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = SenderType.SYSTEM,
                        text = "📋 AI Summary: $cloudSummaryText",
                        timestamp = System.currentTimeMillis()
                    )
                )
                persistCallRecord(CallStatus.ENDED, cloudSummaryText)
            }.onFailure {
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = SenderType.SYSTEM,
                        text = "📋 Summary: $localSummary",
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
        if (_isLocalMode.value) {
            return
        }
        webSocketClient.sendAudio(audioBytes)
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun startAudioCapture(callId: String) {
        if (isAudioCaptureRunning) {
            return
        }

        val context = AICallShieldApp.appContext
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            addMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = SenderType.SYSTEM,
                    text = "🎤 Microphone permission is required for live AI call attendance.",
                    timestamp = System.currentTimeMillis(),
                )
            )
            return
        }

        AudioProcessingService.audioChunkListener = { chunk ->
            sendAudioChunk(chunk)
        }

        val intent = Intent(context, AudioProcessingService::class.java).apply {
            action = AudioProcessingService.ACTION_START
            putExtra(AudioProcessingService.EXTRA_CALL_ID, callId)
        }

        try {
            ContextCompat.startForegroundService(context, intent)
            isAudioCaptureRunning = true
            Log.i(TAG, "Audio capture started for call: $callId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio capture", e)
        }
    }

    private fun stopAudioCapture() {
        if (!isAudioCaptureRunning) {
            AudioProcessingService.audioChunkListener = null
            return
        }

        val context = AICallShieldApp.appContext
        val intent = Intent(context, AudioProcessingService::class.java).apply {
            action = AudioProcessingService.ACTION_STOP
        }

        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio capture", e)
        } finally {
            AudioProcessingService.audioChunkListener = null
            isAudioCaptureRunning = false
        }
    }

    private fun isLocalCallId(): Boolean {
        return _callId.value.startsWith("local_")
    }

    private fun generateLocalSummary(): String {
        val callerMessages = _messages.value.filter { it.sender == SenderType.CALLER }
        val transcriptSummary = if (callerMessages.isEmpty()) {
            "No caller transcript captured."
        } else {
            "Caller shared ${callerMessages.size} message(s)."
        }

        val keywords = localScreeningEngine.extractKeywords(
            _messages.value.joinToString(" ") { it.text }
        )
        val keywordText = if (keywords.isEmpty()) {
            "No strong scam keywords detected."
        } else {
            "Detected keywords: ${keywords.take(5).joinToString(", ")}."
        }

        val recommendation = when (_currentRiskLevel.value) {
            RiskLevel.CRITICAL,
            RiskLevel.HIGH -> "Recommendation: block and avoid sharing personal information."

            RiskLevel.MEDIUM -> "Recommendation: verify caller identity through official channels."
            RiskLevel.LOW -> "Recommendation: low risk, proceed with normal caution."
        }

        return "$transcriptSummary Risk level: ${_currentRiskLevel.value.name.lowercase()}. $keywordText $recommendation"
    }

    private fun persistCallRecord(status: CallStatus, summary: String?) {
        val start = if (callStartTimeMs > 0L) callStartTimeMs else System.currentTimeMillis()
        val end = System.currentTimeMillis()
        val durationSec = ((end - start) / 1000).toInt().coerceAtLeast(0)

        val record = CallRecord(
            id = _callId.value.ifBlank { "local_$start" },
            callerNumber = _callerNumber.value.ifBlank { "Unknown" },
            status = status,
            startTime = start,
            endTime = end,
            durationSeconds = durationSec,
            transcript = _messages.value,
            aiSummary = summary,
            spamScore = _currentSpamScore.value,
            riskLevel = _currentRiskLevel.value,
            scamKeywordsFound = localScreeningEngine.extractKeywords(
                _messages.value.joinToString(" ") { it.text }
            ),
            isBlocked = status == CallStatus.BLOCKED,
        )

        LocalCallStore.upsertCall(record)
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
        stopAudioCapture()
        CallSpeechEngine.shutdown()
        webSocketMessagesJob?.cancel()
        webSocketClient.destroy()
    }
}
