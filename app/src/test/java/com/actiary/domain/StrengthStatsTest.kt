package com.actiary.domain

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.EntryWithStrengthLogs
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.StrengthLogItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StrengthStatsTest {
    private fun d(y: Int, m: Int, day: Int): LocalDate = LocalDate.of(y, m, day)

    private val march = rangeOf(d(2025, 3, 15), RangeKind.MONTH, DayOfWeek.MONDAY)

    private fun exercise(
        id: Long,
        primary: Int,
        secondary: Int = 0,
        region: BodyRegion = BodyRegion.CHEST,
    ) = StrengthExerciseEntity(
        id = id,
        builtinKey = null,
        nameZh = "Ex$id",
        nameEn = "Ex$id",
        bodyRegion = region.key,
        primaryMuscleMask = primary,
        secondaryMuscleMask = secondary,
        defaultSets = 4,
        defaultReps = 10,
    )

    private fun log(
        entryId: Long,
        exerciseId: Long,
        sets: Int,
        reps: Int = 10,
        sortOrder: Int = 0,
    ) = StrengthLogItemEntity(
        id = 0,
        entryId = entryId,
        exerciseId = exerciseId,
        sets = sets,
        reps = reps,
        weightKg = null,
        sortOrder = sortOrder,
    )

    private fun entryWithLogs(
        date: LocalDate,
        typeId: Long,
        entryId: Long = 1L,
        logs: List<StrengthLogItemEntity> = emptyList(),
    ) = EntryWithStrengthLogs(
        entry = ExerciseEntryEntity(
            id = entryId,
            epochDay = date.toEpochDay(),
            typeId = typeId,
            durationMinutes = null,
            distanceMeters = null,
            calories = null,
            avgHeartRate = null,
            sortOrder = 0,
        ),
        strengthLogs = logs,
    )

    private fun day(
        date: LocalDate,
        status: DayStatus = DayStatus.EXERCISED,
        vararg entries: EntryWithStrengthLogs,
    ) = DayWithDetails(
        record = DailyRecordEntity(date.toEpochDay(), status, "", 0L),
        entriesWithLogs = entries.toList(),
        photos = emptyList(),
    )

    @Test
    fun sameDayDuplicateExercisesCountOnceInDaysAndSumSets() {
        val bench = exercise(1L, primary = muscleMaskOf(MuscleGroup.PECTORALS))
        val fly = exercise(2L, primary = muscleMaskOf(MuscleGroup.PECTORALS))
        val exerciseById = listOf(bench, fly).associateBy { it.id }

        val days = listOf(
            day(
                d(2025, 3, 5),
                DayStatus.EXERCISED,
                entryWithLogs(
                    d(2025, 3, 5),
                    typeId = 8L,
                    entryId = 10L,
                    logs = listOf(log(10L, 1L, sets = 4), log(10L, 2L, sets = 3)),
                ),
                entryWithLogs(
                    d(2025, 3, 5),
                    typeId = 8L,
                    entryId = 11L,
                    logs = listOf(log(11L, 1L, sets = 2)),
                ),
            ),
            day(
                d(2025, 3, 10),
                DayStatus.EXERCISED,
                entryWithLogs(
                    d(2025, 3, 10),
                    typeId = 8L,
                    entryId = 12L,
                    logs = listOf(log(12L, 2L, sets = 5)),
                ),
            ),
        )

        val stats = computeMuscleStats(days, march, exerciseById)
        assertEquals(1, stats.ranked.size)
        val pec = stats.ranked.single()
        assertEquals(MuscleGroup.PECTORALS, pec.muscle)
        assertEquals(2, pec.days)
        assertEquals(14, pec.totalSets)
        assertEquals(3, pec.heatLevel)
        assertEquals(mapOf(MuscleGroup.PECTORALS to 3), stats.levels)
    }

    @Test
    fun secondaryMusclesExcludedFromStats() {
        val bench = exercise(
            1L,
            primary = muscleMaskOf(MuscleGroup.PECTORALS),
            secondary = muscleMaskOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_SIDE_DELTS),
        )
        val exerciseById = mapOf(1L to bench)

        val days = listOf(
            day(
                d(2025, 3, 4),
                DayStatus.EXERCISED,
                entryWithLogs(
                    d(2025, 3, 4),
                    typeId = 8L,
                    entryId = 1L,
                    logs = listOf(log(1L, 1L, sets = 4)),
                ),
            ),
        )

        val stats = computeMuscleStats(days, march, exerciseById)
        assertEquals(listOf(MuscleGroup.PECTORALS), stats.ranked.map { it.muscle })
        assertFalse(MuscleGroup.TRICEPS in stats.levels)
        assertFalse(MuscleGroup.FRONT_SIDE_DELTS in stats.levels)
    }

    @Test
    fun activeTypeIdsInRangeExcludesRestAndOutOfRange() {
        val days = listOf(
            day(d(2025, 2, 28), DayStatus.EXERCISED, entryWithLogs(d(2025, 2, 28), typeId = 1L)),
            day(d(2025, 3, 1), DayStatus.REST, entryWithLogs(d(2025, 3, 1), typeId = 2L)),
            day(d(2025, 3, 2), DayStatus.EXERCISED, entryWithLogs(d(2025, 3, 2), typeId = 3L)),
            day(
                d(2025, 3, 15),
                DayStatus.EXERCISED,
                entryWithLogs(d(2025, 3, 15), typeId = 8L),
                entryWithLogs(d(2025, 3, 15), typeId = 3L),
            ),
            day(d(2025, 4, 1), DayStatus.EXERCISED, entryWithLogs(d(2025, 4, 1), typeId = 4L)),
        )

        assertEquals(setOf(3L, 8L), activeTypeIdsInRange(days, march))
    }

    @Test
    fun resolveEffectiveTypeFilterResetsWhenMissingInNewRange() {
        val active = setOf(2L, 8L)
        assertEquals(8L, resolveEffectiveTypeFilter(8L, active))
        assertNull(resolveEffectiveTypeFilter(5L, active))
        assertNull(resolveEffectiveTypeFilter(null, active))
    }

    @Test
    fun filterDaysByMuscleFiltersByPrimaryMuscleOnly() {
        val bench = exercise(
            1L,
            primary = muscleMaskOf(MuscleGroup.PECTORALS),
            secondary = muscleMaskOf(MuscleGroup.TRICEPS),
        )
        val pushdown = exercise(
            2L,
            primary = muscleMaskOf(MuscleGroup.TRICEPS),
            secondary = 0,
            region = BodyRegion.ARMS,
        )
        val exerciseById = listOf(bench, pushdown).associateBy { it.id }

        val pecOnlyDay = day(
            d(2025, 3, 1),
            DayStatus.EXERCISED,
            entryWithLogs(d(2025, 3, 1), typeId = 8L, entryId = 1L, logs = listOf(log(1L, 1L, sets = 4))),
        )
        val tricepsDay = day(
            d(2025, 3, 2),
            DayStatus.EXERCISED,
            entryWithLogs(d(2025, 3, 2), typeId = 8L, entryId = 2L, logs = listOf(log(2L, 2L, sets = 3))),
        )
        val restDay = day(
            d(2025, 3, 3),
            DayStatus.REST,
            entryWithLogs(d(2025, 3, 3), typeId = 8L, entryId = 3L, logs = listOf(log(3L, 2L, sets = 3))),
        )
        val allDays = listOf(pecOnlyDay, tricepsDay, restDay)

        assertEquals(allDays, filterDaysByMuscle(allDays, null, exerciseById))
        assertEquals(listOf(tricepsDay), filterDaysByMuscle(allDays, MuscleGroup.TRICEPS, exerciseById))
        assertEquals(listOf(pecOnlyDay), filterDaysByMuscle(allDays, MuscleGroup.PECTORALS, exerciseById))
        assertTrue(filterDaysByMuscle(allDays, MuscleGroup.QUADS, exerciseById).isEmpty())
    }

    @Test
    fun filterDaysByMuscleExcludesZeroSetLogs() {
        val bench = exercise(1L, primary = muscleMaskOf(MuscleGroup.PECTORALS))
        val exerciseById = mapOf(1L to bench)
        val zeroSetsDay = day(
            d(2025, 3, 5),
            DayStatus.EXERCISED,
            entryWithLogs(d(2025, 3, 5), typeId = 8L, entryId = 1L, logs = listOf(log(1L, 1L, sets = 0))),
        )
        assertTrue(filterDaysByMuscle(listOf(zeroSetsDay), MuscleGroup.PECTORALS, exerciseById).isEmpty())
    }

    @Test
    fun computeMuscleLastTrainedDaysAgoUsesPrimaryMusclesAndMostRecentDay() {
        val today = d(2025, 3, 15)
        val bench = exercise(
            1L,
            primary = muscleMaskOf(MuscleGroup.PECTORALS),
            secondary = muscleMaskOf(MuscleGroup.TRICEPS),
        )
        val fly = exercise(2L, primary = muscleMaskOf(MuscleGroup.PECTORALS))
        val curl = exercise(3L, primary = muscleMaskOf(MuscleGroup.BICEPS), region = BodyRegion.ARMS)
        val squat = exercise(4L, primary = muscleMaskOf(MuscleGroup.QUADS, MuscleGroup.GLUTES), region = BodyRegion.LEGS)
        val exerciseById = listOf(bench, fly, curl, squat).associateBy { it.id }

        assertEquals(emptyMap<MuscleGroup, Int>(), computeMuscleLastTrainedDaysAgo(emptyMap(), today, exerciseById))

        val latestByExercise = mapOf(
            1L to d(2025, 3, 10).toEpochDay(), // 5 days ago
            2L to d(2025, 3, 14).toEpochDay(), // 1 day ago -> wins for PECTORALS
            3L to d(2025, 3, 15).toEpochDay(), // 0 days ago (today)
            4L to d(2025, 3, 18).toEpochDay(), // future date -> clamped to 0
            999L to d(2025, 3, 12).toEpochDay(), // missing exercise ignored
        )

        val daysAgo = computeMuscleLastTrainedDaysAgo(latestByExercise, today, exerciseById)
        assertEquals(
            mapOf(
                MuscleGroup.PECTORALS to 1,
                MuscleGroup.BICEPS to 0,
                MuscleGroup.QUADS to 0,
                MuscleGroup.GLUTES to 0,
            ),
            daysAgo,
        )
        assertFalse(MuscleGroup.TRICEPS in daysAgo)
    }

    @Test
    fun computeMuscleStatsRanksBySetsThenDaysThenOrdinalAndComputesHeatLevels() {
        val pec = exercise(1L, primary = muscleMaskOf(MuscleGroup.PECTORALS))
        val lats = exercise(2L, primary = muscleMaskOf(MuscleGroup.LATS), region = BodyRegion.BACK)
        val biceps = exercise(3L, primary = muscleMaskOf(MuscleGroup.BICEPS), region = BodyRegion.ARMS)
        val calves = exercise(4L, primary = muscleMaskOf(MuscleGroup.CALVES), region = BodyRegion.LEGS)
        val exerciseById = listOf(pec, lats, biceps, calves).associateBy { it.id }

        val days = listOf(
            day(
                d(2025, 3, 1),
                DayStatus.EXERCISED,
                entryWithLogs(
                    d(2025, 3, 1),
                    typeId = 8L,
                    entryId = 1L,
                    logs = listOf(
                        log(1L, 1L, sets = 10),
                        log(1L, 2L, sets = 3),
                        log(1L, 3L, sets = 3),
                        log(1L, 4L, sets = 6),
                    ),
                ),
            ),
            day(
                d(2025, 3, 2),
                DayStatus.EXERCISED,
                entryWithLogs(
                    d(2025, 3, 2),
                    typeId = 8L,
                    entryId = 2L,
                    logs = listOf(log(2L, 3L, sets = 3)),
                ),
            ),
            // Out of range & REST ignored
            day(
                d(2025, 3, 3),
                DayStatus.REST,
                entryWithLogs(d(2025, 3, 3), typeId = 8L, entryId = 3L, logs = listOf(log(3L, 2L, sets = 20))),
            ),
            day(
                d(2025, 2, 28),
                DayStatus.EXERCISED,
                entryWithLogs(d(2025, 2, 28), typeId = 8L, entryId = 4L, logs = listOf(log(4L, 2L, sets = 20))),
            ),
        )

        val stats = computeMuscleStats(days, march, exerciseById)
        // PECTORALS: 10 sets, 1 day, level 3
        // BICEPS: 6 sets, 2 days, level 2
        // CALVES: 6 sets, 1 day, level 2 (tie with BICEPS on sets -> BICEPS wins on days)
        // LATS: 3 sets, 1 day, level 1
        assertEquals(
            listOf(
                MuscleStat(MuscleGroup.PECTORALS, days = 1, totalSets = 10, heatLevel = 3),
                MuscleStat(MuscleGroup.BICEPS, days = 2, totalSets = 6, heatLevel = 2),
                MuscleStat(MuscleGroup.CALVES, days = 1, totalSets = 6, heatLevel = 2),
                MuscleStat(MuscleGroup.LATS, days = 1, totalSets = 3, heatLevel = 1),
            ),
            stats.ranked,
        )
        assertEquals(
            mapOf(
                MuscleGroup.PECTORALS to 3,
                MuscleGroup.BICEPS to 2,
                MuscleGroup.CALVES to 2,
                MuscleGroup.LATS to 1,
            ),
            stats.levels,
        )
    }

    @Test
    fun computeMuscleStatsAndFilterDaysRespectTypeFilter() {
        val pec = exercise(1L, primary = muscleMaskOf(MuscleGroup.PECTORALS))
        val exerciseById = mapOf(1L to pec)
        val freeformId = 8L
        val runningId = 1L

        val strengthDay = day(
            d(2025, 3, 6),
            DayStatus.EXERCISED,
            entryWithLogs(
                d(2025, 3, 6),
                typeId = freeformId,
                entryId = 10L,
                logs = listOf(log(10L, 1L, sets = 4)),
            ),
        )
        val runningDay = day(
            d(2025, 3, 7),
            DayStatus.EXERCISED,
            entryWithLogs(
                d(2025, 3, 7),
                typeId = runningId,
                entryId = 11L,
                logs = emptyList(),
            ),
        )
        val days = listOf(strengthDay, runningDay)

        val allStats = computeMuscleStats(days, march, exerciseById, typeFilter = null)
        assertEquals(listOf(MuscleGroup.PECTORALS), allStats.ranked.map { it.muscle })

        val freeformStats = computeMuscleStats(days, march, exerciseById, typeFilter = freeformId)
        assertEquals(listOf(MuscleGroup.PECTORALS), freeformStats.ranked.map { it.muscle })

        val runningStats = computeMuscleStats(days, march, exerciseById, typeFilter = runningId)
        assertTrue(runningStats.ranked.isEmpty())
        assertTrue(runningStats.levels.isEmpty())

        assertEquals(
            listOf(strengthDay),
            filterDaysByMuscle(days, MuscleGroup.PECTORALS, exerciseById, typeFilter = freeformId),
        )
        assertTrue(
            filterDaysByMuscle(days, MuscleGroup.PECTORALS, exerciseById, typeFilter = runningId).isEmpty(),
        )
    }
}
