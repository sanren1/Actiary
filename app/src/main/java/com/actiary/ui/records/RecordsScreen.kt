package com.actiary.ui.records

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.data.StrengthRepository
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.photo.PhotoStore
import com.actiary.data.appearance.ChartPalette
import com.actiary.data.appearance.RecordsView
import com.actiary.data.appearance.UiStyle
import com.actiary.domain.Bucket
import com.actiary.domain.ChartMetric
import com.actiary.domain.DateRange
import com.actiary.domain.MuscleGroup
import com.actiary.domain.MuscleRangeStats
import com.actiary.domain.RangeKind
import com.actiary.domain.activeTypeIdsInRange
import com.actiary.domain.computeMuscleLastTrainedDaysAgo
import com.actiary.domain.computeMuscleStats
import com.actiary.domain.filterDaysByMuscle
import com.actiary.domain.rangeOf
import com.actiary.domain.resolveEffectiveTypeFilter
import com.actiary.domain.shiftRange
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassPanel
import com.actiary.glass.GlassSegmentedPicker
import com.actiary.p5.P5ShapeVariant
import com.actiary.p5.p5Surface
import com.actiary.theme.ChartColors
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.theme.resolveChartColors
import com.actiary.theme.resolveP5ChartColors
import com.actiary.ui.calendar.buildStrengthSummaryText
import com.actiary.ui.common.AnatomyBodyMap
import com.actiary.ui.common.BodyMapStyle
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.common.PinnedScrollPage
import com.actiary.ui.common.RootTitle
import com.actiary.ui.common.SelectionMenuRow
import com.actiary.ui.common.exerciseTypeIcon
import com.actiary.ui.common.exerciseTypeLabel
import com.actiary.ui.common.formatDaysAgoLabel
import com.actiary.ui.common.labelRes
import com.actiary.ui.motion.Motion
import com.actiary.ui.motion.rememberMotionReduced
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val BOTTOM_CLEARANCE = 24.dp
private val TOP_SPACING = 12.dp
private val KINDS = RangeKind.entries

internal fun nextEntered(entered: Set<Long>, visible: List<Long>): Pair<Set<Long>, List<Long>> {
    val newIds = visible.filterNot { it in entered }
    return (if (newIds.isEmpty()) entered else entered + newIds) to newIds
}

private data class RecordsChart(
    val range: DateRange,
    val filter: Long?,
    val metric: ChartMetric,
    val buckets: List<Bucket>,
)

@get:StringRes
private val ChartMetric.label: Int get() = when (this) {
    ChartMetric.DURATION -> R.string.records_stat_duration
    ChartMetric.CALORIES -> R.string.records_stat_calories
    ChartMetric.COUNT -> R.string.records_stat_count
}

/** Null (no bar) when the bucket has no value for the metric or it is 0. */
private fun Bucket.valueOf(m: ChartMetric): Int? = when (m) {
    ChartMetric.DURATION -> minutes
    ChartMetric.CALORIES -> calories
    ChartMetric.COUNT -> count
}?.takeIf { it > 0 }

/** Index of the first bucket reaching the maximum value, or -1 when no bucket has a value. */
internal fun maxValueIndex(values: List<Int?>): Int {
    val max = values.filterNotNull().maxOrNull() ?: return -1
    return values.indexOfFirst { it == max }
}

