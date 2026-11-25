package com.actiary.data.backup

import com.actiary.data.db.DayStatus
import com.actiary.domain.WorkoutMetrics

const val BACKUP_FORMAT = 3

data class BackupData(
    val format: Int,
    val appVersion: String,
    val exportedAt: Long,
    val appearance: Map<String, String>,
    val types: List<BackupType>,
    val records: List<BackupRecord>,
    val entries: List<BackupEntry>,
    val photos: List<BackupPhoto>,
    val strengthExercises: List<BackupStrengthExercise> = emptyList(),
    val workoutPlans: List<BackupWorkoutPlan> = emptyList(),
    val strengthLogs: List<BackupStrengthLog> = emptyList(),
)

data class BackupType(
    val id: Long,
    val builtinKey: String?,
    val name: String?,
    val sortOrder: Int,
    val archived: Boolean,
    val hasDistance: Boolean,
    val iconKey: String?,
    val extraMetricMask: Int? = null,
)

data class BackupRecord(val epochDay: Long, val status: DayStatus, val note: String, val updatedAt: Long)

data class BackupEntry(
    val id: Long,
    val epochDay: Long,
    val typeId: Long,
    val durationMinutes: Int?,
    val distanceMeters: Int?,
    val calories: Int?,
    val avgHeartRate: Int?,
    val sortOrder: Int,
    val metrics: WorkoutMetrics = WorkoutMetrics(),
    val planId: Long? = null,
)

data class BackupPhoto(val epochDay: Long, val fileName: String, val sortOrder: Int, val addedAt: Long)

data class BackupStrengthExercise(
    val id: Long,
    val builtinKey: String?,
    val nameZh: String,
    val nameEn: String,
    val bodyRegion: String,
    val primaryMuscleMask: Int,
    val secondaryMuscleMask: Int,
    val defaultSets: Int,
    val defaultReps: Int,
    val archived: Boolean,
    val isTimed: Boolean = false,
)

data class BackupWorkoutPlanItem(
    val exerciseId: Long,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeightKg: Float?,
    val sortOrder: Int,
)

data class BackupWorkoutPlan(
    val id: Long,
    val builtinKey: String?,
    val name: String?,
    val sortOrder: Int,
    val items: List<BackupWorkoutPlanItem>,
)

data class BackupStrengthLog(
    val entryId: Long,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val weightKg: Float?,
    val sortOrder: Int,
)

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)
