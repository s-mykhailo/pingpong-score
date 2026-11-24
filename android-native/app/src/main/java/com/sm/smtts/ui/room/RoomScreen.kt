package com.sm.smtts.ui.room

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.MatchRepository
import com.sm.smtts.data.PendingQueue
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.Session
import com.sm.smtts.data.ThemeStore
import com.sm.smtts.ui.theme.SmttsNavDurationMillis
import com.sm.smtts.ui.theme.SmttsNavEasing
import com.sm.smtts.ui.theme.TopoBackground
import kotlinx.coroutines.launch

private enum class RoomTab(val label: String) { HOME("Головна"), STATS("Статистика"), PLAYERS("Гравці"), SETTINGS("Налаштування") }
private enum class RoomRoute { TABS, MATCH_SETUP, SCOREBOARD }

private fun RoomRoute.depth() = when (this) {
    RoomRoute.TABS -> 0
    RoomRoute.MATCH_SETUP -> 1
    RoomRoute.SCOREBOARD -> 2
}

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
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = RoomTab.HOME.ordinal) { RoomTab.entries.size }
    val pagerScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        tab = RoomTab.entries[pagerState.currentPage]
    }

    BackHandler(enabled = route != RoomRoute.TABS) { route = RoomRoute.TABS }

    AnimatedContent(
        targetState = route,
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
        label = "roomRoute",
    ) { currentRoute ->
        when (currentRoute) {
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
                        fun goTo(target: RoomTab) {
                            pagerScope.launch {
                                pagerState.animateScrollToPage(
                                    page = target.ordinal,
                                    animationSpec = tween(220),
                                )
                            }
                        }
                        NavigationBarItem(
                            selected = tab == RoomTab.HOME,
                            onClick = { goTo(RoomTab.HOME) },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text(RoomTab.HOME.label) },
                        )
                        NavigationBarItem(
                            selected = tab == RoomTab.STATS,
                            onClick = { goTo(RoomTab.STATS) },
                            icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                            label = { Text(RoomTab.STATS.label) },
                        )
                        NavigationBarItem(
                            selected = tab == RoomTab.PLAYERS,
                            onClick = { goTo(RoomTab.PLAYERS) },
                            icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                            label = { Text(RoomTab.PLAYERS.label) },
                        )
                        NavigationBarItem(
                            selected = tab == RoomTab.SETTINGS,
                            onClick = { goTo(RoomTab.SETTINGS) },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text(RoomTab.SETTINGS.label) },
                        )
                    }
                },
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    TopoBackground(modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.padding(padding)) {
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = {
                                refreshing = true
                                scope.launch {
                                    vm.refresh().join()
                                    refreshing = false
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                when (RoomTab.entries[page]) {
                                    RoomTab.HOME -> RoomHomeTab(
                                        vm = vm,
                                        roomName = roomName,
                                        onNewMatch = { route = RoomRoute.MATCH_SETUP },
                                        onResumeMatch = { route = RoomRoute.SCOREBOARD },
                                    )
                                    RoomTab.STATS -> StatsTab(vm)
                                    RoomTab.PLAYERS -> PlayersTab(
                                        vm = vm,
                                        isAdmin = isAdmin,
                                        session = session,
                                        roomId = roomId,
                                        roomRepository = roomRepository,
                                    )
                                    RoomTab.SETTINGS -> SettingsTab(
                                        session = session,
                                        roomId = roomId,
                                        isAdmin = isAdmin,
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
        }
    }
}
