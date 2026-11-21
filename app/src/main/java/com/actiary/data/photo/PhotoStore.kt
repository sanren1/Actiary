package com.actiary.data.photo

import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.InputStream
import java.util.UUID

class PhotoStore(private val dir: File) {
    // Files created by save or restore are unreferenced until their Room transaction commits.
    internal val mutations = Mutex()

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

    fun listFileNames(): List<String> = dir.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()

    fun delete(fileName: String): Boolean = file(fileName).delete()
}
