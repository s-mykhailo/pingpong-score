package com.sm.smtts.ui.room

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.MatchRepository
import com.sm.smtts.data.PendingQueue
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.Session
import com.sm.smtts.data.ThemeStore
import com.sm.smtts.ui.theme.DriftingBackground

private enum class RoomTab(val label: String) { HOME("Головна"), STATS("Статистика"), PLAYERS("Гравці"), SETTINGS("Налаштування") }
private enum class RoomRoute { TABS, MATCH_SETUP, SCOREBOARD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    session: Session,
    roomId: String,
    roomName: String,
    isAdmin: Boolean,
    matchRepository: MatchRepository,
    roomRepository: RoomRepository,
    authRepository: AuthRepository,
    pendingQueue: PendingQueue,
    themeStore: ThemeStore,
    onLoggedOut: () -> Unit,
    onExitToRoomList: () -> Unit,
) {
    val vm: RoomViewModel = viewModel(
        key = "room-$roomId",
        factory = RoomViewModel.factory(session.token, roomId, matchRepository, pendingQueue),
    )
    var route by remember { mutableStateOf(RoomRoute.TABS) }
    var tab by remember { mutableStateOf(RoomTab.HOME) }

    when (route) {
        RoomRoute.MATCH_SETUP -> MatchSetupScreen(
            vm = vm,
            onBack = { route = RoomRoute.TABS },
            onStart = { route = RoomRoute.SCOREBOARD },
        )
        RoomRoute.SCOREBOARD -> ScoreboardScreen(vm = vm, onExit = { route = RoomRoute.TABS })
        RoomRoute.TABS -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(roomName) },
                    navigationIcon = {
                        IconButton(onClick = onExitToRoomList) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == RoomTab.HOME,
                        onClick = { tab = RoomTab.HOME },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(RoomTab.HOME.label) },
                    )
                    NavigationBarItem(
                        selected = tab == RoomTab.STATS,
                        onClick = { tab = RoomTab.STATS },
                        icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                        label = { Text(RoomTab.STATS.label) },
                    )
                    NavigationBarItem(
                        selected = tab == RoomTab.PLAYERS,
                        onClick = { tab = RoomTab.PLAYERS },
                        icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                        label = { Text(RoomTab.PLAYERS.label) },
                    )
                    NavigationBarItem(
                        selected = tab == RoomTab.SETTINGS,
                        onClick = { tab = RoomTab.SETTINGS },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(RoomTab.SETTINGS.label) },
                    )
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                DriftingBackground(modifier = Modifier.fillMaxSize())
                AnimatedContent(
                    modifier = Modifier.padding(padding),
                    targetState = tab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val towards = if (forward) 1 else -1
                        (slideInHorizontally(animationSpec = tween(220)) { width -> towards * width / 4 } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { width -> -towards * width / 4 } + fadeOut(tween(140)))
                    },
                    label = "room-tab",
                ) { targetTab ->
                    when (targetTab) {
                        RoomTab.HOME -> RoomHomeTab(
                            vm = vm,
                            roomName = roomName,
                            onNewMatch = { route = RoomRoute.MATCH_SETUP },
                            onResumeMatch = { route = RoomRoute.SCOREBOARD },
                        )
                        RoomTab.STATS -> StatsTab(vm)
                        RoomTab.PLAYERS -> PlayersTab(vm, isAdmin)
                        RoomTab.SETTINGS -> SettingsTab(
                            session = session,
                            roomId = roomId,
                            isAdmin = isAdmin,
                            roomRepository = roomRepository,
                            authRepository = authRepository,
                            themeStore = themeStore,
                            onLoggedOut = onLoggedOut,
                        )
                    }
                }
            }
        }
    }
}
