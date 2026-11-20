package com.actiary.data.photo

import java.io.File
import java.util.UUID

object CameraCapture {
    const val AUTHORITY = "com.actiary.fileprovider"

    fun createTempFile(cacheDir: File): File {
        val dir = File(cacheDir, "capture")
        dir.mkdirs()
        return File.createTempFile("capture_${UUID.randomUUID()}_", ".jpg", dir)
    }

    fun cleanup(file: File) {
        file.delete()
    }
}
