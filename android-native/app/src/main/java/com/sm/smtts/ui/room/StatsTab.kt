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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.sm.smtts.data.MvpEntry
import com.sm.smtts.data.PlayerStats
import com.sm.smtts.data.StatsEngine
import com.sm.smtts.ui.theme.SmttsElevation

private const val DAY_MS = 24L * 60 * 60 * 1000

@Composable
fun StatsTab(vm: RoomViewModel) {
    val data by vm.data.collectAsState()
    val players = data?.players ?: emptyList()
    val matches = data?.matches ?: emptyList()

    val leaderboard = players
        .map { StatsEngine.computePlayerStats(matches, it.id) to it.name }
        .filter { it.first.matchesPlayed > 0 }
        .sortedWith(compareByDescending<Pair<PlayerStats, String>> { it.first.wins }.thenByDescending { it.first.winRate })

    val now = System.currentTimeMillis()
    val mvpWeek = StatsEngine.computeMvp(matches, now - 7 * DAY_MS)
    val mvpMonth = StatsEngine.computeMvp(matches, now - 30 * DAY_MS)
    val mvpAllTime = StatsEngine.computeMvp(matches, null)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Статистика", style = MaterialTheme.typography.headlineSmall)
        if (players.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MvpCard("MVP тижня", Icons.Filled.MilitaryTech, mvpWeek, players, Modifier.weight(1f))
                MvpCard("MVP місяця", Icons.Filled.EmojiEvents, mvpMonth, players, Modifier.weight(1f))
                MvpCard("MVP усіх часів", Icons.Filled.WorkspacePremium, mvpAllTime, players, Modifier.weight(1f))
            }
        }
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
private fun MvpCard(label: String, icon: ImageVector, mvp: MvpEntry?, players: List<com.sm.smtts.data.Player>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = SmttsElevation.card),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (mvp != null) {
                Text(
                    StatsEngine.playerName(players, mvp.playerId),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    "${mvp.wins}W / ${mvp.matchesPlayed - mvp.wins}L",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("—", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatsRow(name: String, s: PlayerStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = SmttsElevation.card),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("${(s.winRate * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            }
            val winColor = MaterialTheme.colorScheme.primary
            val lossColor = MaterialTheme.colorScheme.error
            val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = winColor)) { append("${s.wins}W") }
                    append(" / ")
                    withStyle(SpanStyle(color = lossColor)) { append("${s.losses}L") }
                    withStyle(SpanStyle(color = mutedColor)) {
                        append(" · сети ${s.setsWon}:${s.setsLost}")
                        if (s.currentStreak != 0) {
                            append(" · серія ${if (s.currentStreak > 0) "+${s.currentStreak}" else s.currentStreak}")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
