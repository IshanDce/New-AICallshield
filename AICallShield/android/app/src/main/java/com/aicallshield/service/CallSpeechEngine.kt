package com.aicallshield.service

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.ArrayDeque
import java.util.Locale

/**
 * Shared TTS engine for speaking assistant messages during live screening.
 */
object CallSpeechEngine {

    private const val TAG = "CallSpeechEngine"

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val pendingQueue = ArrayDeque<String>()

    @Synchronized
    fun speak(context: Context, text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) {
            return
        }

        ensureInitialized(context.applicationContext)

        if (!isReady) {
            pendingQueue.addLast(cleaned)
            return
        }

        speakNow(cleaned)
    }

    @Synchronized
    fun stop() {
        tts?.stop()
    }

    @Synchronized
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        pendingQueue.clear()
    }

    @Synchronized
    private fun ensureInitialized(context: Context) {
        if (tts != null) {
            return
        }

        tts = TextToSpeech(context) { status ->
            synchronized(this) {
                isReady = status == TextToSpeech.SUCCESS
                if (!isReady) {
                    Log.e(TAG, "TextToSpeech initialization failed with status: $status")
                    return@synchronized
                }

                tts?.language = Locale.US
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f)
                while (pendingQueue.isNotEmpty()) {
                    val next = pendingQueue.removeFirst()
                    speakNow(next)
                }
            }
        }
    }

    @Synchronized
    private fun speakNow(text: String) {
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(
            text,
            TextToSpeech.QUEUE_ADD,
            params,
            "screening_tts_${SystemClock.elapsedRealtime()}",
        )
    }
}