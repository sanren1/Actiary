package com.actiary.domain

import com.actiary.data.db.ExerciseTypeEntity
import java.io.Serializable

internal data class WorkoutScreenshot(val uri: String, val workout: ParsedWorkout) : Serializable

internal fun DayDraft.withRecognizedWorkout(workout: ParsedWorkout, type: ExerciseTypeEntity): DayDraft {
    val existing = entries.filter { it.typeId == type.id }.singleOrNull()
    val entry = existing ?: DraftEntry(type.id, id = (entries.maxOfOrNull { it.id } ?: 0L) + 1)
    val updated = entry.copy(
        durationMinutes = workout.durationMinutes ?: entry.durationMinutes,
        distanceMeters = workout.distanceMeters?.takeIf { type.hasDistance } ?: entry.distanceMeters,
        calories = workout.calories ?: entry.calories,
        avgHeartRate = workout.avgHeartRate ?: entry.avgHeartRate,
    )
    return if (existing == null) copy(entries = entries + updated) else withEntry(updated)
}
