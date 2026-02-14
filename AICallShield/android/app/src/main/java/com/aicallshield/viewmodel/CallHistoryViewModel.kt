package com.aicallshield.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallshield.data.model.CallRecord
import com.aicallshield.data.model.CallStatus
import com.aicallshield.data.model.ChatMessage
import com.aicallshield.data.model.RiskLevel
import com.aicallshield.data.repository.CallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Call History screen.
 *
 * Manages call history data, search, and CRUD operations.
 */
class CallHistoryViewModel : ViewModel() {

    companion object {
        private const val TAG = "CallHistoryVM"
    }

    private val repository = CallRepository()

    // ── State ────────────────────────────────────────────────────────

    private val _calls = MutableStateFlow<List<CallRecord>>(emptyList())
    val calls: StateFlow<List<CallRecord>> = _calls.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCall = MutableStateFlow<CallRecord?>(null)
    val selectedCall: StateFlow<CallRecord?> = _selectedCall.asStateFlow()

    init {
        loadCallHistory()
    }

    // ── Actions ──────────────────────────────────────────────────────

    fun loadCallHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getCallHistory()
                result.onSuccess { callMaps ->
                    _calls.value = callMaps.map { parseCallRecord(it) }
                }.onFailure {
                    Log.e(TAG, "Failed to load history", it)
                    // Load demo data for testing
                    _calls.value = getDemoCallHistory()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadCallHistory()
        } else {
            searchCalls(query)
        }
    }

    private fun searchCalls(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.searchCalls(query)
                result.onSuccess { callMaps ->
                    _calls.value = callMaps.map { parseCallRecord(it) }
                }.onFailure {
                    Log.e(TAG, "Search failed", it)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCall(call: CallRecord) {
        _selectedCall.value = call
    }

    fun clearSelectedCall() {
        _selectedCall.value = null
    }

    fun deleteCall(call: CallRecord) {
        viewModelScope.launch {
            repository.endCall(call.id) // Use as delete
            _calls.value = _calls.value.filter { it.id != call.id }
        }
    }

    fun blockCaller(call: CallRecord) {
        viewModelScope.launch {
            repository.blockCaller(call.id)
            _calls.value = _calls.value.map {
                if (it.id == call.id) it.copy(isBlocked = true) else it
            }
        }
    }

    // ── Parsing ──────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun parseCallRecord(map: Map<String, Any>): CallRecord {
        return CallRecord(
            id = map["id"] as? String ?: "",
            callerNumber = map["caller_number"] as? String ?: "Unknown",
            callerName = map["caller_name"] as? String,
            status = parseCallStatus(map["status"] as? String),
            startTime = parseLong(map["start_time"]),
            endTime = parseLongOrNull(map["end_time"]),
            durationSeconds = (map["duration_seconds"] as? Number)?.toInt(),
            aiSummary = map["ai_summary"] as? String,
            spamScore = (map["spam_score"] as? Number)?.toFloat() ?: 0f,
            riskLevel = parseRiskLevel(map["risk_level"] as? String),
            scamKeywordsFound = (map["scam_keywords_found"] as? List<String>) ?: emptyList(),
            sentiment = map["sentiment"] as? String,
            isBlocked = map["is_blocked"] as? Boolean ?: false,
        )
    }

    private fun parseCallStatus(status: String?): CallStatus {
        return when (status?.lowercase()) {
            "screening" -> CallStatus.SCREENING
            "user_joined" -> CallStatus.USER_JOINED
            "ended" -> CallStatus.ENDED
            "blocked" -> CallStatus.BLOCKED
            else -> CallStatus.ENDED
        }
    }

    private fun parseRiskLevel(level: String?): RiskLevel {
        return when (level?.lowercase()) {
            "low" -> RiskLevel.LOW
            "medium" -> RiskLevel.MEDIUM
            "high" -> RiskLevel.HIGH
            "critical" -> RiskLevel.CRITICAL
            else -> RiskLevel.LOW
        }
    }

    private fun parseLong(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            is String -> try { value.toLong() } catch (_: Exception) { System.currentTimeMillis() }
            else -> System.currentTimeMillis()
        }
    }

    private fun parseLongOrNull(value: Any?): Long? {
        return when (value) {
            is Number -> value.toLong()
            is String -> try { value.toLong() } catch (_: Exception) { null }
            else -> null
        }
    }

    // ── Demo Data ────────────────────────────────────────────────────

    private fun getDemoCallHistory(): List<CallRecord> {
        return listOf(
            CallRecord(
                id = "demo_1",
                callerNumber = "+1 (555) 123-4567",
                callerName = null,
                status = CallStatus.ENDED,
                startTime = System.currentTimeMillis() - 3_600_000,
                durationSeconds = 45,
                transcript = listOf(
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.AI, text = "Hello! This is an AI assistant screening calls. May I know who's calling?"),
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.CALLER, text = "Hi, I'm calling about your car's extended warranty."),
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.AI, text = "Thank you. I'll pass your message along. Is there a specific number where you can be reached?"),
                ),
                aiSummary = "Caller inquired about extended car warranty. Likely telemarketing/spam call.",
                spamScore = 0.75f,
                riskLevel = RiskLevel.HIGH,
                scamKeywordsFound = listOf("warranty"),
                sentiment = "neutral",
            ),
            CallRecord(
                id = "demo_2",
                callerNumber = "+91 98765 43210",
                callerName = null,
                status = CallStatus.BLOCKED,
                startTime = System.currentTimeMillis() - 7_200_000,
                durationSeconds = 22,
                transcript = listOf(
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.AI, text = "Hello! This is an AI assistant. How can I help you?"),
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.CALLER, text = "Sir, your bank account will be blocked. Please share your OTP now."),
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.AI, text = "I'm not able to share any personal information or OTP codes. If this is regarding a bank issue, please contact the bank directly through their official number."),
                ),
                aiSummary = "⚠ Scam call detected. Caller attempted OTP phishing for bank account. Call was blocked.",
                spamScore = 0.92f,
                riskLevel = RiskLevel.CRITICAL,
                scamKeywordsFound = listOf("bank account", "otp", "blocked account"),
                sentiment = "negative",
                isBlocked = true,
            ),
            CallRecord(
                id = "demo_3",
                callerNumber = "+1 (555) 987-6543",
                callerName = "John from FedEx",
                status = CallStatus.USER_JOINED,
                startTime = System.currentTimeMillis() - 1_800_000,
                durationSeconds = 120,
                transcript = listOf(
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.AI, text = "Hello! This is an AI assistant screening calls. May I know who's calling and the purpose of your call?"),
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.CALLER, text = "Hi, this is John from FedEx. I have a delivery for your address but nobody was home."),
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.AI, text = "Thank you John. Let me check with the recipient. Can you hold for a moment?"),
                    ChatMessage(sender = com.aicallshield.data.model.SenderType.USER, text = "Hi John, I'll be home after 5 PM. Can you redeliver then?"),
                ),
                aiSummary = "Legitimate call from FedEx delivery driver. User joined and arranged redelivery for 5 PM.",
                spamScore = 0.08f,
                riskLevel = RiskLevel.LOW,
                sentiment = "positive",
            ),
        )
    }
}
