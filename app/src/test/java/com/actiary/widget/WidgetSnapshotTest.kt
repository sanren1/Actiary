package com.actiary.widget

import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.UiStyle
import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.PhotoAttachmentEntity
import com.actiary.domain.DayState
import com.actiary.theme.contrast
import com.actiary.theme.resolveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetSnapshotTest {
    private val today = LocalDate.of(2026, 11, 22)

    private fun day(
        date: LocalDate,
        status: DayStatus,
        note: String = "",
        typeIds: List<Long> = emptyList(),
        photos: Int = 0,
    ) = DayWithDetails(
        record = DailyRecordEntity(date.toEpochDay(), status, note, 0L),
        entries = typeIds.mapIndexed { idx, id ->
            ExerciseEntryEntity(0, date.toEpochDay(), id, 30, null, null, null, idx)
        },
        photos = List(photos) { idx ->
            PhotoAttachmentEntity(0, date.toEpochDay(), "p$idx.jpg", idx, 0L)
        },
    )

    @Test
    fun decideWidgetRestActionProtectsExistingWorkoutsAndNotes() {
        assertEquals(WidgetRestAction.MARK_REST, decideWidgetRestAction(null))
        assertEquals(WidgetRestAction.CLEAR_REST, decideWidgetRestAction(day(today, DayStatus.REST)))
        assertEquals(
            WidgetRestAction.OPEN_EDITOR,
            decideWidgetRestAction(day(today, DayStatus.REST, note = "recovering")),
        )
        assertEquals(
            WidgetRestAction.OPEN_EDITOR,
            decideWidgetRestAction(day(today, DayStatus.REST, photos = 1)),
        )
        assertEquals(
            WidgetRestAction.OPEN_EDITOR,
            decideWidgetRestAction(day(today, DayStatus.EXERCISED, typeIds = listOf(1L))),
        )
    }

    @Test
    fun buildWidgetSnapshotBuildsSevenDayStripAndDistinctLabels() {
        val types = mapOf(
            1L to ExerciseTypeEntity(id = 1L, builtinKey = "running", name = "Running", sortOrder = 0),
            2L to ExerciseTypeEntity(id = 2L, builtinKey = "cycling", name = "Cycling", sortOrder = 1),
        )
        val recent = listOf(
            day(today.minusDays(6), DayStatus.EXERCISED, typeIds = listOf(1L)),
            day(today.minusDays(3), DayStatus.REST),
            day(today, DayStatus.EXERCISED, typeIds = listOf(1L, 2L, 1L)),
        )

        val snapshot = buildWidgetSnapshot(today, recent, types) { it.name.orEmpty() }

        assertEquals(DayState.EXERCISED, snapshot.todayState)
        assertEquals(2, snapshot.recentExercisedDays)
        assertEquals(listOf("Running", "Cycling"), snapshot.typeLabels)
        assertEquals(7, snapshot.weekDots.size)
        assertEquals(today.minusDays(6), snapshot.weekDots.first().date)
        assertEquals(today, snapshot.weekDots.last().date)
        assertTrue(snapshot.weekDots.last().isToday)
        assertFalse(snapshot.weekDots.first().isToday)
        assertEquals(
            listOf(
                DayState.EXERCISED,
                DayState.UNRECORDED,
                DayState.UNRECORDED,
                DayState.REST,
                DayState.UNRECORDED,
                DayState.UNRECORDED,
                DayState.EXERCISED,
            ),
            snapshot.weekDots.map { it.state },
        )
        assertEquals(0, snapshot.streakDays)
        assertNull(snapshot.strengthSummary)
        assertNull(snapshot.recommendedPlanLabel)
    }

    @Test
    fun buildWidgetSnapshotAndResolveSummaryForStrengthAndNonFreeformTypes() {
        val types = mapOf(
            1L to ExerciseTypeEntity(id = 1L, builtinKey = "freeform", name = "Freeform", sortOrder = 0),
            2L to ExerciseTypeEntity(id = 2L, builtinKey = "running", name = "Running", sortOrder = 1),
        )
        val recent = listOf(
            day(today.minusDays(2), DayStatus.EXERCISED, typeIds = listOf(2L)),
            day(today.minusDays(1), DayStatus.EXERCISED, typeIds = listOf(1L)),
            day(today, DayStatus.EXERCISED, typeIds = listOf(1L, 2L)),
        )

        val snapshot = buildWidgetSnapshot(
            today = today,
            recentDays = recent,
            typesById = types,
            streakDays = 5,
            strengthSummary = "胸大肌 · 肱三头肌 · 3 动作 · 10 组",
            recommendedPlanLabel = "拉日",
        ) { it.name.orEmpty() }

        assertEquals(5, snapshot.streakDays)
        assertEquals("胸大肌 · 肱三头肌 · 3 动作 · 10 组", snapshot.strengthSummary)
        assertNull(snapshot.recommendedPlanLabel)
        assertEquals(listOf("Running"), snapshot.nonFreeformTypeLabels)
        assertEquals(
            "胸大肌 · 肱三头肌 · 3 动作 · 10 组 · Running",
            resolveWidgetSummary(
                snapshot = snapshot,
                checkedInFallback = "Checked in",
                restSummary = "Rest day",
                emptySummary = "No workout logged today",
                recommendedPlanText = { "Next: $it" },
            ),
        )
    }

    @Test
    fun buildWidgetSnapshotAndResolveSummaryForUnrecordedRecommendedPlan() {
        val types = mapOf(
            1L to ExerciseTypeEntity(id = 1L, builtinKey = "freeform", name = "Freeform", sortOrder = 0),
        )
        val recent = listOf(
            day(today.minusDays(1), DayStatus.EXERCISED, typeIds = listOf(1L)),
        )

        val snapshot = buildWidgetSnapshot(
            today = today,
            recentDays = recent,
            typesById = types,
            streakDays = 3,
            strengthSummary = "ignored when unrecorded",
            recommendedPlanLabel = "拉日",
        ) { it.name.orEmpty() }

        assertEquals(DayState.UNRECORDED, snapshot.todayState)
        assertEquals(3, snapshot.streakDays)
        assertNull(snapshot.strengthSummary)
        assertEquals("拉日", snapshot.recommendedPlanLabel)
        assertEquals(
            "推荐：拉日",
            resolveWidgetSummary(
                snapshot = snapshot,
                checkedInFallback = "Checked in",
                restSummary = "Rest day",
                emptySummary = "No workout logged today",
                recommendedPlanText = { "推荐：$it" },
            ),
        )
    }

    @Test
    fun resolveWidgetVisualStyleLiquidGlassMatchesTokens() {
        val settings = AppearanceSettings(uiStyle = UiStyle.LIQUID_GLASS)
        val tokens = resolveTheme(settings, systemDark = false)

        val style = resolveWidgetVisualStyle(settings, tokens)

        assertFalse(style.isPersona5)
        assertEquals(tokens.neutrals.surface, style.backgroundColor)
        assertEquals(tokens.neutrals.textPrimary, style.textPrimary)
        assertEquals(tokens.neutrals.textSecondary, style.textSecondary)
        assertEquals(tokens.accent.accentAction, style.accentFill)
        assertEquals(tokens.accent.onAccentAction, style.onAccentFill)
    }

    @Test
    fun resolveWidgetVisualStylePersona5CoversAllPalettesWithContrast() {
        assertEquals(4, P5Palette.entries.size)
        for (palette in P5Palette.entries) {
            val settings = AppearanceSettings(
                uiStyle = UiStyle.PERSONA_5,
                p5Palette = palette,
            )
            val tokens = resolveTheme(settings, systemDark = false)

            val style = resolveWidgetVisualStyle(settings, tokens)

            assertTrue("Expected isPersona5 for $palette", style.isPersona5)
            assertTrue(
                "textPrimary contrast failed for $palette",
                contrast(style.textPrimary, style.backgroundColor) >= 4.5,
            )
            assertTrue(
                "textSecondary contrast failed for $palette",
                contrast(style.textSecondary, style.backgroundColor) >= 4.5,
            )
            assertTrue(
                "onAccentFill contrast failed for $palette",
                contrast(style.onAccentFill, style.accentFill) >= 4.5,
            )
            assertTrue(
                "strokeColor contrast failed for $palette",
                contrast(style.strokeColor, style.backgroundColor) >= 4.5,
            )
            if (palette == P5Palette.ROYAL_GOLD) {
                assertEquals(0xFF0D0D0D.toInt(), style.onAccentFill)
            }
            if (palette == P5Palette.CALLING_CARD) {
                assertTrue(style.isLightSurface)
                assertEquals(0xFFF7F6F2.toInt(), style.backgroundColor)
                assertEquals(0xFF0D0D0D.toInt(), style.textPrimary)
                assertEquals(0xFF45454A.toInt(), style.textSecondary)
                assertEquals(0xFFD91E26.toInt(), style.accentFill)
                assertEquals(0xFFFFFFFF.toInt(), style.onAccentFill)
                assertEquals(0xFF0D0D0D.toInt(), style.strokeColor)
                assertTrue(
                    "highlightColor contrast failed for CALLING_CARD",
                    contrast(style.highlightColor, style.backgroundColor) >= 4.5,
                )
            } else {
                assertFalse("Expected dark surface for $palette", style.isLightSurface)
            }
        }
    }
}
