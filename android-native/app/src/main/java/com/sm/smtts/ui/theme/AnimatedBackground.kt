package com.sm.smtts.ui.theme

import androidx.compose.animation.core.InfiniteRepeatableSpec
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

/**
 * Повільний, малоконтрастний шар із трьома дрейфуючими плямами градієнта - фонова
 * текстура, а не елемент уваги. Читає значення анімації лише всередині draw-фази
 * (DrawScope), тому рухається тільки сам Canvas - решта екрана не перекомпоновується.
 */
@Composable
fun DriftingBackground(modifier: Modifier = Modifier, darkTheme: Boolean = LocalDarkTheme.current) {
    val infinite = rememberInfiniteTransition(label = "drift")
    val t1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = driftSpec(42_000),
        label = "drift1",
    )
    val t2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = driftSpec(57_000),
        label = "drift2",
    )
    val t3 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = driftSpec(65_000),
        label = "drift3",
    )

    val blobAlpha = if (darkTheme) 0.05f else 0.035f
    val colors = if (darkTheme) {
        listOf(TertiaryDark, PrimaryDark, SecondaryDark)
    } else {
        listOf(TertiaryLight, PrimaryLight, SecondaryLight)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawBlob(center(0.30f, 0.22f, t1, 0.08f, 0.10f), size.minDimension * 0.55f, colors[0], blobAlpha)
        drawBlob(center(0.75f, 0.68f, t2, 0.10f, 0.07f), size.minDimension * 0.65f, colors[1], blobAlpha)
        drawBlob(center(0.40f, 0.85f, t3, 0.07f, 0.09f), size.minDimension * 0.50f, colors[2], blobAlpha)
    }
}

private fun driftSpec(durationMillis: Int): InfiniteRepeatableSpec<Float> =
    infiniteRepeatable(
        animation = tween(durationMillis = durationMillis, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
    )

private fun DrawScope.center(baseX: Float, baseY: Float, t: Float, ampX: Float, ampY: Float): Offset {
    val x = (baseX + ampX * cos(t.toDouble()).toFloat()) * this.size.width
    val y = (baseY + ampY * sin(t.toDouble()).toFloat()) * this.size.height
    return Offset(x, y)
}

private fun DrawScope.drawBlob(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
