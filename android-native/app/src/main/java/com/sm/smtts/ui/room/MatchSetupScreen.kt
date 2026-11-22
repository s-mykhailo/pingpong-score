@file:OptIn(ExperimentalMaterial3Api::class)

package com.sm.smtts.ui.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sm.smtts.data.Player
import com.sm.smtts.data.Preset
import com.sm.smtts.data.Rules

private val DefaultRules = Rules()

@Composable
fun MatchSetupScreen(vm: RoomViewModel, onBack: () -> Unit, onStart: () -> Unit) {
    val data by vm.data.collectAsState()
    val players = data?.players ?: emptyList()
    val presets = data?.presets ?: emptyList()

    var player1 by remember { mutableStateOf("") }
    var player2 by remember { mutableStateOf("") }
    var presetId by remember { mutableStateOf<String?>(null) }
    var customRules by remember { mutableStateOf(DefaultRules) }
    var firstServer by remember { mutableStateOf(0) }
    var newPlayerName by remember { mutableStateOf("") }
    var presetNameToSave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    if (presetId == null && presets.isNotEmpty()) presetId = presets.first().id
    val usingCustom = presetId == "custom" || (presetId == null && presets.isEmpty())
    val selectedPreset = presets.find { it.id == presetId }
    val rules = if (usingCustom) customRules else (selectedPreset?.rules ?: DefaultRules)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
            Text("Новий матч", style = MaterialTheme.typography.headlineSmall)
        }

        Text("Гравці", style = MaterialTheme.typography.titleMedium)
        PlayerDropdown("Гравець 1", players.filter { it.id != player2 }, player1) { player1 = it }
        PlayerDropdown("Гравець 2", players.filter { it.id != player1 }, player2) { player2 = it }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newPlayerName,
                onValueChange = { newPlayerName = it },
                label = { Text("Новий гравець") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                val name = newPlayerName.trim()
                if (name.isBlank()) return@Button
                vm.addPlayer(name) { msg -> error = msg }
                newPlayerName = ""
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Додати")
            }
        }

        Text("Правила", style = MaterialTheme.typography.titleMedium)
        PresetDropdown(presets, presetId, usingCustom) { presetId = it }

        if (usingCustom) {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Очок для перемоги", customRules.pointsToWin, Modifier.weight(1f)) {
                            customRules = customRules.copy(pointsToWin = it)
                        }
                        NumberField("Сетів (нечітне)", customRules.bestOf, Modifier.weight(1f)) {
                            customRules = customRules.copy(bestOf = it)
                        }
                    }
                    Row {
                        RadioRow("Перевага 1", customRules.winBy == 1) { customRules = customRules.copy(winBy = 1) }
                        RadioRow("Перевага 2", customRules.winBy == 2) { customRules = customRules.copy(winBy = 2) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Подача кожні", customRules.serveEvery, Modifier.weight(1f)) {
                            customRules = customRules.copy(serveEvery = it)
                        }
                        NumberField("Подача при рівному", customRules.deuceServeEvery, Modifier.weight(1f)) {
                            customRules = customRules.copy(deuceServeEvery = it)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = presetNameToSave,
                            onValueChange = { presetNameToSave = it },
                            label = { Text("Назва пресету (щоб зберегти)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = {
                            val name = presetNameToSave.trim()
                            if (name.isBlank()) return@Button
                            vm.savePreset(name, customRules, onSaved = { preset ->
                                presetId = preset.id
                                presetNameToSave = ""
                            }, onError = { error = it })
                        }) { Text("Зберегти") }
                    }
                }
            }
        }

        Text("Перша подача", style = MaterialTheme.typography.titleMedium)
        Row {
            RadioRow(players.find { it.id == player1 }?.name ?: "Гравець 1", firstServer == 0) { firstServer = 0 }
            RadioRow(players.find { it.id == player2 }?.name ?: "Гравець 2", firstServer == 1) { firstServer = 1 }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                if (player1.isBlank() || player2.isBlank() || player1 == player2) {
                    error = "Оберіть двох різних гравців."
                    return@Button
                }
                if (rules.bestOf % 2 == 0 || rules.bestOf < 1 || rules.pointsToWin < 1) {
                    error = "Перевірте правила матчу."
                    return@Button
                }
                val presetName = if (usingCustom) "" else (selectedPreset?.name ?: "")
                vm.startMatch(player1, player2, rules, presetName, firstServer)
                onStart()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text("Почати матч")
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun NumberField(label: String, value: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun PlayerDropdown(label: String, options: List<Player>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.find { it.id == selectedId }?.name ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { p ->
                DropdownMenuItem(text = { Text(p.name) }, onClick = { onSelect(p.id); expanded = false })
            }
        }
    }
}

@Composable
private fun PresetDropdown(presets: List<Preset>, selectedId: String?, usingCustom: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (usingCustom) "Кастомні правила" else (presets.find { it.id == selectedId }?.name ?: "")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Пресет") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEach { p ->
                DropdownMenuItem(text = { Text(p.name) }, onClick = { onSelect(p.id); expanded = false })
            }
            DropdownMenuItem(text = { Text("Кастомні правила") }, onClick = { onSelect("custom"); expanded = false })
        }
    }
}
