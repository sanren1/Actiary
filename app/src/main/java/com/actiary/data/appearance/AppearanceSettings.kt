package com.actiary.data.appearance

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class AccentPreset { DYNAMIC, MIST_TEAL, SLATE_BLUE, SAGE, SAND, DUSTY_LAVENDER, GRAPHITE, CUSTOM }
enum class GlassLevel { SOFT, BALANCED, CLEAR }
enum class GlassTintStrength { LOW, MEDIUM, HIGH }
enum class ChartPalette { NEUTRAL_FIRST, FOLLOW_ACCENT, MUTED_CATEGORIES }
enum class UiStyle { LIQUID_GLASS, PERSONA_5 }
enum class P5Palette { PHANTOM_RED, ROYAL_GOLD, VELVET_BLUE, CALLING_CARD }
enum class P5MotionLevel { STANDARD, COMPACT }

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentPreset: AccentPreset = AccentPreset.SLATE_BLUE,
    val customAccentHex: String = "#77879D",
    val glassLevel: GlassLevel = GlassLevel.BALANCED,
    val glassTintStrength: GlassTintStrength = GlassTintStrength.LOW,
    val chartPalette: ChartPalette = ChartPalette.NEUTRAL_FIRST,
    val ambientBackground: Boolean = true,
    val amoledBlack: Boolean = false,
    val uiStyle: UiStyle = UiStyle.LIQUID_GLASS,
    val p5Palette: P5Palette = P5Palette.PHANTOM_RED,
    val p5RansomTitle: Boolean = true,
    val p5MotionLevel: P5MotionLevel = P5MotionLevel.STANDARD,
    val reduceInAppMotion: Boolean = false,
)