@Composable
fun RecordsScreen(
    repository: RecordRepository,
    strengthRepository: StrengthRepository,
    photoStore: PhotoStore,
    initialView: RecordsView,
    firstDayOfWeek: DayOfWeek,
    chartPalette: ChartPalette,
    onKind: (RangeKind) -> Unit,
    onMetric: (ChartMetric) -> Unit,
    onView: (LocalDate) -> Unit,
) {
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val colors = remember(chartPalette, t, isP5, p5) {
        if (isP5) resolveP5ChartColors(p5) else resolveChartColors(chartPalette, t)
    }
    val locale = LocalConfiguration.current.locales[0]
    val reduced = rememberMotionReduced()
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose {}
    }
    var kindIx by rememberSaveable { mutableIntStateOf(KINDS.indexOf(initialView.kind)) }
    var anchor by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var metricIx by rememberSaveable { mutableIntStateOf(initialView.metric.ordinal) }
    var filterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedMuscle by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    val range = rangeOf(LocalDate.ofEpochDay(anchor), KINDS[kindIx], firstDayOfWeek)
    val prev = shiftRange(range, -1, today, firstDayOfWeek)?.takeIf { !it.from.isBefore(today.minusYears(10)) }
    val next = shiftRange(range, 1, today, firstDayOfWeek)

    // Keep the previous result paired with its range until the new query emits.
    val loaded by remember(repository, range) { repository.observeRange(range.from, range.to).map { range to it } }
        .collectAsStateWithLifecycle(null)
    val displayRange = loaded?.first ?: range
    val types by repository.allTypes.collectAsStateWithLifecycle(emptyList())
    val typeById = remember(types) { types.associateBy { it.id } }
    val exercisesOrNull by strengthRepository.allExercises.collectAsStateWithLifecycle(null)
    val exerciseById = remember(exercisesOrNull) { exercisesOrNull.orEmpty().associateBy { it.id } }

    val activeTypeIds = remember(loaded) {
        loaded?.let { (loadedRange, days) -> activeTypeIdsInRange(days, loadedRange) }.orEmpty()
    }
    val activeTypes = remember(types, activeTypeIds) {
        types.filter { it.id in activeTypeIds }
    }
    val effectiveFilter = remember(loaded, filterId, activeTypeIds, typeById) {
        if (loaded == null) {
            filterId?.takeIf { it in typeById }
        } else {
            resolveEffectiveTypeFilter(filterId, activeTypeIds)?.takeIf { it in typeById }
        }
    }
    val muscleStats = remember(loaded, effectiveFilter, exerciseById) {
        val current = loaded
        if (current == null) {
            MuscleRangeStats(emptyList(), emptyMap())
        } else {
            computeMuscleStats(current.second, current.first, exerciseById, typeFilter = effectiveFilter)
        }
    }
    val showMuscleDistribution = muscleStats.ranked.isNotEmpty()
    var latestEpochDayByExercise by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    LaunchedEffect(loaded, today) {
        latestEpochDayByExercise = strengthRepository.latestEpochDayByExercise(today)
    }
    val lastTrainedDaysAgo = remember(latestEpochDayByExercise, today, exerciseById) {
        computeMuscleLastTrainedDaysAgo(latestEpochDayByExercise, today, exerciseById)
    }
    val effectiveSelectedMuscle = selectedMuscle?.takeIf { showMuscleDistribution && it in muscleStats.levels }

    LaunchedEffect(loaded, effectiveFilter) {
        if (loaded != null && filterId != null && effectiveFilter == null) {
            filterId = null
            selectedMuscle = null
        }
    }
    LaunchedEffect(loaded, exercisesOrNull, showMuscleDistribution, muscleStats) {
        if (loaded != null && exercisesOrNull != null && selectedMuscle != null && (!showMuscleDistribution || selectedMuscle !in muscleStats.levels)) {
            selectedMuscle = null
        }
    }

    val presentation = remember(loaded, effectiveFilter) {
        loaded?.let { (loadedRange, days) -> recordsPresentation(loadedRange, days, effectiveFilter) }
    }
    val displayedTimeline = remember(presentation, loaded, effectiveFilter, effectiveSelectedMuscle, exerciseById) {
        val currentPresentation = presentation ?: return@remember emptyList()
        val currentLoaded = loaded ?: return@remember currentPresentation.timeline
        if (effectiveSelectedMuscle == null) {
            currentPresentation.timeline
        } else {
            timelineItems(
                days = filterDaysByMuscle(
                    currentLoaded.second,
                    effectiveSelectedMuscle,
                    exerciseById,
                    typeFilter = effectiveFilter,
                ),
                range = currentLoaded.first,
                typeFilter = effectiveFilter,
            )
        }
    }

    val primary = t.neutrals.textPrimary.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val dayFmt = DateTimeFormatter.ofPattern(stringResource(R.string.records_day_pattern), locale)
    val monthFmt = DateTimeFormatter.ofPattern(stringResource(R.string.home_month_pattern), locale)
    val yearFmt = DateTimeFormatter.ofPattern(stringResource(R.string.records_year_pattern), locale)
    val cardDateFmt = DateTimeFormatter.ofPattern(stringResource(R.string.day_title_pattern), locale)
    val spokenDateFmt = DateTimeFormatter.ofPattern(stringResource(R.string.day_accessibility_pattern), locale)

    val listState = rememberLazyListState()
    val topSpacingPx = with(LocalDensity.current) { TOP_SPACING.roundToPx() }
    var entered by rememberSaveable(
        stateSaver = Saver<Set<Long>, LongArray>(save = { it.toLongArray() }, restore = { it.toSet() }),
    ) { mutableStateOf(emptySet()) }
    LaunchedEffect(listState, topSpacingPx) {
        snapshotFlow {
            val info = listState.layoutInfo
            // Keep the title-covered portion of the viewport out of first-entry tracking.
            val visibleTop = info.viewportStartOffset + info.beforeContentPadding - topSpacingPx
            info.visibleItemsInfo.mapNotNull { item ->
                (item.key as? Long)?.takeIf {
                    item.offset < info.viewportEndOffset && item.offset + item.size > visibleTop
                }
            }
        }.collect { visible ->
            val (seen, newIds) = nextEntered(entered, visible)
            if (newIds.isNotEmpty()) entered = seen
        }
    }
    PinnedScrollPage(
        scrollOffset = { if (listState.firstVisibleItemIndex > 0) Int.MAX_VALUE else listState.firstVisibleItemScrollOffset },
        header = { RootTitle(stringResource(R.string.tab_records), Modifier.padding(horizontal = 16.dp)) },
    ) { headerHeight, _ ->
        val safePadding = WindowInsets.safeDrawing.asPaddingValues()
        val direction = LocalLayoutDirection.current
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = safePadding.calculateStartPadding(direction) + 16.dp,
                end = safePadding.calculateEndPadding(direction) + 16.dp,
                top = headerHeight + TOP_SPACING,
                bottom = LocalFloatingInsets.current.bottom + BOTTOM_CLEARANCE,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "rangePicker") {
                GlassSegmentedPicker(
                    listOf(R.string.records_week, R.string.records_month, R.string.records_year).map { stringResource(it) },
                    kindIx, { kindIx = it; onKind(KINDS[it]) },
                )
            }
            item(key = "rangeHeader") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RecordsCrossfade(displayRange, reduced, Modifier.weight(1f).padding(start = 8.dp)) { shown, visibility ->
                        val text = when (shown.kind) {
                            RangeKind.WEEK -> stringResource(R.string.records_week_range, shown.from.format(dayFmt), shown.to.format(dayFmt))
                            RangeKind.MONTH -> shown.from.format(monthFmt)
                            RangeKind.YEAR -> shown.from.format(yearFmt)
                        }
                        BasicText(text, visibility, style = TextStyle(primary, 18.sp, FontWeight.SemiBold))
                    }
                    GlassIconButton(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.records_prev),
                        { prev?.let { anchor = it.from.toEpochDay() } }, enabled = prev != null, glass = false, iconSize = 28.dp,
                    )
                    GlassIconButton(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.records_next),
                        { next?.let { anchor = it.from.toEpochDay() } }, enabled = next != null, glass = false, iconSize = 28.dp,
                    )
                }
            }
            if (presentation != null) {
                item(key = "summary") {
                    SummaryCard(
                        presentation,
                        ChartMetric.entries[metricIx], { metricIx = it.ordinal; onMetric(it) },
                        typeById, colors, reduced,
                    ) {
                        val options = listOf(null to stringResource(R.string.records_all)) +
                            activeTypes.map { it.id to exerciseTypeLabel(it) }
                        SelectionMenuRow(
                            label = stringResource(R.string.settings_custom_types),
                            options = options,
                            selected = effectiveFilter,
                            onSelect = {
                                if (it != filterId) selectedMuscle = null
                                filterId = it
                            },
                        )
                    }
                }
                if (showMuscleDistribution) {
                    item(key = "muscleDistribution") {
                        val cardAnimation = if (reduced) Modifier else Modifier.animateItem(
                            fadeInSpec = tween(Motion.FADE_IN_MS),
                            fadeOutSpec = tween(Motion.FADE_OUT_MS),
                        )
                        MuscleDistributionCard(
                            muscleStats = muscleStats,
                            lastTrainedDaysAgo = lastTrainedDaysAgo,
                            selectedMuscle = effectiveSelectedMuscle,
                            onToggleMuscle = { muscle ->
                                selectedMuscle = if (selectedMuscle == muscle) null else muscle
                            },
                            onClearMuscle = { selectedMuscle = null },
                            modifier = cardAnimation,
                        )
                    }
                }
                items(displayedTimeline, key = { if (it is TimelineItem.Day) it.day.record.epochDay else "m${(it as TimelineItem.Month).month}" }) { item ->
                    val firstEntrance = item is TimelineItem.Day && item.day.record.epochDay !in entered
                    val animation = if (reduced) Modifier else Modifier.animateItem(
                        fadeInSpec = if (firstEntrance) null else tween(Motion.FADE_IN_MS),
                        fadeOutSpec = tween(Motion.FADE_OUT_MS),
                    )
                    when (item) {
                        is TimelineItem.Month -> BasicText(
                            item.month.format(monthFmt),
                            animation.fillMaxWidth().padding(top = 8.dp).semantics { heading() },
                            style = TextStyle(secondary, 15.sp, FontWeight.SemiBold),
                        )
                        is TimelineItem.Day -> {
                            val id = item.day.record.epochDay
                            val entrance = if (reduced) Modifier else {
                                val opacity = animateFloatAsState(
                                    if (id in entered) 1f else 0f, tween(Motion.FADE_IN_MS), label = "Timeline entry",
                                )
                                Modifier.graphicsLayer { alpha = opacity.value }
                            }
                            DayCard(item.day, typeById, exerciseById, photoStore, cardDateFmt, spokenDateFmt, animation.then(entrance)) {
                                onView(LocalDate.ofEpochDay(id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> RecordsCrossfade(
    target: T,
    reduced: Boolean,
    modifier: Modifier = Modifier,
    contentKey: (T) -> Any? = { it },
    content: @Composable (T, Modifier) -> Unit,
) {
    if (reduced) {
        Box(modifier) { content(target, Modifier) }
    } else {
        AnimatedContent(
            targetState = target,
            modifier = modifier,
            contentKey = contentKey,
            transitionSpec = {
                (fadeIn(tween(Motion.FADE_IN_MS)) togetherWith fadeOut(tween(Motion.FADE_OUT_MS))).using(
                    SizeTransform(clip = false) { _, _ -> tween(Motion.FADE_IN_MS) },
                )
            },
            label = "Records content",
        ) { shown ->
            val semantics = if (contentKey(shown) == contentKey(target)) Modifier
            else Modifier.clearAndSetSemantics { hideFromAccessibility() }
            content(shown, semantics)
        }
    }
}

@Composable
private fun durationText(minutes: Int): String {
    val (h, m) = splitMinutes(minutes)
    return when {
        h == 0 -> stringResource(R.string.records_min, m)
        m == 0 -> stringResource(R.string.records_h, h)
        else -> stringResource(R.string.records_hm, h, m)
    }
}

@Composable
private fun SummaryCard(
    summary: RecordsPresentation,
    metric: ChartMetric,
    onMetric: (ChartMetric) -> Unit,
    typeById: Map<Long, ExerciseTypeEntity>,
    colors: ChartColors,
    reduced: Boolean,
    filter: @Composable () -> Unit,
) {
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val secondary = t.neutrals.textSecondary.asColor()
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            filter()
            val stats = summary.stats
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Stat(Modifier.weight(1f), stringResource(R.string.records_stat_days), stats.exerciseDays.toString(), reduced)
                    Stat(Modifier.weight(1f), stringResource(R.string.records_stat_count), stats.entryCount.toString(), reduced)
                }
                Row(Modifier.fillMaxWidth()) {
                    Stat(Modifier.weight(1f), stringResource(R.string.records_stat_duration), stats.totalMinutes?.let { durationText(it) }, reduced)
                    Stat(Modifier.weight(1f), stringResource(R.string.records_stat_calories), stats.totalCalories?.let { stringResource(R.string.records_kcal, it) }, reduced)
                }
                RecordsCrossfade(stats.byType, reduced, Modifier.fillMaxWidth()) { byType, visibility ->
                    val top = byType.firstOrNull()?.second ?: 0
                    if (byType.isNotEmpty()) {
                        Column(visibility.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            byType.forEachIndexed { i, (id, count) ->
                                val type = typeById[id] ?: return@forEachIndexed
                                val fraction = count.toFloat() / top
                                val color = if (isP5) {
                                    when (i % 3) {
                                        0 -> p5.brandFill.asColor()
                                        1 -> p5.highlightBadge.asColor()
                                        else -> t.accent.accentAction.asColor()
                                    }
                                } else {
                                    colors.categories[i % colors.categories.size].asColor()
                                }
                                val rowPrimary = t.neutrals.textPrimary.asColor()
                                val rowIconTint = if (isP5) rowPrimary.copy(alpha = 0.85f) else secondary
                                val barTrack = if (t.dark) Color(0xFF25272C) else t.neutrals.surfaceMuted.asColor()
                                val barBorder = if (t.dark) p5.strokeLight.asColor().copy(alpha = 0.35f) else p5.strokeDark.asColor()
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(exerciseTypeIcon(type), null, Modifier.size(18.dp), tint = rowIconTint)
                                    BasicText(
                                        exerciseTypeLabel(type), Modifier.width(88.dp),
                                        style = TextStyle(rowPrimary, 14.sp, if (isP5) FontWeight.Bold else FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    if (isP5) {
                                        Canvas(
                                            Modifier
                                                .weight(1f)
                                                .height(10.dp)
                                                .background(barTrack)
                                                .border(1.dp, barBorder),
                                        ) {
                                            val barWidth = size.width * fraction
                                            val x = if (layoutDirection == LayoutDirection.Rtl) size.width - barWidth else 0f
                                            drawRect(color, Offset(x, 0f), Size(barWidth, size.height))
                                        }
                                    } else {
                                        Canvas(
                                            Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(t.neutrals.surfaceMuted.asColor()),
                                        ) {
                                            val barWidth = size.width * fraction
                                            val x = if (layoutDirection == LayoutDirection.Rtl) size.width - barWidth else 0f
                                            drawRoundRect(color, Offset(x, 0f), Size(barWidth, size.height), CornerRadius(size.height / 2f))
                                        }
                                    }
                                    BasicText(
                                        count.toString(),
                                        Modifier.width(28.dp),
                                        style = TextStyle(rowPrimary, 14.sp, if (isP5) FontWeight.Black else FontWeight.Medium),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassSegmentedPicker(
                    ChartMetric.entries.map { stringResource(it.label) }, metric.ordinal,
                    { onMetric(ChartMetric.entries[it]) },
                )
                val chart = RecordsChart(summary.range, summary.filter, metric, summary.stats.buckets)
                if (isP5) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .p5Surface(
                                variant = P5ShapeVariant.CARD,
                                fillColor = p5.lightCard.asColor(),
                                strokeColor = p5.strokeDark.asColor(),
                                shadowColor = p5.shadowColor.asColor(),
                                strokeWidth = 2.5.dp,
                                shadowOffset = DpOffset(4.dp, 4.dp),
                            )
                            .padding(14.dp),
                    ) {
                        BarChart(chart, colors, reduced, Modifier.fillMaxWidth())
                    }
                } else {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(t.neutrals.surface.asColor()).padding(12.dp),
                    ) {
                        BarChart(chart, colors, reduced, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
    if (isP5) {
        Box(
            Modifier
                .fillMaxWidth()
                .p5Surface(
                    variant = P5ShapeVariant.PANEL,
                    fillColor = if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor(),
                    strokeColor = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor(),
                    shadowColor = if (t.dark) p5.shadowColor.asColor() else p5.brandFill.asColor(),
                    strokeWidth = 2.dp,
                    shadowOffset = DpOffset(4.dp, 4.dp),
                ),
        ) {
            content()
        }
    } else {
        GlassPanel(Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun Stat(modifier: Modifier, label: String, value: String?, reduced: Boolean) {
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    Column(modifier) {
        RecordsCrossfade(value, reduced) { shown, visibility ->
            if (shown == null) {
                BasicText(
                    stringResource(R.string.records_none),
                    visibility,
                    style = TextStyle(
                        color = t.neutrals.textSecondary.asColor().copy(alpha = if (isP5) 0.7f else 1f),
                        fontSize = if (isP5) 20.sp else 16.sp,
                        fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            } else {
                BasicText(
                    shown,
                    visibility,
                    style = TextStyle(
                        color = t.neutrals.textPrimary.asColor(),
                        fontSize = if (isP5) 28.sp else 22.sp,
                        fontWeight = if (isP5) FontWeight.Black else FontWeight.Bold,
                    ),
                )
            }
        }
        BasicText(
            label,
            style = TextStyle(
                color = if (isP5) t.accent.accentAction.asColor() else t.neutrals.textSecondary.asColor(),
                fontSize = 13.sp,
                fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun metricText(metric: ChartMetric, v: Int): String = when (metric) {
    ChartMetric.DURATION -> durationText(v)
    ChartMetric.CALORIES -> stringResource(R.string.records_kcal, v)
    ChartMetric.COUNT -> v.toString()
}

@Composable
private fun BarChart(chart: RecordsChart, colors: ChartColors, reduced: Boolean, modifier: Modifier) {
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val buckets = chart.buckets
    val metric = chart.metric
    val values = buckets.map { it.valueOf(metric) }
    val max = values.filterNotNull().maxOrNull()
    val secondary = if (isP5) p5.onLightCardMuted.asColor() else t.neutrals.textSecondary.asColor()
    val maxLabelColor = if (isP5) p5.onLightCard.asColor() else secondary
    // First bucket reaching the maximum is highlighted; ties keep the earliest bar.
    val maxIndex = maxValueIndex(values)
    val series = colors.series.asColor()
    val highlight = colors.highlight.asColor()
    val strokeDark = p5.strokeDark.asColor()
    val maxText = if (max == null) stringResource(R.string.records_none) else metricText(metric, max)
    val description = if (max == null) maxText else
        stringResource(R.string.records_chart_desc, stringResource(metric.label), maxText)
    val slot = if (isP5) p5.onLightCardMuted.asColor().copy(alpha = 0.28f) else t.neutrals.divider.asColor()
    fun label(i: Int) = buckets[i].start.let { if (chart.range.kind == RangeKind.YEAR) it.monthValue else it.dayOfMonth }.toString()

    val targetBars = remember(values, maxIndex, rtl) { buildBarSpecs(values, maxIndex, rtl) }
    var fromBars by remember { mutableStateOf(targetBars) }
    var toBars by remember { mutableStateOf(targetBars) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(targetBars, reduced) {
        if (targetBars == toBars && progress.value >= 1f) return@LaunchedEffect
        if (reduced) {
            fromBars = targetBars
            toBars = targetBars
            progress.snapTo(1f)
        } else {
            fromBars = interpolateBars(fromBars, toBars, progress.value)
            toBars = targetBars
            progress.snapTo(0f)
            progress.animateTo(1f, Motion.pushSpec())
        }
    }
    val displayedBars = interpolateBars(fromBars, toBars, progress.value)
    val ticks = chartAxisTicks(chart.range.kind, buckets.size)
    val tickEntries = ticks.map { it to label(it) }

    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RecordsCrossfade(maxText, reduced) { shownMax, visibility ->
            BasicText(
                shownMax,
                visibility,
                style = TextStyle(
                    color = maxLabelColor,
                    fontSize = 12.sp,
                    fontWeight = if (isP5) FontWeight.Black else FontWeight.Normal,
                ),
            )
        }
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val minH = 2.dp.toPx()
            if (isP5) {
                val gridColor = secondary.copy(alpha = 0.22f)
                val gridStroke = 1.dp.toPx()
                drawLine(gridColor, Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), strokeWidth = gridStroke)
                drawLine(strokeDark.copy(alpha = 0.5f), Offset(0f, size.height - gridStroke / 2f), Offset(size.width, size.height - gridStroke / 2f), strokeWidth = gridStroke)
            }
            displayedBars.forEach { bar ->
                if (bar.alpha <= 0.01f) return@forEach
                val barW = size.width * bar.widthNorm
                val x = size.width * bar.centerNorm - barW / 2f
                val h = size.height * bar.fraction
                val drawnH = h.coerceAtLeast(minH)
                val topLeft = Offset(x, size.height - drawnH)
                val barSize = Size(barW, drawnH)
                val baseColor = if (h < minH) slot else if (bar.isMax) highlight else series
                if (isP5) {
                    drawRect(
                        color = baseColor.copy(alpha = baseColor.alpha * bar.alpha),
                        topLeft = topLeft,
                        size = barSize,
                    )
                    if (h >= minH) {
                        drawRect(
                            color = strokeDark.copy(alpha = bar.alpha),
                            topLeft = topLeft,
                            size = barSize,
                            style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Miter),
                        )
                    }
                } else {
                    drawRoundRect(
                        baseColor.copy(alpha = baseColor.alpha * bar.alpha),
                        topLeft,
                        barSize,
                        CornerRadius(barW.coerceAtMost(4.dp.toPx())),
                    )
                }
            }
        }
        RecordsCrossfade(tickEntries to buckets.size, reduced, Modifier.fillMaxWidth()) { (shownTicks, bucketCount), visibility ->
            Layout(
                content = {
                    shownTicks.forEach { (_, text) ->
                        BasicText(
                            text,
                            style = TextStyle(
                                color = secondary,
                                fontSize = 12.sp,
                                fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal,
                            ),
                            maxLines = 1,
                        )
                    }
                },
                modifier = visibility.fillMaxWidth(),
            ) { measurables, constraints ->
                val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = measurables.map { it.measure(childConstraints) }
                val width = constraints.maxWidth
                val height = placeables.maxOfOrNull { it.height } ?: 0
                val slotW = if (bucketCount == 0) 0f else width.toFloat() / bucketCount
                val isRtl = layoutDirection == LayoutDirection.Rtl
                val lastIdx = bucketCount - 1
                layout(width, height) {
                    shownTicks.forEachIndexed { idx, (tick, _) ->
                        val placeable = placeables[idx]
                        val maxX = (width - placeable.width).coerceAtLeast(0)
                        val x = when (tick) {
                            0 -> if (isRtl) maxX else 0
                            lastIdx -> if (isRtl) 0 else maxX
                            else -> {
                                val visualIndex = if (isRtl) lastIdx - tick else tick
                                val center = (visualIndex + 0.5f) * slotW
                                (center - placeable.width / 2f).roundToInt().coerceIn(0, maxX)
                            }
                        }
                        placeable.place(x, 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleDistributionCard(
    muscleStats: MuscleRangeStats,
    lastTrainedDaysAgo: Map<MuscleGroup, Int>,
    selectedMuscle: MuscleGroup?,
    onToggleMuscle: (MuscleGroup) -> Unit,
    onClearMuscle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val primary = t.neutrals.textPrimary.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val accent = if (isP5) p5.brandFill.asColor() else t.accent.accentAction.asColor()
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val p5Unworked = if (t.dark) Color(0xFF25272C) else Color(0xFFD8D6CE)
    val p5Level1 = if (t.dark) Color(0xFF7A141A) else p5.brandFill.asColor().copy(alpha = 0.42f)

    val cardContent: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicText(
                    stringResource(R.string.records_muscle_title),
                    Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(primary, 16.sp, if (isP5) FontWeight.Black else FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selectedMuscle != null) {
                    val chipShape = RoundedCornerShape(50)
                    val clearLabel = stringResource(R.string.strength_picker_clear_muscle)
                    val muscleName = stringResource(selectedMuscle.labelRes())
                    val selectedStat = muscleStats.ranked.firstOrNull { it.muscle == selectedMuscle }
                    val chipLabel = if (selectedStat != null) {
                        stringResource(
                            R.string.records_muscle_filtered_stats,
                            muscleName,
                            selectedStat.days,
                            selectedStat.totalSets,
                        )
                    } else {
                        stringResource(R.string.records_muscle_filtered, muscleName)
                    }
                    val chipModifier = if (isP5) {
                        Modifier
                            .widthIn(max = 220.dp)
                            .p5Surface(
                                variant = P5ShapeVariant.BADGE,
                                fillColor = p5.brandFill.asColor(),
                                strokeColor = p5Stroke,
                                shadowColor = p5.strokeDark.asColor(),
                                strokeWidth = 1.5.dp,
                                shadowOffset = DpOffset(2.dp, 2.dp),
                            )
                            .clickable(role = Role.Button, onClickLabel = clearLabel, onClick = onClearMuscle)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    } else {
                        Modifier
                            .widthIn(max = 220.dp)
                            .clip(chipShape)
                            .background(t.accent.accentSubtle.asColor())
                            .border(1.dp, t.accent.accentStroke.asColor(), chipShape)
                            .clickable(role = Role.Button, onClickLabel = clearLabel, onClick = onClearMuscle)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    }
                    val chipFg = if (isP5) p5.onBrandFill.asColor() else accent
                    Row(
                        chipModifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BasicText(
                            chipLabel,
                            Modifier.weight(1f, fill = false),
                            style = TextStyle(chipFg, 12.sp, if (isP5) FontWeight.Bold else FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            Icons.Outlined.Close,
                            clearLabel,
                            Modifier.size(14.dp),
                            tint = chipFg,
                        )
                    }
                }
            }

            Column(
                if (isP5) {
                    Modifier
                        .fillMaxWidth()
                        .background(p5.canvas.asColor())
                        .border(2.dp, if (t.dark) p5.strokeLight.asColor().copy(alpha = 0.5f) else p5.strokeDark.asColor())
                        .padding(12.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(t.neutrals.surface.asColor())
                        .padding(12.dp)
                },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnatomyBodyMap(
                    style = BodyMapStyle.Heatmap(muscleStats.levels, selected = selectedMuscle),
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                    onMuscleClick = { muscle ->
                        if (muscle in muscleStats.levels) {
                            onToggleMuscle(muscle)
                        }
                    },
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            stringResource(R.string.records_muscle_legend_less),
                            style = TextStyle(secondary, 11.sp, if (isP5) FontWeight.Bold else FontWeight.Normal),
                        )
                        val swatches = if (isP5) {
                            listOf(
                                p5Unworked,
                                p5Level1,
                                p5.brandFill.asColor(),
                                p5.highlightBadge.asColor(),
                            )
                        } else {
                            val neutralFill = secondary.copy(alpha = if (t.dark) 0.12f else 0.085f)
                            listOf(
                                neutralFill,
                                accent.copy(alpha = 0.28f),
                                accent.copy(alpha = 0.60f),
                                accent.copy(alpha = 0.95f),
                            )
                        }
                        swatches.forEach { swatchColor ->
                            Box(
                                if (isP5) {
                                    Modifier
                                        .size(10.dp)
                                        .background(swatchColor)
                                        .border(1.dp, if (t.dark) p5.strokeLight.asColor().copy(alpha = 0.55f) else p5.strokeDark.asColor())
                                } else {
                                    Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(swatchColor)
                                },
                            )
                        }
                        BasicText(
                            stringResource(R.string.records_muscle_legend_more),
                            style = TextStyle(secondary, 11.sp, if (isP5) FontWeight.Bold else FontWeight.Normal),
                        )
                    }
                }
            }

            val maxSets = muscleStats.ranked.first().totalSets.coerceAtLeast(1)
            val barTrack = if (isP5) p5Unworked else t.neutrals.surfaceMuted.asColor()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                muscleStats.ranked.chunked(2).forEach { rowStats ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowStats.forEach { stat ->
                            val isSelected = selectedMuscle == stat.muscle
                            val fraction = (stat.totalSets / maxSets.toFloat()).coerceIn(0f, 1f)
                            val tileShape = RoundedCornerShape(10.dp)
                            val barColor = if (isP5) {
                                when {
                                    isSelected -> p5.highlightBadge.asColor()
                                    stat.heatLevel == 3 -> p5.highlightBadge.asColor()
                                    stat.heatLevel == 2 -> p5.brandFill.asColor()
                                    else -> p5Level1
                                }
                            } else {
                                when {
                                    isSelected -> accent
                                    stat.heatLevel == 3 -> accent.copy(alpha = 0.95f)
                                    stat.heatLevel == 2 -> accent.copy(alpha = 0.72f)
                                    else -> accent.copy(alpha = 0.45f)
                                }
                            }
                            val daysText = stringResource(R.string.records_muscle_days, stat.days)
                            val setsText = stringResource(R.string.records_muscle_sets, stat.totalSets)
                            val tileModifier = if (isP5) {
                                Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) t.accent.accentSubtle.asColor()
                                        else if (t.dark) p5.canvas.asColor() else t.neutrals.surfaceMuted.asColor(),
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) p5.brandFill.asColor() else if (t.dark) p5.strokeLight.asColor().copy(alpha = 0.4f) else p5.strokeDark.asColor(),
                                    )
                                    .clickable(role = Role.Button) { onToggleMuscle(stat.muscle) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            } else {
                                Modifier
                                    .weight(1f)
                                    .clip(tileShape)
                                    .background(
                                        if (isSelected) t.accent.accentSubtle.asColor()
                                        else t.neutrals.surface.asColor(),
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(1.dp, t.accent.accentStroke.asColor(), tileShape)
                                        else Modifier,
                                    )
                                    .clickable(role = Role.Button) { onToggleMuscle(stat.muscle) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            }
                            Column(
                                tileModifier,
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    BasicText(
                                        stringResource(stat.muscle.labelRes()),
                                        Modifier.weight(1f),
                                        style = TextStyle(
                                            color = if (isSelected && !isP5) accent else primary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isP5) FontWeight.Black else if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val daysAgo = lastTrainedDaysAgo[stat.muscle]
                                    if (daysAgo != null) {
                                        BasicText(
                                            formatDaysAgoLabel(daysAgo),
                                            style = TextStyle(
                                                color = if (isP5) t.accent.accentAction.asColor() else secondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal,
                                            ),
                                            maxLines = 1,
                                        )
                                    }
                                }
                                BasicText(
                                    "$daysText · $setsText",
                                    style = TextStyle(
                                        color = secondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isP5) FontWeight.Medium else FontWeight.Normal,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isP5) {
                                    Canvas(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .background(barTrack),
                                    ) {
                                        val barWidth = size.width * fraction
                                        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - barWidth else 0f
                                        drawRect(barColor, Offset(x, 0f), Size(barWidth, size.height))
                                    }
                                } else {
                                    Canvas(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(barTrack),
                                    ) {
                                        val barWidth = size.width * fraction
                                        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - barWidth else 0f
                                        drawRoundRect(
                                            barColor,
                                            Offset(x, 0f),
                                            Size(barWidth, size.height),
                                            CornerRadius(size.height / 2f),
                                        )
                                    }
                                }
                            }
                        }
                        if (rowStats.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    if (isP5) {
        Box(
            modifier
                .fillMaxWidth()
                .p5Surface(
                    variant = P5ShapeVariant.PANEL,
                    fillColor = if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor(),
                    strokeColor = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor(),
                    shadowColor = if (t.dark) p5.shadowColor.asColor() else p5.brandFill.asColor(),
                    strokeWidth = 2.dp,
                    shadowOffset = DpOffset(4.dp, 4.dp),
                ),
        ) {
            cardContent()
        }
    } else {
        GlassPanel(modifier.fillMaxWidth()) {
            cardContent()
        }
    }
}

@Composable
private fun DayCard(
    day: DayWithDetails,
    typeById: Map<Long, ExerciseTypeEntity>,
    exerciseById: Map<Long, StrengthExerciseEntity>,
    photoStore: PhotoStore,
    dateFmt: DateTimeFormatter,
    spokenDateFmt: DateTimeFormatter,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val primary = t.neutrals.textPrimary.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val resources = LocalContext.current.resources
    val photo = day.photos.firstOrNull()
    val date = LocalDate.ofEpochDay(day.record.epochDay)
    val rowModifier = if (isP5) {
        modifier
            .fillMaxWidth()
            .p5Surface(
                variant = P5ShapeVariant.CARD,
                fillColor = if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor(),
                strokeColor = if (t.dark) p5.strokeLight.asColor().copy(alpha = 0.6f) else p5.strokeDark.asColor(),
                shadowColor = if (t.dark) p5.shadowColor.asColor() else p5.brandFill.asColor(),
                strokeWidth = 1.5.dp,
                shadowOffset = DpOffset(3.dp, 3.dp),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp)
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(t.neutrals.surface.asColor())
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp)
    }
    Row(
        rowModifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(
                date.format(dateFmt), Modifier.clearAndSetSemantics { contentDescription = date.format(spokenDateFmt) },
                style = TextStyle(primary, 15.sp, FontWeight.SemiBold),
            )
            if (day.record.status == DayStatus.REST) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Bedtime, null, Modifier.size(18.dp), tint = secondary)
                    BasicText(stringResource(R.string.day_rest), style = TextStyle(secondary, 14.sp))
                }
            } else if (day.entries.isEmpty()) {
                BasicText(stringResource(R.string.day_checked_in), style = TextStyle(secondary, 14.sp))
            } else {
                day.entriesWithLogs.sortedBy { it.entry.sortOrder }.forEach { entryWithLogs ->
                    val e = entryWithLogs.entry
                    val type = typeById[e.typeId] ?: return@forEach
                    val strengthSummary = buildStrengthSummaryText(
                        logs = entryWithLogs.strengthLogs,
                        exercises = exerciseById,
                        muscleLabel = { resources.getString(it.labelRes()) },
                        countsLabel = { count, sets -> resources.getString(R.string.strength_summary_counts, count, sets) },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(exerciseTypeIcon(type), null, Modifier.size(18.dp), tint = primary)
                            BasicText(exerciseTypeLabel(type), style = TextStyle(primary, 14.sp))
                            val metrics = entryMetrics(e, type) { unit, value -> resources.getString(unit, value) }
                                .filterNot { (label, _) -> label == R.string.record_heart_rate }
                                .joinToString(" · ") { it.second }
                            if (metrics.isNotEmpty()) {
                                BasicText(
                                    metrics, Modifier.weight(1f), style = TextStyle(secondary, 13.sp),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (strengthSummary != null) {
                            BasicText(
                                strengthSummary,
                                Modifier.padding(start = 24.dp),
                                style = TextStyle(secondary, 12.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (day.record.note.isNotBlank()) {
                BasicText(day.record.note, style = TextStyle(secondary, 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (photo != null) {
            AsyncImage(
                photoStore.file(photo.fileName), stringResource(R.string.day_photo, 1),
                Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(t.neutrals.surfaceMuted.asColor()),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
