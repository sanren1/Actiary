package com.actiary.glass

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
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
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

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

// Feel constants (user-visible): tune here.
private const val BAR_THUMB_SCALE = 1.12f
private const val PICKER_THUMB_SCALE = 1.08f
private const val BAR_LABEL_GROW = 0.08f
private val BAR_PANEL_GROW = 6.dp
private val THUMB_HEIGHT_LIFT = 20.dp

/** The lens provides touch feedback while the border shows keyboard focus. */
@Composable
internal fun Modifier.glassControlFocus(enhanced: Boolean, shape: Shape): Modifier {
    if (!enhanced) return this
    var focused by remember { mutableStateOf(false) }
    val color = LocalThemeTokens.current.accent.accentStroke.asColor()
    return onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(2.dp, color, shape) else Modifier)
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

/** Selected-thumb state shared by bar and segmented picker; follows [selected] (snap when [still]). */
@Composable
private fun rememberThumbState(selected: Int, count: Int, pressedScale: Float, still: Boolean, enhance: Boolean): PressDragState {
    val scope = rememberCoroutineScope()
    val state = remember(count, still, enhance) { PressDragState(selected.toFloat(), 0f..(count - 1).toFloat(), scope, pressedScale) }
    LaunchedEffect(state, selected) {
        val target = selected.toFloat()
        if (state.targetValue != target) {
            if (still) state.snapTo(target) else state.tapTo(target, lift = enhance)
        }
    }
    return state
}

/** Pressed lift times velocity deformation; Backdrop compensates for the scale when sampling. */
private fun GraphicsLayerScope.applyThumbScale(state: PressDragState) {
    val (dx, dy) = deformScale(state.normalizedVelocity)
    scaleX = state.scaleX * dx
    // Both tracks inset the resting thumb 4 dp on each side. Lift beyond the full track height
    // even for a taller segmented row, without making long labels widen the lifted capsule.
    scaleY = (1f + THUMB_HEIGHT_LIFT.toPx() * state.pressProgress / size.height.coerceAtLeast(1f)) * dy
}

/** The static fill and the morphing glass surface share the same resting color. */
private fun Modifier.thumbSurface(
    state: PressDragState,
    glassOn: Boolean,
    backdrop: GlassBackdrop,
    shape: CornerBasedShape,
    profile: GlassProfile,
    dark: Boolean,
    surface: Color,
): Modifier = drawBehind {
    val a = 1f - state.pressProgress
    val r = CornerRadius(size.height / 2f)
    drawRoundRect(Color.Black, Offset(0f, 1.5.dp.toPx()), size, r, alpha = 0.10f * a)
    if (!glassOn) drawRoundRect(surface, cornerRadius = r)
}.then(
    if (glassOn) Modifier.glass(
        backdrop, shape, profile, dark,
        press = { state.pressProgress },
        layerBlock = { applyThumbScale(state) },
        thumbRestingColor = surface,
    )
    else Modifier,
)

@Composable
private fun Modifier.thumbGestures(
    state: PressDragState,
    selected: Int,
    onSelect: (Int) -> Unit,
    still: Boolean,
    enhance: Boolean,
    allowDrag: Boolean,
    hitTest: (Float) -> Int?,
    dragTarget: (Float, Float) -> Float,
): Modifier {
    val scope = rememberCoroutineScope()
    val selectedNow by rememberUpdatedState(selected)
    val onSelectNow by rememberUpdatedState(onSelect)
    val hitTestNow by rememberUpdatedState(hitTest)
    val dragTargetNow by rememberUpdatedState(dragTarget)
    if (!enhance && !allowDrag) return this
    var start = 0f
    var target = 0f
    return inspectDragGestures(
        state = state,
        allowDrag = allowDrag,
        onPress = { point ->
            val index = hitTestNow(point.x) ?: return@inspectDragGestures false
            start = index.toFloat()
            target = start
            if (enhance) {
                state.press()
                if (state.targetValue != start) state.updateValue(start)
            }
            true
        },
        onDrag = { delta ->
            val next = dragTargetNow(start, delta)
            target = next
            if (still) scope.launch { state.snapTo(next) } else state.updateValue(next)
        },
        onRelease = { dragged ->
            if (dragged) {
                val index = settleIndex(target, state.range.endInclusive.toInt() + 1)
                scope.launch { if (still) state.snapTo(index.toFloat()) else state.animateToValue(index.toFloat()) }
                if (index != selectedNow) onSelectNow(index)
            } else if (enhance) state.release()
        },
        onCancel = {
            val selectedValue = selectedNow.toFloat()
            if (still) scope.launch { state.snapTo(selectedValue) }
            else state.updateValue(selectedValue)
            if (enhance) state.release(cancelled = true)
        },
    )
}

