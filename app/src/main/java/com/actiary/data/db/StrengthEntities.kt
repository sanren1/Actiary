package com.actiary.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    "strength_exercise",
    indices = [Index(value = ["builtinKey"], unique = true)],
)
data class StrengthExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val builtinKey: String?,
    val nameZh: String,
    val nameEn: String,
    val bodyRegion: String,
    val primaryMuscleMask: Int,
    val secondaryMuscleMask: Int,
    val defaultSets: Int,
    val defaultReps: Int,
    val archived: Boolean = false,
    val isTimed: Boolean = false,
)

@Entity(
    "workout_plan",
    indices = [Index(value = ["builtinKey"], unique = true)],
)
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val builtinKey: String? = null,
    val name: String? = null,
    val sortOrder: Int,
)

@Entity(
    "workout_plan_item",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StrengthExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId"), Index("exerciseId")],
)
data class WorkoutPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val exerciseId: Long,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeightKg: Float? = null,
    val sortOrder: Int,
)

@Entity(
    "strength_log_item",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StrengthExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("entryId"), Index("exerciseId")],
)
data class StrengthLogItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val weightKg: Float? = null,
    val sortOrder: Int,
)

data class EntryWithStrengthLogs(
    @Embedded val entry: ExerciseEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val strengthLogs: List<StrengthLogItemEntity> = emptyList(),
)

data class WorkoutPlanWithItems(
    @Embedded val plan: WorkoutPlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val items: List<WorkoutPlanItemEntity> = emptyList(),
)
