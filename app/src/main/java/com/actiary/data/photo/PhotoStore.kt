package com.actiary.data.photo

import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.InputStream
import java.util.UUID

class PhotoStore(
    private val dir: File,
    private val compressor: ((source: File, target: File) -> Boolean)? = null,
) {
    // Files created by save or restore are unreferenced until their Room transaction commits.
    internal val mutations = Mutex()

    private val draftDir = File(dir, "drafts")

    fun copyIn(open: () -> InputStream): String = copyTo(dir, open, compress = true)

    fun copyRaw(open: () -> InputStream): String = copyTo(dir, open, compress = false)

    internal fun stage(open: () -> InputStream): String = copyTo(draftDir, open, compress = true)

    fun stagedFile(fileName: String): File = File(draftDir, fileName)

    fun clearDrafts() {
        draftDir.listFiles()?.forEach { it.delete() }
    }

    fun listDraftFileNames(): List<String> = draftDir.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()

    private fun copyTo(directory: File, open: () -> InputStream, compress: Boolean): String {
        val name = "${UUID.randomUUID()}.jpg"
        val target = File(directory, name)
        val activeCompressor = if (compress) compressor else null
        var tempSource: File? = null
        try {
            directory.mkdirs()
            if (activeCompressor == null) {
                open().use { input -> target.outputStream().use { input.copyTo(it) } }
            } else {
                val raw = File(directory, "$name.tmp")
                tempSource = raw
                open().use { input -> raw.outputStream().use { input.copyTo(it) } }
                val compressed = activeCompressor(raw, target)
                if (!compressed || !target.exists() || target.length() == 0L) {
                    target.delete()
                    if (!raw.renameTo(target)) {
                        raw.copyTo(target, overwrite = true)
                    }
                }
            }
        } catch (e: Throwable) {
            target.delete()
            throw e
        } finally {
            tempSource?.delete()
        }
        return name
    }

    fun file(fileName: String): File = File(dir, fileName)

    fun listFileNames(): List<String> = dir.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()

    fun delete(fileName: String): Boolean = file(fileName).delete()
}
