package com.actiary.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

typealias GlassLayer = LayerBackdrop
typealias GlassBackdrop = Backdrop

@Composable
fun rememberGlassLayer(): GlassLayer = rememberLayerBackdrop()

fun Modifier.glassLayer(layer: GlassLayer): Modifier = layerBackdrop(layer)

val LocalGlassProfile = compositionLocalOf<GlassProfile> { error("no glass") }

/** background = ambient layer; content = page content layer (floating glass samples this one). */
data class GlassLayers(val background: Backdrop, val content: Backdrop)

val LocalGlassLayers = staticCompositionLocalOf<GlassLayers> { error("no glass layers") }

enum class GlassSampling { Background, Content }

/**
 * The only drawBackdrop call site in the app. [flat] uses a compact shadow (in-content panels).
 * Degraded profiles skip Backdrop entirely: translucent neutral surface + hairline highlight.
 * [press] (0..1, read in draw phase) turns this into an interactive thumb: at 0 it is a solid surface, and the
 * lens, highlight and shadows grow with it. [thumbRestingColor] is the thumb fill at rest.
 * Ignored when degraded. [layerBlock] goes to drawBackdrop as is.
 */
fun Modifier.glass(
    backdrop: Backdrop,
    shape: CornerBasedShape,
    profile: GlassProfile,
    dark: Boolean,
    exported: LayerBackdrop? = null,
    flat: Boolean = false,
    press: (() -> Float)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    thumbRestingColor: Color? = null,
): Modifier {
    val surface = Color(profile.surfaceColor)
    if (profile.degraded) {
        val edge = Color.White.copy(alpha = if (dark) 0.18f else 0.30f)
        return background(surface, shape).border(0.5.dp, edge, shape).clip(shape)
    }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            if (press == null) {
                blur(profile.blurDp.dp.toPx())
                val minDim = size.minDimension
                val smallSurface = minDim < 44.dp.toPx()
                lens(
                    minOf(profile.refractionHeightDp.dp.toPx(), minDim * 0.30f),
                    minOf(profile.refractionAmountDp.dp.toPx(), minDim * 0.35f),
                    chromaticAberration = profile.chromaticAberration && !smallSurface,
                )
            } else {
                val p = press()
                val minDim = size.minDimension
                lens(
                    minOf(profile.refractionHeightDp.dp.toPx(), minDim * 0.30f) * p,
                    minOf(profile.refractionAmountDp.dp.toPx() * 0.55f, minDim * 0.35f) * p,
                    chromaticAberration = false,
                )
            }
        },
        highlight = {
            val highlightFactor = press?.let { 0.45f + 0.55f * it() } ?: 1f
            // Default highlight needs the runtime shader (API 33+); 31-32 use the flat stroke.
            Highlight(
                width = 0.5.dp,
                alpha = profile.highlightAlpha * highlightFactor,
                style = if (isRuntimeShaderSupported()) {
                    HighlightStyle.Default(
                        color = Color.White.copy(alpha = if (dark) 0.24f else 0.72f),
                        blendMode = DrawScope.DefaultBlendMode,
                        falloff = 0.65f,
                    )
                } else {
                    HighlightStyle.Plain
                },
            )
        },
        shadow = when {
            flat -> ({
                Shadow(
                    radius = if (dark) 8.dp else 6.dp,
                    offset = DpOffset(0.dp, 1.5.dp),
                    color = Color.Black.copy(alpha = if (dark) 0.26f else 0.09f),
                )
            })
            press == null -> ({ Shadow.Default })
            else -> ({
                val p = press()
                Shadow(
                    radius = (5f + 11f * p).dp,
                    offset = DpOffset(0.dp, (1.5f + 2.5f * p).dp),
                    alpha = p,
                )
            })
        },
        innerShadow = press?.let { { InnerShadow(radius = 4.dp * it(), alpha = it()) } },
        layerBlock = layerBlock,
        exportedBackdrop = exported,
        onDrawSurface = {
            drawRect(if (press == null) surface else lerp(thumbRestingColor ?: surface.copy(alpha = 1f), surface, press()))
        },
    )
}

/**
 * Optical liquid-glass lens sampling [backdrop] with zero blur (keeping underlying text/icons razor-sharp),
 * AGSL refraction driven by [flowPulse] (0..1, read in draw phase), specular highlight rim, compact shadow,
 * and a subtle uniform [tint] surface. Degraded profiles render a translucent accent-tinted rounded rect
 * with a subtle highlight rim.
 */
fun Modifier.glassLens(
    backdrop: Backdrop,
    shape: CornerBasedShape,
    profile: GlassProfile,
    dark: Boolean,
    tint: Color,
    flowPulse: () -> Float = { 0f },
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
): Modifier {
    if (profile.degraded) {
        val fill = tint.copy(alpha = if (dark) 0.18f else 0.12f)
        val edge = Color.White.copy(alpha = if (dark) 0.20f else 0.45f)
        return (if (layerBlock != null) graphicsLayer(layerBlock) else this)
            .background(fill, shape)
            .border(0.6.dp, edge, shape)
            .clip(shape)
    }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            val pulse = flowPulse().coerceIn(0f, 1f)
            val minDim = size.minDimension
            lens(
                minOf(4.5.dp.toPx(), minDim * 0.12f) * (1f + 0.85f * pulse),
                minOf(5.dp.toPx(), minDim * 0.14f) * (1f + 1.1f * pulse),
                chromaticAberration = false,
            )
        },
        highlight = {
            val pulse = flowPulse().coerceIn(0f, 1f)
            Highlight(
                width = 0.6.dp,
                alpha = profile.highlightAlpha * (0.75f + 0.25f * pulse),
                style = if (isRuntimeShaderSupported()) {
                    HighlightStyle.Default(
                        color = Color.White.copy(alpha = if (dark) 0.28f else 0.78f),
                        blendMode = DrawScope.DefaultBlendMode,
                        falloff = 0.65f,
                    )
                } else {
                    HighlightStyle.Plain
                },
            )
        },
        shadow = {
            Shadow(
                radius = if (dark) 8.dp else 6.dp,
                offset = DpOffset(0.dp, 2.dp),
                color = Color.Black.copy(alpha = if (dark) 0.32f else 0.12f),
            )
        },
        layerBlock = layerBlock,
        onDrawSurface = {
            drawRect(tint.copy(alpha = if (dark) 0.16f else 0.10f))
        },
    )
}
