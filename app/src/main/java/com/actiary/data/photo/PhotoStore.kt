package com.actiary.data.photo

import java.io.File
import java.io.InputStream
import java.util.UUID

class PhotoStore(private val dir: File) {
    fun copyIn(open: () -> InputStream): String {
        val name = "${UUID.randomUUID()}.jpg"
        val target = file(name)
        try {
            dir.mkdirs()
            open().use { input -> target.outputStream().use { input.copyTo(it) } }
        } catch (e: Throwable) {
            target.delete()
            throw e
        }
        return name
    }

    fun file(fileName: String): File = File(dir, fileName)

    fun delete(fileName: String) {
        file(fileName).delete()
    }
}
