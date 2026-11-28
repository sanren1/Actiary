package com.actiary.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.data.StrengthRepository
import com.actiary.data.appearance.P5MotionLevel
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.UiStyle
import com.actiary.data.db.DayWithDetails
import com.actiary.data.photo.PhotoStore
import com.actiary.domain.computeHomeStreakSummary
import com.actiary.domain.toDayState
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassPanel
import com.actiary.glass.GlassSampling
import com.actiary.glass.rememberGlassLayer
import com.actiary.p5.P5IconButton
import com.actiary.p5.P5ShapeVariant
import com.actiary.p5.p5Surface
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.common.PinnedScrollPage
import com.actiary.ui.common.RootTitle
import com.actiary.ui.motion.Motion
import com.actiary.ui.motion.rememberMotionReduced
import com.kizitonwose.calendar.compose.rememberCalendarState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val BOTTOM_CLEARANCE = 24.dp

internal fun canEditDayDetail(selected: LocalDate, loaded: LocalDate?, displayed: LocalDate?, today: LocalDate): Boolean =
    selected == loaded && selected == displayed && !isFuture(selected, today)

private fun highlightP5Digits(text: String, digitColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        if (text[i].isDigit()) {
            val start = i
            while (i < text.length && text[i].isDigit()) i++
            withStyle(SpanStyle(color = digitColor, fontWeight = FontWeight.Black)) {
                append(text, start, i)
            }
        } else {
            val start = i
            while (i < text.length && !text[i].isDigit()) i++
            append(text, start, i)
        }
    }
}

