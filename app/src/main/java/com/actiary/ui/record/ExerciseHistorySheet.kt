package com.actiary.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.StrengthRepository
import com.actiary.data.appearance.UiStyle
import com.actiary.data.db.ExercisePerformanceRow
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.domain.formatWeightKg
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassSheet
import com.actiary.p5.P5ShapeVariant
import com.actiary.p5.p5Surface
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.ui.common.exerciseDisplayName
import com.actiary.ui.common.formatStrengthItemMetrics
import com.actiary.ui.common.isChineseLocale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExerciseHistorySheet(
    visible: Boolean,
    exercise: StrengthExerciseEntity?,
    strengthRepository: StrengthRepository,
    onDismiss: () -> Unit,
    onExited: () -> Unit = {},
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val p5Shadow = if (t.dark) p5.shadowColor.asColor() else p5.strokeDark.asColor()
    val focus = LocalFocusManager.current
    val isChinese = isChineseLocale()
    val locale = LocalConfiguration.current.locales[0]

    var retainedExercise by remember { mutableStateOf(exercise) }
    if (exercise != null) {
        retainedExercise = exercise
    }
    val shownExercise = exercise ?: retainedExercise

    val targetId = shownExercise?.id
    var history by remember(targetId) { mutableStateOf<List<ExercisePerformanceRow>?>(null) }
    LaunchedEffect(visible, targetId) {
        if (visible && targetId != null) {
            history = strengthRepository.getExerciseHistory(targetId, limit = 20)
        }
    }

    val title = shownExercise?.let { exerciseDisplayName(it, isChinese) }.orEmpty()
    val dateFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.day_accessibility_pattern), locale)

    fun requestClose(): Boolean {
        focus.clearFocus()
        onDismiss()
        return true
    }

    GlassSheet(
        visible = visible,
        onRequestClose = ::requestClose,
        onExited = {
            if (!visible) retainedExercise = null
            onExited()
        },
        title = title,
    ) {
        if (shownExercise == null) return@GlassSheet

        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BasicText(
                        title,
                        Modifier.semantics { heading() },
                        style = TextStyle(n.textPrimary.asColor(), 19.sp, if (isP5) FontWeight.Black else FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        stringResource(R.string.strength_history_title),
                        style = TextStyle(n.textSecondary.asColor(), 12.sp),
                        maxLines = 1,
                    )
                }
                GlassIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.overlay_close),
                    onClick = { requestClose() },
                    glass = false,
                )
            }

            val loadedHistory = history
            when {
                loadedHistory == null -> Unit
                loadedHistory.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (isP5) {
                                    Modifier.p5Surface(
                                        variant = P5ShapeVariant.CARD,
                                        fillColor = n.surface.asColor(),
                                        strokeColor = p5Stroke,
                                        shadowColor = p5Shadow,
                                        strokeWidth = 2.dp,
                                        shadowOffset = DpOffset(2.dp, 2.dp),
                                    )
                                } else {
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(n.surface.asColor())
                                },
                            )
                            .padding(horizontal = 14.dp, vertical = 18.dp),
                    ) {
                        BasicText(
                            stringResource(R.string.strength_history_empty),
                            style = TextStyle(n.textSecondary.asColor(), 14.sp),
                        )
                    }
                }
                else -> {
                    val bestWeight = loadedHistory.mapNotNull { it.weightKg }.filter { it > 0f }.maxOrNull()
                    val bestReps = loadedHistory.maxOfOrNull { it.reps }?.takeIf { it > 0 }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HistorySummaryPill(
                            text = stringResource(R.string.strength_history_sessions, loadedHistory.size),
                            highlighted = true,
                        )
                        if (bestWeight != null) {
                            HistorySummaryPill(
                                text = stringResource(R.string.strength_history_best_weight, formatWeightKg(bestWeight)),
                            )
                        }
                        if (bestReps != null) {
                            HistorySummaryPill(
                                text = stringResource(
                                    if (shownExercise.isTimed) R.string.strength_history_best_secs
                                    else R.string.strength_history_best_reps,
                                    bestReps,
                                ),
                            )
                        }
                    }

                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        itemsIndexed(
                            items = loadedHistory,
                            key = { index, row -> "${row.epochDay}_$index" },
                        ) { _, row ->
                            val dateText = LocalDate.ofEpochDay(row.epochDay).format(dateFormatter)
                            val metricsText = formatStrengthItemMetrics(
                                sets = row.sets,
                                reps = row.reps,
                                weightKg = row.weightKg,
                                isTimed = shownExercise.isTimed,
                            )
                            val rowShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(rowShape)
                                    .background(n.surface.asColor())
                                    .then(
                                        if (isP5) {
                                            Modifier.border(
                                                1.5.dp,
                                                p5Stroke.copy(alpha = if (t.dark) 0.6f else 0.85f),
                                                rowShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                BasicText(
                                    dateText,
                                    Modifier.weight(1f),
                                    style = TextStyle(n.textSecondary.asColor(), 13.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                BasicText(
                                    metricsText,
                                    style = TextStyle(n.textPrimary.asColor(), 14.sp, if (isP5) FontWeight.Bold else FontWeight.Medium),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryPill(
    text: String,
    highlighted: Boolean = false,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .then(
                if (isP5) {
                    Modifier.p5Surface(
                        variant = P5ShapeVariant.BADGE,
                        fillColor = if (highlighted) p5.brandFill.asColor() else n.surface.asColor(),
                        strokeColor = p5Stroke,
                        shadowColor = if (highlighted) p5.strokeDark.asColor() else Color.Transparent,
                        strokeWidth = if (highlighted) 2.dp else 1.5.dp,
                        shadowOffset = if (highlighted) DpOffset(2.dp, 2.dp) else DpOffset.Zero,
                    )
                } else {
                    Modifier
                        .clip(shape)
                        .background(if (highlighted) a.accentSubtle.asColor() else n.surfaceMuted.asColor())
                },
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text,
            style = TextStyle(
                color = when {
                    isP5 && highlighted -> p5.onBrandFill.asColor()
                    isP5 -> n.textPrimary.asColor()
                    highlighted -> a.accentAction.asColor()
                    else -> n.textPrimary.asColor()
                },
                fontSize = 12.sp,
                fontWeight = if (isP5) FontWeight.Bold else FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}
