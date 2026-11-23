package com.sm.smtts.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.MatchRepository
import com.sm.smtts.data.PendingQueue
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.RoomSummary
import com.sm.smtts.data.SessionStore
import com.sm.smtts.data.ThemeStore
import com.sm.smtts.ui.room.RoomScreen
import com.sm.smtts.ui.screens.AccountSettingsScreen
import com.sm.smtts.ui.screens.AuthScreen
import com.sm.smtts.ui.screens.HomeScreen
import kotlinx.coroutines.launch

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
}

@Composable
fun SmttsNavHost(
    sessionStore: SessionStore,
    authRepository: AuthRepository,
    roomRepository: RoomRepository,
    matchRepository: MatchRepository,
    pendingQueue: PendingQueue,
    themeStore: ThemeStore,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val session by sessionStore.session.collectAsState(initial = null)
    // Backed by DataStore and rendered here, outside any single route: saving the session
    // right after register() re-roots the NavHost to Routes.HOME, which would otherwise tear
    // down an in-screen dialog holding this one-time code before the user can read it.
    val pendingRecoveryCode by sessionStore.pendingRecoveryCode.collectAsState(initial = null)

    NavHost(
        navController = navController,
        startDestination = if (session == null) Routes.AUTH else Routes.HOME,
    ) {
        composable(Routes.AUTH) {
            AuthScreen(authRepository = authRepository, onAuthed = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.AUTH) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            val current = session
            if (current == null) {
                AuthScreen(authRepository = authRepository, onAuthed = { })
            } else {
                var selectedRoom by remember { mutableStateOf<RoomSummary?>(null) }
                var showAccountSettings by remember { mutableStateOf(false) }
                val room = selectedRoom

                if (showAccountSettings) {
                    BackHandler { showAccountSettings = false }
                    AccountSettingsScreen(
                        session = current,
                        authRepository = authRepository,
                        themeStore = themeStore,
                        onBack = { showAccountSettings = false },
                        onLoggedOut = {
                            showAccountSettings = false
                            selectedRoom = null
                            navController.navigate(Routes.AUTH) { popUpTo(Routes.HOME) { inclusive = true } }
                        },
                    )
                } else if (room != null) {
                    BackHandler { selectedRoom = null }
                    RoomScreen(
                        session = current,
                        roomId = room.id,
                        roomName = room.name,
                        isAdmin = room.isAdmin,
                        matchRepository = matchRepository,
                        roomRepository = roomRepository,
                        authRepository = authRepository,
                        pendingQueue = pendingQueue,
                        themeStore = themeStore,
                        onLoggedOut = {
                            selectedRoom = null
                            navController.navigate(Routes.AUTH) { popUpTo(Routes.HOME) { inclusive = true } }
                        },
                    )
                } else {
                    HomeScreen(
                        session = current,
                        roomRepository = roomRepository,
                        authRepository = authRepository,
                        onOpenRoom = { selectedRoom = it },
                        onOpenSettings = { showAccountSettings = true },
                        onLoggedOut = {
                            navController.navigate(Routes.AUTH) { popUpTo(Routes.HOME) { inclusive = true } }
                        },
                    )
                }
            }
        }
    }

    pendingRecoveryCode?.let { code ->
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("Збережи код відновлення") },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text("Цей код показується один раз. Він потрібен, щоб увійти в акаунт з іншого пристрою.")
                    androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                    SelectionContainer {
                        Text(code, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { sessionStore.clearPendingRecoveryCode() }
                }) { Text("Зберіг, продовжити") }
            },
        )
    }
}
