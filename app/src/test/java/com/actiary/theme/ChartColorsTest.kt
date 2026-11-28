package com.actiary.theme

import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ChartPalette
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.ThemeMode
import com.actiary.data.appearance.UiStyle
import com.actiary.p5.resolveP5Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ChartColorsTest {
    private val seeds = listOf(
        "#758B84", "#77879D", "#AC9680",
        "#FFFFFF", "#000000", "#FFFF00", "#00FF00", "#FF00FF",
    )
    private val palettes = ChartPalette.entries

    private fun tokens(hex: String, dark: Boolean): ThemeTokens = resolveTheme(
        AppearanceSettings(
            accentPreset = AccentPreset.CUSTOM,
            customAccentHex = hex,
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
        ),
        systemDark = dark,
    )

    private fun hueDiff(a: Double, b: Double) = abs(a - b).let { minOf(it, 360 - it) }

    @Test fun seriesContrastAtLeastThree() {
        for (hex in seeds) for (dark in listOf(false, true)) {
            val t = tokens(hex, dark)
            for (palette in palettes) {
                val colors = resolveChartColors(palette, t)
                val c = contrast(colors.series, t.neutrals.background)
                assertTrue(
                    "$hex dark=$dark palette=$palette series contrast=$c < 3",
                    c >= 3.0,
                )
            }
        }
    }

    @Test fun categoryContrastAtLeastThree() {
        for (hex in seeds) for (dark in listOf(false, true)) {
            val t = tokens(hex, dark)
            for (palette in palettes) {
                val colors = resolveChartColors(palette, t)
                colors.categories.forEachIndexed { i, cat ->
                    val c = contrast(cat, t.neutrals.background)
                    assertTrue(
                        "$hex dark=$dark palette=$palette category[$i] contrast=$c < 3",
                        c >= 3.0,
                    )
                }
            }
        }
    }

    @Test fun categoriesHasAtLeastSixEntries() {
        for (hex in seeds) for (dark in listOf(false, true)) {
            val t = tokens(hex, dark)
            for (palette in palettes) {
                val colors = resolveChartColors(palette, t)
                assertTrue(
                    "$hex dark=$dark palette=$palette categories size=${colors.categories.size} < 6",
                    colors.categories.size >= 6,
                )
            }
        }
    }

    @Test fun mutedCategoriesArePairwiseDistinguishable() {
        for (hex in seeds) for (dark in listOf(false, true)) {
            val t = tokens(hex, dark)
            val cats = resolveChartColors(ChartPalette.MUTED_CATEGORIES, t).categories
            for (i in cats.indices) for (j in i + 1 until cats.size) {
                val a = cats[i].toOklch()
                val b = cats[j].toOklch()
                val dh = hueDiff(a.h, b.h)
                val dl = abs(a.l - b.l)
                assertTrue(
                    "$hex dark=$dark pair($i,$j) hueDiff=$dh lightDiff=$dl not distinguishable",
                    dh >= 40.0 || dl >= 0.1,
                )
            }
        }
    }

    @Test fun mutedCategoriesAreLowChroma() {
        for (hex in seeds) for (dark in listOf(false, true)) {
            val t = tokens(hex, dark)
            val cats = resolveChartColors(ChartPalette.MUTED_CATEGORIES, t).categories
            cats.forEachIndexed { i, cat ->
                val c = cat.toOklch().c
                assertTrue("$hex dark=$dark category[$i] chroma=$c > 0.08", c <= 0.08)
            }
        }
    }

    @Test fun paletteColorContracts() {
        for (hex in seeds) for (dark in listOf(false, true)) {
            val t = tokens(hex, dark)
            val neutral = resolveChartColors(ChartPalette.NEUTRAL_FIRST, t)
            assertTrue(neutral.categories.all { it == neutral.series })
            assertEquals(t.accent.accentChart, neutral.highlight)

            val accent = resolveChartColors(ChartPalette.FOLLOW_ACCENT, t)
            assertEquals(t.accent.accentChart, accent.series)
            assertEquals(t.accent.accentChart, accent.highlight)
            assertTrue(accent.categories.all { it == t.accent.accentChart })

            val muted = resolveChartColors(ChartPalette.MUTED_CATEGORIES, t)
            assertEquals(t.accent.accentChart, muted.series)
            assertEquals(t.accent.accentChart, muted.highlight)
        }
    }

    @Test fun p5ChartColorsMeetContrastOnLightCard() {
        assertTrue(P5Palette.CALLING_CARD in P5Palette.entries)
        for (palette in P5Palette.entries) {
            val p5 = resolveP5Tokens(AppearanceSettings(uiStyle = UiStyle.PERSONA_5, p5Palette = palette))
            val colors = resolveP5ChartColors(p5)
            val seriesContrast = contrast(colors.series, p5.lightCard)
            assertTrue(
                "palette=$palette series contrast=$seriesContrast < 3.0",
                seriesContrast >= 3.0,
            )
            val highlightContrast = contrast(colors.highlight, p5.lightCard)
            assertTrue(
                "palette=$palette highlight contrast=$highlightContrast < 3.0",
                highlightContrast >= 3.0,
            )
            assertTrue(
                "palette=$palette categories size=${colors.categories.size} < 6",
                colors.categories.size >= 6,
            )
            colors.categories.forEachIndexed { i, cat ->
                val c = contrast(cat, p5.lightCard)
                assertTrue(
                    "palette=$palette category[$i] contrast=$c < 3.0",
                    c >= 3.0,
                )
            }
        }
    }
}
