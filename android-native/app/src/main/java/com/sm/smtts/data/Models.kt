package com.sm.smtts.data

import org.json.JSONObject

data class User(
    val id: String,
    val nickname: String,
    val avatar: String,
    val role: String,
) {
    companion object {
        fun fromJson(o: JSONObject) = User(
            id = o.getString("id"),
            nickname = o.optString("nickname", "?"),
            avatar = o.optString("avatar", ""),
            role = o.optString("role", "user"),
        )
    }
}

data class Session(
    val token: String,
    val user: User,
)

data class RoomSummary(
    val id: String,
    val name: String,
    val isAdmin: Boolean,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("isAdmin", isAdmin)
    }

    companion object {
        fun fromJson(o: JSONObject) = RoomSummary(
            id = o.getString("id"),
            name = o.optString("name", "Кімната"),
            isAdmin = o.optBoolean("isAdmin", false),
        )
    }
}

/** A createRoom request queued locally while it hasn't reached the server yet. */
data class PendingRoomCreate(
    val localId: String,
    val name: String,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("localId", localId)
        put("name", name)
    }

    companion object {
        fun fromJson(o: JSONObject) = PendingRoomCreate(
            localId = o.getString("localId"),
            name = o.getString("name"),
        )
    }
}

/** A joinRoom request queued locally while it hasn't reached the server yet — the real
 *  room id/name are only known once the server validates the invite code. */
data class PendingRoomJoin(
    val localId: String,
    val code: String,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("localId", localId)
        put("code", code)
    }

    companion object {
        fun fromJson(o: JSONObject) = PendingRoomJoin(
            localId = o.getString("localId"),
            code = o.getString("code"),
        )
    }
}

data class Player(
    val id: String,
    val name: String,
    val createdAt: String,
) {
    companion object {
        fun fromJson(o: JSONObject) = Player(
            id = o.getString("id"),
            name = o.optString("name", "?"),
            createdAt = o.optString("createdAt", ""),
        )
    }
}

data class Preset(
    val id: String,
    val name: String,
    val rules: Rules,
) {
    companion object {
        fun fromJson(o: JSONObject) = Preset(
            id = o.getString("id"),
            name = o.optString("name", ""),
            rules = rulesFromJson(o.getJSONObject("rules")),
        )
    }
}

data class Match(
    val id: String,
    val playedAt: String,
    val player1: String,
    val player2: String,
    val sets: List<SetScore>,
    val winner: String,
    val rules: Rules,
    val presetName: String,
) {
    companion object {
        fun fromJson(o: JSONObject): Match {
            val setsArr = o.getJSONArray("sets")
            val sets = List(setsArr.length()) { i ->
                val pair = setsArr.getJSONArray(i)
                SetScore(pair.getInt(0), pair.getInt(1))
            }
            return Match(
                id = o.getString("id"),
                playedAt = o.optString("playedAt", ""),
                player1 = o.getString("player1"),
                player2 = o.getString("player2"),
                sets = sets,
                winner = o.optString("winner", ""),
                rules = rulesFromJson(o.getJSONObject("rules")),
                presetName = o.optString("presetName", ""),
            )
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("playedAt", playedAt)
        put("player1", player1)
        put("player2", player2)
        put("sets", org.json.JSONArray().apply { sets.forEach { put(org.json.JSONArray().put(it.a).put(it.b)) } })
        put("winner", winner)
        put("rules", rulesToJson(rules))
        put("presetName", presetName)
    }
}

fun rulesFromJson(o: JSONObject) = Rules(
    pointsToWin = o.optInt("pointsToWin", 11),
    winBy = o.optInt("winBy", 2),
    bestOf = o.optInt("bestOf", 3),
    serveEvery = o.optInt("serveEvery", 2),
    deuceServeEvery = o.optInt("deuceServeEvery", 1),
)

fun rulesToJson(r: Rules): JSONObject = JSONObject().apply {
    put("pointsToWin", r.pointsToWin)
    put("winBy", r.winBy)
    put("bestOf", r.bestOf)
    put("serveEvery", r.serveEvery)
    put("deuceServeEvery", r.deuceServeEvery)
}

data class RoomData(
    val players: List<Player> = emptyList(),
    val matches: List<Match> = emptyList(),
    val presets: List<Preset> = emptyList(),
)
