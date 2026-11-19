package com.actiary.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DailyRecordEntity::class, ExerciseTypeEntity::class, ExerciseEntryEntity::class, PhotoAttachmentEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ActiaryDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        val BUILTIN_TYPE_KEYS = listOf("running", "strength", "cycling", "swimming", "yoga", "hiking", "ball")

        fun seedBuiltinTypes(db: SupportSQLiteDatabase) {
            BUILTIN_TYPE_KEYS.forEachIndexed { i, key ->
                db.execSQL("INSERT INTO exercise_type (builtinKey, name, sortOrder, archived) VALUES ('$key', NULL, $i, 0)")
            }
        }
    }
}

fun ActiaryDatabase.Companion.build(context: Context): ActiaryDatabase =
    Room.databaseBuilder(context.applicationContext, ActiaryDatabase::class.java, "actiary.db")
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) = ActiaryDatabase.seedBuiltinTypes(db)
        })
        .build()
