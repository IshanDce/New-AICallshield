package com.aicallshield.data.api

import com.aicallshield.data.model.AIReplyResponse
import com.aicallshield.data.model.CallSummaryResponse
import com.aicallshield.data.model.SpamAnalysisResult
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for the AICallShield backend.
 */
interface AICallShieldApi {

    // ── Call Management ──────────────────────────────────────────────

    @POST("api/v1/calls/")
    suspend fun createCall(
        @Body body: Map<String, String>
    ): Response<Map<String, String>>

    @GET("api/v1/calls/history")
    suspend fun getCallHistory(
        @Query("user_id") userId: String = "default_user",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<Map<String, Any>>

    @GET("api/v1/calls/search")
    suspend fun searchCalls(
        @Query("q") query: String,
        @Query("user_id") userId: String = "default_user"
    ): Response<Map<String, Any>>

    @GET("api/v1/calls/{callId}")
    suspend fun getCall(
        @Path("callId") callId: String,
        @Query("user_id") userId: String = "default_user"
    ): Response<Map<String, Any>>

    @PUT("api/v1/calls/{callId}")
    suspend fun updateCall(
        @Path("callId") callId: String,
        @Body updates: Map<String, Any>,
        @Query("user_id") userId: String = "default_user"
    ): Response<Map<String, String>>

    @POST("api/v1/calls/{callId}/messages")
    suspend fun addMessage(
        @Path("callId") callId: String,
        @Body message: Map<String, String>,
        @Query("user_id") userId: String = "default_user"
    ): Response<Map<String, Any>>

    @POST("api/v1/calls/{callId}/end")
    suspend fun endCall(
        @Path("callId") callId: String,
        @Query("user_id") userId: String = "default_user"
    ): Response<Map<String, Any>>

    @POST("api/v1/calls/{callId}/block")
    suspend fun blockCaller(
        @Path("callId") callId: String,
        @Query("user_id") userId: String = "default_user"
    ): Response<Map<String, String>>

    @DELETE("api/v1/calls/{callId}")
    suspend fun deleteCall(
        @Path("callId") callId: String,
        @Query("user_id") userId: String = "default_user"
    ): Response<Map<String, String>>

    // ── AI Processing ────────────────────────────────────────────────

    @POST("api/v1/ai/reply")
    suspend fun getAIReply(
        @Body request: Map<String, Any>
    ): Response<AIReplyResponse>

    @Multipart
    @POST("api/v1/ai/transcribe")
    suspend fun transcribeAudio(
        @Part audio: MultipartBody.Part,
        @Part("language") language: RequestBody? = null
    ): Response<Map<String, Any>>

    @POST("api/v1/ai/summary")
    suspend fun getCallSummary(
        @Body request: Map<String, Any>
    ): Response<CallSummaryResponse>

    @POST("api/v1/ai/spam-check")
    suspend fun checkSpam(
        @Body request: Map<String, String?>
    ): Response<SpamAnalysisResult>

    @FormUrlEncoded
    @POST("api/v1/ai/sentiment")
    suspend fun checkSentiment(
        @Field("text") text: String
    ): Response<Map<String, Any>>

    // ── Health Check ─────────────────────────────────────────────────

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}
