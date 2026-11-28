package com.actiary.widget

import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.UiStyle
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.domain.DayState
import com.actiary.domain.toDayState
import com.actiary.p5.resolveP5Tokens
import com.actiary.theme.ThemeTokens
import com.actiary.theme.mix
import java.time.LocalDate

internal data class WidgetVisualStyle(
    val isPersona5: Boolean,
    val backgroundColor: Int,
    val accentFill: Int,
    val onAccentFill: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val strokeColor: Int,
    val highlightColor: Int,
    val isLightSurface: Boolean = false,
)

internal fun resolveWidgetVisualStyle(
    settings: AppearanceSettings,
    tokens: ThemeTokens,
): WidgetVisualStyle {
    if (settings.uiStyle != UiStyle.PERSONA_5) {
        return WidgetVisualStyle(
            isPersona5 = false,
            backgroundColor = tokens.neutrals.surface,
            accentFill = tokens.accent.accentAction,
            onAccentFill = tokens.accent.onAccentAction,
            textPrimary = tokens.neutrals.textPrimary,
            textSecondary = tokens.neutrals.textSecondary,
            strokeColor = tokens.neutrals.divider,
            highlightColor = tokens.accent.accentAction,
        )
    }
    val p5 = resolveP5Tokens(settings)
    if (settings.p5Palette == P5Palette.CALLING_CARD) {
        return WidgetVisualStyle(
            isPersona5 = true,
            backgroundColor = p5.canvas,
            accentFill = p5.brandFill,
            onAccentFill = p5.onBrandFill,
            textPrimary = p5.onLightCard,
            textSecondary = p5.onLightCardMuted,
            strokeColor = p5.strokeDark,
            highlightColor = p5.brandFill,
            isLightSurface = true,
        )
    }
    return WidgetVisualStyle(
        isPersona5 = true,
        backgroundColor = p5.darkCard,
        accentFill = p5.brandFill,
        onAccentFill = p5.onBrandFill,
        textPrimary = p5.strokeLight,
        textSecondary = mix(p5.strokeLight, p5.darkCard, 0.30),
        strokeColor = p5.strokeLight,
        highlightColor = p5.highlightBadge,
        isLightSurface = false,
    )
}

internal enum class WidgetRestAction {
    MARK_REST,
    CLEAR_REST,
    OPEN_EDITOR,
}

internal data class WidgetDayDot(
    val date: LocalDate,
    val state: DayState,
    val isToday: Boolean,
)

internal data class WidgetSnapshot(
    val today: LocalDate,
    val todayState: DayState,
    val typeLabels: List<String>,
    val weekDots: List<WidgetDayDot>,
    val streakDays: Int = 0,
    val strengthSummary: String? = null,
    val recommendedPlanLabel: String? = null,
    val nonFreeformTypeLabels: List<String> = emptyList(),
) {
    val recentExercisedDays: Int get() = weekDots.count { it.state == DayState.EXERCISED }
}

/**
 * Decides what tapping the widget's "Rest day" button should do without risking data loss:
 * - Unrecorded day -> mark as rest directly in background.
 * - Pure rest day (no note, no photos) -> clear back to unrecorded.
 * - Exercised day or rest day with note/photos -> open editor for explicit confirmation.
 */
internal fun decideWidgetRestAction(todayDetail: DayWithDetails?): WidgetRestAction = when {
    todayDetail == null -> WidgetRestAction.MARK_REST
    todayDetail.record.status == DayStatus.REST &&
        todayDetail.record.note.isBlank() &&
        todayDetail.photos.isEmpty() -> WidgetRestAction.CLEAR_REST
    else -> WidgetRestAction.OPEN_EDITOR
}

internal fun resolveWidgetSummary(
    snapshot: WidgetSnapshot,
    checkedInFallback: String,
    restSummary: String,
    emptySummary: String,
    recommendedPlanText: (String) -> String,
): String = when (snapshot.todayState) {
    DayState.EXERCISED -> {
        val strength = snapshot.strengthSummary?.takeIf { it.isNotBlank() }
        when {
            strength != null -> (listOf(strength) + snapshot.nonFreeformTypeLabels).joinToString(" · ")
            snapshot.typeLabels.isNotEmpty() -> snapshot.typeLabels.joinToString(" · ")
            else -> checkedInFallback
        }
    }
    DayState.REST -> restSummary
    DayState.UNRECORDED -> {
        val plan = snapshot.recommendedPlanLabel?.takeIf { it.isNotBlank() }
        if (plan != null) recommendedPlanText(plan) else emptySummary
    }
}

internal fun buildWidgetSnapshot(
    today: LocalDate,
    recentDays: List<DayWithDetails>,
    typesById: Map<Long, ExerciseTypeEntity>,
    streakDays: Int = 0,
    strengthSummary: String? = null,
    recommendedPlanLabel: String? = null,
    labelOf: (ExerciseTypeEntity) -> String,
): WidgetSnapshot {
    val byEpoch = recentDays.associateBy { it.record.epochDay }
    val todayDetail = byEpoch[today.toEpochDay()]
    val todayState = todayDetail?.record?.toDayState() ?: DayState.UNRECORDED
    val todayEntries = if (todayState == DayState.EXERCISED) {
        todayDetail?.entries.orEmpty()
    } else {
        emptyList()
    }
    val labels = todayEntries
        .mapNotNull { entry -> typesById[entry.typeId]?.let(labelOf)?.takeIf(String::isNotBlank) }
        .distinct()
    val nonFreeformLabels = todayEntries
        .mapNotNull { entry ->
            typesById[entry.typeId]
                ?.takeIf { it.builtinKey != "freeform" }
                ?.let(labelOf)
                ?.takeIf(String::isNotBlank)
        }
        .distinct()
    val weekDots = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val state = byEpoch[date.toEpochDay()]?.record?.toDayState() ?: DayState.UNRECORDED
        WidgetDayDot(date = date, state = state, isToday = offset == 0)
    }
    return WidgetSnapshot(
        today = today,
        todayState = todayState,
        typeLabels = labels,
        weekDots = weekDots,
        streakDays = streakDays.coerceAtLeast(0),
        strengthSummary = strengthSummary?.takeIf { todayState == DayState.EXERCISED && it.isNotBlank() },
        recommendedPlanLabel = recommendedPlanLabel?.takeIf { todayState == DayState.UNRECORDED && it.isNotBlank() },
        nonFreeformTypeLabels = nonFreeformLabels,
    )
}
