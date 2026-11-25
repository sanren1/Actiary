package com.actiary.ui.record

import com.actiary.R
import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.EntryWithStrengthLogs
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.PhotoAttachmentEntity
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.StrengthLogItemEntity
import com.actiary.domain.MuscleActivation
import com.actiary.domain.MuscleGroup
import com.actiary.domain.WorkoutMetrics
import com.actiary.domain.muscleMaskOf
import com.actiary.ui.calendar.buildStrengthSummaryText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDetailModelTest {
    private val swimming = ExerciseTypeEntity(1, "swimming", null, 0, hasDistance = true)
    private val running = ExerciseTypeEntity(2, "running", null, 1, hasDistance = true)
    private val types = listOf(swimming, running).associateBy { it.id }

    private fun model(
        status: DayStatus = DayStatus.EXERCISED,
        entries: List<ExerciseEntryEntity> = emptyList(),
        note: String = "",
        photos: List<PhotoAttachmentEntity> = emptyList(),
    ): DetailModel = buildDetailModel(DayWithDetails(DailyRecordEntity(0, status, note, 0), entries, photos), types) { unit, value ->
        when (unit) {
            R.string.records_min -> "$value min"
            R.string.records_kcal -> "$value kcal"
            R.string.record_detail_bpm -> "$value bpm"
            R.string.records_m -> "$value m"
            R.string.records_km -> "$value km"
            R.string.workout_metric_steps_per_min -> "$value steps/min"
            R.string.workout_metric_pace_km -> "$value /km"
            else -> error("Unexpected metric format $unit")
        }
    }

    @Test fun restDayIsNotAnEmptyWorkout() {
        val detail = model(status = DayStatus.REST)
        assertEquals(DayStatus.REST, detail.status)
        assertFalse(detail.empty)
    }

    @Test fun bareCheckInHasEmptyWorkoutMessage() {
        assertTrue(model().empty)
    }

    @Test fun sameTypeSessionsKeepTheirOwnPopulatedMetrics() {
        val detail = model(entries = listOf(
            ExerciseEntryEntity(2, 0, running.id, null, null, 92, 131, 1),
            ExerciseEntryEntity(1, 0, running.id, 0, null, null, null, 0),
        ))
        assertEquals(
            listOf(
                DetailEntry(running.id, listOf(R.string.records_stat_duration to "0 min")),
                DetailEntry(running.id, listOf(
                    R.string.records_stat_calories to "92 kcal",
                    R.string.record_heart_rate to "131 bpm",
                )),
            ),
            detail.entries,
        )
        assertFalse(detail.empty)
    }

    @Test fun swimmingUsesMetersAndRunningUsesKilometers() {
        val detail = model(entries = listOf(
            ExerciseEntryEntity(1, 0, swimming.id, null, 1250, null, null, 0),
            ExerciseEntryEntity(2, 0, running.id, null, 1250, null, null, 1),
        ))
        assertEquals(
            listOf(
                DetailEntry(swimming.id, listOf(R.string.record_detail_distance to "1250 m")),
                DetailEntry(running.id, listOf(R.string.record_detail_distance to "1.25 km")),
            ),
            detail.entries,
        )
    }

    @Test fun savedExtrasAndExactPaceRemainVisibleWithInputFieldsDisabled() {
        val type = running.copy(hasDistance = false, extraMetricMask = 0)
        val entry = ExerciseEntryEntity(
            1, 0, type.id, 32, 6000, null, null, 0,
            WorkoutMetrics(durationSeconds = 1932, cadenceSpm = 172, stepCount = 5820),
        )
        val day = DayWithDetails(DailyRecordEntity(0, DayStatus.EXERCISED, "", 0), listOf(entry), emptyList())
        val detail = buildDetailModel(day, mapOf(type.id to type)) { _, value -> value.toString() }
        assertEquals(
            listOf(
                R.string.records_stat_duration to "32:12",
                R.string.record_detail_distance to "6",
                R.string.workout_metric_cadence to "172",
                R.string.workout_metric_steps to "5820",
                R.string.workout_metric_pace to "5:22",
            ),
            detail.entries.single().metrics,
        )
        val legacy = model(entries = listOf(entry.copy(metrics = WorkoutMetrics())))
        assertEquals(
            listOf(R.string.records_stat_duration to "32 min", R.string.record_detail_distance to "6 km"),
            legacy.entries.single().metrics,
        )
    }

    @Test fun keepsFullNoteAndAllPhotosInSavedOrder() {
        val note = "Warm-up\n" + "Long note. ".repeat(30)
        val photos = List(9) { PhotoAttachmentEntity(epochDay = 0, fileName = "photo-$it.jpg", sortOrder = it, addedAt = 0) }
        val detail = model(note = note, photos = photos.reversed())
        assertEquals(note, detail.note)
        assertEquals(List(9) { "photo-$it.jpg" }, detail.photos)
        assertFalse(detail.empty)
    }

    @Test fun buildDetailModelPopulatesStrengthItemsAndMuscleActivations() {
        val freeform = ExerciseTypeEntity(3, "freeform", null, 2)
        val allTypes = types + (freeform.id to freeform)
        val bench = StrengthExerciseEntity(
            id = 10,
            builtinKey = "barbell_bench_press",
            nameZh = "杠铃平板卧推",
            nameEn = "Barbell Bench Press",
            bodyRegion = "chest",
            primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
            secondaryMuscleMask = muscleMaskOf(MuscleGroup.FRONT_SIDE_DELTS, MuscleGroup.TRICEPS),
            defaultSets = 4,
            defaultReps = 10,
        )
        val archivedDip = StrengthExerciseEntity(
            id = 11,
            builtinKey = null,
            nameZh = "旧版自重臂屈伸",
            nameEn = "Legacy Dip",
            bodyRegion = "arms",
            primaryMuscleMask = muscleMaskOf(MuscleGroup.TRICEPS),
            secondaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
            defaultSets = 3,
            defaultReps = 15,
            archived = true,
        )
        val plank = StrengthExerciseEntity(
            id = 12,
            builtinKey = "plank",
            nameZh = "平板支撑",
            nameEn = "Plank",
            bodyRegion = "abs",
            primaryMuscleMask = muscleMaskOf(MuscleGroup.ABS),
            secondaryMuscleMask = 0,
            defaultSets = 3,
            defaultReps = 60,
            isTimed = true,
        )
        val exercises = mapOf(bench.id to bench, archivedDip.id to archivedDip, plank.id to plank)
        val entry = ExerciseEntryEntity(1, 0, freeform.id, 45, null, null, null, 0)
        val logs = listOf(
            StrengthLogItemEntity(id = 3, entryId = 1, exerciseId = plank.id, sets = 3, reps = 60, weightKg = null, sortOrder = 2),
            StrengthLogItemEntity(id = 2, entryId = 1, exerciseId = archivedDip.id, sets = 3, reps = 15, weightKg = null, sortOrder = 1),
            StrengthLogItemEntity(id = 1, entryId = 1, exerciseId = bench.id, sets = 4, reps = 10, weightKg = 60f, sortOrder = 0),
        )
        val day = DayWithDetails(
            record = DailyRecordEntity(0, DayStatus.EXERCISED, "", 0),
            entriesWithLogs = listOf(EntryWithStrengthLogs(entry, logs)),
            photos = emptyList(),
        )

        val detailZh = buildDetailModel(
            day = day,
            types = allTypes,
            exercises = exercises,
            isChinese = true,
            format = { unit, value ->
                if (unit == R.string.records_min) "$value min" else value.toString()
            },
        )

        assertEquals(
            listOf(
                DetailStrengthItem("杠铃平板卧推", 4, 10, 60f, exerciseId = bench.id, isTimed = false),
                DetailStrengthItem("旧版自重臂屈伸", 3, 15, null, exerciseId = archivedDip.id, isTimed = false),
                DetailStrengthItem("平板支撑", 3, 60, null, exerciseId = plank.id, isTimed = true),
            ),
            detailZh.entries.single().strengthItems,
        )
        assertEquals(
            mapOf(
                MuscleGroup.PECTORALS to MuscleActivation.PRIMARY,
                MuscleGroup.TRICEPS to MuscleActivation.PRIMARY,
                MuscleGroup.ABS to MuscleActivation.PRIMARY,
                MuscleGroup.FRONT_SIDE_DELTS to MuscleActivation.SECONDARY,
            ),
            detailZh.entries.single().muscleActivations,
        )
        assertEquals(
            listOf(R.string.records_stat_duration to "45 min"),
            detailZh.entries.single().metrics,
        )

        val detailEn = buildDetailModel(
            day = day,
            types = allTypes,
            exercises = exercises,
            isChinese = false,
            format = { _, value -> value.toString() },
        )
        assertEquals(
            listOf(
                DetailStrengthItem("Barbell Bench Press", 4, 10, 60f, exerciseId = bench.id, isTimed = false),
                DetailStrengthItem("Legacy Dip", 3, 15, null, exerciseId = archivedDip.id, isTimed = false),
                DetailStrengthItem("Plank", 3, 60, null, exerciseId = plank.id, isTimed = true),
            ),
            detailEn.entries.single().strengthItems,
        )
    }

    @Test fun buildDetailModelPreservesLegacyFreeformNoteWhenNoStrengthLogs() {
        val freeform = ExerciseTypeEntity(3, "freeform", null, 2)
        val allTypes = types + (freeform.id to freeform)
        val entry = ExerciseEntryEntity(1, 0, freeform.id, 30, null, null, null, 0)
        val note = "练胸：卧推 4 组"
        val day = DayWithDetails(
            record = DailyRecordEntity(0, DayStatus.EXERCISED, note, 0),
            entriesWithLogs = listOf(EntryWithStrengthLogs(entry, emptyList())),
            photos = emptyList(),
        )

        val detail = buildDetailModel(day, allTypes) { _, value -> value.toString() }
        assertTrue(detail.entries.single().strengthItems.isEmpty())
        assertTrue(detail.entries.single().muscleActivations.isEmpty())
        assertEquals(note, detail.note)
        assertFalse(detail.empty)
    }

    @Test fun buildStrengthSummaryTextFormatsMusclesAndCounts() {
        val chest = StrengthExerciseEntity(1, null, "卧推", "Bench", "chest", muscleMaskOf(MuscleGroup.PECTORALS), 0, 4, 10)
        val fly = StrengthExerciseEntity(2, null, "飞鸟", "Fly", "chest", muscleMaskOf(MuscleGroup.PECTORALS), 0, 3, 12)
        val tri = StrengthExerciseEntity(3, null, "下压", "Pushdown", "arms", muscleMaskOf(MuscleGroup.TRICEPS), 0, 3, 12)
        val exercises = listOf(chest, fly, tri).associateBy { it.id }

        val muscleNames = mapOf(
            MuscleGroup.PECTORALS to "胸大肌",
            MuscleGroup.TRICEPS to "肱三头肌",
        )

        assertNull(
            buildStrengthSummaryText(
                logs = emptyList(),
                exercises = exercises,
                muscleLabel = { muscleNames[it].orEmpty() },
                countsLabel = { count, sets -> "$count 动作 · $sets 组" },
            ),
        )

        val logs = listOf(
            StrengthLogItemEntity(1, 1, chest.id, sets = 4, reps = 10, weightKg = 60f, sortOrder = 0),
            StrengthLogItemEntity(2, 1, fly.id, sets = 3, reps = 12, weightKg = 15f, sortOrder = 1),
            StrengthLogItemEntity(3, 1, tri.id, sets = 3, reps = 12, weightKg = null, sortOrder = 2),
        )
        val summary = buildStrengthSummaryText(
            logs = logs,
            exercises = exercises,
            muscleLabel = { muscleNames[it].orEmpty() },
            countsLabel = { count, sets -> "$count 动作 · $sets 组" },
        )
        assertEquals("胸大肌 · 肱三头肌 · 3 动作 · 10 组", summary)
    }
}
