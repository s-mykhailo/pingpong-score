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
    companion object {
        fun fromJson(o: JSONObject) = RoomSummary(
            id = o.getString("id"),
            name = o.optString("name", "Кімната"),
            isAdmin = o.optBoolean("isAdmin", false),
        )
    }
}