/** Thumb x offset (px) at fractional [index] over items of [widths]; linear between neighbouring items. */
internal fun thumbOffset(index: Float, widths: List<Float>): Float {
    if (widths.isEmpty()) return 0f
    val i = index.coerceIn(0f, (widths.size - 1).toFloat())
    val lo = i.toInt()
    var start = 0f
    for (k in 0 until lo) start += widths[k]
    return start + widths[lo] * (i - lo)
}

/** Thumb width at fractional [index]; linear between neighbouring items. */
internal fun thumbWidth(index: Float, widths: List<Float>): Float {
    if (widths.isEmpty()) return 0f
    val i = index.coerceIn(0f, (widths.size - 1).toFloat())
    val lo = i.toInt()
    val hi = minOf(lo + 1, widths.size - 1)
    return widths[lo] + (widths[hi] - widths[lo]) * (i - lo)
}

/** Touched option, using pixel widths from the row layout. */
internal fun segmentIndexAt(x: Float, widths: List<Float>, ltr: Boolean): Int? {
    val width = widths.sum()
    if (x < 0f || x >= width) return null
    var px = x
    for (index in if (ltr) widths.indices else widths.indices.reversed()) {
        if (px < widths[index]) return index
        px -= widths[index]
    }
    return null
}

/**
 * Adapted from Kyant0/AndroidLiquidGlass catalog (components/LiquidBottomTabs.kt, LiquidBottomTab.kt),
 * Apache-2.0, Copyright 2025 Kyant. Modified for Actiary: solid selected thumb (no stroke) at rest,
 * ThemeTokens colours, selectable tabs, no elastic panel offset or interactive highlight, tint copy drawn
 * on a plain surface instead of a second glass node.
 *
 * Layers (Box order): glass panel with the real tabs, invisible accent-tinted label copy recorded into
 * [rememberLayerBackdrop] (only when refraction is available), and a draggable thumb sampling that copy.
 * Applies no insets or outer padding: the caller supplies them through [modifier].
 */
