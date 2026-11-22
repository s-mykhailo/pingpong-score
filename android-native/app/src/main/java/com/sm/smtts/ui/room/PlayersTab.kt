package com.sm.smtts.ui.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.sm.smtts.data.StatsEngine

@Composable
fun PlayersTab(vm: RoomViewModel, isAdmin: Boolean) {
    val data by vm.data.collectAsState()
    val players = data?.players ?: emptyList()
    val matches = data?.matches ?: emptyList()
    var newName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Гравці", style = MaterialTheme.typography.headlineSmall)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Ім'я гравця") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                val name = newName.trim()
                if (name.isBlank()) return@Button
                vm.addPlayer(name) { error = it }
                newName = ""
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Додати")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (players.isEmpty()) {
            Text("Поки немає гравців", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(players) { p -> PlayerRow(p, matches, isAdmin) { vm.deletePlayer(p.id) { msg -> error = msg } } }
            }
        }
    }
}

@Composable
private fun PlayerRow(player: Player, matches: List<com.sm.smtts.data.Match>, isAdmin: Boolean, onDelete: () -> Unit) {
    val stats = StatsEngine.computePlayerStats(matches, player.id)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(player.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${stats.wins}П / ${stats.losses}П · ${(stats.winRate * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (isAdmin) {
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Видалити") }
            }
        }
    }
}
