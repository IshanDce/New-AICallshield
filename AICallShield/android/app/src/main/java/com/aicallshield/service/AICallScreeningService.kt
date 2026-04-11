package com.aicallshield.service

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Android CallScreeningService that intercepts incoming calls.
 *
 * This service is triggered by the system when an incoming call arrives.
 * It determines whether the call should be screened by AI.
 *
 * Requirements:
 * - App must be set as default call screening app in Settings
 * - Android 10+ (API 29+)
 */
class AICallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "AICallScreening"
        private const val ASSISTANT_GREETING = "HI, I AM ASSISTANT. I will screen this call and protect your privacy."

        // Shared state for communication with UI
        @Volatile
        var currentCallNumber: String? = null
            private set

        @Volatile
        var isScreening: Boolean = false
            private set

        // Listener for call events
        var callEventListener: CallEventListener? = null
    }

    interface CallEventListener {
        fun onIncomingCall(number: String)
        fun onCallAnswered()
        fun onCallEnded()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val handle: Uri? = callDetails.handle
        val phoneNumber = handle?.schemeSpecificPart ?: "Unknown"
        val callerName = callDetails.callerDisplayName ?: ""

        Log.i(TAG, "Incoming call from: $phoneNumber ($callerName)")

        // Product behavior: screen every incoming call (known or unknown).
        currentCallNumber = phoneNumber
        isScreening = true

        callEventListener?.onIncomingCall(phoneNumber)
        launchScreeningUI(phoneNumber)

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
        Handler(Looper.getMainLooper()).postDelayed(
            { tryAutoAnswerUnknownCall() },
            250L
        )
        Handler(Looper.getMainLooper()).postDelayed(
            { CallSpeechEngine.speak(this, ASSISTANT_GREETING) },
            900L,
        )
    }

    /**
     * Try to answer the ringing call so AI can start attending immediately.
     * This is best-effort and depends on OEM/Android policy and dialer role.
     */
    @Suppress("DEPRECATION")
    private fun tryAutoAnswerUnknownCall() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ANSWER_PHONE_CALLS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "ANSWER_PHONE_CALLS permission missing. Cannot auto-answer.")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "Auto-answer not supported below Android O in this implementation.")
            return
        }

        try {
            val telecomManager = getSystemService(TelecomManager::class.java)
            telecomManager?.acceptRingingCall()
            Log.i(TAG, "Auto-answer requested for unknown call.")
            callEventListener?.onCallAnswered()
        } catch (e: SecurityException) {
            Log.e(TAG, "Auto-answer blocked by OS policy/role restrictions.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-answer incoming call.", e)
        }
    }

    /**
     * Launch the call screening UI activity.
     */
    private fun launchScreeningUI(phoneNumber: String) {
        try {
            val intent = Intent(this, Class.forName("com.aicallshield.ui.CallScreeningActivity")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("caller_number", phoneNumber)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch screening UI", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CallSpeechEngine.shutdown()
        currentCallNumber = null
        isScreening = false
    }
}
