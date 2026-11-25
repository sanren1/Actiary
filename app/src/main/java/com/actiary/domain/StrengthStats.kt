package com.actiary.domain

import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.ui.common.heatmapLevel
import java.time.LocalDate

data class MuscleStat(
    val muscle: MuscleGroup,
    val days: Int,
    val totalSets: Int,
    val heatLevel: Int,
)

data class MuscleRangeStats(
    val ranked: List<MuscleStat>,
    val levels: Map<MuscleGroup, Int>,
)

fun computeMuscleStats(
    days: List<DayWithDetails>,
    range: DateRange,
    exerciseById: Map<Long, StrengthExerciseEntity>,
    typeFilter: Long? = null,
): MuscleRangeStats {
    val setsByMuscle = IntArray(MuscleGroup.entries.size)
    val daysByMuscle = Array(MuscleGroup.entries.size) { HashSet<Long>() }

    for (day in days) {
        if (day.record.status != DayStatus.EXERCISED) continue
        val date = LocalDate.ofEpochDay(day.record.epochDay)
        if (date.isBefore(range.from) || date.isAfter(range.to)) continue

        for (entryWithLogs in day.entriesWithLogs) {
            if (typeFilter != null && entryWithLogs.entry.typeId != typeFilter) continue
            for (log in entryWithLogs.strengthLogs) {
                if (log.sets <= 0) continue
                val exercise = exerciseById[log.exerciseId] ?: continue
                for (muscle in musclesFromMask(exercise.primaryMuscleMask)) {
                    setsByMuscle[muscle.ordinal] += log.sets
                    daysByMuscle[muscle.ordinal].add(day.record.epochDay)
                }
            }
        }
    }

    val maxSets = setsByMuscle.maxOrNull() ?: 0
    if (maxSets <= 0) return MuscleRangeStats(ranked = emptyList(), levels = emptyMap())

    val ranked = MuscleGroup.entries
        .mapNotNull { muscle ->
            val totalSets = setsByMuscle[muscle.ordinal]
            if (totalSets <= 0) return@mapNotNull null
            MuscleStat(
                muscle = muscle,
                days = daysByMuscle[muscle.ordinal].size,
                totalSets = totalSets,
                heatLevel = heatmapLevel(totalSets, maxSets),
            )
        }
        .sortedWith(
            compareByDescending<MuscleStat> { it.totalSets }
                .thenByDescending { it.days }
                .thenBy { it.muscle.ordinal },
        )

    val levels = ranked.associate { it.muscle to it.heatLevel }
    return MuscleRangeStats(ranked = ranked, levels = levels)
}

fun activeTypeIdsInRange(days: List<DayWithDetails>, range: DateRange): Set<Long> {
    val active = LinkedHashSet<Long>()
    for (day in days) {
        if (day.record.status != DayStatus.EXERCISED) continue
        val date = LocalDate.ofEpochDay(day.record.epochDay)
        if (date.isBefore(range.from) || date.isAfter(range.to)) continue
        for (entry in day.entries) {
            active.add(entry.typeId)
        }
    }
    return active
}

fun resolveEffectiveTypeFilter(currentFilter: Long?, activeTypeIds: Set<Long>): Long? =
    currentFilter?.takeIf { it in activeTypeIds }

fun filterDaysByMuscle(
    days: List<DayWithDetails>,
    muscle: MuscleGroup?,
    exerciseById: Map<Long, StrengthExerciseEntity>,
    typeFilter: Long? = null,
): List<DayWithDetails> {
    if (muscle == null) return days
    val muscleBit = 1 shl muscle.bit
    return days.filter { day ->
        day.record.status == DayStatus.EXERCISED && day.entriesWithLogs.any { entryWithLogs ->
            (typeFilter == null || entryWithLogs.entry.typeId == typeFilter) &&
                entryWithLogs.strengthLogs.any { log ->
                    if (log.sets <= 0) return@any false
                    val exercise = exerciseById[log.exerciseId] ?: return@any false
                    (exercise.primaryMuscleMask and muscleBit) != 0
                }
        }
    }
}

fun computeMuscleLastTrainedDaysAgo(
    latestEpochDayByExercise: Map<Long, Long>,
    today: LocalDate,
    exerciseById: Map<Long, StrengthExerciseEntity>,
): Map<MuscleGroup, Int> {
    if (latestEpochDayByExercise.isEmpty()) return emptyMap()
    val maxDayByMuscle = HashMap<MuscleGroup, Long>()
    for ((exerciseId, epochDay) in latestEpochDayByExercise) {
        val exercise = exerciseById[exerciseId] ?: continue
        for (muscle in musclesFromMask(exercise.primaryMuscleMask)) {
            val current = maxDayByMuscle[muscle]
            if (current == null || epochDay > current) {
                maxDayByMuscle[muscle] = epochDay
            }
        }
    }
    if (maxDayByMuscle.isEmpty()) return emptyMap()
    val todayEpoch = today.toEpochDay()
    val result = LinkedHashMap<MuscleGroup, Int>()
    for (muscle in MuscleGroup.entries) {
        val maxDay = maxDayByMuscle[muscle] ?: continue
        result[muscle] = (todayEpoch - maxDay).toInt().coerceAtLeast(0)
    }
    return result
}
