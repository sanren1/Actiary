package com.actiary.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DailyRecordEntity::class, ExerciseTypeEntity::class, ExerciseEntryEntity::class, PhotoAttachmentEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ActiaryDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        val BUILTIN_TYPE_KEYS = listOf(
            "running", "strength", "cycling", "swimming", "yoga", "hiking", "ball", "freeform",
        )
        private val DISTANCE_TYPE_KEYS = setOf("running", "cycling", "swimming", "hiking")

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_entry ADD COLUMN calories INTEGER")
                db.execSQL("ALTER TABLE exercise_entry ADD COLUMN avgHeartRate INTEGER")
                db.execSQL("ALTER TABLE exercise_type ADD COLUMN hasDistance INTEGER NOT NULL DEFAULT 0")
                val keys = DISTANCE_TYPE_KEYS.joinToString(",") { "'$it'" }
                db.execSQL("UPDATE exercise_type SET hasDistance = 1 WHERE builtinKey IN ($keys)")
                db.execSQL("INSERT INTO exercise_type (builtinKey, name, sortOrder, archived, hasDistance) VALUES ('freeform', NULL, 7, 0, 0)")
            }
        }

        fun seedBuiltinTypes(db: SupportSQLiteDatabase) {
            BUILTIN_TYPE_KEYS.forEachIndexed { i, key ->
                val hasDistance = if (key in DISTANCE_TYPE_KEYS) 1 else 0
                db.execSQL("INSERT INTO exercise_type (builtinKey, name, sortOrder, archived, hasDistance) VALUES ('$key', NULL, $i, 0, $hasDistance)")
            }
        }
    }
}

fun ActiaryDatabase.Companion.build(context: Context): ActiaryDatabase =
    Room.databaseBuilder(context.applicationContext, ActiaryDatabase::class.java, "actiary.db")
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) = ActiaryDatabase.seedBuiltinTypes(db)
        })
        .addMigrations(ActiaryDatabase.MIGRATION_1_2)
        .build()
