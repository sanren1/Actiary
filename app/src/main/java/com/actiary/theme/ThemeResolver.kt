package com.actiary.theme

import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.ThemeMode
import com.actiary.data.appearance.UiStyle
import com.actiary.p5.resolveP5Tokens

fun resolveTheme(settings: AppearanceSettings, systemDark: Boolean, dynamicSeed: Int? = null): ThemeTokens {
    if (settings.uiStyle == UiStyle.PERSONA_5) {
        return resolvePersona5Theme(settings)
    }
    val dark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val seed = when (settings.accentPreset) {
        AccentPreset.DYNAMIC -> dynamicSeed ?: DEFAULT_SEED
        AccentPreset.CUSTOM -> parseHex(settings.customAccentHex) ?: DEFAULT_SEED
        else -> settings.accentPreset.seed() ?: DEFAULT_SEED
    }
    val n = when {
        !dark -> LightNeutrals
        settings.amoledBlack -> AmoledDarkNeutrals
        else -> DarkNeutrals
    }
    return ThemeTokens(dark, n, deriveAccent(seed, n, dark), deriveAmbient(seed, n, dark), settings.ambientBackground)
}

private fun stepLightnessDown(seed: Int, ok: (Int) -> Boolean): Int {
    if (ok(seed)) return seed
    val o = seed.toOklch()
    var l = o.l
    while (true) {
        l = (l - 0.005).coerceIn(0.0, 1.0)
        val candidate = Oklch(l, o.c, o.h).toArgb()
        if (ok(candidate) || l <= 0.0) return candidate
    }
}

private fun resolvePersona5Theme(settings: AppearanceSettings): ThemeTokens {
    val p5 = resolveP5Tokens(settings)
    if (p5.palette == P5Palette.CALLING_CARD) {
        val neutrals = NeutralTokens(
            background = p5.canvas,
            surface = p5.lightCard,
            surfaceMuted = 0xFFEAE8E1.toInt(),
            textPrimary = p5.onLightCard,
            textSecondary = p5.onLightCardMuted,
            divider = 0xFF1F1F23.toInt(),
            glassNeutralTint = p5.lightCard,
        )
        val onAccentAction = 0xFFFFFFFF.toInt()
        val accentAction = stepLightnessDown(p5.brandFill) {
            contrast(it, p5.lightCard) >= 4.5 &&
                contrast(it, p5.canvas) >= 4.5 &&
                contrast(onAccentAction, it) >= 4.5
        }
        val accent = AccentTokens(
            accentSeed = p5.brandFill,
            accentAction = accentAction,
            onAccentAction = onAccentAction,
            accentSubtle = mix(p5.lightCard, p5.brandFill, 0.10),
            accentStroke = accentAction,
            accentChart = accentAction,
            glassAccentTint = p5.brandFill,
        )
        val ambient = AmbientTokens(
            base = p5.canvas,
            blobA = p5.canvas,
            blobB = p5.canvas,
        )
        return ThemeTokens(
            dark = false,
            neutrals = neutrals,
            accent = accent,
            ambient = ambient,
            ambientEnabled = false,
        )
    }
    val neutrals = NeutralTokens(
        background = p5.canvas,
        surface = p5.darkCard,
        surfaceMuted = mix(p5.darkCard, p5.strokeLight, 0.08),
        textPrimary = p5.strokeLight,
        textSecondary = mix(p5.strokeLight, p5.darkCard, 0.30),
        divider = mix(p5.darkCard, p5.strokeLight, 0.16),
        glassNeutralTint = p5.darkCard,
    )
    val accent = AccentTokens(
        accentSeed = p5.brandFill,
        accentAction = p5.accentOnDark,
        onAccentAction = p5.canvas,
        accentSubtle = mix(p5.darkCard, p5.brandFill, 0.14),
        accentStroke = p5.accentOnDark,
        accentChart = p5.accentOnDark,
        glassAccentTint = p5.brandFill,
    )
    val ambient = AmbientTokens(
        base = p5.canvas,
        blobA = p5.canvas,
        blobB = p5.canvas,
    )
    return ThemeTokens(
        dark = true,
        neutrals = neutrals,
        accent = accent,
        ambient = ambient,
        ambientEnabled = false,
    )
}
