package com.actiary.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal const val ACTIARY_DATABASE_NAME = "actiary.db"

@Database(
    entities = [DailyRecordEntity::class, ExerciseTypeEntity::class, ExerciseEntryEntity::class, PhotoAttachmentEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class ActiaryDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        val BUILTIN_TYPE_KEYS = listOf(
            "running", "cycling", "swimming", "hiking", "ball", "freeform",
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_type ADD COLUMN iconKey TEXT")
                // Entries reference types with RESTRICT: unused rows go, used ones are archived and keep their history.
                db.execSQL("DELETE FROM exercise_type WHERE builtinKey IN ('strength','yoga') AND id NOT IN (SELECT typeId FROM exercise_entry)")
                db.execSQL("UPDATE exercise_type SET archived = 1 WHERE builtinKey IN ('strength','yoga')")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """UPDATE exercise_entry
                        SET typeId = (SELECT id FROM exercise_type WHERE builtinKey = 'freeform')
                        WHERE typeId = (SELECT id FROM exercise_type WHERE builtinKey = 'strength')""",
                )
                db.execSQL("DELETE FROM exercise_type WHERE builtinKey = 'strength'")
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
    Room.databaseBuilder(context.applicationContext, ActiaryDatabase::class.java, ACTIARY_DATABASE_NAME)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) = ActiaryDatabase.seedBuiltinTypes(db)
        })
        .addMigrations(ActiaryDatabase.MIGRATION_1_2, ActiaryDatabase.MIGRATION_2_3, ActiaryDatabase.MIGRATION_3_4)
        .build()
