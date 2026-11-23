package com.sm.smtts.ui.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sm.smtts.data.ThemeStore
import com.sm.smtts.data.friendlyMessage
import com.sm.smtts.ui.screens.AccountSettings
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
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Налаштування", style = MaterialTheme.typography.headlineSmall)

        AccountSettings(session = session, authRepository = authRepository, themeStore = themeStore)

        if (isAdmin) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            inviteCode = roomRepository.createRoomInvite(session.token, roomId)
                        } catch (e: Exception) {
                            error = friendlyMessage(e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Запросити в кімнату") }
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

    inviteCode?.let { code ->
        AlertDialog(
            onDismissRequest = { inviteCode = null },
            title = { Text("Код для приєднання в кімнату") },
            text = {
                SelectionContainer {
                    Text(code, style = MaterialTheme.typography.headlineSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { inviteCode = null }) { Text("Готово") }
            },
        )
    }
}
