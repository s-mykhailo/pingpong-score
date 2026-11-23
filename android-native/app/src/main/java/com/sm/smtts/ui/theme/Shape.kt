package com.sm.smtts.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Generous corner radii throughout — the rounder, "soft" look reads closer to iOS
// than Material's default shape scale, while every component stays plain M3.
val SmttsShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

// Default Material3 Card/Button elevation is 0dp — with containerColor close to the
// background that reads as flat. Giving cards a real shadow lifts them off the page.
object SmttsElevation {
    val card = 3.dp
    val cardPressed = 1.dp
    val raised = 6.dp
}
