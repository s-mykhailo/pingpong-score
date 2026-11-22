package com.sm.smtts.ui.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sm.smtts.data.PlayerStats
import com.sm.smtts.data.StatsEngine

@Composable
fun StatsTab(vm: RoomViewModel) {
    val data by vm.data.collectAsState()
    val players = data?.players ?: emptyList()
    val matches = data?.matches ?: emptyList()

    val leaderboard = players
        .map { StatsEngine.computePlayerStats(matches, it.id) to it.name }
        .filter { it.first.matchesPlayed > 0 }
        .sortedWith(compareByDescending<Pair<PlayerStats, String>> { it.first.wins }.thenByDescending { it.first.winRate })

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Статистика", style = MaterialTheme.typography.headlineSmall)
        if (leaderboard.isEmpty()) {
            Text("Ще немає зіграних матчів", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(leaderboard) { (stats, name) -> StatsRow(name, stats) }
            }
        }
    }
}

@Composable
private fun StatsRow(name: String, s: PlayerStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("${(s.winRate * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "${s.wins}П / ${s.losses}П · сети ${s.setsWon}:${s.setsLost}" +
                    if (s.currentStreak != 0) " · серія ${if (s.currentStreak > 0) "+${s.currentStreak}" else s.currentStreak}" else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
