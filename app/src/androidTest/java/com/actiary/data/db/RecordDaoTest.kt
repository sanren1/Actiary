package com.actiary.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class RecordDaoTest {
    private lateinit var db: ActiaryDatabase
    private lateinit var dao: RecordDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ActiaryDatabase::class.java)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = ActiaryDatabase.seedBuiltinTypes(db)
            })
            .build()
        dao = db.recordDao()
    }

    @After
    fun tearDown() = db.close()

    private fun record(day: Long) = DailyRecordEntity(day, DayStatus.EXERCISED, "", 0)
    private fun entry(day: Long, type: Long = 1) = ExerciseEntryEntity(epochDay = day, typeId = type, durationMinutes = 30, distanceMeters = null, calories = null, avgHeartRate = null, sortOrder = 0)
    private fun photo(day: Long, name: String) = PhotoAttachmentEntity(epochDay = day, fileName = name, sortOrder = 0, addedAt = 0)

    private fun count(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun seedsTwelveBuiltinTypesInOrder() = runBlocking {
        val types = dao.observeTypes().first()
        assertEquals(
            listOf(
                "running", "cycling", "swimming", "hiking", "ball", "freeform",
                "chest", "back", "shoulders", "arms", "legs", "abs",
            ),
            types.map { it.builtinKey },
        )
        assertEquals(
            setOf("running", "cycling", "swimming", "hiking"),
            types.filter { it.hasDistance }.map { it.builtinKey }.toSet(),
        )
    }

    @Test
    fun removingTypeReferencedByEntryArchivesItAndPreservesHistory() = runBlocking {
        val typeId = dao.insertType(ExerciseTypeEntity(builtinKey = null, name = "custom", sortOrder = 99))
        dao.replaceDay(record(20), listOf(entry(20, typeId)), emptyList())
        dao.removeType(typeId)

        assertTrue(typeId !in dao.observeTypes().first().map { it.id })
        assertTrue(typeId in dao.observeAllTypes().first().map { it.id })

        val day = dao.get(20)!!
        assertEquals(typeId, day.entries.single().typeId)
        assertNotNull(dao.observeAllTypes().first().firstOrNull { it.id == typeId }?.name)

        try {
            db.openHelper.writableDatabase.execSQL("DELETE FROM exercise_type WHERE id = $typeId")
            fail("RESTRICT 应阻止物理删除被引用的类型")
        } catch (expected: SQLiteConstraintException) {
            // 引用仍在，只能归档不能删除
        }
    }

    @Test
    fun removingUnusedTypeDeletesOnlyCustomType() = runBlocking {
        val builtin = dao.observeTypes().first().first()
        val custom = dao.insertType(ExerciseTypeEntity(builtinKey = null, name = "custom", sortOrder = 99))
        dao.removeType(custom)
        dao.removeType(builtin.id)

        assertTrue(dao.observeAllTypes().first().none { it.id == custom })
        assertEquals(builtin, dao.observeTypes().first().first())
    }

    @Test
    fun moveTypeUsesActiveOrderAndHandlesTiedSortValues() = runBlocking {
        val initial = dao.observeTypes().first().map { it.id }
        val custom = dao.insertType(ExerciseTypeEntity(builtinKey = null, name = "custom", sortOrder = 0))
        val archived = dao.insertType(ExerciseTypeEntity(builtinKey = null, name = "archived", sortOrder = 0, archived = true))

        dao.moveType(custom, -1)
        dao.moveType(archived, 1)
        dao.moveType(custom, -1)
        assertEquals(listOf(custom) + initial, dao.observeTypes().first().map { it.id })

        dao.moveType(initial.first(), 1)
        assertEquals(listOf(custom, initial[1], initial.first()) + initial.drop(2), dao.observeTypes().first().map { it.id })
        assertTrue(dao.observeAllTypes().first().single { it.id == archived }.archived)
    }

    @Test
    fun replaceDayReplacesEntriesAndPhotos() = runBlocking {
        dao.replaceDay(record(10), listOf(entry(10), entry(10)), listOf(photo(10, "a.jpg")))
        dao.replaceDay(record(10), listOf(entry(10)), listOf(photo(10, "b.jpg"), photo(10, "c.jpg")))
        val day = dao.get(10)!!
        assertEquals(1, day.entries.size)
        assertEquals(listOf("b.jpg", "c.jpg"), day.photos.map { it.fileName }.sorted())
    }

    @Test
    fun deleteDayCascades() = runBlocking {
        dao.replaceDay(record(10), listOf(entry(10)), listOf(photo(10, "a.jpg")))
        dao.deleteDay(10)
        assertNull(dao.get(10))
        assertEquals(0, count("exercise_entry"))
        assertEquals(0, count("photo_attachment"))
    }

    @Test
    fun observeRangeExcludesOutsideDays() = runBlocking {
        listOf(9L, 10L, 12L, 13L).forEach { dao.replaceDay(record(it), emptyList(), emptyList()) }
        assertEquals(listOf(10L, 12L), dao.observeRange(10, 12).first().map { it.record.epochDay })
    }

    @Test
    fun updateTypeModifiesActiveRowAndIgnoresArchivedRow() = runBlocking {
        val activeId = dao.insertType(ExerciseTypeEntity(builtinKey = null, name = "Pilates", sortOrder = 10, hasDistance = false))
        val archivedId = dao.insertType(ExerciseTypeEntity(builtinKey = null, name = "Old", sortOrder = 11, archived = true))

        val updatedActive = dao.updateType(activeId, "Core", true, "stretch", 7)
        val updatedArchived = dao.updateType(archivedId, "Renamed", true, "climb", 3)

        assertEquals(1, updatedActive)
        assertEquals(0, updatedArchived)

        val active = dao.observeAllTypes().first().single { it.id == activeId }
        assertEquals("Core", active.name)
        assertTrue(active.hasDistance)
        assertEquals("stretch", active.iconKey)
        assertEquals(7, active.extraMetricMask)

        val archived = dao.observeAllTypes().first().single { it.id == archivedId }
        assertEquals("Old", archived.name)
        assertNull(archived.iconKey)
    }
}
