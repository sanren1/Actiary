package com.actiary.ui.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.P5MotionLevel
import com.actiary.data.appearance.UiStyle
import com.actiary.domain.DayState
import com.actiary.glass.LocalGlassProfile
import com.actiary.glass.glassLayer
import com.actiary.glass.glassLens
import com.actiary.glass.rememberGlassLayer
import com.actiary.p5.P5ShapeVariant
import com.actiary.p5.P5Tokens
import com.actiary.p5.buildP5PolygonPath
import com.actiary.p5.computeP5PolygonPoints
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.ui.motion.rememberMotionReduced
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as NameStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal val CELL_HEIGHT = 48.dp
private val CELL_CONTENT_WIDTH = 40.dp
private val CELL_CONTENT_HEIGHT = 42.dp
private val CELL_CONTENT_SHAPE = RoundedCornerShape(14.dp)
private val ICON_SLOT_HEIGHT = 11.dp

@Composable
fun MonthCalendar(
    state: CalendarState,
    states: Map<LocalDate, DayState>,
    selected: LocalDate,
    today: LocalDate,
    locale: Locale,
    onSelect: (LocalDate) -> Unit,
    collapse: Float = 0f,
    userScrollEnabled: Boolean = true,
    isEnabled: (LocalDate) -> Boolean = { true },
) {
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val profile = LocalGlassProfile.current
    val reducedMotion = rememberMotionReduced()
    val motionStill = reducedMotion || (isP5 && p5.motionLevel == P5MotionLevel.COMPACT)
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    val cell = with(density) { CELL_HEIGHT.toPx() }
    val lensWidthPx = with(density) { CELL_CONTENT_WIDTH.toPx() }
    val lensHeightPx = with(density) { CELL_CONTENT_HEIGHT.toPx() }
    val month = state.firstVisibleMonth.yearMonth
    val rows = keptRowIndex(month, month.atEndOfMonth(), state.firstDayOfWeek) + 1
    val kept = keptRowIndex(month, selected, state.firstDayOfWeek)
    val collapseY = if (collapse > 0f) rowOffset(kept, cell, collapse) else 0f
    val gridBackdrop = if (isP5) null else rememberGlassLayer()
    val accentColor = t.accent.accentAction.asColor()

    var gridWidthPx by remember { mutableFloatStateOf(0f) }
    val lensX = remember { Animatable(0f) }
    val lensY = remember { Animatable(0f) }
    val travelPulse = remember { Animatable(0f) }
    var lastMonth by remember { mutableStateOf<YearMonth?>(null) }
    var initialized by remember { mutableStateOf(false) }
    var travelDirX by remember { mutableFloatStateOf(0f) }
    var travelDirY by remember { mutableFloatStateOf(0f) }

    val selectedInMonth = YearMonth.from(selected) == month
    val showLens = gridWidthPx > 0f && selectedInMonth && !state.isScrollInProgress
    val lensAlpha by animateFloatAsState(
        targetValue = if (showLens) 1f else 0f,
        animationSpec = if (motionStill) snap() else tween(durationMillis = 160),
        label = "calendarLensAlpha",
    )

    LaunchedEffect(selected, month, gridWidthPx, ltr, motionStill, isP5, state.firstDayOfWeek) {
        if (gridWidthPx <= 0f || !selectedInMonth) return@LaunchedEffect
        val colWidth = gridWidthPx / 7f
        val rawCol = ((selected.dayOfWeek.value - state.firstDayOfWeek.value) + 7) % 7
        val visualCol = if (ltr) rawCol else 6 - rawCol
        val row = keptRowIndex(month, selected, state.firstDayOfWeek)
        val targetX = visualCol * colWidth + (colWidth - lensWidthPx) / 2f
        val targetY = row * cell + (cell - lensHeightPx) / 2f

        val shouldSnap = !initialized || lastMonth != month || motionStill
        lastMonth = month
        if (shouldSnap) {
            initialized = true
            lensX.snapTo(targetX)
            lensY.snapTo(targetY)
            travelPulse.snapTo(0f)
        } else {
            val dx = targetX - lensX.value
            val dy = targetY - lensY.value
            val dist = hypot(dx, dy)
            if (dist > 0.5f) {
                travelDirX = abs(dx) / dist
                travelDirY = abs(dy) / dist
                if (isP5) {
                    val p5Spring = spring<Float>(dampingRatio = 0.78f, stiffness = 560f)
                    travelPulse.snapTo(0f)
                    coroutineScope {
                        launch { lensX.animateTo(targetX, p5Spring) }
                        launch { lensY.animateTo(targetY, p5Spring) }
                    }
                } else {
                    val liquidSpring = spring<Float>(dampingRatio = 0.76f, stiffness = 420f)
                    coroutineScope {
                        launch { lensX.animateTo(targetX, liquidSpring) }
                        launch { lensY.animateTo(targetY, liquidSpring) }
                        launch {
                            travelPulse.animateTo(1f, tween(durationMillis = 90))
                            travelPulse.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 360f))
                        }
                    }
                }
            } else {
                lensX.snapTo(targetX)
                lensY.snapTo(targetY)
            }
        }
    }

    Column {
        Row(Modifier.fillMaxWidth()) {
            val weekdayColor = if (isP5) p5.onLightCardMuted.asColor() else t.neutrals.textSecondary.asColor()
            val weekdayWeight = if (isP5) FontWeight.Black else FontWeight.Normal
            for (d in daysOfWeek(firstDayOfWeek = state.firstDayOfWeek)) {
                BasicText(
                    d.getDisplayName(NameStyle.SHORT, locale),
                    Modifier.weight(1f).padding(vertical = 6.dp),
                    style = TextStyle(weekdayColor, 12.sp, fontWeight = weekdayWeight, textAlign = TextAlign.Center),
                )
            }
        }
        // The grid slides inside a clipped box so the kept week row ends up at the top.
        Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { gridWidthPx = it.width.toFloat() }
                .layout { m, c ->
                    val pl = m.measure(c.copy(maxHeight = Constraints.Infinity))
                    // Expanded: let the pager wrap to its tallest visible page; only force the height while collapsing.
                    val h = if (collapse > 0f) collapsedHeight(rows * cell, cell, collapse).roundToInt() else pl.height
                    layout(pl.width, h) { pl.place(0, 0) }
                }
                .clipToBounds(),
        ) {
            Box(
                if (gridBackdrop != null) Modifier.fillMaxWidth().glassLayer(gridBackdrop) else Modifier.fillMaxWidth(),
            ) {
                HorizontalCalendar(
                    modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true)
                        .graphicsLayer { translationY = collapseY },
                    state = state,
                    userScrollEnabled = userScrollEnabled,
                    dayContent = { day ->
                        if (day.position == DayPosition.MonthDate) {
                            // Fully collapsed: rows clipped away must not stay reachable for TalkBack.
                            val hidden = collapse >= 1f && keptRowIndex(YearMonth.from(day.date), day.date, state.firstDayOfWeek) != kept
                            DayCell(
                                day.date,
                                states[day.date] ?: DayState.UNRECORDED,
                                day.date == selected,
                                day.date == today,
                                onSelect,
                                isEnabled(day.date),
                                hidden,
                                motionStill,
                            )
                        } else Box(Modifier.height(CELL_HEIGHT))
                    },
                )
            }
            if (gridWidthPx > 0f && lensAlpha > 0.001f) {
                if (isP5) {
                    P5SelectionFrame(
                        p5 = p5,
                        modifier = Modifier
                            .align(AbsoluteAlignment.TopLeft)
                            .graphicsLayer {
                                translationX = lensX.value
                                translationY = lensY.value + collapseY
                                alpha = lensAlpha
                            }
                            .size(CELL_CONTENT_WIDTH, CELL_CONTENT_HEIGHT),
                    )
                } else if (gridBackdrop != null) {
                    Box(
                        Modifier
                            .align(AbsoluteAlignment.TopLeft)
                            .graphicsLayer {
                                translationX = lensX.value
                                translationY = lensY.value + collapseY
                            }
                            .size(CELL_CONTENT_WIDTH, CELL_CONTENT_HEIGHT)
                            .glassLens(
                                backdrop = gridBackdrop,
                                shape = CELL_CONTENT_SHAPE,
                                profile = profile,
                                dark = t.dark,
                                tint = accentColor,
                                flowPulse = { travelPulse.value },
                                layerBlock = {
                                    val p = travelPulse.value
                                    val baseLift = 1f + 0.04f * p
                                    scaleX = baseLift * (1f + 0.05f * p * travelDirX - 0.02f * p * travelDirY)
                                    scaleY = baseLift * (1f + 0.05f * p * travelDirY - 0.02f * p * travelDirX)
                                    alpha = lensAlpha
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun P5SelectionFrame(p5: P5Tokens, modifier: Modifier = Modifier) {
    val outerStroke = p5.strokeDark.asColor()
    val innerStroke = p5.highlightBadge.asColor()
    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val outerPoints = computeP5PolygonPoints(size.width, size.height, P5ShapeVariant.BADGE)
        val outerPath = buildP5PolygonPath(outerPoints)
        drawPath(
            path = outerPath,
            color = outerStroke,
            style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Miter),
        )
        val inset = 1.75.dp.toPx()
        val innerW = (size.width - inset * 2f).coerceAtLeast(1f)
        val innerH = (size.height - inset * 2f).coerceAtLeast(1f)
        val innerPoints = computeP5PolygonPoints(innerW, innerH, P5ShapeVariant.BADGE)
        val innerPath = buildP5PolygonPath(innerPoints, offsetX = inset, offsetY = inset)
        drawPath(
            path = innerPath,
            color = innerStroke,
            style = Stroke(width = 1.75.dp.toPx(), join = StrokeJoin.Miter),
        )
    }
}

@Composable
private fun P5DayCellBadge(
    state: DayState,
    today: Boolean,
    p5: P5Tokens,
    modifier: Modifier = Modifier,
) {
    if (state == DayState.UNRECORDED && !today) return
    val brandFill = p5.brandFill.asColor()
    val strokeDark = p5.strokeDark.asColor()
    val highlight = p5.highlightBadge.asColor()
    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val pad = 2.dp.toPx()
        val w = (size.width - pad * 2f).coerceAtLeast(1f)
        val h = (size.height - pad * 2f).coerceAtLeast(1f)
        when (state) {
            DayState.EXERCISED -> {
                // Solid skewed diamond-like rhombus badge with thick dark outline
                val skew = (w * 0.14f).coerceAtMost(6.dp.toPx())
                val path = Path().apply {
                    moveTo(pad + skew, pad)
                    lineTo(pad + w, pad + 1.5.dp.toPx())
                    lineTo(pad + w - skew, pad + h)
                    lineTo(pad, pad + h - 1.5.dp.toPx())
                    close()
                }
                drawPath(path = path, color = brandFill)
                drawPath(
                    path = path,
                    color = strokeDark,
                    style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Miter),
                )
                if (today) {
                    val corner = 7.dp.toPx()
                    val marker = Path().apply {
                        moveTo(pad + skew, pad)
                        lineTo(pad + skew + corner, pad)
                        lineTo(pad + skew - 1.5.dp.toPx(), pad + corner)
                        close()
                    }
                    drawPath(path = marker, color = highlight)
                }
            }
            DayState.REST -> {
                val points = computeP5PolygonPoints(w, h, P5ShapeVariant.BADGE)
                val path = buildP5PolygonPath(points, offsetX = pad, offsetY = pad)
                drawPath(path = path, color = Color(0xFF2B2B30))
                clipPath(path) {
                    val step = 5.dp.toPx()
                    val hatchColor = Color(0xFF4A4A54)
                    val strokePx = 1.4.dp.toPx()
                    var x = -h
                    while (x < w + h) {
                        drawLine(
                            color = hatchColor,
                            start = Offset(pad + x, pad + h),
                            end = Offset(pad + x + h, pad),
                            strokeWidth = strokePx,
                        )
                        x += step
                    }
                }
                drawPath(
                    path = path,
                    color = if (today) highlight else strokeDark,
                    style = Stroke(width = if (today) 2.dp.toPx() else 1.5.dp.toPx(), join = StrokeJoin.Miter),
                )
            }
            DayState.UNRECORDED -> {
                // Today unrecorded: high-contrast highlightBadge fill + thick black skewed frame
                val points = computeP5PolygonPoints(w, h, P5ShapeVariant.BADGE)
                val path = buildP5PolygonPath(points, offsetX = pad, offsetY = pad)
                drawPath(path = path, color = highlight)
                drawPath(
                    path = path,
                    color = strokeDark,
                    style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Miter),
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    state: DayState,
    selected: Boolean,
    today: Boolean,
    onSelect: (LocalDate) -> Unit,
    enabled: Boolean,
    hidden: Boolean,
    reducedMotion: Boolean,
) {
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val locale = LocalConfiguration.current.locales[0]
    val stateText = stringResource(when (state) {
        DayState.EXERCISED -> R.string.day_state_logged
        DayState.REST -> R.string.day_rest
        DayState.UNRECORDED -> R.string.day_unrecorded
    })
    val description = listOfNotNull(
        date.format(DateTimeFormatter.ofPattern(stringResource(R.string.day_accessibility_pattern), locale)),
        stateText,
        if (today) stringResource(R.string.day_today) else null,
    ).joinToString(", ")
    val accentColor = t.accent.accentAction.asColor()
    val contentScale by animateFloatAsState(
        targetValue = when {
            !selected -> 1f
            isP5 -> if (reducedMotion) 1.03f else 1.05f
            reducedMotion -> 1.06f
            else -> 1.09f
        },
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.72f, stiffness = 480f),
        label = "dayCellScale",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(CELL_HEIGHT)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, role = Role.Button) { onSelect(date) }
            .semantics(mergeDescendants = true) {
                contentDescription = description; this.selected = selected
                if (hidden) hideFromAccessibility()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(CELL_CONTENT_WIDTH, CELL_CONTENT_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            if (isP5) {
                P5DayCellBadge(
                    state = state,
                    today = today,
                    p5 = p5,
                    modifier = Modifier.matchParentSize(),
                )
            }
            val numberColor = if (isP5) {
                when (state) {
                    DayState.EXERCISED -> p5.onBrandFill.asColor()
                    DayState.REST -> Color.White
                    DayState.UNRECORDED -> if (today) p5.onHighlightBadge.asColor() else p5.onLightCard.asColor()
                }
            } else {
                if (today) accentColor else t.neutrals.textPrimary.asColor()
            }
            val numberWeight = if (isP5) {
                when {
                    selected || today || state == DayState.EXERCISED -> FontWeight.Black
                    state == DayState.REST -> FontWeight.Bold
                    else -> FontWeight.Medium
                }
            } else {
                when {
                    selected || today -> FontWeight.Bold
                    state == DayState.EXERCISED -> FontWeight.Medium
                    else -> FontWeight.Normal
                }
            }
            Column(
                Modifier.graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            ) {
                BasicText(
                    date.dayOfMonth.toString(),
                    style = TextStyle(
                        color = numberColor,
                        fontSize = 14.sp,
                        fontWeight = numberWeight,
                    ),
                )
                Box(Modifier.height(ICON_SLOT_HEIGHT), contentAlignment = Alignment.Center) {
                    when (state) {
                        DayState.EXERCISED -> ExercisedCheckIcon(if (isP5) p5.onBrandFill.asColor() else accentColor)
                        DayState.REST -> RestMoonIcon(if (isP5) Color.White else t.neutrals.textSecondary.asColor())
                        DayState.UNRECORDED -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisedCheckIcon(color: Color) {
    val path = remember { Path() }
    var cachedSize by remember { mutableStateOf(Size.Zero) }
    Canvas(Modifier.size(11.dp)) {
        if (cachedSize != size) {
            cachedSize = size
            path.reset()
            path.moveTo(size.width * 0.17f, size.height * 0.53f)
            path.lineTo(size.width * 0.41f, size.height * 0.77f)
            path.lineTo(size.width * 0.83f, size.height * 0.25f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun RestMoonIcon(color: Color) {
    val path = remember { Path() }
    var cachedSize by remember { mutableStateOf(Size.Zero) }
    Canvas(Modifier.size(11.dp)) {
        if (cachedSize != size) {
            cachedSize = size
            val s = size.minDimension / 24f
            path.reset()
            path.arcTo(
                rect = Rect(3f * s, 3f * s, 21f * s, 21f * s),
                startAngleDegrees = -90f,
                sweepAngleDegrees = -270f,
                forceMoveTo = true,
            )
            path.arcTo(
                rect = Rect(10.14f * s, 1.14f * s, 22.86f * s, 13.86f * s),
                startAngleDegrees = 45f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            path.close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.45.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
