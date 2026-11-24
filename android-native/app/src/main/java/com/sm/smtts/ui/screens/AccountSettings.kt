package com.sm.smtts.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sm.smtts.BuildConfig
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.Session
import com.sm.smtts.data.ThemeMode
import com.sm.smtts.data.ThemeStore
import com.sm.smtts.data.friendlyMessage
import com.sm.smtts.ui.components.MaskedCodeField
import com.sm.smtts.ui.components.PressButton
import com.sm.smtts.ui.theme.SmttsElevation
import kotlinx.coroutines.launch

/** Full-screen wrapper around [AccountSettings], reachable from the room list (no room required). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    session: Session,
    authRepository: AuthRepository,
    themeStore: ThemeStore,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налаштування") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
        ) {
            AccountSettings(
                session = session,
                authRepository = authRepository,
                themeStore = themeStore,
                modifier = Modifier.fillMaxWidth(),
            )
            PressButton(
                onClick = {
                    scope.launch {
                        authRepository.logout(session.token)
                        onLoggedOut()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("Вийти")
            }
        }
    }
}

/**
 * Account-level settings, shared by the global profile screen and the in-room settings tab.
 * Everything here is about the signed-in user, not about any one room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettings(
    session: Session,
    authRepository: AuthRepository,
    themeStore: ThemeStore,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val themeMode by themeStore.mode.collectAsState(initial = ThemeMode.SYSTEM)
    var masterKeyInput by remember { mutableStateOf("") }
    var adminCodeInput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var codeToShow by remember { mutableStateOf<Pair<String, String>?>(null) } // title to code

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = SmttsElevation.card),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(session.user.nickname, style = MaterialTheme.typography.titleMedium)
                Text(roleLabel(session.user.role), color = roleAccentColor(session.user.role))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Тема", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { scope.launch { themeStore.setMode(ThemeMode.SYSTEM) } },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                ) { Text("Системна") }
                SegmentedButton(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { scope.launch { themeStore.setMode(ThemeMode.LIGHT) } },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                ) { Text("Світла") }
                SegmentedButton(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { scope.launch { themeStore.setMode(ThemeMode.DARK) } },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                ) { Text("Темна") }
            }
        }

        if (session.user.role != "masterkey") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Активувати мастер-ключ", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Мастер-ключ задається в Script Properties бекенду. Дає право створювати кімнати і видавати адмінку.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = masterKeyInput,
                    onValueChange = { masterKeyInput = it },
                    label = { Text("Master key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PressButton(
                    onClick = {
                        error = null
                        notice = null
                        busy = true
                        scope.launch {
                            try {
                                authRepository.claimMasterkey(session.token, masterKeyInput.trim())
                                masterKeyInput = ""
                                notice = "Мастер-ключ активовано."
                            } catch (e: Exception) {
                                error = friendlyMessage(e)
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && masterKeyInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Активувати") }
            }
        }

        if (session.user.role == "user") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Активувати адмінку", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Код від мастер-кея",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MaskedCodeField(
                    value = adminCodeInput,
                    onValueChange = { adminCodeInput = it },
                    codeLength = 10,
                    modifier = Modifier.fillMaxWidth(),
                )
                PressButton(
                    onClick = {
                        error = null
                        notice = null
                        busy = true
                        scope.launch {
                            try {
                                authRepository.claimAdmin(session.token, adminCodeInput.trim())
                                adminCodeInput = ""
                                notice = "Адмінку активовано."
                            } catch (e: Exception) {
                                error = friendlyMessage(e)
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && adminCodeInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Активувати") }
            }
        }

        if (session.user.role == "masterkey") {
            OutlinedButton(
                onClick = {
                    error = null
                    scope.launch {
                        try {
                            val code = authRepository.createAdminInvite(session.token)
                            codeToShow = "Код для видачі адмінки" to code
                        } catch (e: Exception) {
                            error = friendlyMessage(e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Видати адмінку") }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        InviteFriendSection()
        AboutSection()
    }

    codeToShow?.let { (title, code) ->
        AlertDialog(
            onDismissRequest = { codeToShow = null },
            title = { Text(title) },
            text = {
                SelectionContainer {
                    Text(code, style = MaterialTheme.typography.headlineSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { codeToShow = null }) { Text("Готово") }
            },
        )
    }
}

internal fun roleLabel(role: String) = when (role) {
    "masterkey" -> "Master"
    "admin" -> "Admin"
    else -> "User"
}

/** Distinct accent per role, matching the role's "privilege level" - masterkey > admin > user. */
@Composable
internal fun roleAccentColor(role: String) = when (role) {
    "masterkey" -> MaterialTheme.colorScheme.tertiary
    "admin" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private const val WEB_VERSION_URL = "https://s-mykhailo.github.io/pingpong-score/"
private const val TELEGRAM_CHANNEL_URL = "https://t.me/s_mykhailo_dev"

/** "Запросити друга" - ділиться посиланням на веб-версію або канал через системний шер-лист. */
@Composable
private fun InviteFriendSection() {
    val context = LocalContext.current
    fun shareUrl(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Поділитися"))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Запросити друга", style = MaterialTheme.typography.titleMedium)
        Text(
            "Надішли посилання на веб-версію SM TTS, щоб друг міг переглянути рахунок без встановлення застосунку.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PressButton(
            onClick = { shareUrl("Переглянь рахунок у SM TTS: $WEB_VERSION_URL") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Поділитися веб-версією") }
        OutlinedButton(
            onClick = { shareUrl("SM TTS - трекер рахунку настільного тенісу. Канал: $TELEGRAM_CHANNEL_URL Веб-версія: $WEB_VERSION_URL") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Поділитися каналом і веб-версією") }
    }
}

/** "Про додаток" - версія збірки та посилання на Telegram-канал і автора. */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Про додаток", style = MaterialTheme.typography.titleMedium)
        Text(
            "Версія ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = { openUrl(WEB_VERSION_URL) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Веб-версія") }
        TextButton(
            onClick = { openUrl(TELEGRAM_CHANNEL_URL) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Telegram-канал") }
        TextButton(
            onClick = { openUrl("https://t.me/savchenko_mykhailo") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Автор у Telegram") }
    }
}
