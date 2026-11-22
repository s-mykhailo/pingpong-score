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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(
    session: Session,
    roomId: String,
    isAdmin: Boolean,
    roomRepository: RoomRepository,
    authRepository: AuthRepository,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var codeToShow by remember { mutableStateOf<Pair<String, String>?>(null) } // title to code
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Налаштування", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(session.user.nickname, style = MaterialTheme.typography.titleMedium)
                Text(roleLabel(session.user.role), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            error = e.message
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
                            error = e.message
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
