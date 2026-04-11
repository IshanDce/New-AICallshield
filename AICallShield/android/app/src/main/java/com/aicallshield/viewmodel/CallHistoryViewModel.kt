package com.aicallshield.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallshield.data.local.LocalCallStore
import com.aicallshield.data.model.CallRecord
import com.aicallshield.data.model.CallStatus
import com.aicallshield.data.model.RiskLevel
import com.aicallshield.data.repository.CallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

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
                    val parsed = callMaps.map { parseCallRecord(it) }
                    _calls.value = parsed
                    if (parsed.isNotEmpty()) {
                        LocalCallStore.setCalls(parsed)
                    } else {
                        _calls.value = LocalCallStore.getCalls()
                    }
                }.onFailure {
                    Log.e(TAG, "Failed to load history", it)
                    _calls.value = LocalCallStore.getCalls()
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
                    _calls.value = LocalCallStore.searchCalls(query)
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
        LocalCallStore.deleteCall(call.id)
        _calls.value = _calls.value.filter { it.id != call.id }

        viewModelScope.launch {
            repository.deleteCall(call.id)
        }
    }

    fun blockCaller(call: CallRecord) {
        val updatedCall = call.copy(
            status = CallStatus.BLOCKED,
            isBlocked = true,
            riskLevel = if (call.riskLevel.ordinal < RiskLevel.HIGH.ordinal) RiskLevel.HIGH else call.riskLevel,
        )

        LocalCallStore.upsertCall(updatedCall)
        _calls.value = _calls.value.map {
            if (it.id == call.id) updatedCall else it
        }

        viewModelScope.launch {
            repository.blockCaller(call.id)
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
            is String -> {
                value.toLongOrNull()
                    ?: runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
            }
            else -> System.currentTimeMillis()
        }
    }

    private fun parseLongOrNull(value: Any?): Long? {
        return when (value) {
            is Number -> value.toLong()
            is String -> {
                value.toLongOrNull()
                    ?: runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            }
            else -> null
        }
    }
}
