package com.sm.smtts.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sm.smtts.R
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.friendlyMessage
import com.sm.smtts.ui.components.PressButton
import com.sm.smtts.ui.components.SegmentedCodeField
import com.sm.smtts.ui.theme.LocalDarkTheme
import com.sm.smtts.ui.theme.SmttsElevation
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onAuthed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(0) } // 0 = register, 1 = recover
    var nickname by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val darkTheme = LocalDarkTheme.current
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(if (darkTheme) R.drawable.home_wallpaper_dark else R.drawable.home_wallpaper_light),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
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

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(defaultElevation = SmttsElevation.raised),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                    if (mode == 0) {
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = { Text("Нікнейм") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            "Код відновлення",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SegmentedCodeField(
                            value = code,
                            onValueChange = { code = it },
                            codeLength = 14,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    PressButton(
                        onClick = {
                            error = null
                            loading = true
                            scope.launch {
                                try {
                                    if (mode == 0) {
                                        authRepository.register(nickname.trim().ifBlank { "Гравець" })
                                    } else {
                                        authRepository.recoverAccount(code.trim())
                                    }
                                    onAuthed()
                                } catch (e: Exception) {
                                    error = friendlyMessage(e)
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
            }
        }
    }
}
