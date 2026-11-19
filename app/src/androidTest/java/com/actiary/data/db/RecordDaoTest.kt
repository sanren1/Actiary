package com.actiary.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private fun entry(day: Long, type: Long = 1) = ExerciseEntryEntity(epochDay = day, typeId = type, durationMinutes = 30, distanceMeters = null, sortOrder = 0)
    private fun photo(day: Long, name: String) = PhotoAttachmentEntity(epochDay = day, fileName = name, sortOrder = 0, addedAt = 0)

    private fun count(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun seedsSevenBuiltinTypesInOrder() = runBlocking {
        val keys = dao.observeTypes().first().map { it.builtinKey }
        assertEquals(listOf("running", "strength", "cycling", "swimming", "yoga", "hiking", "ball"), keys)
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
}
