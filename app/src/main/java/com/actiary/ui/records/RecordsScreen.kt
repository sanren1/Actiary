package com.actiary.ui.records

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bedtime
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.photo.PhotoStore
import com.actiary.data.appearance.ChartPalette
import com.actiary.data.appearance.RecordsView
import com.actiary.domain.Bucket
import com.actiary.domain.ChartMetric
import com.actiary.domain.DateRange
import com.actiary.domain.RangeKind
import com.actiary.domain.rangeOf
import com.actiary.domain.shiftRange
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassPanel
import com.actiary.glass.GlassSegmentedPicker
import com.actiary.theme.ChartColors
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.theme.resolveChartColors
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.common.PinnedScrollPage
import com.actiary.ui.common.RootTitle
import com.actiary.ui.common.SelectionMenuRow
import com.actiary.ui.common.exerciseTypeIcon
import com.actiary.ui.common.exerciseTypeLabel
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
    photoStore: PhotoStore,
    initialView: RecordsView,
    firstDayOfWeek: DayOfWeek,
    chartPalette: ChartPalette,
    onKind: (RangeKind) -> Unit,
    onMetric: (ChartMetric) -> Unit,
    onView: (LocalDate) -> Unit,
) {
    val t = LocalThemeTokens.current
    val colors = remember(chartPalette, t) { resolveChartColors(chartPalette, t) }
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
    val range = rangeOf(LocalDate.ofEpochDay(anchor), KINDS[kindIx], firstDayOfWeek)
    val prev = shiftRange(range, -1, today, firstDayOfWeek)?.takeIf { !it.from.isBefore(today.minusYears(10)) }
    val next = shiftRange(range, 1, today, firstDayOfWeek)

    // Keep the previous result paired with its range until the new query emits.
    val loaded by remember(repository, range) { repository.observeRange(range.from, range.to).map { range to it } }
        .collectAsStateWithLifecycle(null)
    val displayRange = loaded?.first ?: range
    val types by repository.allTypes.collectAsStateWithLifecycle(emptyList())
    val typeById = remember(types) { types.associateBy { it.id } }
    val filter = filterId?.takeIf { it in typeById }
    val presentation = remember(loaded, filter) {
        loaded?.let { (loadedRange, days) -> recordsPresentation(loadedRange, days, filter) }
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
                            types.map { it.id to exerciseTypeLabel(it) }
                        SelectionMenuRow(
                            label = stringResource(R.string.settings_custom_types),
                            options = options,
                            selected = filter,
                            onSelect = { filterId = it },
                        )
                    }
                }
                items(presentation.timeline, key = { if (it is TimelineItem.Day) it.day.record.epochDay else "m${(it as TimelineItem.Month).month}" }) { item ->
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
                            DayCard(item.day, typeById, photoStore, cardDateFmt, spokenDateFmt, animation.then(entrance)) {
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
    val secondary = t.neutrals.textSecondary.asColor()
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            filter()
            RecordsCrossfade(summary, reduced, Modifier.fillMaxWidth(), contentKey = { it.range to it.filter }) { shown, visibility ->
                val stats = shown.stats
                Column(visibility.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Stat(Modifier.weight(1f), stringResource(R.string.records_stat_days), stats.exerciseDays.toString())
                        Stat(Modifier.weight(1f), stringResource(R.string.records_stat_count), stats.entryCount.toString())
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Stat(Modifier.weight(1f), stringResource(R.string.records_stat_duration), stats.totalMinutes?.let { durationText(it) })
                        Stat(Modifier.weight(1f), stringResource(R.string.records_stat_calories), stats.totalCalories?.let { stringResource(R.string.records_kcal, it) })
                    }
                    val top = stats.byType.firstOrNull()?.second ?: 0
                    if (stats.byType.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            stats.byType.forEachIndexed { i, (id, count) ->
                                val type = typeById[id] ?: return@forEachIndexed
                                val fraction = count.toFloat() / top
                                val color = colors.categories[i % colors.categories.size].asColor()
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(exerciseTypeIcon(type), null, Modifier.size(18.dp), tint = secondary)
                                    BasicText(
                                        exerciseTypeLabel(type), Modifier.width(88.dp),
                                        style = TextStyle(t.neutrals.textPrimary.asColor(), 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Canvas(
                                        Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(t.neutrals.surfaceMuted.asColor()),
                                    ) {
                                        val barWidth = size.width * fraction
                                        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - barWidth else 0f
                                        drawRoundRect(color, Offset(x, 0f), Size(barWidth, size.height), CornerRadius(size.height / 2f))
                                    }
                                    BasicText(count.toString(), Modifier.width(28.dp), style = TextStyle(t.neutrals.textPrimary.asColor(), 14.sp, FontWeight.Medium))
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
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(t.neutrals.surface.asColor()).padding(12.dp),
                ) {
                    val chart = RecordsChart(summary.range, summary.filter, metric, summary.stats.buckets)
                    RecordsCrossfade(chart, reduced, Modifier.fillMaxWidth(), contentKey = { Triple(it.range, it.filter, it.metric) }) { shown, visibility ->
                        BarChart(shown, colors, reduced, visibility)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(modifier: Modifier, label: String, value: String?) {
    val t = LocalThemeTokens.current
    Column(modifier) {
        if (value == null) {
            BasicText(stringResource(R.string.records_none), style = TextStyle(t.neutrals.textSecondary.asColor(), 16.sp))
        } else {
            BasicText(value, style = TextStyle(t.neutrals.textPrimary.asColor(), 22.sp, FontWeight.Bold))
        }
        BasicText(label, style = TextStyle(t.neutrals.textSecondary.asColor(), 13.sp))
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
    val buckets = chart.buckets
    val metric = chart.metric
    val values = buckets.map { it.valueOf(metric) }
    val max = values.filterNotNull().maxOrNull()
    val fractions = values.map { if (it == null || max == null) 0f else it.toFloat() / max }
    val heights = if (reduced) null else fractions.map {
        animateFloatAsState(it, Motion.pushSpec(), label = "Bar height")
    }
    val secondary = t.neutrals.textSecondary.asColor()
    // First bucket reaching the maximum is highlighted; ties keep the earliest bar.
    val maxIndex = maxValueIndex(values)
    val series = colors.series.asColor()
    val highlight = colors.highlight.asColor()
    val maxText = if (max == null) stringResource(R.string.records_none) else metricText(metric, max)
    val description = if (max == null) maxText else
        stringResource(R.string.records_chart_desc, stringResource(metric.label), maxText)
    val slot = t.neutrals.divider.asColor()
    fun label(i: Int) = buckets[i].start.let { if (chart.range.kind == RangeKind.YEAR) it.monthValue else it.dayOfMonth }.toString()
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(maxText, style = TextStyle(secondary, 12.sp))
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val slotW = size.width / buckets.size
            val barW = slotW * 0.6f
            val minH = 2.dp.toPx()
            values.indices.forEach { i ->
                val index = if (layoutDirection == LayoutDirection.Rtl) buckets.lastIndex - i else i
                val x = index * slotW + (slotW - barW) / 2
                val fraction = heights?.get(i)?.value ?: fractions[i]
                val h = size.height * fraction
                drawRoundRect(
                    if (h < minH) slot else if (i == maxIndex) highlight else series,
                    Offset(x, size.height - h.coerceAtLeast(minH)), Size(barW, h.coerceAtLeast(minH)),
                    CornerRadius(barW.coerceAtMost(4.dp.toPx())),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(label(0), style = TextStyle(secondary, 12.sp))
            BasicText(label(buckets.size - 1), style = TextStyle(secondary, 12.sp))
        }
    }
}

@Composable
private fun DayCard(
    day: DayWithDetails,
    typeById: Map<Long, ExerciseTypeEntity>,
    photoStore: PhotoStore,
    dateFmt: DateTimeFormatter,
    spokenDateFmt: DateTimeFormatter,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val primary = t.neutrals.textPrimary.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val resources = LocalContext.current.resources
    val photo = day.photos.firstOrNull()
    val date = LocalDate.ofEpochDay(day.record.epochDay)
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(t.neutrals.surface.asColor())
            .clickable(role = Role.Button, onClick = onClick).padding(12.dp),
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
                day.entries.forEach { e ->
                    val type = typeById[e.typeId] ?: return@forEach
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
