package com.actiary.data.backup

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.actiary.data.appearance.AppearanceRepository
import com.actiary.data.appearance.ThemeMode
import com.actiary.data.db.ActiaryDatabase
import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.PhotoAttachmentEntity
import com.actiary.data.photo.PhotoStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

class BackupRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var root: File
    private lateinit var source: Store
    private lateinit var destination: Store

    @Before fun setup() {
        root = Files.createTempDirectory(context.cacheDir.toPath(), "backup-test-").toFile()
        source = store("source")
        destination = store("destination")
    }

    @After fun tearDown() {
        source.db.close()
        destination.db.close()
        scope.cancel()
        root.deleteRecursively()
    }

    @Test fun overwriteRestoresDayTypeAppearanceAndPhotoBytes() = runBlocking {
        val legacy = source.db.recordDao().insertType(ExerciseTypeEntity(
            builtinKey = "yoga", name = null, sortOrder = 7, archived = true,
        ))
        source.putDay(20, "源记录", "imported bytes", legacy)
        source.appearance.setThemeMode(ThemeMode.DARK)
        destination.putDay(10, "old", "different bytes at same file name", type = 1)

        val input = ByteArrayOutputStream().also { assertEquals(ExportSummary(1, 1), source.repo.export(it)) }
        destination.repo.load(input.toByteArray().inputStream()).use {
            assertNull(destination.db.recordDao().get(20))
            assertEquals("different bytes at same file name", destination.photos.file("test.jpg").readText())
            assertEquals(ImportSummary(1, 0), destination.repo.apply(it, ImportMode.OVERWRITE))
        }
        val dao = destination.db.recordDao()
        val day = dao.get(20)!!
        val photo = day.photos.single()
        assertNull(dao.get(10))
        assertEquals("源记录", day.record.note)
        assertEquals(35, day.entries.single().durationMinutes)
        assertEquals("yoga", dao.observeAllTypes().first().single { it.id == day.entries.single().typeId }.builtinKey)
        assertEquals("imported bytes", destination.photos.file(photo.fileName).readText())
        assertNotEquals("test.jpg", photo.fileName)
        assertEquals(listOf(photo.fileName), destination.photos.listFileNames())
        assertEquals(ThemeMode.DARK, destination.appearance.settings.first().themeMode)
    }

    @Test fun mergeSkipsExistingWholeDayAndItsPhoto() = runBlocking {
        source.putDay(20, "archive", "archive photo", type = 1)
        source.db.recordDao().upsertRecord(DailyRecordEntity(21, DayStatus.REST, "new day", 9))
        source.appearance.setThemeMode(ThemeMode.DARK)
        destination.db.recordDao().upsertRecord(DailyRecordEntity(20, DayStatus.REST, "local", 10))

        transfer(ImportMode.MERGE).also { assertEquals(ImportSummary(1, 1), it) }
        val local = destination.db.recordDao().get(20)!!
        assertEquals("local", local.record.note)
        assertTrue(local.entries.isEmpty())
        assertTrue(local.photos.isEmpty())
        assertTrue(destination.photos.listFileNames().isEmpty())
        assertEquals("new day", destination.db.recordDao().get(21)!!.record.note)
        assertEquals(ThemeMode.LIGHT, destination.appearance.settings.first().themeMode)
    }

    @Test fun failedAttachmentInsertRollsBackRowsAndNewFiles() = runBlocking {
        source.putDay(21, "archive", "archive photo", type = 1)
        destination.putDay(20, "local", "local photo", type = 1)
        destination.appearance.setThemeMode(ThemeMode.DARK)
        destination.db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_import BEFORE INSERT ON photo_attachment
            WHEN NEW.epochDay = 21 BEGIN SELECT RAISE(ABORT, 'test rollback'); END
        """.trimIndent())

        try {
            transfer(ImportMode.OVERWRITE)
            fail("The trigger should abort the import")
        } catch (_: SQLiteConstraintException) {
            assertEquals("local", destination.db.recordDao().get(20)!!.record.note)
            assertNull(destination.db.recordDao().get(21))
            assertEquals(listOf("test.jpg"), destination.photos.listFileNames())
            assertEquals("local photo", destination.photos.file("test.jpg").readText())
            assertEquals(ThemeMode.DARK, destination.appearance.settings.first().themeMode)
        }
    }

    @Test fun orphanCleanupKeepsReferencedFiles() = runBlocking {
        destination.putDay(20, "local", "local photo", type = 1)
        destination.photos.copyIn { "orphan".byteInputStream() }
        assertEquals(1, destination.repo.orphanPhotoCount())
        assertEquals(1, destination.repo.deleteOrphanPhotos())
        assertEquals(listOf("test.jpg"), destination.photos.listFileNames())
        assertEquals(0, destination.repo.orphanPhotoCount())
    }

    @Test fun firstLoadRemovesAbandonedSessionsAndKeepsLiveSessions() = runBlocking {
        val imports = File(root, "destination/import")
        File(imports, "abandoned/photo-0").apply {
            parentFile!!.mkdirs()
            writeText("abandoned staging file")
        }
        val output = ByteArrayOutputStream()
        source.repo.export(output)
        val input = output.toByteArray()
        destination.repo.load(input.inputStream()).use {
            assertEquals(1, imports.listFiles()!!.size)
            assertTrue(!File(imports, "abandoned").exists())
            destination.repo.load(input.inputStream()).use {
                assertEquals(2, imports.listFiles()!!.size)
            }
            assertEquals(1, imports.listFiles()!!.size)
        }
        assertEquals(0, imports.listFiles()!!.size)
    }

    private suspend fun transfer(mode: ImportMode): ImportSummary {
        val out = ByteArrayOutputStream()
        source.repo.export(out)
        return destination.repo.load(out.toByteArray().inputStream()).use { destination.repo.apply(it, mode) }
    }

    private fun store(name: String): Store {
        val dir = File(root, name).apply { mkdirs() }
        val db = Room.inMemoryDatabaseBuilder(context, ActiaryDatabase::class.java)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = ActiaryDatabase.seedBuiltinTypes(db)
            }).build()
        val photos = PhotoStore(File(dir, "photos").apply { mkdirs() })
        val appearance = AppearanceRepository(PreferenceDataStoreFactory.create(scope = scope) {
            File(dir, "appearance.preferences_pb")
        })
        return Store(db, photos, appearance, BackupRepository(db, photos, appearance, "0.13.0", dir))
    }

    private data class Store(
        val db: ActiaryDatabase,
        val photos: PhotoStore,
        val appearance: AppearanceRepository,
        val repo: BackupRepository,
    ) {
        suspend fun putDay(day: Long, note: String, photo: String, type: Long) {
            photos.file("test.jpg").writeText(photo)
            db.recordDao().replaceDay(
                DailyRecordEntity(day, DayStatus.EXERCISED, note, 12),
                listOf(ExerciseEntryEntity(epochDay = day, typeId = type, durationMinutes = 35,
                    distanceMeters = null, calories = null, avgHeartRate = null, sortOrder = 0)),
                listOf(PhotoAttachmentEntity(epochDay = day, fileName = "test.jpg", sortOrder = 0, addedAt = 12)),
            )
        }
    }
}
