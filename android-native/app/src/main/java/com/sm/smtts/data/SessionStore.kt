package com.sm.smtts.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val userKey = stringPreferencesKey("user_json")
    private val pendingRecoveryKey = stringPreferencesKey("pending_recovery_code")

    val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        val token = prefs[tokenKey] ?: return@map null
        val userJson = prefs[userKey] ?: return@map null
        Session(token, User.fromJson(JSONObject(userJson)))
    }

    /**
     * A freshly issued recovery code the user has not yet confirmed writing down.
     * Persisted rather than held in composition state: the code is shown exactly once,
     * ever, and saving the session re-roots the whole UI tree, so in-memory state for it
     * is lost the moment registration succeeds.
     */
    val pendingRecoveryCode: Flow<String?> = context.dataStore.data.map { it[pendingRecoveryKey] }

    suspend fun setPendingRecoveryCode(code: String) {
        context.dataStore.edit { it[pendingRecoveryKey] = code }
    }

    suspend fun clearPendingRecoveryCode() {
        context.dataStore.edit { it.remove(pendingRecoveryKey) }
    }

    suspend fun save(session: Session) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = session.token
            prefs[userKey] = JSONObject().apply {
                put("id", session.user.id)
                put("nickname", session.user.nickname)
                put("avatar", session.user.avatar)
                put("role", session.user.role)
            }.toString()
        }
    }

    suspend fun updateUser(user: User) {
        context.dataStore.edit { prefs ->
            prefs[userKey] = JSONObject().apply {
                put("id", user.id)
                put("nickname", user.nickname)
                put("avatar", user.avatar)
                put("role", user.role)
            }.toString()
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(tokenKey)
            prefs.remove(userKey)
            prefs.remove(pendingRecoveryKey)
        }
    }
}
