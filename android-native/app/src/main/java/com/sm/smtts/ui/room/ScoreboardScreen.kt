package com.sm.smtts.ui.room

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.smtts.data.Match
import com.sm.smtts.data.StatsEngine
import com.sm.smtts.ui.components.PressButton
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

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
        val titleScale = remember { Animatable(0.6f) }
        LaunchedEffect(result) {
            titleScale.animateTo(
                1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            )
        }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    "$winnerName переміг!",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.scale(titleScale.value),
                )
                Text(
                    result.sets.joinToString(", ") { "${it.a}:${it.b}" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PressButton(onClick = { vm.clearLiveMatch(); onExit() }) { Text("Готово") }
            }
            ConfettiOverlay(modifier = Modifier.fillMaxSize())
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
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
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
                Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
            }
            Text(name, style = MaterialTheme.typography.titleLarge, color = content)
        }
        AnimatedContent(
            targetState = score,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(220)) { height -> height / 3 } + fadeIn(tween(220)))
                    .togetherWith(slideOutVertically(animationSpec = tween(180)) { height -> -height / 3 } + fadeOut(tween(140)))
            },
            label = "score",
        ) { animatedScore ->
            Text(animatedScore.toString(), fontSize = 96.sp, color = content, style = MaterialTheme.typography.displayLarge)
        }
        if (sets.isNotEmpty()) {
            Text(sets.joinToString(" · "), color = content, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private class ConfettiPiece(
    val xFraction: Float,
    val startDelay: Float,
    val drift: Float,
    val phase: Float,
    val rotationSpeed: Float,
    val pieceSize: Float,
    val color: Color,
)

/** One-shot particle burst over the winner screen — purely decorative, doesn't intercept touches. */
@Composable
private fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFFFFC107),
        Color(0xFF4CAF50),
        Color(0xFF29B6F6),
    )
    val pieces = remember {
        List(70) { i ->
            ConfettiPiece(
                xFraction = Random.nextFloat(),
                startDelay = Random.nextFloat() * 0.35f,
                drift = (Random.nextFloat() - 0.5f) * 160f,
                phase = Random.nextFloat() * 2f * PI.toFloat(),
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                pieceSize = 6f + Random.nextFloat() * 7f,
                color = palette[i % palette.size],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(2400, easing = LinearEasing))
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        for (p in pieces) {
            val local = ((progress.value - p.startDelay) / (1f - p.startDelay)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val y = -40f + local * (h + 80f)
            val x = p.xFraction * w + sin(local * 2f * PI.toFloat() * 1.5f + p.phase) * p.drift
            val alpha = if (local > 0.85f) (1f - (local - 0.85f) / 0.15f).coerceIn(0f, 1f) else 1f
            rotate(degrees = local * p.rotationSpeed, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.pieceSize / 2, y - p.pieceSize),
                    size = Size(p.pieceSize, p.pieceSize * 1.6f),
                )
            }
        }
    }
}
