package com.actiary.theme

data class NeutralTokens(
    val background: Int, val surface: Int, val surfaceMuted: Int,
    val textPrimary: Int, val textSecondary: Int, val divider: Int, val glassNeutralTint: Int,
)

val LightNeutrals = NeutralTokens(
    background = 0xFFF8F8F6.toInt(), surface = 0xFFFFFFFF.toInt(), surfaceMuted = 0xFFF0F1F0.toInt(),
    textPrimary = 0xFF22262A.toInt(), textSecondary = 0xFF586068.toInt(),
    divider = 0xFFDEE1E2.toInt(), glassNeutralTint = 0xFFFFFFFF.toInt(),
)

val DarkNeutrals = NeutralTokens(
    background = 0xFF141619.toInt(), surface = 0xFF1E2125.toInt(), surfaceMuted = 0xFF282C31.toInt(),
    textPrimary = 0xFFF1F2F2.toInt(), textSecondary = 0xFFADB4BC.toInt(),
    divider = 0xFF383D43.toInt(), glassNeutralTint = 0xFF333941.toInt(),
)

val AmoledDarkNeutrals = NeutralTokens(
    background = 0xFF000000.toInt(), surface = 0xFF101214.toInt(), surfaceMuted = 0xFF1B1E22.toInt(),
    textPrimary = 0xFFF1F2F2.toInt(), textSecondary = 0xFFADB4BC.toInt(),
    divider = 0xFF2B3036.toInt(), glassNeutralTint = 0xFF1D2126.toInt(),
)

data class AccentTokens(
    val accentSeed: Int, val accentAction: Int, val onAccentAction: Int, val accentSubtle: Int,
    val accentStroke: Int, val accentChart: Int, val glassAccentTint: Int,
)

data class AmbientTokens(val base: Int, val blobA: Int, val blobB: Int)

data class ThemeTokens(
    val dark: Boolean,
    val neutrals: NeutralTokens,
    val accent: AccentTokens,
    val ambient: AmbientTokens,
    val ambientEnabled: Boolean,
)
