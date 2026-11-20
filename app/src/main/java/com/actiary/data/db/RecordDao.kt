package com.actiary.data.db

import androidx.room.Dao
import androidx.room.Insert
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

    @Query("SELECT * FROM exercise_type WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeTypes(): Flow<List<ExerciseTypeEntity>>

    @Query("SELECT * FROM exercise_type ORDER BY sortOrder, id")
    fun observeAllTypes(): Flow<List<ExerciseTypeEntity>>

    @Query("SELECT * FROM exercise_type WHERE archived = 0 ORDER BY sortOrder, id")
    suspend fun getActiveTypes(): List<ExerciseTypeEntity>

    @Upsert
    suspend fun upsertRecord(record: DailyRecordEntity)

    @Query("DELETE FROM exercise_entry WHERE epochDay = :epochDay")
    suspend fun deleteEntries(epochDay: Long)

    @Query("DELETE FROM photo_attachment WHERE epochDay = :epochDay")
    suspend fun deletePhotos(epochDay: Long)

    @Insert
    suspend fun insertEntries(entries: List<ExerciseEntryEntity>)

    @Insert
    suspend fun insertType(type: ExerciseTypeEntity): Long

    @Query("SELECT MAX(sortOrder) FROM exercise_type")
    suspend fun maxTypeSortOrder(): Int?

    @Transaction
    suspend fun appendType(type: ExerciseTypeEntity): Long =
        insertType(type.copy(sortOrder = (maxTypeSortOrder() ?: -1) + 1))

    @Query("UPDATE exercise_type SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setTypeSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun moveType(id: Long, offset: Int) {
        val types = getActiveTypes().toMutableList()
        val index = types.indexOfFirst { it.id == id }
        val destination = index + offset
        if (index < 0 || destination !in types.indices) return
        types.add(destination, types.removeAt(index))
        types.forEachIndexed { order, type ->
            if (type.sortOrder != order) setTypeSortOrder(type.id, order)
        }
    }

    @Query("UPDATE exercise_type SET archived = 1 WHERE id = :id AND builtinKey IS NULL")
    suspend fun archiveType(id: Long)

    @Query("DELETE FROM exercise_type WHERE id = :id AND builtinKey IS NULL AND NOT EXISTS (SELECT 1 FROM exercise_entry WHERE typeId = :id)")
    suspend fun deleteUnusedType(id: Long): Int

    @Transaction
    suspend fun removeType(id: Long) {
        if (deleteUnusedType(id) == 0) archiveType(id)
    }

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
