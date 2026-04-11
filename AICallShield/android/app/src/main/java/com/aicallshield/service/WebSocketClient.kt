package com.aicallshield.service

import android.util.Base64
import android.util.Log
import com.aicallshield.data.model.*
import com.aicallshield.util.Constants
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for real-time communication with the backend
 * during call screening sessions.
 *
 * Handles:
 * - Sending audio chunks for transcription
 * - Receiving AI responses
 * - Receiving spam alerts
 * - Call control messages (user join, end, block)
 */
class WebSocketClient {

    companion object {
        private const val TAG = "WebSocketClient"
    }

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep WebSocket open indefinitely
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // State flows for UI consumption
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableSharedFlow<WebSocketMessage>(replay = 0, extraBufferCapacity = 50)
    val messages: SharedFlow<WebSocketMessage> = _messages

    enum class ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED, ERROR
    }

    /**
     * Connect to the WebSocket server for a specific call.
     */
    fun connect(callId: String) {
        if (webSocket != null) {
            Log.w(TAG, "Already connected. Disconnecting first.")
            disconnect()
        }

        _connectionState.value = ConnectionState.CONNECTING

        val url = "${Constants.WS_BASE_URL}${Constants.WS_CALL_PATH}$callId"
        val request = Request.Builder().url(url).build()

        Log.i(TAG, "Connecting to WebSocket: $url")

        webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected")
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = gson.fromJson(text, WebSocketMessage::class.java)
                    Log.d(TAG, "Received: ${message.type}")

                    scope.launch {
                        _messages.emit(message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                _connectionState.value = ConnectionState.ERROR
            }
        })
    }

    /**
     * Send audio data for transcription.
     */
    fun sendAudio(audioBytes: ByteArray, format: String = "wav") {
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val message = mapOf(
            "type" to "audio",
            "data" to base64Audio,
            "format" to format
        )
        send(gson.toJson(message))
    }

    /**
     * Send a simulated text message (for testing without real audio).
     */
    fun sendCallerText(text: String) {
        val message = mapOf(
            "type" to "text",
            "sender" to "caller",
            "text" to text
        )
        send(gson.toJson(message))
    }

    /**
     * Notify server that user has joined the call.
     */
    fun sendUserJoin() {
        val message = mapOf("type" to "user_join")
        send(gson.toJson(message))
    }

    /**
     * Send a user message after joining.
     */
    fun sendUserMessage(text: String) {
        val message = mapOf(
            "type" to "user_message",
            "text" to text
        )
        send(gson.toJson(message))
    }

    /**
     * Notify server to end the call.
     */
    fun sendEndCall() {
        val message = mapOf("type" to "end_call")
        send(gson.toJson(message))
    }

    /**
     * Notify server to block the caller.
     */
    fun sendBlockCaller() {
        val message = mapOf("type" to "block_caller")
        send(gson.toJson(message))
    }

    /**
     * Send raw JSON string.
     */
    private fun send(json: String) {
        if (webSocket == null || _connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Not connected. Cannot send message.")
            return
        }

        val sent = webSocket?.send(json) ?: false
        if (!sent) {
            Log.e(TAG, "Failed to send message")
        }
    }

    /**
     * Disconnect the WebSocket.
     */
    fun disconnect() {
        try {
            webSocket?.close(1000, "Client disconnect")
            webSocket?.cancel()
            webSocket = null
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.i(TAG, "WebSocket disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    /**
     * Cleanup resources.
     */
    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
