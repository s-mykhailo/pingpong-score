package com.sm.smtts.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray

private val Context.queueDataStore by preferencesDataStore(name = "pending_matches")

/** Persists not-yet-synced matches per room, so a finished match survives app restart while offline. */
class PendingQueue(private val context: Context) {

    private fun keyFor(roomId: String) = stringPreferencesKey("pending_$roomId")

    suspend fun add(roomId: String, match: Match) {
        val key = keyFor(roomId)
        context.queueDataStore.edit { prefs ->
            val arr = JSONArray(prefs[key] ?: "[]")
            arr.put(match.toJson())
            prefs[key] = arr.toString()
        }
    }

    suspend fun getAll(roomId: String): List<Match> {
        val raw = context.queueDataStore.data.first()[keyFor(roomId)] ?: return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) { Match.fromJson(arr.getJSONObject(it)) }
    }

    suspend fun remove(roomId: String, matchId: String) {
        val key = keyFor(roomId)
        context.queueDataStore.edit { prefs ->
            val arr = JSONArray(prefs[key] ?: "[]")
            val kept = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getString("id") != matchId) kept.put(o)
            }
            prefs[key] = kept.toString()
        }
    }
}
