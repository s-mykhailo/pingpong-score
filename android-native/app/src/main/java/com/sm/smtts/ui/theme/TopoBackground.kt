package com.sm.smtts.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val LEVELS = floatArrayOf(-0.55f, -0.4f, -0.25f, -0.1f, 0.05f, 0.2f, 0.35f, 0.5f)
private const val CELL_DP = 32f
private const val NOISE_SCALE = 0.09f
private const val Z_RANGE = 0.6f
private const val LINE_ALPHA = 0.30f
private const val STROKE_DP = 1.3f

private class TopoBuffers {
    var cols = 0
    var rows = 0
    var widthPx = -1
    var heightPx = -1
    var field = FloatArray(0)
    val path = Path()
}

/**
 * Той самий топографічний контурний фон, що й у веб-версії (3D simplex-шум +
 * marching squares, SimplexNoise.kt), портований під мобільний Canvas замість плям
 * DriftingBackground. Значення анімації (t) читається на рівні композиції цього
 * маленького компонента - так само, як раніше в DriftingBackground - тому
 * перекомпоновується лише сам фон, а не весь екран. Буфери (сітка шуму, Path)
 * тримаються в remember і перевикористовуються кожен кадр: жодних алокацій у
 * гарячому циклі, лише перебудова вже існуючих масивів/Path. Анімація йде вперед-назад
 * (Reverse), а не Restart, щоб уникнути візуального "стрибка" шуму в момент цикл-репіту.
 */
@Composable
fun TopoBackground(modifier: Modifier = Modifier, darkTheme: Boolean = LocalDarkTheme.current) {
    val infinite = rememberInfiniteTransition(label = "topo")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "topoZ",
    )

    val buffers = remember { TopoBuffers() }
    val tint = (if (darkTheme) PrimaryDark else PrimaryLight).copy(alpha = LINE_ALPHA)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cellPx = CELL_DP.dp.toPx()
        val widthPx = size.width.toInt()
        val heightPx = size.height.toInt()

        if (widthPx != buffers.widthPx || heightPx != buffers.heightPx) {
            buffers.widthPx = widthPx
            buffers.heightPx = heightPx
            buffers.cols = (size.width / cellPx).toInt() + 2
            buffers.rows = (size.height / cellPx).toInt() + 2
            buffers.field = FloatArray(buffers.cols * buffers.rows)
        }

        val cols = buffers.cols
        val rows = buffers.rows
        val field = buffers.field
        val z = t * Z_RANGE

        for (j in 0 until rows) {
            for (i in 0 until cols) {
                field[j * cols + i] = SimplexNoise.noise3(i * NOISE_SCALE, j * NOISE_SCALE, z)
            }
        }

        val strokeWidth = STROKE_DP.dp.toPx()

        for (level in LEVELS) {
            val path = buffers.path
            path.reset()
            for (j in 0 until rows - 1) {
                for (i in 0 until cols - 1) {
                    val tl = field[j * cols + i]
                    val tr = field[j * cols + i + 1]
                    val br = field[(j + 1) * cols + i + 1]
                    val bl = field[(j + 1) * cols + i]

                    var idx = 0
                    if (tl > level) idx = idx or 8
                    if (tr > level) idx = idx or 4
                    if (br > level) idx = idx or 2
                    if (bl > level) idx = idx or 1
                    if (idx == 0 || idx == 15) continue

                    val x0 = i * cellPx
                    val y0 = j * cellPx
                    val x1 = x0 + cellPx
                    val y1 = y0 + cellPx

                    val topX = lerp(x0, x1, tl, tr, level)
                    val rightY = lerp(y0, y1, tr, br, level)
                    val bottomX = lerp(x0, x1, bl, br, level)
                    val leftY = lerp(y0, y1, tl, bl, level)

                    drawCase(path, idx, topX, y0, x1, rightY, bottomX, y1, x0, leftY)
                }
            }
            drawPath(path, color = tint, style = Stroke(width = strokeWidth))
        }
    }
}

private fun lerp(a: Float, b: Float, va: Float, vb: Float, level: Float): Float {
    val ratio = if (vb == va) 0.5f else (level - va) / (vb - va)
    return a + (b - a) * ratio
}

private fun drawCase(
    path: Path,
    idx: Int,
    topX: Float,
    topY: Float,
    rightX: Float,
    rightY: Float,
    bottomX: Float,
    bottomY: Float,
    leftX: Float,
    leftY: Float,
) {
    fun seg(ax: Float, ay: Float, bx: Float, by: Float) {
        path.moveTo(ax, ay)
        path.lineTo(bx, by)
    }
    when (idx) {
        1, 14 -> seg(leftX, leftY, bottomX, bottomY)
        2, 13 -> seg(bottomX, bottomY, rightX, rightY)
        3, 12 -> seg(leftX, leftY, rightX, rightY)
        4, 11 -> seg(topX, topY, rightX, rightY)
        5 -> {
            seg(leftX, leftY, topX, topY)
            seg(bottomX, bottomY, rightX, rightY)
        }
        6, 9 -> seg(topX, topY, bottomX, bottomY)
        7, 8 -> seg(leftX, leftY, topX, topY)
        10 -> {
            seg(leftX, leftY, bottomX, bottomY)
            seg(topX, topY, rightX, rightY)
        }
    }
}
