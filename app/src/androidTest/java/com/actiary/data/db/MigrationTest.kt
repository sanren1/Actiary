package com.actiary.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
    fun migrate1To2KeepsOldDataAndAddsNewColumns() = runBlocking {
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

        helper.runMigrationsAndValidate(TEST_DB, 2, true, ActiaryDatabase.MIGRATION_1_2).close()

        val room = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ActiaryDatabase::class.java,
            TEST_DB,
        ).build()
        try {
            val day = room.recordDao().get(100)!!
            val entry = day.entries.single()
            assertEquals(30, entry.durationMinutes)
            assertEquals(5000, entry.distanceMeters)
            assertNull(entry.calories)
            assertNull(entry.avgHeartRate)

            val types = room.recordDao().observeAllTypes().first().filter { it.builtinKey != null }
            assertEquals(8, types.size)
            assertEquals("freeform", types.last().builtinKey)
            assertEquals(
                setOf("running", "cycling", "swimming", "hiking"),
                types.filter { it.hasDistance }.map { it.builtinKey }.toSet(),
            )
        } finally {
            room.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
