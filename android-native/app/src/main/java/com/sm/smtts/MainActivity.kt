package com.sm.smtts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sm.smtts.ui.nav.SmttsNavHost
import com.sm.smtts.ui.theme.SmttsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SmttsApp
        setContent {
            SmttsTheme {
                SmttsNavHost(
                    sessionStore = app.sessionStore,
                    authRepository = app.authRepository,
                    roomRepository = app.roomRepository,
                    matchRepository = app.matchRepository,
                    pendingQueue = app.pendingQueue,
                )
            }
        }
    }
}
