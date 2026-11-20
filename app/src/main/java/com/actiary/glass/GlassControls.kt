package com.actiary.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import kotlinx.coroutines.launch
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf

@Composable
internal fun Modifier.glassSurface(
    shape: CornerBasedShape,
    sampling: GlassSampling,
    exported: GlassLayer? = null,
    backdrop: GlassBackdrop? = null,
): Modifier {
    val layers = LocalGlassLayers.current
    val src = backdrop ?: if (sampling == GlassSampling.Background) layers.background else layers.content
    return glass(src, shape, LocalGlassProfile.current, LocalThemeTokens.current.dark, exported, flat = sampling == GlassSampling.Background)
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(28.dp),
    sampling: GlassSampling = GlassSampling.Background,
    exported: GlassLayer? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.glassSurface(shape, sampling, exported), content = content)
}

/** Default Content sampling: place in GlassHost's overlay slot, or pass [backdrop] / Background sampling inside content. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sampling: GlassSampling = GlassSampling.Content,
    backdrop: GlassBackdrop? = null,
    enabled: Boolean = true,
    glass: Boolean = true,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier
            .size(48.dp)
            .then(if (glass) Modifier.glassSurface(CircleShape, sampling, backdrop = backdrop) else Modifier)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription, Modifier.size(iconSize),
            tint = LocalThemeTokens.current.neutrals.textPrimary.asColor().copy(alpha = if (enabled) 1f else 0.3f),
        )
    }
}

/** Samples the content layer: place in GlassHost's overlay slot. */
@Composable
fun GlassFloatingAction(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .glassSurface(shape, GlassSampling.Content)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = t.accent.accentAction.asColor())
        Text(text, color = t.neutrals.textPrimary.asColor(), fontWeight = FontWeight.Medium)
    }
}

/** Height of [GlassBottomBar]; MainActivity derives page insets from it. */
val GlassBottomBarHeight = 64.dp

data class GlassTab(val icon: ImageVector, val label: String)

/** Thumb x offset (px) from the start edge; mirrored in RTL. */
internal fun thumbOffsetPx(value: Float, tabWidthPx: Float, ltr: Boolean): Float = value * tabWidthPx * if (ltr) 1f else -1f

/** New thumb value after dragging [dragPx] on screen from [target]; clamped to the option range. */
internal fun dragValue(target: Float, dragPx: Float, tabWidthPx: Float, count: Int, ltr: Boolean): Float =
    (target + dragPx / tabWidthPx * if (ltr) 1f else -1f).coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat())

/** Option the thumb settles on when released at [value]. */
internal fun settleIndex(value: Float, count: Int): Int = valueFor(Math.round(value), count).toInt()

/** True while the thumb is pressed or its scale is still settling; the thumb glass node and tint copy exist only then. */
internal fun isInteracting(pressProgress: Float, scaleX: Float, scaleY: Float): Boolean =
    pressProgress > 0.001f || abs(scaleX - 1f) >= 0.002f || abs(scaleY - 1f) >= 0.002f

/** Whole-panel growth while pressed: +[growPx] of width at full press. */
internal fun panelScale(progress: Float, widthPx: Float, growPx: Float): Float = 1f + growPx / widthPx * progress

/**
 * Adapted from Kyant0/AndroidLiquidGlass catalog (components/LiquidBottomTabs.kt, LiquidBottomTab.kt),
 * Apache-2.0, Copyright 2025 Kyant. Modified for Actiary: solid selected thumb with accent stroke at rest,
 * ThemeTokens colours, selectable tabs, no elastic panel offset or interactive highlight, tint copy drawn
 * on a plain surface instead of a second glass node.
 *
 * Layers (Box order): glass panel with the real tabs, invisible accent-tinted label copy recorded into
 * [rememberLayerBackdrop] (only when refraction is available), draggable thumb that samples content + copy.
 * Applies no insets or outer padding: the caller supplies them through [modifier].
 */
