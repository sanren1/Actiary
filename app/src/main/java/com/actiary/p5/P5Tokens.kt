package com.actiary.p5

import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.P5MotionLevel
import com.actiary.data.appearance.P5Palette
import com.actiary.theme.Oklch
import com.actiary.theme.contrast
import com.actiary.theme.toArgb
import com.actiary.theme.toOklch

data class P5Tokens(
    val palette: P5Palette,
    val brandFill: Int,
    val onBrandFill: Int,
    val canvas: Int,
    val darkCard: Int,
    val lightCard: Int,
    val onLightCard: Int,
    val onLightCardMuted: Int,
    val strokeDark: Int,
    val strokeLight: Int,
    val shadowColor: Int,
    val highlightBadge: Int,
    val onHighlightBadge: Int,
    val accentOnDark: Int,
    val accentChartOnLight: Int,
    val ransomTitleEnabled: Boolean,
    val motionLevel: P5MotionLevel,
)

private data class P5PaletteSpec(
    val brandFill: Int,
    val onBrandFill: Int,
    val canvas: Int,
    val darkCard: Int,
    val lightCard: Int,
    val onLightCard: Int,
    val onLightCardMuted: Int,
    val strokeDark: Int,
    val strokeLight: Int,
    val shadowColor: Int,
    val highlightBadge: Int,
    val onHighlightBadge: Int,
)

private val PhantomRedSpec = P5PaletteSpec(
    brandFill = 0xFFD91E26.toInt(),
    onBrandFill = 0xFFFFFFFF.toInt(),
    canvas = 0xFF0D0D0D.toInt(),
    darkCard = 0xFF141416.toInt(),
    lightCard = 0xFFF7F6F2.toInt(),
    onLightCard = 0xFF0D0D0D.toInt(),
    onLightCardMuted = 0xFF4A4A4F.toInt(),
    strokeDark = 0xFF0D0D0D.toInt(),
    strokeLight = 0xFFF7F6F2.toInt(),
    shadowColor = 0xFFD91E26.toInt(),
    highlightBadge = 0xFFFFE600.toInt(),
    onHighlightBadge = 0xFF0D0D0D.toInt(),
)

private val RoyalGoldSpec = P5PaletteSpec(
    brandFill = 0xFFE5A910.toInt(),
    onBrandFill = 0xFF0D0D0D.toInt(),
    canvas = 0xFF0E0E10.toInt(),
    darkCard = 0xFF16161A.toInt(),
    lightCard = 0xFFF8F6EE.toInt(),
    onLightCard = 0xFF0E0E10.toInt(),
    onLightCardMuted = 0xFF4A4945.toInt(),
    strokeDark = 0xFF0D0D0D.toInt(),
    strokeLight = 0xFFF8F6EE.toInt(),
    shadowColor = 0xFFE5A910.toInt(),
    highlightBadge = 0xFFD91E26.toInt(),
    onHighlightBadge = 0xFFFFFFFF.toInt(),
)

private val VelvetBlueSpec = P5PaletteSpec(
    brandFill = 0xFF1A4BD8.toInt(),
    onBrandFill = 0xFFFFFFFF.toInt(),
    canvas = 0xFF0A0D16.toInt(),
    darkCard = 0xFF111624.toInt(),
    lightCard = 0xFFF4F6FA.toInt(),
    onLightCard = 0xFF0A0D16.toInt(),
    onLightCardMuted = 0xFF434A5C.toInt(),
    strokeDark = 0xFF0A0D16.toInt(),
    strokeLight = 0xFFF4F6FA.toInt(),
    shadowColor = 0xFF1A4BD8.toInt(),
    highlightBadge = 0xFFF5C400.toInt(),
    onHighlightBadge = 0xFF0A0D16.toInt(),
)

private val CallingCardSpec = P5PaletteSpec(
    brandFill = 0xFFD91E26.toInt(),
    onBrandFill = 0xFFFFFFFF.toInt(),
    canvas = 0xFFF7F6F2.toInt(),
    darkCard = 0xFF121214.toInt(),
    lightCard = 0xFFFFFFFF.toInt(),
    onLightCard = 0xFF0D0D0D.toInt(),
    onLightCardMuted = 0xFF45454A.toInt(),
    strokeDark = 0xFF0D0D0D.toInt(),
    strokeLight = 0xFFFFFFFF.toInt(),
    shadowColor = 0xFF0D0D0D.toInt(),
    highlightBadge = 0xFFFFE600.toInt(),
    onHighlightBadge = 0xFF0D0D0D.toInt(),
)

private fun stepLightness(seed: Int, lighten: Boolean, ok: (Int) -> Boolean): Int {
    if (ok(seed)) return seed
    val o = seed.toOklch()
    var l = o.l
    while (true) {
        l = (if (lighten) l + 0.005 else l - 0.005).coerceIn(0.0, 1.0)
        val candidate = Oklch(l, o.c, o.h).toArgb()
        if (ok(candidate) || l <= 0.0 || l >= 1.0) return candidate
    }
}

fun resolveP5Tokens(settings: AppearanceSettings): P5Tokens {
    val spec = when (settings.p5Palette) {
        P5Palette.PHANTOM_RED -> PhantomRedSpec
        P5Palette.ROYAL_GOLD -> RoyalGoldSpec
        P5Palette.VELVET_BLUE -> VelvetBlueSpec
        P5Palette.CALLING_CARD -> CallingCardSpec
    }
    val isLightCanvas = settings.p5Palette == P5Palette.CALLING_CARD
    val accentOnDark = stepLightness(spec.brandFill, lighten = true) {
        contrast(it, spec.darkCard) >= 4.5 && (isLightCanvas || contrast(it, spec.canvas) >= 4.5)
    }
    val accentChartOnLight = stepLightness(spec.brandFill, lighten = false) {
        contrast(it, spec.lightCard) >= 3.0
    }
    return P5Tokens(
        palette = settings.p5Palette,
        brandFill = spec.brandFill,
        onBrandFill = spec.onBrandFill,
        canvas = spec.canvas,
        darkCard = spec.darkCard,
        lightCard = spec.lightCard,
        onLightCard = spec.onLightCard,
        onLightCardMuted = spec.onLightCardMuted,
        strokeDark = spec.strokeDark,
        strokeLight = spec.strokeLight,
        shadowColor = spec.shadowColor,
        highlightBadge = spec.highlightBadge,
        onHighlightBadge = spec.onHighlightBadge,
        accentOnDark = accentOnDark,
        accentChartOnLight = accentChartOnLight,
        ransomTitleEnabled = settings.p5RansomTitle,
        motionLevel = settings.p5MotionLevel,
    )
}
