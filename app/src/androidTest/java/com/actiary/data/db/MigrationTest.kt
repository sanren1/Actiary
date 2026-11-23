package com.actiary.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.actiary.domain.WorkoutMetric
import com.actiary.domain.WorkoutMetrics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ActiaryDatabase::class.java,
    )

    @Test
    fun migrate1To2KeepsOldDataAndAddsNewColumns() {
        // 模拟真实 v1 安装：7 个内置类型 + 1 个自定义类型 + 1 条旧条目
        val db = helper.createDatabase(TEST_DB, 1)
        listOf("running", "strength", "cycling", "swimming", "yoga", "hiking", "ball")
            .forEachIndexed { i, key ->
                db.execSQL("INSERT INTO exercise_type (builtinKey, name, sortOrder, archived) VALUES ('$key', NULL, $i, 0)")
            }
        db.execSQL("INSERT INTO exercise_type (builtinKey, name, sortOrder, archived) VALUES (NULL, 'custom', 7, 0)")
        db.execSQL("INSERT INTO daily_record (epochDay, status, note, updatedAt) VALUES (100, 'EXERCISED', '', 0)")
        db.execSQL("INSERT INTO exercise_entry (epochDay, typeId, durationMinutes, distanceMeters, sortOrder) VALUES (100, 8, 30, 5000, 0)")
        db.close()

        helper.runMigrationsAndValidate(TEST_DB, 2, true, ActiaryDatabase.MIGRATION_1_2).use { migrated ->
            migrated.query("SELECT durationMinutes, distanceMeters, calories, avgHeartRate FROM exercise_entry").use { entries ->
                assertEquals(1, entries.count)
                entries.moveToFirst()
                assertEquals(30, entries.getInt(0))
                assertEquals(5000, entries.getInt(1))
                assertNull(entries.getString(2))
                assertNull(entries.getString(3))
            }
            migrated.query("SELECT builtinKey FROM exercise_type WHERE builtinKey IS NOT NULL ORDER BY sortOrder").use { types ->
                assertEquals(8, types.count)
                types.moveToLast()
                assertEquals("freeform", types.getString(0))
            }
            migrated.query("SELECT builtinKey FROM exercise_type WHERE hasDistance = 1").use { types ->
                val keys = buildSet { while (types.moveToNext()) add(types.getString(0)) }
                assertEquals(setOf("running", "cycling", "swimming", "hiking"), keys)
            }
        }
    }

    @Test
    fun migrate2To3DropsUnusedStrengthYogaArchivesUsedOnesAndAddsIconKey() {
        val db = helper.createDatabase(TEST_DB, 2)
        listOf("running", "strength", "cycling", "swimming", "yoga", "hiking", "ball", "freeform")
            .forEachIndexed { i, key ->
                db.execSQL("INSERT INTO exercise_type (builtinKey, name, sortOrder, archived, hasDistance) VALUES ('$key', NULL, $i, 0, 0)")
            }
        db.execSQL("INSERT INTO daily_record (epochDay, status, note, updatedAt) VALUES (100, 'EXERCISED', '', 0)")
        // strength has id 2 and is referenced; yoga (id 5) is not.
        db.execSQL("INSERT INTO exercise_entry (epochDay, typeId, durationMinutes, distanceMeters, calories, avgHeartRate, sortOrder) VALUES (100, 2, 30, NULL, NULL, NULL, 0)")
        db.close()

        helper.runMigrationsAndValidate(TEST_DB, 3, true, ActiaryDatabase.MIGRATION_2_3).use { migrated ->
            migrated.query("SELECT builtinKey, archived, iconKey FROM exercise_type ORDER BY sortOrder").use { types ->
                val keys = mutableListOf<String>()
                val archived = mutableListOf<String>()
                while (types.moveToNext()) {
                    val key = types.getString(0)
                    keys.add(key)
                    if (types.getInt(1) == 1) archived.add(key)
                    assertNull(types.getString(2))
                }
                assertEquals(listOf("running", "strength", "cycling", "swimming", "hiking", "ball", "freeform"), keys)
                assertEquals(listOf("strength"), archived)
            }
            migrated.query("SELECT durationMinutes FROM exercise_entry").use { entries ->
                assertEquals(1, entries.count)
                entries.moveToFirst()
                assertEquals(30, entries.getInt(0))
            }
        }
    }

    @Test
    fun migrate3To4MovesOnlyBuiltinStrengthEntriesToFreeform() = runBlocking {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                """INSERT INTO exercise_type (id, builtinKey, name, sortOrder, archived, hasDistance) VALUES
                    (2, 'strength', NULL, 1, 1, 0),
                    (5, 'yoga', NULL, 4, 1, 0),
                    (8, 'freeform', NULL, 7, 0, 0),
                    (9, NULL, 'Strength', 8, 0, 0)""",
            )
            db.execSQL("INSERT INTO daily_record (epochDay, status, note, updatedAt) VALUES (100, 'EXERCISED', 'training note', 1234)")
            db.execSQL("INSERT INTO photo_attachment (id, epochDay, fileName, sortOrder, addedAt) VALUES (31, 100, 'a.jpg', 0, 1234)")
            db.execSQL(
                """INSERT INTO exercise_entry (id, epochDay, typeId, durationMinutes, distanceMeters, calories, avgHeartRate, sortOrder) VALUES
                    (21, 100, 2, 30, 750, 320, 145, 0),
                    (22, 100, 8, 45, NULL, 200, 120, 1),
                    (23, 100, 5, 50, NULL, NULL, NULL, 2),
                    (24, 100, 9, 60, NULL, 500, NULL, 3)""",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, ActiaryDatabase.MIGRATION_3_4).close()

        val room = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ActiaryDatabase::class.java,
            TEST_DB,
        ).addMigrations(ActiaryDatabase.MIGRATION_4_5, ActiaryDatabase.MIGRATION_5_6).build()
        try {
            val dao = room.recordDao()
            assertEquals(
                listOf("yoga", "freeform", null, "chest", "back", "shoulders", "arms", "legs", "abs"),
                dao.observeAllTypes().first().map { it.builtinKey },
            )
            val day = dao.get(100)!!
            assertEquals(DailyRecordEntity(100, DayStatus.EXERCISED, "training note", 1234), day.record)
            assertEquals(listOf(PhotoAttachmentEntity(31, 100, "a.jpg", 0, 1234)), day.photos)
            assertEquals(
                listOf(
                    ExerciseEntryEntity(21, 100, 8, 30, 750, 320, 145, 0),
                    ExerciseEntryEntity(22, 100, 8, 45, null, 200, 120, 1),
                    ExerciseEntryEntity(23, 100, 5, 50, null, null, null, 2),
                    ExerciseEntryEntity(24, 100, 9, 60, null, 500, null, 3),
                ),
                day.entries.sortedBy { it.sortOrder },
            )
        } finally {
            room.close()
        }
    }

    @Test
    fun migrate4To5KeepsExistingSessionsAndSavesOptionalMeasurements() = runBlocking {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL("INSERT INTO exercise_type (id, builtinKey, sortOrder, archived, hasDistance) VALUES (5, 'running', 0, 0, 1)")
            db.execSQL("INSERT INTO daily_record (epochDay, status, note, updatedAt) VALUES (100, 'EXERCISED', 'old workout', 1234)")
            db.execSQL(
                "INSERT INTO exercise_entry (id, epochDay, typeId, durationMinutes, distanceMeters, calories, avgHeartRate, sortOrder) " +
                    "VALUES (20, 100, 5, 30, 5000, 320, 145, 0)",
            )
            db.execSQL("INSERT INTO photo_attachment (id, epochDay, fileName, sortOrder, addedAt) VALUES (31, 100, 'a.jpg', 0, 1234)")
        }

        helper.runMigrationsAndValidate(TEST_DB, 5, true, ActiaryDatabase.MIGRATION_4_5).close()
        val room = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ActiaryDatabase::class.java,
            TEST_DB,
        ).addMigrations(ActiaryDatabase.MIGRATION_5_6, ActiaryDatabase.MIGRATION_6_7).build()
        try {
            val dao = room.recordDao()
            val original = dao.get(100)!!
            assertEquals("old workout", original.record.note)
            assertEquals(ExerciseEntryEntity(20, 100, 5, 30, 5000, 320, 145, 0), original.entries.single())
            assertEquals(listOf(PhotoAttachmentEntity(31, 100, "a.jpg", 0, 1234)), original.photos)
            assertNull(dao.getActiveTypes().first { it.id == 5L }.extraMetricMask)

            val mask = WorkoutMetric.CADENCE.bit or WorkoutMetric.STEPS.bit or WorkoutMetric.ELEVATION_GAIN.bit
            assertEquals(1, dao.updateType(5, null, true, null, mask))
            assertEquals(mask, dao.getActiveTypes().first { it.id == 5L }.extraMetricMask)
            val metrics = WorkoutMetrics(durationSeconds = 1805, cadenceSpm = 172, stepCount = 8600, elevationGainCm = 240)
            dao.replaceDay(original.record, listOf(original.entries.single().copy(metrics = metrics)), original.photos)
            assertEquals(metrics, dao.get(100)!!.entries.single().metrics)
        } finally {
            room.close()
        }
    }

    @Test
    fun migrate5To6AppendsSixBodyPartBuiltinTypesWithoutDisturbingExistingOrder() = runBlocking {
        helper.createDatabase(TEST_DB, 5).use { db ->
            db.execSQL("INSERT INTO exercise_type (id, builtinKey, name, sortOrder, archived, hasDistance) VALUES (1, 'running', NULL, 0, 0, 1)")
            db.execSQL("INSERT INTO exercise_type (id, builtinKey, name, sortOrder, archived, hasDistance) VALUES (2, NULL, 'Custom', 1, 0, 0)")
        }
        helper.runMigrationsAndValidate(TEST_DB, 6, true, ActiaryDatabase.MIGRATION_5_6).use { migrated ->
            migrated.query("SELECT builtinKey, sortOrder FROM exercise_type ORDER BY sortOrder").use { cursor ->
                val keys = mutableListOf<String?>()
                while (cursor.moveToNext()) keys.add(cursor.getString(0))
                assertEquals(
                    listOf("running", null, "chest", "back", "shoulders", "arms", "legs", "abs"),
                    keys,
                )
            }
        }
    }

    @Test
    fun migrate6To7DropsUnusedBodyPartTypesAndArchivesUsedOnes() = runBlocking {
        helper.createDatabase(TEST_DB, 6).use { db ->
            db.execSQL("INSERT INTO exercise_type (id, builtinKey, name, sortOrder, archived, hasDistance) VALUES (1, 'freeform', NULL, 0, 0, 0)")
            db.execSQL("INSERT INTO exercise_type (id, builtinKey, name, sortOrder, archived, hasDistance) VALUES (2, 'chest', NULL, 1, 0, 0)")
            db.execSQL("INSERT INTO exercise_type (id, builtinKey, name, sortOrder, archived, hasDistance) VALUES (3, 'back', NULL, 2, 0, 0)")
            db.execSQL("INSERT INTO daily_record (epochDay, status, note, updatedAt) VALUES (200, 'EXERCISED', '', 1234)")
            db.execSQL("INSERT INTO exercise_entry (id, epochDay, typeId, sortOrder) VALUES (10, 200, 2, 0)")
        }
        helper.runMigrationsAndValidate(TEST_DB, 7, true, ActiaryDatabase.MIGRATION_6_7).use { migrated ->
            migrated.query("SELECT builtinKey, archived FROM exercise_type ORDER BY id").use { cursor ->
                val rows = mutableListOf<Pair<String?, Int>>()
                while (cursor.moveToNext()) rows.add(cursor.getString(0) to cursor.getInt(1))
                assertEquals(listOf("freeform" to 0, "chest" to 1), rows)
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
