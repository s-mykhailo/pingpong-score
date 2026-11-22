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

    val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        val token = prefs[tokenKey] ?: return@map null
        val userJson = prefs[userKey] ?: return@map null
        Session(token, User.fromJson(JSONObject(userJson)))
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
        }
    }
}
