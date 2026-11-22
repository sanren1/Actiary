package com.actiary.domain

import com.actiary.data.db.ExerciseTypeEntity
import java.io.Serializable

internal data class WorkoutScreenshot(val uri: String, val workout: ParsedWorkout) : Serializable

internal fun DayDraft.withRecognizedWorkout(workout: ParsedWorkout, type: ExerciseTypeEntity): DayDraft {
    val existing = entries.filter { it.typeId == type.id }.singleOrNull()
    val entry = existing ?: DraftEntry(type.id, id = (entries.maxOfOrNull { it.id } ?: 0L) + 1)
    val recognized = workout.metrics
    val updated = entry.copy(
        durationMinutes = workout.durationMinutes ?: entry.durationMinutes,
        distanceMeters = workout.distanceMeters?.takeIf { type.hasDistance } ?: entry.distanceMeters,
        calories = workout.calories ?: entry.calories,
        avgHeartRate = workout.avgHeartRate ?: entry.avgHeartRate,
        metrics = entry.metrics.copy(
            // A replacement duration must not keep the precision from another workout.
            durationSeconds = if (workout.durationMinutes != null) recognized?.durationSeconds
                else entry.metrics.durationSeconds,
            cadenceSpm = recognized?.cadenceSpm ?: entry.metrics.cadenceSpm,
            stepCount = recognized?.stepCount ?: entry.metrics.stepCount,
            elevationGainCm = recognized?.elevationGainCm ?: entry.metrics.elevationGainCm,
            poolLengthCm = recognized?.poolLengthCm ?: entry.metrics.poolLengthCm,
            lapCount = recognized?.lapCount ?: entry.metrics.lapCount,
            strokeCount = recognized?.strokeCount ?: entry.metrics.strokeCount,
        ),
    )
    return if (existing == null) copy(entries = entries + updated) else withEntry(updated)
}
