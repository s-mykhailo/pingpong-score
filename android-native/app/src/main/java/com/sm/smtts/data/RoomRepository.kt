package com.sm.smtts.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray

private val Context.roomsCacheDataStore by preferencesDataStore(name = "rooms_cache")

class RoomRepository(private val context: Context) {

    private fun keyFor(token: String) = stringPreferencesKey("rooms_$token")

    /** Last list fetched for this token, so the UI can render instantly instead of a blank spinner. */
    suspend fun cachedRooms(token: String): List<RoomSummary> {
        val raw = context.roomsCacheDataStore.data.first()[keyFor(token)] ?: return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) { RoomSummary.fromJson(arr.getJSONObject(it)) }
    }

    suspend fun myRooms(token: String): List<RoomSummary> {
        val res = ApiClient.call("myRooms", token = token)
        val arr = res.getJSONArray("rooms")
        val rooms = List(arr.length()) { i -> RoomSummary.fromJson(arr.getJSONObject(i)) }
        val out = JSONArray()
        rooms.forEach { out.put(it.toJson()) }
        context.roomsCacheDataStore.edit { prefs -> prefs[keyFor(token)] = out.toString() }
        return rooms
    }

    suspend fun createRoom(token: String, name: String): RoomSummary {
        val res = ApiClient.call("createRoom", token = token, payload = mapOf("name" to name))
        return RoomSummary.fromJson(res.getJSONObject("room"))
    }

    suspend fun createRoomInvite(token: String, roomId: String): String {
        val res = ApiClient.call("createRoomInvite", token = token, payload = mapOf("roomId" to roomId))
        return res.getString("code")
    }

    suspend fun joinRoom(token: String, code: String): RoomSummary {
        val res = ApiClient.call("joinRoom", token = token, payload = mapOf("code" to code))
        return RoomSummary.fromJson(res.getJSONObject("room"))
    }
}
