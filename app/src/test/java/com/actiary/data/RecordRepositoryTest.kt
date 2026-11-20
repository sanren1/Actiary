package com.actiary.data

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.PhotoAttachmentEntity
import com.actiary.data.db.RecordDao
import com.actiary.data.photo.PhotoStore
import com.actiary.domain.DayDraft
import com.actiary.domain.DayState
import com.actiary.domain.DraftEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.YearMonth

private class FakeDao : RecordDao {
    val rows = MutableStateFlow<Map<Long, DayWithDetails>>(emptyMap())
    val typeRows = MutableStateFlow<List<ExerciseTypeEntity>>(emptyList())
    var failReplace = false
    var lastRange: Pair<Long, Long>? = null

    override fun observeRange(from: Long, to: Long): Flow<List<DayWithDetails>> {
        lastRange = from to to
        return rows.map { m -> m.values.filter { it.record.epochDay in from..to }.sortedBy { it.record.epochDay } }
    }
    override suspend fun get(epochDay: Long) = rows.value[epochDay]
    override fun observeDay(epochDay: Long) = rows.map { it[epochDay] }
    override fun observeTypes(): Flow<List<ExerciseTypeEntity>> = typeRows.map { list -> list.filter { !it.archived } }
    override fun observeAllTypes(): Flow<List<ExerciseTypeEntity>> = typeRows
    override suspend fun insertRecordIfAbsent(record: DailyRecordEntity) {
        if (record.epochDay !in rows.value) rows.value = rows.value + (record.epochDay to DayWithDetails(record, emptyList(), emptyList()))
    }
    override suspend fun upsertRecord(record: DailyRecordEntity) = error("unused")
    override suspend fun deleteEntries(epochDay: Long) = error("unused")
    override suspend fun deletePhotos(epochDay: Long) = error("unused")
    override suspend fun insertEntries(entries: List<ExerciseEntryEntity>) = error("unused")
    override suspend fun insertType(type: ExerciseTypeEntity): Long {
        val id = (typeRows.value.maxOfOrNull { it.id } ?: 0L) + 1
        typeRows.value = typeRows.value + type.copy(id = id)
        return id
    }
    override suspend fun archiveType(id: Long) {
        typeRows.value = typeRows.value.map { if (it.id == id) it.copy(archived = true) else it }
    }
    override suspend fun insertPhotos(photos: List<PhotoAttachmentEntity>) = error("unused")
    override suspend fun replaceDay(
        record: DailyRecordEntity,
        entries: List<ExerciseEntryEntity>,
        photos: List<PhotoAttachmentEntity>,
    ) {
        if (failReplace) throw IllegalStateException("db")
        rows.value = rows.value + (record.epochDay to DayWithDetails(record, entries, photos))
    }
    override suspend fun deleteDay(epochDay: Long) { rows.value = rows.value - epochDay }
}

class RecordRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val date = LocalDate.of(2024, 2, 10)
    private val dao = FakeDao()
    private val dir by lazy { tmp.newFolder("photos") }
    private val store by lazy { PhotoStore(dir) }
    private var failUri: String? = null
    private fun repo() = RecordRepository(
        dao, store,
        { if (it == failUri) throw FileNotFoundException(it) else ByteArrayInputStream(it.toByteArray()) },
        { 42L },
    )
    private fun draft() = DayDraft.from(date, null)

    private fun seed() = runBlocking {
        store.file("a.jpg").writeText("a"); store.file("b.jpg").writeText("b")
        dao.replaceDay(
            DailyRecordEntity(date.toEpochDay(), DayStatus.EXERCISED, "", 1),
            emptyList(),
            listOf(
                PhotoAttachmentEntity(1, date.toEpochDay(), "a.jpg", 0, 1),
                PhotoAttachmentEntity(2, date.toEpochDay(), "b.jpg", 1, 1),
            ),
        )
    }

    @Test fun saveCopiesPickedPhotos() = runBlocking {
        repo().save(draft().addPhotos(listOf("a", "b")))
        assertEquals(2, dao.get(date.toEpochDay())!!.photos.size)
        assertEquals(2, dir.list()!!.size)
    }

    @Test fun copyFailureCleansUp() = runBlocking {
        failUri = "b"
        try { repo().save(draft().addPhotos(listOf("a", "b"))); fail() } catch (_: FileNotFoundException) {}
        assertNull(dao.get(date.toEpochDay()))
        assertEquals(0, dir.list()!!.size)
    }

    @Test fun editRemovesDroppedFileOnly() = runBlocking {
        seed()
        repo().save(DayDraft.from(date, dao.get(date.toEpochDay())).removePhoto(0))
        assertEquals(listOf("b.jpg"), dao.get(date.toEpochDay())!!.photos.map { it.fileName })
        assertTrue(!store.file("a.jpg").exists())
        assertTrue(store.file("b.jpg").exists())
    }

    @Test fun dbFailureKeepsOldAndRemovesNew() = runBlocking {
        seed()
        dao.failReplace = true
        val d = DayDraft.from(date, dao.get(date.toEpochDay())).removePhoto(0).addPhotos(listOf("n"))
        try { repo().save(d); fail() } catch (_: IllegalStateException) {}
        assertEquals(setOf("a.jpg", "b.jpg"), dir.list()!!.toSet())
    }

    @Test fun quickCheckInDoesNotOverwrite() = runBlocking {
        seed()
        repo().quickCheckIn(date)
        assertEquals(2, dao.get(date.toEpochDay())!!.photos.size)
        val other = date.plusDays(1)
        repo().quickCheckIn(other)
        assertEquals(DayStatus.EXERCISED, dao.get(other.toEpochDay())!!.record.status)
    }

    @Test fun clearDayDeletesRowAndFiles() = runBlocking {
        seed()
        repo().clearDay(date)
        assertNull(dao.get(date.toEpochDay()))
        assertEquals(0, dir.list()!!.size)
    }

    @Test fun observeMonthRange() = runBlocking {
        seed()
        val rest = date.plusDays(2)
        dao.replaceDay(DailyRecordEntity(rest.toEpochDay(), DayStatus.REST, "", 1), emptyList(), emptyList())
        val m = repo().observeMonth(YearMonth.of(2024, 2)).first()
        assertEquals(mapOf(date to DayState.EXERCISED, rest to DayState.REST), m)
        assertEquals(LocalDate.of(2024, 2, 1).toEpochDay() to LocalDate.of(2024, 2, 29).toEpochDay(), dao.lastRange)
    }

    @Test fun saveWritesCaloriesAndHeartRate() = runBlocking {
        val d = DayDraft(
            date, DayStatus.EXERCISED,
            listOf(DraftEntry(1L, 30, 5000, 320, 145)), "", emptyList(),
        )
        repo().save(d)
        val e = dao.get(date.toEpochDay())!!.entries.single()
        assertEquals(320, e.calories)
        assertEquals(145, e.avgHeartRate)
    }

    @Test fun addTypeAppendsSortedTrimsAndClearsBuiltinKey() = runBlocking {
        dao.insertType(ExerciseTypeEntity(builtinKey = "run", name = "跑步", sortOrder = 5))
        val id = repo().addType("  瑜伽  ", false)
        val t = repo().allTypes.first().single { it.id == id }
        assertEquals("瑜伽", t.name)
        assertNull(t.builtinKey)
        assertFalse(t.archived)
        assertFalse(t.hasDistance)
        assertEquals(6, t.sortOrder)
    }

    @Test fun archiveTypeHidesFromTypesButKeepsInAll() = runBlocking {
        val id = repo().addType("瑜伽", false)
        repo().archiveType(id)
        assertTrue(repo().types.first().none { it.id == id })
        assertTrue(repo().allTypes.first().any { it.id == id })
    }
}
