package com.sm.smtts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onAuthed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(0) } // 0 = register, 1 = recover
    var nickname by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var recoveryCodeToShow by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.SportsTennis,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text("SM TTS", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Table Tennis Score",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.size(24.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == 0,
                onClick = { mode = 0; error = null },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Новий профіль") }
            SegmentedButton(
                selected = mode == 1,
                onClick = { mode = 1; error = null },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Відновити") }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))

        if (mode == 0) {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Нікнейм") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = { Text("Код відновлення") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))

        Button(
            onClick = {
                error = null
                loading = true
                scope.launch {
                    try {
                        if (mode == 0) {
                            val (_, recoveryCode) = authRepository.register(nickname.trim().ifBlank { "Гравець" })
                            recoveryCodeToShow = recoveryCode
                        } else {
                            authRepository.recoverAccount(code.trim())
                            onAuthed()
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Щось пішло не так"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && (if (mode == 0) nickname.isNotBlank() else code.isNotBlank()),
            shape = ButtonDefaults.shape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (mode == 0) "Створити профіль" else "Увійти")
            }
        }
    }

    recoveryCodeToShow?.let { recCode ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Збережи код відновлення") },
            text = {
                Column {
                    Text("Цей код показується один раз. Він потрібен, щоб увійти в акаунт з іншого пристрою.")
                    androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                    SelectionContainer {
                        Text(recCode, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    recoveryCodeToShow = null
                    onAuthed()
                }) { Text("Зберіг, продовжити") }
            },
        )
    }
}
