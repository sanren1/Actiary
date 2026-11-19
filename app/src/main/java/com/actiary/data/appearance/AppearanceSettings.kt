package com.actiary.data.appearance

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class AccentPreset { MIST_TEAL, SLATE_BLUE, SAGE, SAND, DUSTY_LAVENDER, GRAPHITE, CUSTOM }
enum class GlassLevel { SOFT, BALANCED, CLEAR }
enum class GlassTintStrength { LOW, MEDIUM, HIGH }
enum class ChartPalette { NEUTRAL_FIRST, FOLLOW_ACCENT, MUTED_CATEGORIES }

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentPreset: AccentPreset = AccentPreset.SLATE_BLUE,
    val customAccentHex: String = "#77879D",
    val glassLevel: GlassLevel = GlassLevel.BALANCED,
    val glassTintStrength: GlassTintStrength = GlassTintStrength.LOW,
    val chartPalette: ChartPalette = ChartPalette.NEUTRAL_FIRST,
    val ambientBackground: Boolean = true,
)
