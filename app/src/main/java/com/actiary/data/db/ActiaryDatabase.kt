package com.actiary.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.actiary.domain.BuiltinStrengthCatalog

internal const val ACTIARY_DATABASE_NAME = "actiary.db"

@Database(
    entities = [
        DailyRecordEntity::class,
        ExerciseTypeEntity::class,
        ExerciseEntryEntity::class,
        PhotoAttachmentEntity::class,
        StrengthExerciseEntity::class,
        WorkoutPlanEntity::class,
        WorkoutPlanItemEntity::class,
        StrengthLogItemEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class ActiaryDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun strengthDao(): StrengthDao

    companion object {
        val BUILTIN_TYPE_KEYS = listOf(
            "running", "cycling", "swimming", "hiking", "ball", "freeform",
        )
        private val DISTANCE_TYPE_KEYS = setOf("running", "cycling", "swimming", "hiking")
        private val BODY_PART_TYPE_KEYS = listOf("chest", "back", "shoulders", "arms", "legs", "abs")

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_type ADD COLUMN extraMetricMask INTEGER")
                val fields = listOf(
                    "durationSeconds", "cadenceSpm", "stepCount", "elevationGainCm",
                    "poolLengthCm", "lapCount", "strokeCount",
                )
                for (field in fields) db.execSQL("ALTER TABLE exercise_entry ADD COLUMN $field INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                var nextOrder = db.query("SELECT COALESCE(MAX(sortOrder), -1) FROM exercise_type").use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) + 1 else 0
                }
                for (key in BODY_PART_TYPE_KEYS) {
                    db.execSQL(
                        """INSERT INTO exercise_type (builtinKey, name, sortOrder, archived, hasDistance)
                            SELECT '$key', NULL, $nextOrder, 0, 0
                            WHERE NOT EXISTS (SELECT 1 FROM exercise_type WHERE builtinKey = '$key')""",
                    )
                    nextOrder++
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val keys = BODY_PART_TYPE_KEYS.joinToString(",") { "'$it'" }
                // Body-part splits moved into Freeform sub-items: drop unused types, archive used ones to preserve history.
                db.execSQL("DELETE FROM exercise_type WHERE builtinKey IN ($keys) AND id NOT IN (SELECT typeId FROM exercise_entry)")
                db.execSQL("UPDATE exercise_type SET archived = 1 WHERE builtinKey IN ($keys)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `strength_exercise` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `builtinKey` TEXT,
                        `nameZh` TEXT NOT NULL,
                        `nameEn` TEXT NOT NULL,
                        `bodyRegion` TEXT NOT NULL,
                        `primaryMuscleMask` INTEGER NOT NULL,
                        `secondaryMuscleMask` INTEGER NOT NULL,
                        `defaultSets` INTEGER NOT NULL,
                        `defaultReps` INTEGER NOT NULL,
                        `archived` INTEGER NOT NULL
                    )""".trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_strength_exercise_builtinKey` ON `strength_exercise` (`builtinKey`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `workout_plan` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `builtinKey` TEXT,
                        `name` TEXT,
                        `sortOrder` INTEGER NOT NULL
                    )""".trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_plan_builtinKey` ON `workout_plan` (`builtinKey`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `workout_plan_item` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `planId` INTEGER NOT NULL,
                        `exerciseId` INTEGER NOT NULL,
                        `defaultSets` INTEGER NOT NULL,
                        `defaultReps` INTEGER NOT NULL,
                        `defaultWeightKg` REAL,
                        `sortOrder` INTEGER NOT NULL,
                        FOREIGN KEY(`planId`) REFERENCES `workout_plan`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                        FOREIGN KEY(`exerciseId`) REFERENCES `strength_exercise`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_plan_item_planId` ON `workout_plan_item` (`planId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_plan_item_exerciseId` ON `workout_plan_item` (`exerciseId`)")

                db.execSQL(
                    "ALTER TABLE `exercise_entry` ADD COLUMN `planId` INTEGER REFERENCES `workout_plan`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_entry_planId` ON `exercise_entry` (`planId`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `strength_log_item` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entryId` INTEGER NOT NULL,
                        `exerciseId` INTEGER NOT NULL,
                        `sets` INTEGER NOT NULL,
                        `reps` INTEGER NOT NULL,
                        `weightKg` REAL,
                        `sortOrder` INTEGER NOT NULL,
                        FOREIGN KEY(`entryId`) REFERENCES `exercise_entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                        FOREIGN KEY(`exerciseId`) REFERENCES `strength_exercise`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""".trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_log_item_entryId` ON `strength_log_item` (`entryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_log_item_exerciseId` ON `strength_log_item` (`exerciseId`)")

                seedStrengthDefaults(db)
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE strength_exercise ADD COLUMN isTimed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE strength_exercise SET isTimed = 1 WHERE builtinKey IN ('plank','side_plank','wall_sit','dead_hang','hollow_body_hold')")
            }
        }

        fun seedBuiltinTypes(db: SupportSQLiteDatabase) {
            BUILTIN_TYPE_KEYS.forEachIndexed { i, key ->
                val hasDistance = if (key in DISTANCE_TYPE_KEYS) 1 else 0
                db.execSQL("INSERT INTO exercise_type (builtinKey, name, sortOrder, archived, hasDistance) VALUES ('$key', NULL, $i, 0, $hasDistance)")
            }
        }

        fun seedBuiltinExercises(db: SupportSQLiteDatabase) {
            for (exercise in BuiltinStrengthCatalog.exercises) {
                db.execSQL(
                    """INSERT OR IGNORE INTO `strength_exercise` (
                        `builtinKey`, `nameZh`, `nameEn`, `bodyRegion`,
                        `primaryMuscleMask`, `secondaryMuscleMask`, `defaultSets`, `defaultReps`, `archived`, `isTimed`
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)""".trimIndent(),
                    arrayOf<Any?>(
                        exercise.builtinKey,
                        exercise.nameZh,
                        exercise.nameEn,
                        exercise.region.key,
                        exercise.primaryMuscleMask,
                        exercise.secondaryMuscleMask,
                        exercise.defaultSets,
                        exercise.defaultReps,
                        if (exercise.isTimed) 1 else 0,
                    ),
                )
            }
        }

        fun seedStrengthDefaults(db: SupportSQLiteDatabase) {
            seedBuiltinExercises(db)
            BuiltinStrengthCatalog.plans.forEachIndexed { planIndex, plan ->
                db.execSQL(
                    "INSERT OR IGNORE INTO `workout_plan` (`builtinKey`, `name`, `sortOrder`) VALUES (?, NULL, ?)",
                    arrayOf<Any?>(plan.builtinKey, planIndex),
                )
                plan.items.forEachIndexed { itemIndex, item ->
                    db.execSQL(
                        """INSERT INTO `workout_plan_item` (
                            `planId`, `exerciseId`, `defaultSets`, `defaultReps`, `defaultWeightKg`, `sortOrder`
                        )
                        SELECT p.`id`, e.`id`, ?, ?, ?, ?
                        FROM `workout_plan` p
                        INNER JOIN `strength_exercise` e ON e.`builtinKey` = ?
                        WHERE p.`builtinKey` = ?
                          AND NOT EXISTS (
                              SELECT 1 FROM `workout_plan_item` i
                              WHERE i.`planId` = p.`id` AND i.`exerciseId` = e.`id` AND i.`sortOrder` = ?
                          )""".trimIndent(),
                        arrayOf<Any?>(
                            item.defaultSets,
                            item.defaultReps,
                            item.defaultWeightKg,
                            itemIndex,
                            item.exerciseKey,
                            plan.builtinKey,
                            itemIndex,
                        ),
                    )
                }
            }
        }
    }
}

fun ActiaryDatabase.Companion.build(context: Context): ActiaryDatabase =
    Room.databaseBuilder(context.applicationContext, ActiaryDatabase::class.java, ACTIARY_DATABASE_NAME)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                ActiaryDatabase.seedBuiltinTypes(db)
                ActiaryDatabase.seedStrengthDefaults(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                if (!db.isReadOnly) {
                    db.beginTransaction()
                    try {
                        ActiaryDatabase.seedBuiltinExercises(db)
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            }
        })
        .addMigrations(
            ActiaryDatabase.MIGRATION_1_2,
            ActiaryDatabase.MIGRATION_2_3,
            ActiaryDatabase.MIGRATION_3_4,
            ActiaryDatabase.MIGRATION_4_5,
            ActiaryDatabase.MIGRATION_5_6,
            ActiaryDatabase.MIGRATION_6_7,
            ActiaryDatabase.MIGRATION_7_8,
            ActiaryDatabase.MIGRATION_8_9,
        )
        .build()
