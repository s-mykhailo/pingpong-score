package com.sm.smtts.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.smtts.data.Match
import com.sm.smtts.data.StatsEngine

@Composable
fun ScoreboardScreen(vm: RoomViewModel, onExit: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val live by vm.liveMatch.collectAsState()
    val data by vm.data.collectAsState()
    var finished by remember { mutableStateOf<Match?>(null) }

    val state = live
    if (state == null && finished == null) {
        onExit()
        return
    }

    val players = data?.players ?: emptyList()
    fun nameOf(id: String) = StatsEngine.playerName(players, id)

    val result = finished
    if (result != null) {
        val winnerName = nameOf(result.winner)
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Text("$winnerName переміг!", style = MaterialTheme.typography.headlineMedium)
                Text(
                    result.sets.joinToString(", ") { "${it.a}:${it.b}" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { vm.clearLiveMatch(); onExit() }) { Text("Готово") }
            }
        }
        return
    }

    state ?: return
    val swapped = vm.isSideSwapped()
    val leftIdx = if (swapped) 1 else 0
    val rightIdx = if (swapped) 0 else 1
    val ids = listOf(state.player1, state.player2)
    val serverOnLeft = state.server == leftIdx

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            ScoreHalf(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                background = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                name = nameOf(ids[leftIdx]),
                score = state.currentSet.forIdx(leftIdx),
                sets = state.finishedSets.map { it.forIdx(leftIdx) },
                serving = serverOnLeft,
                onTap = { vm.addPoint(leftIdx) { m -> finished = m } },
            )
            ScoreHalf(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                background = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                name = nameOf(ids[rightIdx]),
                score = state.currentSet.forIdx(rightIdx),
                sets = state.finishedSets.map { it.forIdx(rightIdx) },
                serving = !serverOnLeft,
                onTap = { vm.addPoint(rightIdx) { m -> finished = m } },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onExit) { Icon(Icons.Filled.Close, contentDescription = "Призупинити") }
            FilledTonalIconButton(onClick = { vm.undo() }, enabled = vm.canUndo()) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
        }
    }
}

@Composable
private fun ScoreHalf(
    modifier: Modifier,
    background: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    name: String,
    score: Int,
    sets: List<Int>,
    serving: Boolean,
    onTap: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(background)
            .clickable(onClick = onTap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (serving) {
                Box(Modifier.size(10.dp).background(content, CircleShape))
            }
            Text(name, style = MaterialTheme.typography.titleLarge, color = content)
        }
        Text(score.toString(), fontSize = 96.sp, color = content, style = MaterialTheme.typography.displayLarge)
        if (sets.isNotEmpty()) {
            Text(sets.joinToString(" · "), color = content, style = MaterialTheme.typography.titleMedium)
        }
    }
}
