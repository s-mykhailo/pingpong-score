package com.sm.smtts.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sm.smtts.BuildConfig
import com.sm.smtts.R
import com.sm.smtts.data.ApiException
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.AvailableUpdate
import com.sm.smtts.data.NetworkException
import com.sm.smtts.data.PendingRoomCreate
import com.sm.smtts.data.PendingRoomJoin
import com.sm.smtts.data.PendingRoomQueue
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.RoomSummary
import com.sm.smtts.data.Session
import com.sm.smtts.data.UpdateChecker
import com.sm.smtts.data.friendlyMessage
import com.sm.smtts.ui.components.PressFab
import com.sm.smtts.ui.components.SegmentedCodeField
import com.sm.smtts.ui.theme.LocalDarkTheme
import com.sm.smtts.ui.theme.SmttsElevation
import com.sm.smtts.ui.theme.TopoBackground
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    session: Session,
    roomRepository: RoomRepository,
    pendingRoomQueue: PendingRoomQueue,
    authRepository: AuthRepository,
    onOpenRoom: (RoomSummary) -> Unit,
    onOpenSettings: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var availableUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }
    var rooms by remember { mutableStateOf<List<RoomSummary>>(emptyList()) }
    var pendingCreates by remember { mutableStateOf<List<PendingRoomCreate>>(emptyList()) }
    var pendingJoins by remember { mutableStateOf<List<PendingRoomJoin>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val canCreateRooms = session.user.role == "admin" || session.user.role == "masterkey"
    val hasContent = rooms.isNotEmpty() || pendingCreates.isNotEmpty() || pendingJoins.isNotEmpty()
    var syncingRooms by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    fun reload(showSpinner: Boolean = true): Job {
        if (showSpinner) loading = true
        return scope.launch {
            try {
                rooms = roomRepository.myRooms(session.token)
                error = null
            } catch (e: Exception) {
                // Якщо вже показуємо старий кеш - не затирати його помилкою, спробуємо пізніше.
                if (rooms.isEmpty()) error = friendlyMessage(e)
            } finally {
                loading = false
            }
        }
    }

    /** Намагається дотягнути до серверу createRoom/joinRoom, що були створені офлайн або
     *  просто не блокуючи UI. Запити, відхилені сервером назавжди (ApiException), видаляються
     *  з черги з повідомленням про помилку; мережеві - лишаються і повторяться наступного разу. */
    fun syncPendingRooms() {
        if (syncingRooms) return
        syncingRooms = true
        scope.launch {
            try {
                for (item in pendingRoomQueue.getCreates(session.token)) {
                    try {
                        roomRepository.createRoom(session.token, item.name)
                        pendingRoomQueue.removeCreate(session.token, item.localId)
                        pendingCreates = pendingCreates.filter { it.localId != item.localId }
                    } catch (e: NetworkException) {
                        break
                    } catch (e: ApiException) {
                        pendingRoomQueue.removeCreate(session.token, item.localId)
                        pendingCreates = pendingCreates.filter { it.localId != item.localId }
                        error = friendlyMessage(e)
                    }
                }
                for (item in pendingRoomQueue.getJoins(session.token)) {
                    try {
                        roomRepository.joinRoom(session.token, item.code)
                        pendingRoomQueue.removeJoin(session.token, item.localId)
                        pendingJoins = pendingJoins.filter { it.localId != item.localId }
                    } catch (e: NetworkException) {
                        break
                    } catch (e: ApiException) {
                        pendingRoomQueue.removeJoin(session.token, item.localId)
                        pendingJoins = pendingJoins.filter { it.localId != item.localId }
                        error = friendlyMessage(e)
                    }
                }
            } finally {
                syncingRooms = false
            }
            reload(showSpinner = false)
        }
    }

    LaunchedEffect(session.token) {
        val cached = roomRepository.cachedRooms(session.token)
        if (cached.isNotEmpty()) {
            rooms = cached
            loading = false
            reload(showSpinner = false)
        } else {
            reload(showSpinner = true)
        }
        pendingCreates = pendingRoomQueue.getCreates(session.token)
        pendingJoins = pendingRoomQueue.getJoins(session.token)
        syncPendingRooms()
        availableUpdate = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
    }

    Scaffold(
        snackbarHost = {
            val update = availableUpdate
            if (update != null) {
                Snackbar(
                    action = {
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.url)))
                        }) { Text("Відкрити") }
                    },
                    dismissAction = {
                        TextButton(onClick = { availableUpdate = null }) { Text("Закрити") }
                    },
                ) { Text("Доступна нова версія ${update.version}") }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canCreateRooms) {
                    PressFab(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Створити кімнату")
                    }
                }
                PressFab(onClick = { showJoinDialog = true }) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = "Приєднатися за кодом")
                }
            }
        },
    ) { padding ->
        val darkTheme = LocalDarkTheme.current
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(if (darkTheme) R.drawable.home_wallpaper_dark else R.drawable.home_wallpaper_light),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            TopoBackground(modifier = Modifier.fillMaxSize(), darkTheme = darkTheme)
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            session.user.nickname,
                            style = MaterialTheme.typography.headlineSmall,
                            color = contentColorFor(MaterialTheme.colorScheme.surface),
                        )
                        Text(
                            roleLabel(session.user.role),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Налаштування")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                authRepository.logout(session.token)
                                onLoggedOut()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Вийти")
                        }
                    }
                }

                if (error != null && hasContent) {
                    Text(
                        error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        refreshing = true
                        scope.launch {
                            reload(showSpinner = false).join()
                            refreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        error != null && !hasContent -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(error ?: "", color = MaterialTheme.colorScheme.error)
                        }
                        !hasContent -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(
                                "Ще немає кімнат. Приєднайся за кодом.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(pendingCreates) { item -> PendingRoomCard(title = item.name, status = "Створюється…") }
                            items(pendingJoins) { item -> PendingRoomCard(title = "Приєднання…", status = "Код: ${item.code}") }
                            items(rooms) { room -> RoomCard(room, onClick = { onOpenRoom(room) }) }
                        }
                    }
                }
            }
        }
    }

    if (showJoinDialog) {
        JoinRoomDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                showJoinDialog = false
                // id/назва кімнати відомі лише після валідації коду на сервері - показуємо
                // плейсхолдер "Приєднання..." і дотягуємо його в фоні, а не тримаємо діалог
                // відкритим на весь запит.
                val item = PendingRoomJoin(localId = UUID.randomUUID().toString(), code = code)
                pendingJoins = pendingJoins + item
                scope.launch {
                    pendingRoomQueue.addJoin(session.token, item)
                    syncPendingRooms()
                }
            },
        )
    }

    if (showCreateDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                // Назва вже відома локально - показуємо кімнату одразу, реальний id підʼїде
                // після відповіді сервера (фоновий синк), діалог не чекає мережі.
                val item = PendingRoomCreate(localId = UUID.randomUUID().toString(), name = name)
                pendingCreates = pendingCreates + item
                scope.launch {
                    pendingRoomQueue.addCreate(session.token, item)
                    syncPendingRooms()
                }
            },
        )
    }
}

@Composable
private fun PendingRoomCard(title: String, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = SmttsElevation.card),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun RoomCard(room: RoomSummary, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = if (pressed) SmttsElevation.cardPressed else SmttsElevation.card,
        animationSpec = tween(120),
        label = "roomCardElevation",
    )
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(room.name, style = MaterialTheme.typography.titleLarge)
            if (room.isAdmin) {
                Text("Адмін кімнати", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нова кімната") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Назва кімнати") },
                singleLine = true,
                enabled = !creating,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    creating = true
                    onCreate(name.trim())
                },
                enabled = !creating && name.isNotBlank(),
            ) { Text("Створити") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) { Text("Скасувати") }
        },
    )
}

@Composable
private fun JoinRoomDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Приєднатися до кімнати") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Код запрошення",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedCodeField(
                    value = code,
                    onValueChange = { code = it },
                    codeLength = 10,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onJoin(code.trim()) }, enabled = code.isNotBlank()) { Text("Приєднатися") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        },
    )
}
