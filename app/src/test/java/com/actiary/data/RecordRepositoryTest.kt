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
import com.actiary.domain.WorkoutMetric
import com.actiary.domain.WorkoutMetrics
import com.actiary.domain.effectiveWorkoutMetricMask
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
    override fun observeTypes(): Flow<List<ExerciseTypeEntity>> = observeAllTypes().map { list -> list.filter { !it.archived } }
    override fun observeAllTypes(): Flow<List<ExerciseTypeEntity>> = typeRows.map { list -> list.sortedWith(compareBy({ it.sortOrder }, { it.id })) }
    override suspend fun getActiveTypes() = observeTypes().first()
    override suspend fun maxTypeSortOrder() = typeRows.value.maxOfOrNull { it.sortOrder }
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
        typeRows.value = typeRows.value.map { if (it.id == id && it.builtinKey == null) it.copy(archived = true) else it }
    }
    override suspend fun deleteUnusedType(id: Long): Int {
        val original = typeRows.value
        val used = rows.value.values.any { day -> day.entries.any { it.typeId == id } }
        if (used) return 0
        typeRows.value = original.filterNot { it.id == id && it.builtinKey == null }
        return original.size - typeRows.value.size
    }
    override suspend fun setTypeSortOrder(id: Long, sortOrder: Int) {
        typeRows.value = typeRows.value.map { if (it.id == id) it.copy(sortOrder = sortOrder) else it }
    }
    override suspend fun updateType(id: Long, name: String?, hasDistance: Boolean, iconKey: String?, extraMetricMask: Int?): Int {
        val type = typeRows.value.firstOrNull { it.id == id && !it.archived } ?: return 0
        typeRows.value = typeRows.value.map {
            if (it == type) it.copy(name = name, hasDistance = hasDistance, iconKey = iconKey, extraMetricMask = extraMetricMask) else it
        }
        return 1
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
    override suspend fun getAllTypes() = error("unused")
    override suspend fun getAllRecords() = error("unused")
    override suspend fun getAllEntries() = error("unused")
    override suspend fun getAllPhotos() = error("unused")
    override suspend fun getAllPhotoFileNames() = error("unused")
    override suspend fun insertRecords(records: List<DailyRecordEntity>) = error("unused")
    override suspend fun deleteAllEntries() = error("unused")
    override suspend fun deleteAllPhotoRows() = error("unused")
    override suspend fun deleteAllRecords() = error("unused")
    override suspend fun deleteCustomTypes() = error("unused")
    override suspend fun updateBuiltinType(
        builtinKey: String, name: String?, sortOrder: Int, archived: Boolean, hasDistance: Boolean, iconKey: String?,
        extraMetricMask: Int?,
    ) = error("unused")
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

    @Test fun stagedPickerPhotosSaveAfterTheOriginalGrantIsGone() = runBlocking {
        val pickerUri = "content://picker/1"
        val staged = repo().stagePickedPhotos(List(9) { pickerUri })
        assertTrue(store.listFileNames().isEmpty())
        failUri = pickerUri

        repo().save(draft().withNote("x".repeat(200)).copy(photos = staged))

        val saved = dao.get(date.toEpochDay())!!
        assertEquals(200, saved.record.note.length)
        assertEquals((0..8).toList(), saved.photos.map { it.sortOrder })
        assertEquals(List(9) { pickerUri }, saved.photos.map { store.file(it.fileName).readText() })
    }

    @Test fun partialPickerFailureDiscardsEveryNewDraftFile() = runBlocking {
        failUri = "missing"
        try {
            repo().stagePickedPhotos(listOf("readable", "missing"))
            fail()
        } catch (_: FileNotFoundException) {
            assertTrue(dir.walkTopDown().none { it.isFile })
            assertNull(dao.get(date.toEpochDay()))
        }
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

    @Test fun editingTypeMetricSelectionKeepsExistingWorkoutValues() = runBlocking {
        val repository = repo()
        val mask = WorkoutMetric.POOL_LENGTH.bit or WorkoutMetric.LAPS.bit or WorkoutMetric.STROKES.bit
        val id = repository.addType("Pool", true, null, extraMetricMask = mask)
        val metrics = WorkoutMetrics(durationSeconds = 59, poolLengthCm = 2500, lapCount = 4, strokeCount = 82)
        val entry = DraftEntry(id, durationMinutes = 1, distanceMeters = 100, metrics = metrics)
        repository.save(draft().copy(entries = listOf(entry)))

        assertEquals(mask, repository.types.first().single().extraMetricMask)
        repository.updateType(id, "Pool", true, null, extraMetricMask = 0)
        val reopened = DayDraft.from(date, repository.observeDay(date).first())
        repository.save(reopened.withNote("same session"))

        assertEquals(0, repository.types.first().single().extraMetricMask)
        assertEquals(metrics, repository.observeDay(date).first()!!.entries.single().metrics)
    }

    @Test fun legacyMetricSelectionDefaultsBySportButExplicitZeroDisablesIt() {
        assertEquals(
            WorkoutMetric.CADENCE.bit or WorkoutMetric.STEPS.bit or WorkoutMetric.ELEVATION_GAIN.bit,
            effectiveWorkoutMetricMask("running", null),
        )
        assertEquals(
            WorkoutMetric.POOL_LENGTH.bit or WorkoutMetric.LAPS.bit or WorkoutMetric.STROKES.bit,
            effectiveWorkoutMetricMask("swimming", null),
        )
        assertEquals(0, effectiveWorkoutMetricMask(null, null))
        assertEquals(0, effectiveWorkoutMetricMask("running", 0))
    }

    @Test fun addTypeAppendsSortedTrimsAndClearsBuiltinKey() = runBlocking {
        dao.insertType(ExerciseTypeEntity(builtinKey = "run", name = "跑步", sortOrder = 5))
        val id = repo().addType("  瑜伽  ", false, "stretch")
        val t = repo().allTypes.first().single { it.id == id }
        assertEquals("瑜伽", t.name)
        assertNull(t.builtinKey)
        assertFalse(t.archived)
        assertFalse(t.hasDistance)
        assertEquals("stretch", t.iconKey)
        assertEquals(6, t.sortOrder)
    }

    @Test fun removeUsedTypeHidesFromTypesButKeepsInAll() = runBlocking {
        val id = repo().addType("瑜伽", false, null)
        repo().save(draft().toggleType(id))
        repo().removeType(id)
        assertTrue(repo().types.first().none { it.id == id })
        assertTrue(repo().allTypes.first().single { it.id == id }.archived)
        assertEquals(id, repo().observeDay(date).first()!!.entries.single().typeId)
    }

    @Test fun moveTypeChangesRecordingOrder() = runBlocking {
        val running = dao.insertType(ExerciseTypeEntity(builtinKey = "running", name = null, sortOrder = 0))
        val climbing = repo().addType("攀岩", false, "climb")
        repo().moveType(climbing, -1)
        assertEquals(listOf(climbing, running), repo().types.first().map { it.id })
    }

    @Test fun editingBuiltinTypeChangesPresentationWithoutReplacingItsIdentity() = runBlocking {
        val id = dao.insertType(ExerciseTypeEntity(builtinKey = "running", name = null, sortOrder = 3, hasDistance = true))
        repo().save(draft().toggleType(id))

        repo().updateType(id, "  Trail running  ", false, "climb")

        val edited = dao.getActiveTypes().single()
        assertEquals(id, edited.id)
        assertEquals("running", edited.builtinKey)
        assertEquals(3, edited.sortOrder)
        assertEquals("Trail running", edited.name)
        assertFalse(edited.hasDistance)
        assertEquals("climb", edited.iconKey)
        assertEquals(id, dao.get(date.toEpochDay())!!.entries.single().typeId)

        repo().updateType(id, null, true, null)
        assertNull(dao.getActiveTypes().single().name)
        assertNull(dao.getActiveTypes().single().iconKey)
    }
}
