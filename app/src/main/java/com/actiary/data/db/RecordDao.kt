package com.actiary.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Transaction
    @Query("SELECT * FROM daily_record WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    fun observeRange(from: Long, to: Long): Flow<List<DayWithDetails>>

    @Transaction
    @Query("SELECT * FROM daily_record WHERE epochDay = :epochDay")
    suspend fun get(epochDay: Long): DayWithDetails?

    @Transaction
    @Query("SELECT * FROM daily_record WHERE epochDay = :epochDay")
    fun observeDay(epochDay: Long): Flow<DayWithDetails?>

    @Query("SELECT * FROM exercise_type WHERE archived = 0 ORDER BY sortOrder")
    fun observeTypes(): Flow<List<ExerciseTypeEntity>>

    @Query("SELECT * FROM exercise_type ORDER BY sortOrder")
    fun observeAllTypes(): Flow<List<ExerciseTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecordIfAbsent(record: DailyRecordEntity)

    @Upsert
    suspend fun upsertRecord(record: DailyRecordEntity)

    @Query("DELETE FROM exercise_entry WHERE epochDay = :epochDay")
    suspend fun deleteEntries(epochDay: Long)

    @Query("DELETE FROM photo_attachment WHERE epochDay = :epochDay")
    suspend fun deletePhotos(epochDay: Long)

    @Insert
    suspend fun insertEntries(entries: List<ExerciseEntryEntity>)

    @Insert
    suspend fun insertPhotos(photos: List<PhotoAttachmentEntity>)

    @Transaction
    suspend fun replaceDay(
        record: DailyRecordEntity,
        entries: List<ExerciseEntryEntity>,
        photos: List<PhotoAttachmentEntity>,
    ) {
        upsertRecord(record)
        deleteEntries(record.epochDay)
        deletePhotos(record.epochDay)
        insertEntries(entries)
        insertPhotos(photos)
    }

    @Query("DELETE FROM daily_record WHERE epochDay = :epochDay")
    suspend fun deleteDay(epochDay: Long)
}
