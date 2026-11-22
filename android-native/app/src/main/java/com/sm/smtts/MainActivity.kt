package com.sm.smtts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sm.smtts.data.ThemeMode
import com.sm.smtts.ui.nav.SmttsNavHost
import com.sm.smtts.ui.theme.SmttsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SmttsApp
        setContent {
            val themeMode by app.themeStore.mode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            SmttsTheme(darkTheme = darkTheme) {
                SmttsNavHost(
                    sessionStore = app.sessionStore,
                    authRepository = app.authRepository,
                    roomRepository = app.roomRepository,
                    matchRepository = app.matchRepository,
                    pendingQueue = app.pendingQueue,
                    themeStore = app.themeStore,
                )
            }
        }
    }
}
