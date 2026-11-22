package com.sm.smtts.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.MatchRepository
import com.sm.smtts.data.PendingQueue
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.RoomSummary
import com.sm.smtts.data.SessionStore
import com.sm.smtts.ui.room.RoomScreen
import com.sm.smtts.ui.screens.AuthScreen
import com.sm.smtts.ui.screens.HomeScreen

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
) {
    val navController = rememberNavController()
    val session by sessionStore.session.collectAsState(initial = null)

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
                val room = selectedRoom

                if (room != null) {
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
                        onLoggedOut = {
                            navController.navigate(Routes.AUTH) { popUpTo(Routes.HOME) { inclusive = true } }
                        },
                    )
                }
            }
        }
    }
}
