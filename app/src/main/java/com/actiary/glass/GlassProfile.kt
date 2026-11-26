package com.actiary.glass

import com.actiary.data.appearance.GlassLevel
import com.actiary.data.appearance.GlassTintStrength
import com.actiary.theme.ThemeTokens
import com.actiary.theme.mix
import kotlin.math.roundToInt

data class GlassProfile(
    val blurDp: Float,
    val refractionHeightDp: Float,
    val refractionAmountDp: Float,
    val chromaticAberration: Boolean,
    val highlightAlpha: Float,
    /** ARGB, alpha included. */
    val surfaceColor: Int,
    val degraded: Boolean,
)

private data class Params(val blur: Float, val height: Float, val amount: Float, val chroma: Boolean, val highlight: Float, val alpha: Double)

fun resolveGlass(level: GlassLevel, tint: GlassTintStrength, tokens: ThemeTokens, degraded: Boolean): GlassProfile {
    val weight = when (tint) {
        GlassTintStrength.LOW -> 0.03
        GlassTintStrength.MEDIUM -> 0.065
        GlassTintStrength.HIGH -> 0.105
    }
    val p = when (level) {
        GlassLevel.SOFT -> Params(16f, 6f, 8f, false, 0.30f, 0.62)
        GlassLevel.BALANCED -> Params(10f, 14f, 20f, false, 0.55f, 0.42)
        GlassLevel.CLEAR -> Params(1.5f, 26f, 40f, true, 0.90f, 0.12)
    }
    val alpha = if (degraded) 0.85 else p.alpha
    val rgb = mix(tokens.neutrals.glassNeutralTint, tokens.accent.glassAccentTint, weight) and 0xFFFFFF
    val color = ((alpha * 255).roundToInt() shl 24) or rgb
    return if (degraded) GlassProfile(0f, 0f, 0f, false, p.highlight, color, true)
    else GlassProfile(p.blur, p.height, p.amount, p.chroma, p.highlight, color, false)
}