@Composable
fun GlassBottomBar(tabs: List<GlassTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalThemeTokens.current
    val layers = LocalGlassLayers.current
    val profile = LocalGlassProfile.current
    val animationsOff = rememberAnimationsOff()
    val still = profile.degraded || animationsOff
    val enhance = !still && isRuntimeShaderSupported()
    val indication = if (enhance) null else LocalIndication.current
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val count = tabs.size
    val shape = RoundedCornerShape(50)
    val state = rememberThumbState(selected, count, BAR_THUMB_SCALE, still, enhance)
    // Flips only at the ends of the press animation, so recomposition is rare; at rest only the panel is a drawBackdrop node.
    val interactive by remember(state) { derivedStateOf { isInteracting(state.pressProgress, state.scaleX, state.scaleY) } }
    val glassOn = enhance && interactive
    val ltrNow by rememberUpdatedState(ltr)
    val tabsBackdrop = rememberLayerBackdrop()
    val tabScale = { 1f + BAR_LABEL_GROW * state.pressProgress }
    val surface = t.neutrals.surface.asColor()

    BoxWithConstraints(modifier.fillMaxWidth().height(GlassBottomBarHeight), contentAlignment = Alignment.CenterStart) {
        val tabW = (maxWidth - 8.dp) / count
        val tabWpx = with(LocalDensity.current) { tabW.toPx() }
        val tabWpxNow by rememberUpdatedState(tabWpx)
        val padPx = with(LocalDensity.current) { 4.dp.toPx() }

        // Panel: glass node only; labels sit above the thumb so the solid resting thumb never hides the selected one.
        Box(
            Modifier
                .glass(
                    layers.content, shape, profile, t.dark,
                    layerBlock = if (enhance) ({
                        val s = panelScale(state.pressProgress, size.width, BAR_PANEL_GROW.toPx())
                        scaleX = s
                        scaleY = s
                    }) else null,
                )
                .fillMaxSize(),
        )

        if (glassOn) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .fillMaxSize()
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
                    translationX = thumbOffsetPx(state.value, tabWpxNow, ltrNow)
                }
                .thumbSurface(state, glassOn, tabsBackdrop, shape, profile, t.dark, surface)
                .width(tabW).height(56.dp),
        )

        Row(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .thumbGestures(
                    state, selected, onSelect, still, enhance, allowDrag = true,
                    hitTest = { x ->
                        val width = tabWpx * count
                        if (x >= 0f && x < width) {
                            val index = indexAt(x, width, count)
                            if (ltr) index else count - 1 - index
                        } else null
                    },
                    dragTarget = { start, dx -> dragValue(start, dx, tabWpx, count, ltr) },
                )
                .graphicsLayer {
                    if (enhance) {
                        val sc = panelScale(state.pressProgress, size.width + 2f * padPx, BAR_PANEL_GROW.toPx())
                        scaleX = sc
                        scaleY = sc
                    }
                }
                .selectableGroup(),
        ) {
            tabs.forEachIndexed { i, tab ->
                val on = i == selected
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(shape)
                        .glassControlFocus(enhance, shape)
                        .selectable(on, interactionSource = null, indication = indication, role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    TabLabel(
                        tab, (if (on) t.accent.accentAction else t.neutrals.textSecondary).asColor(), on, tabScale,
                        // Refracted copy takes over under the thumb; only fade when that copy exists.
                        { if (glassOn) 1f - state.pressProgress * (1f - abs(state.value - i)).coerceIn(0f, 1f) else 1f },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabLabel(tab: GlassTab, color: Color, bold: Boolean, scale: () -> Float, alpha: () -> Float = { 1f }) {
    Column(
        Modifier.graphicsLayer { scaleX = scale(); scaleY = scale(); this.alpha = alpha() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, null, tint = color, modifier = Modifier.size(24.dp))
        BasicText(
            tab.label,
            Modifier.padding(horizontal = 4.dp),
            style = TextStyle(color = color, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
        )
    }
}

/**
 * Segmented picker: solid surfaceMuted track and a sliding thumb with the same look and interaction as
 * [GlassBottomBar]'s thumb (solid at rest; grows into glass while pressed or dragged).
 *
 * Box order: invisible accent-tinted label copy recorded for refraction (only while the thumb is glass and the
 * runtime shader exists), the thumb, then the real labels. Labels sit above the thumb so the solid resting thumb
 * never hides the selected one; while the thumb is refracting, labels under it fade out and the refracted copy
 * replaces them. Only the thumb ever creates a drawBackdrop node, and only while interacting.
 *
 * [scrollable] or a large system font: items take their text width and the track scrolls to keep the selection visible.
 * Presses lift the thumb on every option, and the scroll owns horizontal drags in this variant.
 */
@Composable
fun GlassSegmentedPicker(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
) {
    val t = LocalThemeTokens.current
    val profile = LocalGlassProfile.current
    val animationsOff = rememberAnimationsOff()
    val still = profile.degraded || animationsOff
    val enhance = !still && isRuntimeShaderSupported()
    val indication = if (enhance) null else LocalIndication.current
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val count = options.size
    val shape = RoundedCornerShape(50)
    val state = rememberThumbState(selected, count, PICKER_THUMB_SCALE, still, enhance)
    val interactive by remember(state) { derivedStateOf { isInteracting(state.pressProgress, state.scaleX, state.scaleY) } }
    val glassOn = enhance && interactive
    val ltrNow by rememberUpdatedState(ltr)
    val copyBackdrop = rememberLayerBackdrop()
    val muted = t.neutrals.surfaceMuted.asColor()
    val accent = t.accent.accentAction.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val fontScale = LocalDensity.current.fontScale
    val allowScroll = scrollable || fontScale >= 1.3f

    // Item widths as laid out (equal weights or text widths); the thumb follows them.
    val widths = remember(count) { mutableStateListOf<Float>().apply { repeat(count) { add(0f) } } }
    var rowHeight by remember { mutableIntStateOf(0) }
    val ready = widths.all { it > 0f }
    val scroll = rememberScrollState()

    BoxWithConstraints(
        modifier.fillMaxWidth()
            .background(muted, shape)
            .border(0.75.dp, t.neutrals.divider.asColor(), shape),
    ) {
        val viewportPx = constraints.maxWidth
        val padPx = with(LocalDensity.current) { 4.dp.toPx() }
        if (allowScroll) LaunchedEffect(selected, ready, viewportPx, fontScale) {
            if (ready) {
                val start = thumbOffset(selected.toFloat(), widths)
                val lo = start + widths[selected] + 2 * padPx - viewportPx
                val target = (if (lo > start) start else scroll.value.toFloat().coerceIn(lo, start)).roundToInt().coerceIn(0, scroll.maxValue)
                if (still) scroll.scrollTo(target) else scroll.animateScrollTo(target)
            }
        }
        Box(Modifier.then(if (allowScroll) Modifier.horizontalScroll(scroll) else Modifier).padding(4.dp)) {
            val rowWidth = if (allowScroll) Modifier else Modifier.fillMaxWidth()
            if (glassOn) {
                Row(Modifier.clearAndSetSemantics {}.alpha(0f).layerBackdrop(copyBackdrop).then(rowWidth)) {
                    options.forEachIndexed { i, label ->
                        Box(
                            Modifier.then(if (allowScroll) Modifier.widthIn(min = 48.dp) else Modifier.weight(1f))
                                .heightIn(min = 48.dp).padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { SegmentLabel(label, accent, i == selected) }
                    }
                }
            }

            Box(
                Modifier
                    .graphicsLayer {
                        translationX = thumbOffset(state.value, widths) * if (ltrNow) 1f else -1f
                    }
                    .thumbSurface(
                        state, glassOn, copyBackdrop, shape, profile, t.dark,
                        t.neutrals.surface.asColor(),
                    )
                    .layout { m, c ->
                        val ok = rowHeight > 0 && widths.all { it > 0f }
                        // Equal weights: constant width, so the layout block does not read the animated value.
                        val w = if (!ok) 0 else (if (allowScroll) thumbWidth(state.value, widths) else widths[0]).roundToInt()
                        val h = if (ok) rowHeight else 0
                        val p = m.measure(Constraints.fixed(w, h))
                        layout(w, h) { p.place(0, 0) }
                    },
            )

            Row(
                Modifier
                    .then(rowWidth)
                    .onSizeChanged { rowHeight = it.height }
                    .thumbGestures(
                        state, selected, onSelect, still, enhance, allowDrag = !allowScroll,
                        hitTest = { x -> if (ready) segmentIndexAt(x, widths, ltr) else null },
                        dragTarget = { start, dx -> dragValue(start, dx, widths[0], count, ltr) },
                    )
                    .selectableGroup(),
            ) {
                options.forEachIndexed { i, label ->
                    val on = i == selected
                    Box(
                        Modifier.then(if (allowScroll) Modifier.widthIn(min = 48.dp) else Modifier.weight(1f)).heightIn(min = 48.dp)
                            .onSizeChanged { widths[i] = it.width.toFloat() }
                            .clip(shape)
                            .glassControlFocus(enhance, shape)
                            .selectable(on, interactionSource = null, indication = indication, role = Role.Tab) { onSelect(i) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        SegmentLabel(
                            label, if (on) accent else secondary, on,
                            Modifier.graphicsLayer {
                                // Refracted copy takes over under the thumb while it is glass.
                                alpha = if (glassOn) 1f - state.pressProgress * (1f - abs(state.value - i)).coerceIn(0f, 1f) else 1f
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentLabel(label: String, color: Color, bold: Boolean, modifier: Modifier = Modifier) {
    Text(
        label, modifier, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}
