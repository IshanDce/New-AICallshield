package com.aicallshield.data.repository

import android.util.Log
import com.aicallshield.data.api.RetrofitClient
import com.aicallshield.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for call-related data operations.
 * Abstracts API communication from ViewModels.
 */
class CallRepository {

    private val api = RetrofitClient.api
    private val gson = Gson()

    companion object {
        private const val TAG = "CallRepository"
    }

    // ── Call Management ──────────────────────────────────────────────

    suspend fun createCall(callerNumber: String, userId: String = "default_user"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "caller_number" to callerNumber,
                    "user_id" to userId
                )
                val response = api.createCall(body)
                if (response.isSuccessful) {
                    val callId = response.body()?.get("call_id") ?: ""
                    Result.success(callId)
                } else {
                    Result.failure(Exception("Create call failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "createCall error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getCallHistory(userId: String = "default_user", limit: Int = 50): Result<List<Map<String, Any>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCallHistory(userId, limit)
                if (response.isSuccessful) {
                    @Suppress("UNCHECKED_CAST")
                    val calls = response.body()?.get("calls") as? List<Map<String, Any>> ?: emptyList()
                    Result.success(calls)
                } else {
                    Result.failure(Exception("Get history failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "getCallHistory error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun searchCalls(query: String, userId: String = "default_user"): Result<List<Map<String, Any>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.searchCalls(query, userId)
                if (response.isSuccessful) {
                    @Suppress("UNCHECKED_CAST")
                    val results = response.body()?.get("results") as? List<Map<String, Any>> ?: emptyList()
                    Result.success(results)
                } else {
                    Result.failure(Exception("Search failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchCalls error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun endCall(callId: String, userId: String = "default_user"): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.endCall(callId, userId)
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("End call failed: ${response.code()}"))
            } catch (e: Exception) {
                Log.e(TAG, "endCall error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun blockCaller(callId: String, userId: String = "default_user"): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.blockCaller(callId, userId)
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Block failed: ${response.code()}"))
            } catch (e: Exception) {
                Log.e(TAG, "blockCaller error", e)
                Result.failure(e)
            }
        }
    }

    // ── AI Processing ────────────────────────────────────────────────

    suspend fun getAIReply(
        callId: String,
        callerMessage: String,
        history: List<ChatMessage> = emptyList()
    ): Result<AIReplyResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val historyMaps = history.map { msg ->
                    mapOf(
                        "sender" to msg.sender.name.lowercase(),
                        "text" to msg.text,
                        "timestamp" to msg.timestamp.toString()
                    )
                }

                val request = mapOf(
                    "call_id" to callId,
                    "caller_message" to callerMessage,
                    "conversation_history" to historyMaps
                )

                val response = api.getAIReply(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("AI reply failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAIReply error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getCallSummary(callId: String, transcript: List<ChatMessage>): Result<CallSummaryResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val transcriptMaps = transcript.map { msg ->
                    mapOf(
                        "sender" to msg.sender.name.lowercase(),
                        "text" to msg.text,
                        "timestamp" to msg.timestamp.toString()
                    )
                }

                val request = mapOf(
                    "call_id" to callId,
                    "transcript" to transcriptMaps
                )

                val response = api.getCallSummary(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Summary failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "getCallSummary error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun checkSpam(text: String, callerNumber: String? = null): Result<SpamAnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "text" to text,
                    "caller_number" to callerNumber
                )
                val response = api.checkSpam(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Spam check failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkSpam error", e)
                Result.failure(e)
            }
        }
    }

    // ── Health Check ─────────────────────────────────────────────────

    suspend fun healthCheck(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.healthCheck()
                Result.success(response.isSuccessful)
            } catch (e: Exception) {
                Log.e(TAG, "healthCheck error", e)
                Result.failure(e)
            }
        }
    }
}
