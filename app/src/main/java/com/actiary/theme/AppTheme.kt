package com.actiary.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private const val THEME_TRANSITION_MS = 320

val LocalThemeTokens = compositionLocalOf<ThemeTokens> { error("no theme") }

fun Int.asColor(): Color = Color(this)

@Composable
private fun animateTokenArgb(targetArgb: Int, reducedMotion: Boolean, label: String): Int {
    val color by animateColorAsState(
        targetValue = Color(targetArgb),
        animationSpec = if (reducedMotion) snap() else tween(THEME_TRANSITION_MS, easing = FastOutSlowInEasing),
        label = label,
    )
    return color.toArgb()
}

@Composable
fun rememberAnimatedThemeTokens(target: ThemeTokens, reducedMotion: Boolean): ThemeTokens {
    val neutrals = NeutralTokens(
        background = animateTokenArgb(target.neutrals.background, reducedMotion, "bg"),
        surface = animateTokenArgb(target.neutrals.surface, reducedMotion, "surface"),
        surfaceMuted = animateTokenArgb(target.neutrals.surfaceMuted, reducedMotion, "surfaceMuted"),
        textPrimary = animateTokenArgb(target.neutrals.textPrimary, reducedMotion, "textPrimary"),
        textSecondary = animateTokenArgb(target.neutrals.textSecondary, reducedMotion, "textSecondary"),
        divider = animateTokenArgb(target.neutrals.divider, reducedMotion, "divider"),
        glassNeutralTint = animateTokenArgb(target.neutrals.glassNeutralTint, reducedMotion, "glassNeutralTint"),
    )
    val accent = AccentTokens(
        accentSeed = animateTokenArgb(target.accent.accentSeed, reducedMotion, "accentSeed"),
        accentAction = animateTokenArgb(target.accent.accentAction, reducedMotion, "accentAction"),
        onAccentAction = animateTokenArgb(target.accent.onAccentAction, reducedMotion, "onAccentAction"),
        accentSubtle = animateTokenArgb(target.accent.accentSubtle, reducedMotion, "accentSubtle"),
        accentStroke = animateTokenArgb(target.accent.accentStroke, reducedMotion, "accentStroke"),
        accentChart = animateTokenArgb(target.accent.accentChart, reducedMotion, "accentChart"),
        glassAccentTint = animateTokenArgb(target.accent.glassAccentTint, reducedMotion, "glassAccentTint"),
    )
    val ambient = AmbientTokens(
        base = animateTokenArgb(target.ambient.base, reducedMotion, "ambientBase"),
        blobA = animateTokenArgb(target.ambient.blobA, reducedMotion, "ambientBlobA"),
        blobB = animateTokenArgb(target.ambient.blobB, reducedMotion, "ambientBlobB"),
    )
    return ThemeTokens(
        dark = target.dark,
        neutrals = neutrals,
        accent = accent,
        ambient = ambient,
        ambientEnabled = target.ambientEnabled,
    )
}

@Composable
fun AppTheme(tokens: ThemeTokens, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThemeTokens provides tokens, content = content)
}
