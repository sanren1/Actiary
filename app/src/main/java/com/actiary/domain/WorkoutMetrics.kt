package com.actiary.domain

import java.io.Serializable

internal fun roundedWorkoutMinutes(seconds: Int): Int = ((seconds.toLong() + 30) / 60).toInt()

/** Optional session measurements. Distances in this model are stored in centimetres. */
data class WorkoutMetrics(
    val durationSeconds: Int? = null,
    val cadenceSpm: Int? = null,
    val stepCount: Int? = null,
    val elevationGainCm: Int? = null,
    val poolLengthCm: Int? = null,
    val lapCount: Int? = null,
    val strokeCount: Int? = null,
) : Serializable {
    private companion object {
        const val serialVersionUID: Long = 1
    }
}

/** [max] is in the stored unit: lengths in centimetres, the rest as plain counts. */
enum class WorkoutMetric(val bit: Int, val max: Int) {
    CADENCE(1, 300),
    STEPS(2, 200_000),
    ELEVATION_GAIN(4, 20_000 * 100),
    POOL_LENGTH(8, 1_000 * 100),
    LAPS(16, 10_000),
    STROKES(32, 100_000);

    fun value(metrics: WorkoutMetrics): Int? = when (this) {
        CADENCE -> metrics.cadenceSpm
        STEPS -> metrics.stepCount
        ELEVATION_GAIN -> metrics.elevationGainCm
        POOL_LENGTH -> metrics.poolLengthCm
        LAPS -> metrics.lapCount
        STROKES -> metrics.strokeCount
    }

    fun withValue(metrics: WorkoutMetrics, value: Int?): WorkoutMetrics = when (this) {
        CADENCE -> metrics.copy(cadenceSpm = value)
        STEPS -> metrics.copy(stepCount = value)
        ELEVATION_GAIN -> metrics.copy(elevationGainCm = value)
        POOL_LENGTH -> metrics.copy(poolLengthCm = value)
        LAPS -> metrics.copy(lapCount = value)
        STROKES -> metrics.copy(strokeCount = value)
    }
}

const val WORKOUT_METRIC_MASK = 0b11_1111

/** Optional metrics offered when configuring a workout type. */
fun availableWorkoutMetrics(builtinKey: String?): List<WorkoutMetric> = when (builtinKey) {
    "running" -> listOf(WorkoutMetric.CADENCE, WorkoutMetric.STEPS, WorkoutMetric.ELEVATION_GAIN)
    "cycling" -> listOf(WorkoutMetric.ELEVATION_GAIN)
    "hiking" -> listOf(WorkoutMetric.STEPS, WorkoutMetric.ELEVATION_GAIN)
    "swimming" -> listOf(WorkoutMetric.POOL_LENGTH, WorkoutMetric.LAPS, WorkoutMetric.STROKES)
    else -> listOf(WorkoutMetric.STEPS)
}

/** A null preference uses sport defaults. An explicit zero hides all optional input fields. */
fun effectiveWorkoutMetricMask(builtinKey: String?, extraMetricMask: Int?): Int =
    extraMetricMask ?: when (builtinKey) {
        "running" -> WorkoutMetric.CADENCE.bit or WorkoutMetric.STEPS.bit or WorkoutMetric.ELEVATION_GAIN.bit
        "swimming" -> WorkoutMetric.POOL_LENGTH.bit or WorkoutMetric.LAPS.bit or WorkoutMetric.STROKES.bit
        else -> 0
    }
