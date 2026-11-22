package com.actiary.glass

internal fun sheetScrimAlpha(offsetPx: Float, heightPx: Float): Float =
    if (heightPx > 0f) (1f - offsetPx / heightPx).coerceIn(0f, 1f) else 1f

internal fun shouldDismissSheet(offsetPx: Float, heightPx: Float, velocity: Float): Boolean =
    offsetPx > heightPx / 3f || velocity > 1500f
