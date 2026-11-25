package com.actiary.ui.record

import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.domain.MuscleActivation
import com.actiary.domain.MuscleGroup
import com.actiary.domain.computeDayMuscleHighlights
import com.actiary.ui.common.exerciseDisplayName
import com.actiary.ui.records.entryMetrics

internal data class DetailStrengthItem(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Float?,
    val exerciseId: Long = 0,
    val isTimed: Boolean = false,
)

internal data class DetailEntry(
    val typeId: Long,
    val metrics: List<Pair<Int, String>>,
    val strengthItems: List<DetailStrengthItem> = emptyList(),
    val muscleActivations: Map<MuscleGroup, MuscleActivation> = emptyMap(),
)

internal data class DetailModel(
    val status: DayStatus,
    val entries: List<DetailEntry>,
    val note: String,
    val photos: List<String>,
    val empty: Boolean,
)

internal fun buildDetailModel(
    day: DayWithDetails,
    types: Map<Long, ExerciseTypeEntity>,
    exercises: Map<Long, StrengthExerciseEntity> = emptyMap(),
    isChinese: Boolean = true,
    format: (Int, Any) -> String,
): DetailModel = DetailModel(
    status = day.record.status,
    entries = day.entriesWithLogs.sortedBy { it.entry.sortOrder }.mapNotNull { entryWithLogs ->
        val entry = entryWithLogs.entry
        types[entry.typeId]?.let { type ->
            val sortedLogs = entryWithLogs.strengthLogs.sortedBy { it.sortOrder }
            val resolvedExercises = sortedLogs.mapNotNull { log ->
                exercises[log.exerciseId]?.let { ex -> log to ex }
            }
            val strengthItems = resolvedExercises.map { (log, ex) ->
                DetailStrengthItem(
                    name = exerciseDisplayName(ex, isChinese),
                    sets = log.sets,
                    reps = log.reps,
                    weightKg = log.weightKg,
                    exerciseId = log.exerciseId,
                    isTimed = ex.isTimed,
                )
            }
            val muscleActivations = if (resolvedExercises.isNotEmpty()) {
                computeDayMuscleHighlights(
                    resolvedExercises.map { (_, ex) -> ex.primaryMuscleMask to ex.secondaryMuscleMask },
                )
            } else {
                emptyMap()
            }
            DetailEntry(
                typeId = entry.typeId,
                metrics = entryMetrics(entry, type, includeOptional = true, format = format),
                strengthItems = strengthItems,
                muscleActivations = muscleActivations,
            )
        }
    },
    note = day.record.note,
    photos = day.photos.sortedBy { it.sortOrder }.map { it.fileName },
    empty = day.record.status == DayStatus.EXERCISED &&
        day.entries.isEmpty() && day.record.note.isBlank() && day.photos.isEmpty(),
)