@Composable
fun GlassBottomBar(tabs: List<GlassTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalThemeTokens.current
    val layers = LocalGlassLayers.current
    val profile = LocalGlassProfile.current
    val still = rememberAnimationsOff()
    val enhance = !profile.degraded && !still
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()
    val count = tabs.size
    val shape = RoundedCornerShape(50)
    val state = remember(count) { PressDragState(selected.toFloat(), 0f..(count - 1).toFloat(), scope, 78f / 56f) }
    // Flips only at the ends of the press animation, so recomposition is rare; at rest only the panel is a drawBackdrop node.
    val interactive by remember(state) { derivedStateOf { isInteracting(state.pressProgress, state.scaleX, state.scaleY) } }
    val glassOn = enhance && interactive
    val refract = glassOn && isRuntimeShaderSupported()
    val selectedNow by rememberUpdatedState(selected)
    val onSelectNow by rememberUpdatedState(onSelect)
    val stillNow by rememberUpdatedState(still)
    val enhanceNow by rememberUpdatedState(enhance)
    val ltrNow by rememberUpdatedState(ltr)
    LaunchedEffect(selected, still) {
        val target = selected.toFloat()
        if (state.targetValue != target) {
            if (still) state.snapTo(target) else state.animateToValue(target)
        }
    }
    val tabsBackdrop = rememberLayerBackdrop()
    val thumbBackdrop = if (refract) rememberCombinedBackdrop(layers.content, tabsBackdrop) else layers.content
    val tabScale = { 1f + 0.2f * state.pressProgress }
    val surface = t.neutrals.surface.asColor()
    val stroke = t.accent.accentStroke.asColor()

    BoxWithConstraints(modifier.fillMaxWidth().height(GlassBottomBarHeight), contentAlignment = Alignment.CenterStart) {
        val tabW = (maxWidth - 8.dp) / count
        val tabWpx = with(LocalDensity.current) { tabW.toPx() }
        val tabWpxNow by rememberUpdatedState(tabWpx)

        Row(
            Modifier
                .glass(
                    layers.content, shape, profile, t.dark,
                    layerBlock = if (enhance) ({
                        val s = panelScale(state.pressProgress, size.width, 16.dp.toPx())
                        scaleX = s
                        scaleY = s
                    }) else null,
                )
                .fillMaxSize().padding(4.dp).selectableGroup(),
        ) {
            tabs.forEachIndexed { i, tab ->
                val on = i == selected
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(shape).selectable(on, role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    TabLabel(tab, (if (on) t.accent.accentAction else t.neutrals.textSecondary).asColor(), on, tabScale)
                }
            }
        }

        if (refract) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .fillMaxSize()
                    .background(Color(profile.surfaceColor), shape)
                    .padding(4.dp),
            ) {
                tabs.forEachIndexed { i, tab ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        TabLabel(tab, t.accent.accentAction.asColor(), i == selected, tabScale)
                    }
                }
            }
        }

        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    // Always present, so scale and deform stay continuous while the glass node mounts and unmounts.
                    translationX = thumbOffsetPx(state.value, tabWpxNow, ltrNow)
                    val (dx, dy) = deformScale(state.normalizedVelocity)
                    scaleX = state.scaleX * dx
                    scaleY = state.scaleY * dy
                }
                .inspectDragGestures(
                    onDragStart = { if (enhanceNow) state.press() },
                    onDragEnd = {
                        val idx = settleIndex(state.targetValue, count)
                        scope.launch { if (stillNow) state.snapTo(idx.toFloat()) else state.animateToValue(idx.toFloat()) }
                        if (idx != selectedNow) onSelectNow(idx)
                    },
                    onDragCancel = { state.release() },
                    onDrag = { _, d ->
                        val v = dragValue(state.targetValue, d.x, tabWpxNow, count, ltrNow)
                        if (stillNow) scope.launch { state.snapTo(v) } else state.updateValue(v)
                    },
                )
                .then(
                    if (glassOn) Modifier.glass(
                        thumbBackdrop, shape, profile, t.dark,
                        press = { state.pressProgress },
                    ) else Modifier,
                )
                .drawBehind {
                    // glass() supplies the pressed look; this is the solid resting thumb (fades out as p -> 1).
                    val a = 1f - if (enhanceNow) state.pressProgress else 0f
                    val r = CornerRadius(size.height / 2f)
                    val sw = 1.5.dp.toPx()
                    drawRoundRect(Color.Black, Offset(0f, 1.5.dp.toPx()), size, r, alpha = 0.10f * a)
                    drawRoundRect(surface, cornerRadius = r, alpha = a)
                    drawRoundRect(
                        stroke, Offset(sw / 2f, sw / 2f), Size(size.width - sw, size.height - sw),
                        CornerRadius(r.x - sw / 2f), Stroke(sw), alpha = a,
                    )
                }
                .width(tabW).height(56.dp),
        )
    }
}

@Composable
private fun TabLabel(tab: GlassTab, color: Color, bold: Boolean, scale: () -> Float) {
    Column(
        Modifier.graphicsLayer { scaleX = scale(); scaleY = scale() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(tab.label, color = color, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

/** Samples the content layer: place in GlassHost's overlay slot. */
@Composable
fun GlassTopBar(title: String, visible: Boolean, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val t = LocalThemeTokens.current
    val still = rememberAnimationsOff()
    AnimatedVisibility(
        visible, modifier,
        enter = if (still) EnterTransition.None else fadeIn(),
        exit = if (still) ExitTransition.None else fadeOut(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, top = 8.dp, end = 72.dp),
            verticalAlignment = Alignment.Top,
        ) {
            BasicText(
                title, Modifier.weight(1f).semantics { heading() },
                style = TextStyle(t.neutrals.textPrimary.asColor(), 28.sp, FontWeight.Bold),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            actions()
        }
    }
}

@Composable
fun GlassSegmentedPicker(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(50)
    Row(modifier.fillMaxWidth().glassSurface(shape, GlassSampling.Background).padding(4.dp).selectableGroup()) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(shape)
                    .then(if (on) Modifier.background(t.neutrals.surface.asColor()) else Modifier)
                    .selectable(on, role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, maxLines = 1, fontWeight = FontWeight.Medium,
                    color = (if (on) t.accent.accentAction else t.neutrals.textSecondary).asColor(),
                )
            }
        }
    }
}
