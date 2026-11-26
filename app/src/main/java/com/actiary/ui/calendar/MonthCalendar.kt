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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.actiary.domain.DayState
import com.actiary.glass.LocalGlassProfile
import com.actiary.glass.glassLayer
import com.actiary.glass.glassLens
import com.actiary.glass.rememberGlassLayer
import com.actiary.theme.LocalThemeTokens
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
    val profile = LocalGlassProfile.current
    val reducedMotion = rememberMotionReduced()
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    val cell = with(density) { CELL_HEIGHT.toPx() }
    val lensWidthPx = with(density) { CELL_CONTENT_WIDTH.toPx() }
    val lensHeightPx = with(density) { CELL_CONTENT_HEIGHT.toPx() }
    val month = state.firstVisibleMonth.yearMonth
    val rows = keptRowIndex(month, month.atEndOfMonth(), state.firstDayOfWeek) + 1
    val kept = keptRowIndex(month, selected, state.firstDayOfWeek)
    val collapseY = if (collapse > 0f) rowOffset(kept, cell, collapse) else 0f
    val gridBackdrop = rememberGlassLayer()
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
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 160),
        label = "calendarLensAlpha",
    )

    LaunchedEffect(selected, month, gridWidthPx, ltr, reducedMotion, state.firstDayOfWeek) {
        if (gridWidthPx <= 0f || !selectedInMonth) return@LaunchedEffect
        val colWidth = gridWidthPx / 7f
        val rawCol = ((selected.dayOfWeek.value - state.firstDayOfWeek.value) + 7) % 7
        val visualCol = if (ltr) rawCol else 6 - rawCol
        val row = keptRowIndex(month, selected, state.firstDayOfWeek)
        val targetX = visualCol * colWidth + (colWidth - lensWidthPx) / 2f
        val targetY = row * cell + (cell - lensHeightPx) / 2f

        val shouldSnap = !initialized || lastMonth != month || reducedMotion
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
                val liquidSpring = spring<Float>(dampingRatio = 0.76f, stiffness = 420f)
                coroutineScope {
                    launch { lensX.animateTo(targetX, liquidSpring) }
                    launch { lensY.animateTo(targetY, liquidSpring) }
                    launch {
                        travelPulse.animateTo(1f, tween(durationMillis = 90))
                        travelPulse.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 360f))
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
            for (d in daysOfWeek(firstDayOfWeek = state.firstDayOfWeek)) {
                BasicText(
                    d.getDisplayName(NameStyle.SHORT, locale),
                    Modifier.weight(1f).padding(vertical = 6.dp),
                    style = TextStyle(t.neutrals.textSecondary.asColor(), 12.sp, textAlign = TextAlign.Center),
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
            Box(Modifier.fillMaxWidth().glassLayer(gridBackdrop)) {
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
                                reducedMotion,
                            )
                        } else Box(Modifier.height(CELL_HEIGHT))
                    },
                )
            }
            if (gridWidthPx > 0f && lensAlpha > 0.001f) {
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
                        color = if (today) accentColor else t.neutrals.textPrimary.asColor(),
                        fontSize = 14.sp,
                        fontWeight = when {
                            selected || today -> FontWeight.Bold
                            state == DayState.EXERCISED -> FontWeight.Medium
                            else -> FontWeight.Normal
                        },
                    ),
                )
                Box(Modifier.height(ICON_SLOT_HEIGHT), contentAlignment = Alignment.Center) {
                    when (state) {
                        DayState.EXERCISED -> ExercisedCheckIcon(accentColor)
                        DayState.REST -> RestMoonIcon(t.neutrals.textSecondary.asColor())
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
