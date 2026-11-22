package com.sm.smtts.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.themeDataStore by preferencesDataStore(name = "theme")

class ThemeStore(private val context: Context) {
    private val key = stringPreferencesKey("mode")

    val mode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[key]) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[key] = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
        }
    }
}
