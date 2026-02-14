package com.aicallshield.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallshield.data.repository.CallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 *
 * Manages user preferences and server connection status.
 */
class SettingsViewModel : ViewModel() {

    companion object {
        private const val TAG = "SettingsVM"
    }

    private val repository = CallRepository()

    // ── State ────────────────────────────────────────────────────────

    private val _isAIScreeningEnabled = MutableStateFlow(true)
    val isAIScreeningEnabled: StateFlow<Boolean> = _isAIScreeningEnabled.asStateFlow()

    private val _isAutoBlockEnabled = MutableStateFlow(false)
    val isAutoBlockEnabled: StateFlow<Boolean> = _isAutoBlockEnabled.asStateFlow()

    private val _spamThreshold = MutableStateFlow(0.65f)
    val spamThreshold: StateFlow<Float> = _spamThreshold.asStateFlow()

    private val _selectedVoice = MutableStateFlow("alloy")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _serverStatus = MutableStateFlow("Unknown")
    val serverStatus: StateFlow<String> = _serverStatus.asStateFlow()

    init {
        testConnection()
    }

    // ── Actions ──────────────────────────────────────────────────────

    fun toggleAIScreening(enabled: Boolean) {
        _isAIScreeningEnabled.value = enabled
    }

    fun toggleAutoBlock(enabled: Boolean) {
        _isAutoBlockEnabled.value = enabled
    }

    fun updateSpamThreshold(threshold: Float) {
        _spamThreshold.value = threshold
    }

    fun updateVoice(voice: String) {
        _selectedVoice.value = voice
    }

    fun testConnection() {
        _serverStatus.value = "Testing..."
        viewModelScope.launch {
            val result = repository.healthCheck()
            result.onSuccess { healthy ->
                _serverStatus.value = if (healthy) "Connected" else "Error"
            }.onFailure {
                Log.e(TAG, "Connection test failed", it)
                _serverStatus.value = "Disconnected"
            }
        }
    }
}
