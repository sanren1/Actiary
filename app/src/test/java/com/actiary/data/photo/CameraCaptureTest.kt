package com.actiary.data.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CameraCaptureTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun createTempFilePlacesJpgInCaptureDir() {
        val cacheDir = temp.root
        val file = CameraCapture.createTempFile(cacheDir)

        val parent = requireNotNull(file.parentFile)
        assertEquals("capture", parent.name)
        assertEquals(cacheDir, parent.parentFile)
        assertTrue(file.name.endsWith(".jpg"))
        assertTrue(parent.exists())
        assertTrue(parent.isDirectory)
    }

    @Test fun distinctTempFilesHaveDistinctNames() {
        val cacheDir = temp.root
        val a = CameraCapture.createTempFile(cacheDir)
        val b = CameraCapture.createTempFile(cacheDir)
        assertTrue(a.name != b.name)
    }

    @Test fun cleanupDeletesFile() {
        val cacheDir = temp.root
        val file = CameraCapture.createTempFile(cacheDir)
        // createTempFile only reserves a path; write something so the file exists on disk.
        file.writeText("x")
        assertTrue(file.exists())
        CameraCapture.cleanup(file)
        assertFalse(file.exists())
    }
}
