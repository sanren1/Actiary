package com.actiary.data.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

class PhotoCompressorTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun targetPhotoSizePreservesSmallImagesAndDownscalesLargeEdges() {
        assertEquals(1200 to 800, targetPhotoSize(1200, 800, 1920))
        assertEquals(1920 to 1080, targetPhotoSize(3840, 2160, 1920))
        assertEquals(1440 to 1920, targetPhotoSize(3000, 4000, 1920))
        assertEquals(1920 to 1920, targetPhotoSize(2500, 2500, 1920))
    }

    @Test
    fun photoStoreUsesCompressorOnCopyInAndSkipsOnCopyRaw() {
        var compressCalls = 0
        val store = PhotoStore(temp.root) { src, dst ->
            compressCalls++
            dst.writeBytes(src.readBytes().take(4).toByteArray())
            true
        }

        val compressedName = store.copyIn { ByteArrayInputStream("1234567890".toByteArray()) }
        assertEquals(1, compressCalls)
        assertEquals("1234", store.file(compressedName).readText())

        val rawName = store.copyRaw { ByteArrayInputStream("1234567890".toByteArray()) }
        assertEquals(1, compressCalls)
        assertEquals("1234567890", store.file(rawName).readText())
    }

    @Test
    fun photoStoreFallsBackToRawCopyWhenCompressorReturnsFalse() {
        val store = PhotoStore(temp.root) { _, _ -> false }
        val name = store.copyIn { ByteArrayInputStream("original-bytes".toByteArray()) }
        assertTrue(store.file(name).exists())
        assertEquals("original-bytes", store.file(name).readText())
    }
}
