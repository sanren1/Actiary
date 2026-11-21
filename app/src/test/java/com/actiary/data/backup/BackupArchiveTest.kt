package com.actiary.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {
    @get:Rule val cache = TemporaryFolder()

    @Test fun stageFilesAndDiscardTemporaryArchive() {
        val input = archive("backup.json" to metadata, "photos/a.jpg" to "image")
        val loaded = unpackBackup(input.inputStream(), cache.root)
        loaded.use {
            assertEquals("image", it.photo("a.jpg")?.readText())
            assertEquals(20L, it.backup.records.single().epochDay)
        }
        assertTrue(cache.root.listFiles()!!.isEmpty())
    }

    @Test fun rejectInvalidZipOrMissingMetadata() {
        reject("garbage".toByteArray())
        reject(archive("photos/a.jpg" to "image"))
        reject(archive("backup.json" to metadata).dropLast(22).toByteArray())
    }

    @Test fun rejectTrailingDataAfterZipDirectory() {
        reject(archive("backup.json" to metadata) + "unexpected trailer".toByteArray())
    }

    @Test fun rejectUnsafeOrDuplicateMembers() {
        reject(archive("backup.json" to metadata, "photos/../a.jpg" to "image"))
        reject(archive("backup.json" to metadata, "photos\\a.jpg" to "image"))
        val duplicate = archive("backup.json" to metadata, "photos/a.jpg" to "image", "photos/b.jpg" to "other").apply {
            val from = "photos/b.jpg".toByteArray()
            for (i in 0..size - from.size) {
                if (from.indices.all { this[i + it] == from[it] }) this[i + 7] = 'a'.code.toByte()
            }
        }
        reject(duplicate)
    }

    private fun reject(input: ByteArray) {
        assertThrows(BackupFormatException::class.java) { unpackBackup(input.inputStream(), cache.root) }
        assertTrue(cache.root.listFiles()!!.isEmpty())
    }

    private fun archive(vararg entries: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }.toByteArray()

    private val metadata = """{
        "format": 1, "appVersion": "0.13.0", "exportedAt": 0, "appearance": {}, "types": [],
        "records": [{"epochDay": 20, "status": "REST", "note": "", "updatedAt": 0}], "entries": [],
        "photos": [{"epochDay": 20, "fileName": "a.jpg", "sortOrder": 0, "addedAt": 0}]
    }"""
}
