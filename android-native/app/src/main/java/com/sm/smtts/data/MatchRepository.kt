package com.sm.smtts.data

class MatchRepository {

    suspend fun getAll(token: String, roomId: String): RoomData {
        val res = ApiClient.call("getAll", token = token, payload = mapOf("roomId" to roomId))
        val players = res.getJSONArray("players")
        val matches = res.getJSONArray("matches")
        val presets = res.getJSONArray("presets")
        return RoomData(
            players = List(players.length()) { Player.fromJson(players.getJSONObject(it)) },
            matches = List(matches.length()) { Match.fromJson(matches.getJSONObject(it)) },
            presets = List(presets.length()) { Preset.fromJson(presets.getJSONObject(it)) },
        )
    }

    suspend fun addPlayer(token: String, roomId: String, name: String): Player {
        val res = ApiClient.call("addPlayer", token = token, payload = mapOf("roomId" to roomId, "name" to name))
        return Player.fromJson(res.getJSONObject("player"))
    }

    suspend fun deletePlayer(token: String, roomId: String, id: String) {
        ApiClient.call("deletePlayer", token = token, payload = mapOf("roomId" to roomId, "id" to id))
    }

    suspend fun addMatch(token: String, roomId: String, match: Match) {
        ApiClient.call("addMatch", token = token, payload = mapOf("roomId" to roomId, "match" to match.toJson()))
    }

    suspend fun deleteMatch(token: String, roomId: String, id: String) {
        ApiClient.call("deleteMatch", token = token, payload = mapOf("roomId" to roomId, "id" to id))
    }

    suspend fun savePreset(token: String, roomId: String, id: String?, name: String, rules: Rules): Preset {
        val payload = mutableMapOf<String, Any?>(
            "roomId" to roomId,
            "preset" to org.json.JSONObject().apply {
                if (id != null) put("id", id)
                put("name", name)
                put("rules", rulesToJson(rules))
            },
        )
        val res = ApiClient.call("savePreset", token = token, payload = payload)
        return Preset.fromJson(res.getJSONObject("preset"))
    }

    suspend fun deletePreset(token: String, roomId: String, id: String) {
        ApiClient.call("deletePreset", token = token, payload = mapOf("roomId" to roomId, "id" to id))
    }
}
