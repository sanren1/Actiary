package com.actiary.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.UiStyle
import com.actiary.p5.P5Background
import com.actiary.p5.P5Tokens
import com.actiary.p5.resolveP5Tokens
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.ThemeTokens
import com.actiary.ui.motion.rememberMotionReduced
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop

internal class GlassAnchoredMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val selected: Boolean? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

internal class GlassAnchoredMenuState {
    var request by mutableStateOf<Request?>(null)
        private set
    internal var present by mutableStateOf(false)

    class Request(val anchor: Rect, val items: List<GlassAnchoredMenuItem>)

    fun show(anchor: Rect, items: List<GlassAnchoredMenuItem>) {
        request = Request(anchor, items)
    }

    fun dismiss() {
        request = null
    }
}

internal val LocalGlassAnchoredMenu = staticCompositionLocalOf<GlassAnchoredMenuState> {
    error("No anchored menu host")
}

@Composable
internal fun rememberAnimationsOff(): Boolean = rememberMotionReduced()

@Composable
fun rememberGlassDegraded(): Boolean = rememberMotionReduced()

/**
 * Two backdrop layers: ambient background and page content. Floating overlays (sheet, dialog, menu,
 * bars) go in [overlay], outside the content layer, so they sample the content below them.
 */
@Composable
fun GlassHost(
    tokens: ThemeTokens,
    profile: GlassProfile,
    uiStyle: UiStyle = UiStyle.LIQUID_GLASS,
    p5Tokens: P5Tokens = resolveP5Tokens(AppearanceSettings()),
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val menu = remember { GlassAnchoredMenuState() }
    val menuVisibility = rememberOverlayVisibility(menu.request != null)
    menu.present = menuVisibility.overlayPresent
    if (uiStyle == UiStyle.PERSONA_5) {
        CompositionLocalProvider(
            LocalThemeTokens provides tokens,
            LocalGlassProfile provides profile,
            LocalUiStyle provides uiStyle,
            LocalP5Tokens provides p5Tokens,
            LocalGlassAnchoredMenu provides menu,
        ) {
            Box(Modifier.fillMaxSize()) {
                P5Background(p5Tokens, Modifier.fillMaxSize())
                val inputEnabled = !menuVisibility.overlayPresent
                Box(Modifier.fillMaxSize().overlayInteraction(inputEnabled)) { content() }
                Box(Modifier.matchParentSize().overlayInteraction(inputEnabled)) { overlay() }
                GlassAnchoredMenu(menu, menuVisibility)
            }
        }
        return
    }
    val bg = rememberGlassLayer()
    val contentLayer = rememberGlassLayer()
    // Overlays sample ambient + content, so transparent page areas still show the background.
    val combined = rememberCombinedBackdrop(bg, contentLayer)
    val layers = remember(bg, combined) { GlassLayers(background = bg, content = combined) }
    CompositionLocalProvider(
        LocalThemeTokens provides tokens,
        LocalGlassProfile provides profile,
        LocalUiStyle provides uiStyle,
        LocalP5Tokens provides p5Tokens,
        LocalGlassLayers provides layers,
        LocalGlassAnchoredMenu provides menu,
    ) {
        Box(Modifier.fillMaxSize()) {
            AmbientBackground(tokens, Modifier.fillMaxSize().layerBackdrop(bg))
            val inputEnabled = !menuVisibility.overlayPresent
            Box(Modifier.fillMaxSize().layerBackdrop(contentLayer).overlayInteraction(inputEnabled)) { content() }
            Box(Modifier.matchParentSize().overlayInteraction(inputEnabled)) { overlay() }
            GlassAnchoredMenu(menu, menuVisibility)
        }
    }
}