@Composable
fun HomeScreen(
    repository: RecordRepository,
    strengthRepository: StrengthRepository,
    photoStore: PhotoStore,
    selected: LocalDate,
    firstDayOfWeek: DayOfWeek,
    onSelect: (LocalDate) -> Unit,
    onEdit: (LocalDate) -> Unit,
) {
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val locale = LocalConfiguration.current.locales[0]
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose {}
    }
    val thisMonth = YearMonth.from(today)
    val calendar = rememberCalendarState(
        startMonth = thisMonth.minusYears(10),
        endMonth = thisMonth.plusYears(1),
        firstVisibleMonth = remember { YearMonth.from(selected) },
        firstDayOfWeek = remember { firstDayOfWeek },
    )
    val systemMotionReduced = rememberMotionReduced()
    val animationsOff = systemMotionReduced || (isP5 && p5.motionLevel == P5MotionLevel.COMPACT)
    // Update the saved pager in place so changing the week layout retains the visible month.
    SideEffect { calendar.firstDayOfWeek = firstDayOfWeek }
    var alignedSelection by rememberSaveable { mutableStateOf(selected) }
    LaunchedEffect(selected) {
        if (selected != alignedSelection) {
            val m = YearMonth.from(selected).coerceIn(calendar.startMonth, calendar.endMonth)
            if (m != calendar.firstVisibleMonth.yearMonth) {
                if (animationsOff) calendar.scrollToMonth(m) else calendar.animateScrollToMonth(m)
            }
            alignedSelection = selected
        }
    }
    val visible = calendar.firstVisibleMonth.yearMonth
    // Prefetch both neighbors and retain the previous dated marks until the new window returns.
    val monthStates by remember(repository, visible) {
        repository.observeRange(visible.minusMonths(1).atDay(1), visible.plusMonths(1).atEndOfMonth())
            .map { days -> days.associate { LocalDate.ofEpochDay(it.record.epochDay) to it.record.toDayState() } }
    }.collectAsStateWithLifecycle(emptyMap())
    val recentDays by remember(repository, today) {
        repository.observeRange(today.minusDays(365), today)
    }.collectAsStateWithLifecycle(emptyList())
    val streakSummary = remember(recentDays, today, firstDayOfWeek) {
        computeHomeStreakSummary(recentDays, today, firstDayOfWeek)
    }
    // The collector retains the last dated result while the new date loads.
    val day by remember(repository, selected) { repository.observeDay(selected).map { selected to it } }
        .collectAsStateWithLifecycle(null)
    val types by repository.allTypes.collectAsStateWithLifecycle(emptyList())
    val typesById = remember(types) { types.associateBy { it.id } }
    val exercises by strengthRepository.allExercises.collectAsStateWithLifecycle(emptyList())
    val exercisesById = remember(exercises) { exercises.associateBy { it.id } }
    val dateFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.day_title_pattern), locale)
    val spokenDateFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.day_accessibility_pattern), locale)
    val scope = rememberCoroutineScope()

    // Collapse progress of the calendar: 0 = full month, 1 = only the selected week.
    var p by rememberSaveable { mutableStateOf(0f) }
    // One settle animation at a time; any new drag or settle cancels the previous one.
    var settling by remember { mutableStateOf<Job?>(null) }
    val settle by rememberUpdatedState<(Float) -> Unit> { target ->
        settling?.cancel()
        if (animationsOff) p = target
        else settling = scope.launch { animate(p, target, animationSpec = tween(150)) { v, _ -> p = v } }
    }
    val lastRow = keptRowIndex(visible, visible.atEndOfMonth(), calendar.firstDayOfWeek)
    val rangePx = with(LocalDensity.current) { (CELL_HEIGHT * lastRow).toPx() }
    val range by rememberUpdatedState(rangePx)
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                settling?.cancel()
                if (available.y >= 0f || p >= 1f || range <= 0f) return Offset.Zero
                val next = (p - available.y / range).coerceAtMost(1f)
                val used = -(next - p) * range
                p = next
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                settling?.cancel()
                if (available.y <= 0f || p <= 0f || range <= 0f) return Offset.Zero
                val next = (p - available.y / range).coerceAtLeast(0f)
                val used = -(next - p) * range
                p = next
                return Offset(0f, used)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (p > 0f && p < 1f) settle(snapTarget(p))
                return Velocity.Zero
            }
        }
    }
    val pageScroll = rememberScrollState()

    val calendarLayer = if (isP5) null else rememberGlassLayer()
    val primary = t.neutrals.textPrimary.asColor()
    val p5DigitAccent = if (p5.palette == P5Palette.ROYAL_GOLD) p5.accentOnDark.asColor() else p5.highlightBadge.asColor()
    fun go(target: YearMonth) = scope.launch {
        pageScroll.scrollTo(0)
        if (p > 0f) settle(0f)
        if (animationsOff) calendar.scrollToMonth(target) else calendar.animateScrollToMonth(target)
    }
    PinnedScrollPage(
        scrollOffset = { 0 },
        header = { RootTitle(stringResource(R.string.tab_calendar), Modifier.padding(start = 16.dp, end = 72.dp)) },
    ) { headerHeight, _ ->
        val bottomPadding = LocalFloatingInsets.current.bottom + BOTTOM_CLEARANCE
        Column(
            Modifier.fillMaxSize().nestedScroll(connection)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(start = 16.dp, end = 16.dp, top = headerHeight + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (streakSummary.weekExercisedDays > 0 || streakSummary.currentStreakDays > 0) {
                val progressParts = buildList {
                    if (streakSummary.weekExercisedDays > 0) {
                        add(stringResource(R.string.home_week_progress, streakSummary.weekExercisedDays))
                    }
                    if (streakSummary.currentStreakDays > 0) {
                        add(stringResource(R.string.home_streak_days, streakSummary.currentStreakDays))
                    }
                }
                if (isP5) {
                    Box(
                        Modifier.p5Surface(
                            variant = P5ShapeVariant.BADGE,
                            fillColor = p5.darkCard.asColor(),
                            strokeColor = p5.strokeLight.asColor(),
                            shadowColor = p5.shadowColor.asColor(),
                            strokeWidth = 2.dp,
                            shadowOffset = DpOffset(3.dp, 3.dp),
                        ),
                    ) {
                        Row(
                            Modifier
                                .semantics(mergeDescendants = true) {}
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            progressParts.forEachIndexed { index, part ->
                                if (index > 0) {
                                    BasicText(
                                        "·",
                                        style = TextStyle(p5.brandFill.asColor(), 12.sp, FontWeight.Black),
                                    )
                                }
                                BasicText(
                                    highlightP5Digits(part, p5DigitAccent),
                                    style = TextStyle(p5.strokeLight.asColor(), 12.sp, FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                } else {
                    GlassPanel(shape = RoundedCornerShape(50)) {
                        Row(
                            Modifier
                                .semantics(mergeDescendants = true) {}
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            progressParts.forEachIndexed { index, part ->
                                if (index > 0) {
                                    BasicText(
                                        "·",
                                        style = TextStyle(t.neutrals.textSecondary.asColor(), 12.sp),
                                    )
                                }
                                BasicText(
                                    part,
                                    style = TextStyle(primary, 12.sp, FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            val calendarDragModifier = Modifier.fillMaxWidth().draggable(
                state = rememberDraggableState { delta ->
                    if (range > 0f && (delta < 0f || pageScroll.value == 0)) {
                        settling?.cancel()
                        p = (p - delta / range).coerceIn(0f, 1f)
                    }
                },
                orientation = Orientation.Vertical,
                onDragStopped = { if (p > 0f && p < 1f) settle(snapTarget(p)) },
            )
            val monthLabel = visible.format(DateTimeFormatter.ofPattern(stringResource(R.string.home_month_pattern), locale))
            val hasPrev = visible > calendar.startMonth
            val hasNext = visible < calendar.endMonth
            if (isP5) {
                Box(
                    calendarDragModifier.p5Surface(
                        variant = P5ShapeVariant.PANEL,
                        fillColor = p5.lightCard.asColor(),
                        strokeColor = p5.strokeDark.asColor(),
                        shadowColor = p5.shadowColor.asColor(),
                        strokeWidth = 2.5.dp,
                    ),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .p5Surface(
                                    variant = P5ShapeVariant.BADGE,
                                    fillColor = p5.darkCard.asColor(),
                                    strokeColor = p5.brandFill.asColor(),
                                    shadowColor = p5.strokeDark.asColor(),
                                    strokeWidth = 2.dp,
                                    shadowOffset = DpOffset(3.dp, 3.dp),
                                )
                                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicText(
                                highlightP5Digits(monthLabel, p5DigitAccent),
                                Modifier.weight(1f),
                                style = TextStyle(p5.strokeLight.asColor(), 17.sp, FontWeight.Black),
                            )
                            P5IconButton(
                                icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.home_prev_month),
                                onClick = { go(visible.minusMonths(1)) },
                                enabled = hasPrev,
                                outlined = false,
                                iconSize = 28.dp,
                                tint = p5.strokeLight.asColor(),
                            )
                            P5IconButton(
                                icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.home_next_month),
                                onClick = { go(visible.plusMonths(1)) },
                                enabled = hasNext,
                                outlined = false,
                                iconSize = 28.dp,
                                tint = p5.strokeLight.asColor(),
                            )
                        }
                        MonthCalendar(calendar, monthStates, selected, today, locale, onSelect, collapse = p, userScrollEnabled = p < 1f)
                    }
                }
            } else {
                GlassPanel(
                    calendarDragModifier,
                    exported = calendarLayer,
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicText(
                                monthLabel,
                                Modifier.weight(1f).padding(start = 8.dp),
                                style = TextStyle(primary, 18.sp, FontWeight.SemiBold),
                            )
                            GlassIconButton(
                                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.home_prev_month),
                                { go(visible.minusMonths(1)) }, Modifier,
                                GlassSampling.Background, calendarLayer, hasPrev, iconSize = 28.dp,
                            )
                            GlassIconButton(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.home_next_month),
                                { go(visible.plusMonths(1)) }, Modifier,
                                GlassSampling.Background, calendarLayer, hasNext, iconSize = 28.dp,
                            )
                        }
                        MonthCalendar(calendar, monthStates, selected, today, locale, onSelect, collapse = p, userScrollEnabled = p < 1f)
                    }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(pageScroll).padding(bottom = bottomPadding),
            ) {
                val dayDetailCardContent: @Composable () -> Unit = {
                    val content: @Composable (Pair<LocalDate, DayWithDetails?>?) -> Unit = { shown ->
                        val date = shown?.first ?: selected
                        val details = shown?.second
                        val interaction = when {
                            shown?.first != day?.first -> Modifier.clearAndSetSemantics { hideFromAccessibility() }
                            canEditDayDetail(selected, day?.first, shown?.first, today) -> Modifier.clickable(
                                onClickLabel = stringResource(if (details == null) R.string.record_title else R.string.record_detail_edit),
                                role = Role.Button,
                                onClick = { onEdit(date) },
                            )
                            else -> Modifier
                        }
                        DayDetail(
                            title = date.format(dateFormatter),
                            spokenTitle = date.format(spokenDateFormatter),
                            detail = details,
                            types = typesById,
                            photoStore = photoStore,
                            modifier = interaction.padding(16.dp),
                            loading = shown == null,
                            exercises = exercisesById,
                        )
                    }
                    if (animationsOff) {
                        content(day)
                    } else {
                        AnimatedContent(
                            targetState = day,
                            contentKey = { it?.first },
                            transitionSpec = {
                                val from = initialState?.first
                                val to = targetState?.first
                                val direction = when {
                                    from == null || to == null -> 0
                                    to > from -> 1
                                    else -> -1
                                }
                                val enter = tween<Float>(Motion.FADE_IN_MS)
                                val exit = tween<Float>(Motion.FADE_OUT_MS)
                                (fadeIn(enter) + slideInVertically(tween(Motion.FADE_IN_MS)) {
                                    (it * Motion.DAY_DETAIL_SHIFT * direction).roundToInt()
                                } togetherWith fadeOut(exit) + slideOutVertically(tween(Motion.FADE_OUT_MS)) {
                                    (-it * Motion.DAY_DETAIL_SHIFT * direction).roundToInt()
                                }).using(null)
                            },
                            label = "Day detail",
                        ) { content(it) }
                    }
                }
                if (isP5) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .p5Surface(
                                variant = P5ShapeVariant.CARD,
                                fillColor = if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor(),
                                strokeColor = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor(),
                                shadowColor = if (t.dark) p5.shadowColor.asColor() else p5.brandFill.asColor(),
                            ),
                    ) {
                        dayDetailCardContent()
                    }
                } else {
                    GlassPanel(Modifier.fillMaxWidth()) {
                        dayDetailCardContent()
                    }
                }
            }
        }
    }
}
