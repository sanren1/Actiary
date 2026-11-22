package com.actiary.ui.common

import androidx.annotation.StringRes
import com.actiary.R
import com.actiary.domain.WorkoutMetric

@get:StringRes
internal val WorkoutMetric.labelId: Int
    get() = when (this) {
        WorkoutMetric.CADENCE -> R.string.workout_metric_cadence
        WorkoutMetric.STEPS -> R.string.workout_metric_steps
        WorkoutMetric.ELEVATION_GAIN -> R.string.workout_metric_ascent
        WorkoutMetric.POOL_LENGTH -> R.string.workout_metric_pool_length
        WorkoutMetric.LAPS -> R.string.workout_metric_laps
        WorkoutMetric.STROKES -> R.string.workout_metric_strokes
    }

internal val WorkoutMetric.isLength: Boolean
    get() = this == WorkoutMetric.ELEVATION_GAIN || this == WorkoutMetric.POOL_LENGTH
