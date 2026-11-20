package com.actiary.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Space the floating bottom chrome occupies: bar (64dp) + gap (12dp) + navigation bar inset, or the inset alone without the bar. */
data class FloatingInsets(val bottom: Dp)

val LocalFloatingInsets = staticCompositionLocalOf { FloatingInsets(0.dp) }
