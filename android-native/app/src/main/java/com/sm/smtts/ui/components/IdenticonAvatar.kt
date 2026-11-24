package com.sm.smtts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.zip.CRC32

/** Grid size of the identicon pattern (odd so the middle column is the mirror axis). */
private const val GRID_SIZE = 5

/**
 * GitHub-style identicon: the same [seedText] always renders the same picture, nothing is
 * stored or transmitted. A CRC32 of the string seeds both the 5x5 symmetric cell pattern and
 * an HSV color, so two different nicknames reliably look different while one nickname never
 * changes appearance.
 */
@Composable
fun IdenticonAvatar(
    seedText: String,
    modifier: Modifier = Modifier,
) {
    val cells = remember(seedText) { identiconCells(seedText) }
    val color = remember(seedText) { identiconColor(seedText) }
    Canvas(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
    ) {
        val background = Color(0xFFE8E8EC)
        drawRect(color = background, size = size)

        val cellSize = Size(size.width / GRID_SIZE, size.height / GRID_SIZE)
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                if (cells[row * GRID_SIZE + col]) {
                    drawRect(
                        color = color,
                        topLeft = Offset(col * cellSize.width, row * cellSize.height),
                        size = cellSize,
                    )
                }
            }
        }
    }
}

/**
 * Deterministic 32-bit seed for [text]: CRC32 is enough here (not cryptographic, just needs
 * to be stable and spread bits across the input), and it needs no extra dependency.
 */
private fun seedOf(text: String): Long {
    val crc = CRC32()
    crc.update(text.toByteArray(Charsets.UTF_8))
    return crc.value
}

/**
 * Only the left half (plus the middle column) is derived from the seed; the right half mirrors
 * it, which is what gives identicons their recognizable symmetric-blob look. Each cell gets its
 * own CRC32 (seed text + position) rather than slicing bits off one hash, so the grid isn't
 * limited by how many bits a single 32-bit value can supply.
 */
private fun identiconCells(seedText: String): BooleanArray {
    val cells = BooleanArray(GRID_SIZE * GRID_SIZE)
    val half = (GRID_SIZE + 1) / 2 // columns 0..half-1 are authoritative, mirrored onto the rest
    for (row in 0 until GRID_SIZE) {
        for (col in 0 until half) {
            val on = (seedOf("$seedText:$row:$col") and 1L) == 1L
            cells[row * GRID_SIZE + col] = on
            cells[row * GRID_SIZE + (GRID_SIZE - 1 - col)] = on
        }
    }
    return cells
}

private fun identiconColor(seedText: String): Color {
    val seed = seedOf(seedText)
    val hue = (seed % 360).toFloat()
    val saturation = 0.55f
    val value = 0.85f
    return Color.hsv(hue, saturation, value)
}
