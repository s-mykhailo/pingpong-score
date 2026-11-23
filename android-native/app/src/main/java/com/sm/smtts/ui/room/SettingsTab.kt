package com.sm.smtts.ui.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.Session
import com.sm.smtts.data.ThemeMode
import com.sm.smtts.data.ThemeStore
import com.sm.smtts.data.friendlyMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    session: Session,
    roomId: String,
    isAdmin: Boolean,
    roomRepository: RoomRepository,
    authRepository: AuthRepository,
    themeStore: ThemeStore,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var codeToShow by remember { mutableStateOf<Pair<String, String>?>(null) } // title to code
    var error by remember { mutableStateOf<String?>(null) }
    var masterKeyInput by remember { mutableStateOf("") }
    var adminCodeInput by remember { mutableStateOf("") }
    val themeMode by themeStore.mode.collectAsState(initial = ThemeMode.SYSTEM)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Налаштування", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(session.user.nickname, style = MaterialTheme.typography.titleMedium)
                Text(roleLabel(session.user.role), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                OutlinedTextField(
                    value = masterKeyInput,
                    onValueChange = { masterKeyInput = it },
                    label = { Text("Master key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        error = null
                        scope.launch {
                            try {
                                authRepository.claimMasterkey(session.token, masterKeyInput.trim())
                                masterKeyInput = ""
                            } catch (e: Exception) {
                                error = friendlyMessage(e)
                            }
                        }
                    },
                    enabled = masterKeyInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Активувати") }
            }
        }

        if (session.user.role == "user") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Активувати адмінку", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = adminCodeInput,
                    onValueChange = { adminCodeInput = it.uppercase() },
                    label = { Text("Код від мастер-кея") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        error = null
                        scope.launch {
                            try {
                                authRepository.claimAdmin(session.token, adminCodeInput.trim())
                                adminCodeInput = ""
                            } catch (e: Exception) {
                                error = friendlyMessage(e)
                            }
                        }
                    },
                    enabled = adminCodeInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Активувати") }
            }
        }

        if (isAdmin) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            val code = roomRepository.createRoomInvite(session.token, roomId)
                            codeToShow = "Код для приєднання в кімнату" to code
                        } catch (e: Exception) {
                            error = friendlyMessage(e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Запросити в кімнату") }
        }

        if (session.user.role == "masterkey") {
            OutlinedButton(
                onClick = {
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

        Button(
            onClick = {
                scope.launch {
                    authRepository.logout(session.token)
                    onLoggedOut()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Text("Вийти")
        }
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

private fun roleLabel(role: String) = when (role) {
    "masterkey" -> "Мастерки"
    "admin" -> "Адмін"
    else -> "Гравець"
}
