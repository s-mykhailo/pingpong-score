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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sm.smtts.data.Match
import com.sm.smtts.data.Player
import com.sm.smtts.data.StatsEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RoomHomeTab(vm: RoomViewModel, roomName: String, onNewMatch: () -> Unit, onResumeMatch: () -> Unit) {
    val data by vm.data.collectAsState()
    val live by vm.liveMatch.collectAsState()
    val players = data?.players ?: emptyList()
    val matches = data?.let { StatsEngine.sortedMatchHistory(it.matches).take(10) } ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(roomName, style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { vm.refresh() }) { Icon(Icons.Filled.Refresh, contentDescription = "Оновити") }
        }

        if (live != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Матч триває", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${StatsEngine.playerName(players, live!!.player1)} проти ${StatsEngine.playerName(players, live!!.player2)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onResumeMatch) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("Продовжити")
                    }
                }
            }
        } else {
            Button(onClick = onNewMatch, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Новий матч")
            }
        }

        Text("Останні матчі", style = MaterialTheme.typography.titleMedium)
        if (matches.isEmpty()) {
            Text("Матчів ще немає", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(matches) { m -> MatchRow(m, players) }
            }
        }
    }
}

@Composable
private fun MatchRow(m: Match, players: List<Player>) {
    val p1 = StatsEngine.playerName(players, m.player1)
    val p2 = StatsEngine.playerName(players, m.player2)
    val winnerIsP1 = m.winner == m.player1
    val setsWon1 = m.sets.count { it.a > it.b }
    val setsWon2 = m.sets.size - setsWon1
    val date = remember(m.playedAt) { formatDate(m.playedAt) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${if (winnerIsP1) p1 else p2} переміг ${if (winnerIsP1) p2 else p1}")
                Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "$setsWon1:$setsWon2 (${m.sets.joinToString(", ") { "${it.a}:${it.b}" }})",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun formatDate(iso: String): String = try {
    val instant = java.time.Instant.parse(iso)
    SimpleDateFormat("dd.MM", Locale.forLanguageTag("uk-UA")).format(Date.from(instant))
} catch (e: Exception) {
    ""
}
