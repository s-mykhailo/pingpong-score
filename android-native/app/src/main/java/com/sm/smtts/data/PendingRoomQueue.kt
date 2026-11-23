package com.sm.smtts.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray

private val Context.pendingRoomsDataStore by preferencesDataStore(name = "pending_rooms")

/** Persists not-yet-synced createRoom/joinRoom requests per account, so the dialog can close
 *  immediately and the request retries in the background (and survives app restart while offline). */
class PendingRoomQueue(private val context: Context) {

    private fun createsKey(token: String) = stringPreferencesKey("creates_$token")
    private fun joinsKey(token: String) = stringPreferencesKey("joins_$token")

    suspend fun addCreate(token: String, item: PendingRoomCreate) {
        val key = createsKey(token)
        context.pendingRoomsDataStore.edit { prefs ->
            val arr = JSONArray(prefs[key] ?: "[]")
            arr.put(item.toJson())
            prefs[key] = arr.toString()
        }
    }

    suspend fun getCreates(token: String): List<PendingRoomCreate> {
        val raw = context.pendingRoomsDataStore.data.first()[createsKey(token)] ?: return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) { PendingRoomCreate.fromJson(arr.getJSONObject(it)) }
    }

    suspend fun removeCreate(token: String, localId: String) {
        val key = createsKey(token)
        context.pendingRoomsDataStore.edit { prefs ->
            val arr = JSONArray(prefs[key] ?: "[]")
            val kept = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getString("localId") != localId) kept.put(o)
            }
            prefs[key] = kept.toString()
        }
    }

    suspend fun addJoin(token: String, item: PendingRoomJoin) {
        val key = joinsKey(token)
        context.pendingRoomsDataStore.edit { prefs ->
            val arr = JSONArray(prefs[key] ?: "[]")
            arr.put(item.toJson())
            prefs[key] = arr.toString()
        }
    }

    suspend fun getJoins(token: String): List<PendingRoomJoin> {
        val raw = context.pendingRoomsDataStore.data.first()[joinsKey(token)] ?: return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) { PendingRoomJoin.fromJson(arr.getJSONObject(it)) }
    }

    suspend fun removeJoin(token: String, localId: String) {
        val key = joinsKey(token)
        context.pendingRoomsDataStore.edit { prefs ->
            val arr = JSONArray(prefs[key] ?: "[]")
            val kept = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getString("localId") != localId) kept.put(o)
            }
            prefs[key] = kept.toString()
        }
    }
}
