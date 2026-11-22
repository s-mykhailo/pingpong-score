package com.sm.smtts.data

class AuthRepository(private val sessionStore: SessionStore) {

    suspend fun register(nickname: String, avatar: String = ""): Pair<Session, String> {
        val res = ApiClient.call("register", payload = mapOf("nickname" to nickname, "avatar" to avatar))
        val session = Session(res.getString("token"), User.fromJson(res.getJSONObject("user")))
        sessionStore.save(session)
        return session to res.getString("recoveryCode")
    }

    suspend fun recoverAccount(code: String): Session {
        val res = ApiClient.call("recoverAccount", payload = mapOf("code" to code))
        val session = Session(res.getString("token"), User.fromJson(res.getJSONObject("user")))
        sessionStore.save(session)
        return session
    }

    suspend fun me(token: String): User {
        val res = ApiClient.call("me", token = token)
        val user = User.fromJson(res.getJSONObject("user"))
        sessionStore.updateUser(user)
        return user
    }

    suspend fun updateProfile(token: String, nickname: String, avatar: String): User {
        val res = ApiClient.call("updateProfile", token = token, payload = mapOf("nickname" to nickname, "avatar" to avatar))
        val user = User.fromJson(res.getJSONObject("user"))
        sessionStore.updateUser(user)
        return user
    }

    suspend fun claimMasterkey(token: String, masterKey: String) {
        ApiClient.call("claimMasterkey", token = token, payload = mapOf("masterKey" to masterKey))
    }

    suspend fun claimAdmin(token: String, code: String) {
        ApiClient.call("claimAdmin", token = token, payload = mapOf("code" to code))
    }

    suspend fun createAdminInvite(token: String): String {
        val res = ApiClient.call("createAdminInvite", token = token)
        return res.getString("code")
    }

    suspend fun logout(token: String) {
        try {
            ApiClient.call("logout", token = token)
        } finally {
            sessionStore.clear()
        }
    }
}
