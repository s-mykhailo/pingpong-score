package com.sm.smtts.data

class RoomRepository {

    suspend fun myRooms(token: String): List<RoomSummary> {
        val res = ApiClient.call("myRooms", token = token)
        val arr = res.getJSONArray("rooms")
        return List(arr.length()) { i -> RoomSummary.fromJson(arr.getJSONObject(i)) }
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
