package com.actiary.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class DayStatus { EXERCISED, REST }

@Entity("daily_record")
data class DailyRecordEntity(
    @PrimaryKey val epochDay: Long,
    val status: DayStatus,
    val note: String,
    val updatedAt: Long,
)

@Entity("exercise_type", indices = [Index(value = ["builtinKey"], unique = true)])
data class ExerciseTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val builtinKey: String?,
    val name: String?,
    val sortOrder: Int,
    val archived: Boolean = false,
    val hasDistance: Boolean = false,
)

@Entity(
    "exercise_entry",
    foreignKeys = [
        ForeignKey(
            entity = DailyRecordEntity::class,
            parentColumns = ["epochDay"],
            childColumns = ["epochDay"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["typeId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("epochDay"), Index("typeId")],
)
data class ExerciseEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val typeId: Long,
    val durationMinutes: Int?,
    val distanceMeters: Int?,
    val calories: Int?,
    val avgHeartRate: Int?,
    val sortOrder: Int,
)

@Entity(
    "photo_attachment",
    foreignKeys = [
        ForeignKey(
            entity = DailyRecordEntity::class,
            parentColumns = ["epochDay"],
            childColumns = ["epochDay"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("epochDay")],
)
data class PhotoAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val fileName: String,
    val sortOrder: Int,
    val addedAt: Long,
)

data class DayWithDetails(
    @Embedded val record: DailyRecordEntity,
    @Relation(parentColumn = "epochDay", entityColumn = "epochDay") val entries: List<ExerciseEntryEntity>,
    @Relation(parentColumn = "epochDay", entityColumn = "epochDay") val photos: List<PhotoAttachmentEntity>,
)
