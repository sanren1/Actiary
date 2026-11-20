package com.actiary.theme

import com.actiary.data.appearance.ChartPalette

data class ChartColors(val series: Int, val highlight: Int, val categories: List<Int>)

/** Steps lightness (down in light, up in dark) until [ok] holds; mirrors ColorDerivation.stepUntil. */
private fun stepUntil(base: Int, dark: Boolean, ok: (Int) -> Boolean): Int {
    if (ok(base)) return base
    val o = base.toOklch()
    var l = o.l
    while (true) {
        l = (if (dark) l + 0.005 else l - 0.005).coerceIn(0.0, 1.0)
        val candidate = Oklch(l, o.c, o.h).toArgb()
        if (ok(candidate) || l <= 0.0 || l >= 1.0) return candidate
    }
}

private const val CATEGORY_CHROMA = 0.075
private const val CATEGORY_START_L = 0.62

/** Six low-chroma, evenly-hued categories corrected to contrast >= 3:1 with [background]. */
private fun mutedCategories(background: Int, dark: Boolean): List<Int> =
    (0 until 6).map { i ->
        val base = Oklch(CATEGORY_START_L, CATEGORY_CHROMA, i * 60.0).toArgb()
        stepUntil(base, dark) { contrast(it, background) >= 3.0 }
    }

fun resolveChartColors(palette: ChartPalette, t: ThemeTokens): ChartColors {
    val background = t.neutrals.background
    val accent = t.accent.accentChart
    return when (palette) {
        ChartPalette.NEUTRAL_FIRST -> {
            val series = stepUntil(t.neutrals.textSecondary, t.dark) {
                contrast(it, background) >= 3.0
            }
            ChartColors(series, accent, List(6) { series })
        }
        ChartPalette.FOLLOW_ACCENT -> ChartColors(accent, accent, List(6) { accent })
        ChartPalette.MUTED_CATEGORIES -> ChartColors(accent, accent, mutedCategories(background, t.dark))
    }
}
