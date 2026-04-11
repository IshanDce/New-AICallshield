package com.aicallshield.data.local

import android.content.Context
import com.aicallshield.AICallShieldApp
import com.aicallshield.data.model.CallRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * SharedPreferences-backed local call store used for zero-setup operation.
 */
object LocalCallStore {

    private const val PREFS_NAME = "aicallshield_local_calls"
    private const val KEY_CALLS = "calls_json"
    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<CallRecord>>() {}.type

    private fun prefs() = AICallShieldApp.appContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getCalls(): List<CallRecord> {
        val raw = prefs().getString(KEY_CALLS, null) ?: return emptyList()
        return try {
            gson.fromJson<MutableList<CallRecord>>(raw, listType)
                ?.sortedByDescending { it.startTime }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun setCalls(calls: List<CallRecord>) {
        val sorted = calls.sortedByDescending { it.startTime }
        prefs().edit().putString(KEY_CALLS, gson.toJson(sorted)).apply()
    }

    @Synchronized
    fun upsertCall(call: CallRecord) {
        val current = getCalls().toMutableList()
        val index = current.indexOfFirst { it.id == call.id }
        if (index >= 0) {
            current[index] = call
        } else {
            current.add(call)
        }
        setCalls(current)
    }

    @Synchronized
    fun deleteCall(callId: String) {
        val remaining = getCalls().filterNot { it.id == callId }
        setCalls(remaining)
    }

    @Synchronized
    fun searchCalls(query: String): List<CallRecord> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return getCalls()

        return getCalls().filter { call ->
            val transcriptBlob = call.transcript.joinToString(" ") { it.text }.lowercase()
            call.callerNumber.lowercase().contains(q) ||
                (call.callerName?.lowercase()?.contains(q) == true) ||
                (call.aiSummary?.lowercase()?.contains(q) == true) ||
                transcriptBlob.contains(q)
        }
    }
}
