package com.actiary.data.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class PhotoStoreTest {
    @get:Rule val temp = TemporaryFolder()

    private val store by lazy { PhotoStore(temp.root) }

    @Test
    fun stageWritesFileToDraftDirectory() {
        val bytes = "sample draft image data".toByteArray()
        val name = store.stage { ByteArrayInputStream(bytes) }

        assertTrue(name.endsWith(".jpg"))
        val staged = store.stagedFile(name)
        assertTrue(staged.exists())
        assertEquals("drafts", staged.parentFile?.name)
        assertEquals(String(bytes), staged.readText())
        assertEquals(listOf(name), store.listDraftFileNames())
    }

    @Test
    fun listDraftFileNamesReturnsEmptyWhenNoDrafts() {
        assertTrue(store.listDraftFileNames().isEmpty())
    }

    @Test
    fun clearDraftsRemovesAllDraftFiles() {
        val name1 = store.stage { ByteArrayInputStream("draft 1".toByteArray()) }
        val name2 = store.stage { ByteArrayInputStream("draft 2".toByteArray()) }
        val name3 = store.stage { ByteArrayInputStream("draft 3".toByteArray()) }

        assertEquals(3, store.listDraftFileNames().size)
        assertTrue(store.stagedFile(name1).exists())
        assertTrue(store.stagedFile(name2).exists())
        assertTrue(store.stagedFile(name3).exists())

        store.clearDrafts()

        assertTrue(store.listDraftFileNames().isEmpty())
        assertFalse(store.stagedFile(name1).exists())
        assertFalse(store.stagedFile(name2).exists())
        assertFalse(store.stagedFile(name3).exists())
    }

    @Test
    fun clearDraftsDoesNotAffectPersistedPhotos() {
        val persistedName = store.copyIn { ByteArrayInputStream("persisted photo".toByteArray()) }
        val draftName = store.stage { ByteArrayInputStream("draft photo".toByteArray()) }

        assertTrue(store.file(persistedName).exists())
        assertTrue(store.stagedFile(draftName).exists())
        assertEquals(listOf(persistedName), store.listFileNames())
        assertEquals(listOf(draftName), store.listDraftFileNames())

        store.clearDrafts()

        assertFalse(store.stagedFile(draftName).exists())
        assertTrue(store.listDraftFileNames().isEmpty())
        assertTrue(store.file(persistedName).exists())
        assertEquals(listOf(persistedName), store.listFileNames())
    }

    @Test
    fun clearDraftsIsSafeWhenDraftDirectoryDoesNotExist() {
        store.clearDrafts()
        assertTrue(store.listDraftFileNames().isEmpty())
    }

    @Test
    fun listDraftFileNamesIgnoresDirectories() {
        val draftDir = File(temp.root, "drafts")
        draftDir.mkdirs()
        val subDir = File(draftDir, "subfolder")
        subDir.mkdir()

        val draftName = store.stage { ByteArrayInputStream("draft file".toByteArray()) }

        val drafts = store.listDraftFileNames()
        assertEquals(listOf(draftName), drafts)
        assertFalse(drafts.contains("subfolder"))
    }
}
