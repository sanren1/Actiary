package com.actiary.data

import com.actiary.data.photo.PhotoStore
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class RecordStorageUsageTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun countsDatabaseSidecarsPermanentPhotosAndCacheButIgnoresDrafts() {
        val database = folder.newFile("actiary.db").apply { writeBytes(ByteArray(17)) }
        File(folder.root, "actiary.db-wal").writeBytes(ByteArray(5))
        File(folder.root, "actiary.db-shm").writeBytes(ByteArray(3))
        File(folder.root, "actiary.db-journal").writeBytes(ByteArray(2))
        folder.newFile("backup.zip").writeBytes(ByteArray(90))
        val photos = PhotoStore(folder.newFolder("photos"))
        photos.copyIn { ByteArrayInputStream(ByteArray(7)) }
        photos.copyIn { ByteArrayInputStream(ByteArray(13)) }
        photos.stage { ByteArrayInputStream(ByteArray(100)) }
        val cache = folder.newFolder("cache").apply {
            File(this, "tmp.bin").writeBytes(ByteArray(11))
        }

        val usage = measureRecordStorage(database, photos, cache)

        assertEquals(27L, usage.databaseBytes)
        assertEquals(20L, usage.photoBytes)
        assertEquals(2, usage.photoCount)
        assertEquals(13L, usage.largestPhotoBytes)
        assertEquals(10L, usage.averagePhotoBytes)
        assertEquals(11L, usage.cacheBytes)
        assertEquals(58L, usage.totalBytes)

        clearDirectoryContents(cache)
        assertEquals(0L, directorySizeBytes(cache))
    }
}
