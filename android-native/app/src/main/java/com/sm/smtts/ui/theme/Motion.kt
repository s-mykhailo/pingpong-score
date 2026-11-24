package com.sm.smtts.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/**
 * Спільні параметри для переходів між екранами (RoomScreen, SmttsNavHost) - один
 * "сильний" ease-in-out і одна тривалість на весь застосунок, щоб усі переходи
 * відчувались як одна система, а не набір випадкових анімацій.
 */
val SmttsNavEasing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
const val SmttsNavDurationMillis = 280
