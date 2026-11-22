package com.sm.smtts.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.SessionStore
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
) {
    val navController = rememberNavController()
    val session by sessionStore.session.collectAsState(initial = null)

    NavHost(
        navController = navController,
        startDestination = if (session == null) Routes.AUTH else Routes.HOME,
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                authRepository = authRepository,
                onAuthed = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            val current = session
            if (current == null) {
                AuthScreen(
                    authRepository = authRepository,
                    onAuthed = { },
                )
            } else {
                HomeScreen(
                    session = current,
                    roomRepository = roomRepository,
                    authRepository = authRepository,
                    onLoggedOut = {
                        navController.navigate(Routes.AUTH) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
