package com.sm.smtts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.Session
import com.sm.smtts.data.friendlyMessage
import com.sm.smtts.ui.components.IdenticonAvatar
import kotlinx.coroutines.launch

/**
 * Opened by tapping the nickname on the room list. Only account identity (nickname + its
 * derived avatar) lives here - everything else (theme, master key, admin codes, about) stays
 * in [AccountSettingsScreen] behind the gear icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDialog(
    session: Session,
    authRepository: AuthRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var nicknameInput by remember(session.user.nickname) { mutableStateOf(session.user.nickname) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Акаунт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IdenticonAvatar(
                        seedText = nicknameInput.ifBlank { session.user.nickname },
                        modifier = Modifier.size(56.dp),
                    )
                    Text(roleLabel(session.user.role), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    label = { Text("Нікнейм") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = null
                    busy = true
                    scope.launch {
                        try {
                            authRepository.updateProfile(session.token, nicknameInput.trim(), session.user.avatar)
                            onDismiss()
                        } catch (e: Exception) {
                            error = friendlyMessage(e)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && nicknameInput.isNotBlank() && nicknameInput != session.user.nickname,
            ) { Text("Зберегти") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Скасувати") }
        },
    )
}
