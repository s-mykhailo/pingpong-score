package com.sm.smtts.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.MatchRepository
import com.sm.smtts.data.PendingQueue
import com.sm.smtts.data.PendingRoomQueue
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.RoomSummary
import com.sm.smtts.data.SessionStore
import com.sm.smtts.data.ThemeStore
import com.sm.smtts.ui.room.RoomScreen
import com.sm.smtts.ui.screens.AccountSettingsScreen
import com.sm.smtts.ui.screens.AuthScreen
import com.sm.smtts.ui.screens.HomeScreen
import com.sm.smtts.ui.theme.SmttsNavDurationMillis
import com.sm.smtts.ui.theme.SmttsNavEasing
import kotlinx.coroutines.launch

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
}

// Список кімнат, відкрита кімната й налаштування акаунта - це не стек "вперед/назад" зі
// спільною глибиною (кімната й налаштування - обидва на "глибині 1" від списку), тому
// просто перевіряємо, чи повертаємось до списку (менша глибина = назад).
private sealed class HomeContent {
    data object RoomList : HomeContent()
    data object AccountSettings : HomeContent()
    data class Room(val room: RoomSummary) : HomeContent()
}

private fun HomeContent.depth() = if (this is HomeContent.RoomList) 0 else 1

@Composable
fun SmttsNavHost(
    sessionStore: SessionStore,
    authRepository: AuthRepository,
    roomRepository: RoomRepository,
    matchRepository: MatchRepository,
    pendingQueue: PendingQueue,
    pendingRoomQueue: PendingRoomQueue,
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
        composable(
            Routes.AUTH,
            enterTransition = { fadeIn(tween(SmttsNavDurationMillis, easing = SmttsNavEasing)) },
            exitTransition = { fadeOut(tween(SmttsNavDurationMillis, easing = SmttsNavEasing)) },
            popEnterTransition = { fadeIn(tween(SmttsNavDurationMillis, easing = SmttsNavEasing)) },
            popExitTransition = { fadeOut(tween(SmttsNavDurationMillis, easing = SmttsNavEasing)) },
        ) {
            AuthScreen(authRepository = authRepository, onAuthed = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.AUTH) { inclusive = true } }
            })
        }
        composable(
            Routes.HOME,
            enterTransition = { fadeIn(tween(SmttsNavDurationMillis, easing = SmttsNavEasing)) },
            exitTransition = { fadeOut(tween(SmttsNavDurationMillis, easing = SmttsNavEasing)) },
            popEnterTransition = { fadeIn(tween(SmttsNavDurationMillis, easing = SmttsNavEasing)) },
            popExitTransition = { fadeOut(tween(SmttsNavDurationMillis, easing = SmttsNavEasing)) },
        ) {
            val current = session
            if (current == null) {
                AuthScreen(authRepository = authRepository, onAuthed = { })
            } else {
                var selectedRoom by remember { mutableStateOf<RoomSummary?>(null) }
                var showAccountSettings by remember { mutableStateOf(false) }
                val room = selectedRoom

                val content = when {
                    showAccountSettings -> HomeContent.AccountSettings
                    room != null -> HomeContent.Room(room)
                    else -> HomeContent.RoomList
                }

                BackHandler(enabled = content !is HomeContent.RoomList) {
                    showAccountSettings = false
                    selectedRoom = null
                }

                AnimatedContent(
                    targetState = content,
                    transitionSpec = {
                        val forward = targetState.depth() > initialState.depth()
                        val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(SmttsNavDurationMillis, easing = SmttsNavEasing)
                        val fadeSpec = tween<Float>(SmttsNavDurationMillis, easing = SmttsNavEasing)
                        if (forward) {
                            (slideInHorizontally(animationSpec = slideSpec) { it } + fadeIn(fadeSpec))
                                .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { -it } + fadeOut(fadeSpec))
                        } else {
                            (slideInHorizontally(animationSpec = slideSpec) { -it } + fadeIn(fadeSpec))
                                .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { it } + fadeOut(fadeSpec))
                        }
                    },
                    contentKey = { it::class },
                    label = "homeContent",
                ) { currentContent ->
                    val backgroundFrozen = transition.isRunning
                    when (currentContent) {
                        is HomeContent.AccountSettings -> AccountSettingsScreen(
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
                        is HomeContent.Room -> RoomScreen(
                            session = current,
                            roomId = currentContent.room.id,
                            roomName = currentContent.room.name,
                            isAdmin = currentContent.room.isAdmin,
                            matchRepository = matchRepository,
                            roomRepository = roomRepository,
                            authRepository = authRepository,
                            pendingQueue = pendingQueue,
                            themeStore = themeStore,
                            onLoggedOut = {
                                selectedRoom = null
                                navController.navigate(Routes.AUTH) { popUpTo(Routes.HOME) { inclusive = true } }
                            },
                            onExitToRoomList = { selectedRoom = null },
                            backgroundFrozen = backgroundFrozen,
                        )
                        is HomeContent.RoomList -> HomeScreen(
                            session = current,
                            roomRepository = roomRepository,
                            pendingRoomQueue = pendingRoomQueue,
                            authRepository = authRepository,
                            onOpenRoom = { selectedRoom = it },
                            onOpenSettings = { showAccountSettings = true },
                            onLoggedOut = {
                                navController.navigate(Routes.AUTH) { popUpTo(Routes.HOME) { inclusive = true } }
                            },
                            backgroundFrozen = backgroundFrozen,
                        )
                    }
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
                val clipboardManager = LocalClipboardManager.current
                androidx.compose.foundation.layout.Column {
                    Text("Цей код показується один раз. Він потрібен, щоб увійти в акаунт з іншого пристрою.")
                    androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        SelectionContainer {
                            Text(code, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                        }
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(code)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Копіювати код")
                        }
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
