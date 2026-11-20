package com.actiary.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.Shadow

typealias GlassLayer = LayerBackdrop
typealias GlassBackdrop = Backdrop

@Composable
fun rememberGlassLayer(): GlassLayer = rememberLayerBackdrop()

val LocalGlassProfile = staticCompositionLocalOf<GlassProfile> { error("no glass") }

/** background = ambient layer; content = page content layer (floating glass samples this one). */
data class GlassLayers(val background: Backdrop, val content: Backdrop)

val LocalGlassLayers = staticCompositionLocalOf<GlassLayers> { error("no glass layers") }

enum class GlassSampling { Background, Content }

/**
 * The only drawBackdrop call site in the app. [flat] drops the shadow (in-content panels).
 * Degraded profiles skip Backdrop entirely: translucent neutral surface + hairline highlight.
 */
fun Modifier.glass(
    backdrop: Backdrop,
    shape: CornerBasedShape,
    profile: GlassProfile,
    dark: Boolean,
    exported: LayerBackdrop? = null,
    flat: Boolean = false,
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
            blur(profile.blurDp.dp.toPx())
            lens(
                profile.refractionHeightDp.dp.toPx(),
                profile.refractionAmountDp.dp.toPx(),
                chromaticAberration = profile.chromaticAberration,
            )
        },
        highlight = {
            // Default highlight needs the runtime shader (API 33+); 31-32 use the flat stroke.
            Highlight(
                alpha = profile.highlightAlpha,
                style = if (isRuntimeShaderSupported()) HighlightStyle.Default else HighlightStyle.Plain,
            )
        },
        shadow = if (flat) null else ({ Shadow.Default }),
        exportedBackdrop = exported,
        onDrawSurface = { drawRect(surface) },
    )
}
