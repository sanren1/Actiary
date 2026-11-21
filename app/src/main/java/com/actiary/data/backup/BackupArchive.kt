package com.actiary.data.backup

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CharacterCodingException
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class LoadedBackup internal constructor(
    internal val backup: BackupData,
    private val directory: File,
    private val photos: Map<String, File>,
) : Closeable {
    internal fun photo(name: String): File? = photos[name]

    override fun close() {
        directory.deleteRecursively()
    }
}

private const val METADATA_NAME = "backup.json"
private const val PHOTO_PREFIX = "photos/"
private const val MAX_JSON_BYTES = 8L * 1024 * 1024
private const val MAX_PHOTO_BYTES = 128L * 1024 * 1024
private const val MAX_PAYLOAD_BYTES = 2L * 1024 * 1024 * 1024
private const val MAX_ARCHIVE_BYTES = MAX_PAYLOAD_BYTES + 32L * 1024 * 1024
private const val MAX_MEMBERS = 32_768
private const val MAX_DIRECTORY_BYTES = 16L * 1024 * 1024
private const val ZIP_END_SIGNATURE = 0x06054b50
private const val ZIP_END_BYTES = 22
private const val ZIP_MAX_COMMENT_BYTES = 65_535

internal fun writeBackup(out: OutputStream, backup: BackupData, photos: Map<String, File>) {
    backup.validateLocalRows()
    val json = backup.toJson().toByteArray(Charsets.UTF_8)
    if (json.size > MAX_JSON_BYTES || photos.size + 1 > MAX_MEMBERS ||
        photos.values.any { it.length() > MAX_PHOTO_BYTES } ||
        json.size + photos.values.sumOf { it.length() } > MAX_PAYLOAD_BYTES
    ) {
        throw BackupFormatException("Backup exceeds size limits")
    }
    ZipOutputStream(out).use { zip ->
        zip.putNextEntry(ZipEntry(METADATA_NAME))
        zip.write(json)
        zip.closeEntry()
        for ((name, file) in photos) {
            zip.putNextEntry(ZipEntry(PHOTO_PREFIX + name))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }
}

internal fun unpackBackup(input: InputStream, cacheDir: File): LoadedBackup {
    val directory = Files.createTempDirectory(cacheDir.toPath(), "actiary-import-").toFile()
    try {
        val archive = File(directory, "source.zip")
        archive.outputStream().use { input.copyBounded(it, MAX_ARCHIVE_BYTES) }
        val memberCount = zipDirectoryCount(archive)
        val loaded = ZipFile(archive).use { zip ->
            if (zip.size() != memberCount) throw BackupFormatException("Invalid ZIP member count")
            val members = zip.entries().toList().associateBy { it.name }
            if (members.size != memberCount) throw BackupFormatException("Duplicate ZIP members")
            val metadata = members[METADATA_NAME] ?: throw BackupFormatException("Missing backup.json")
            if (metadata.size !in 0..MAX_JSON_BYTES) throw BackupFormatException("Backup metadata is too large")
            val metadataFile = File(directory, METADATA_NAME)
            metadataFile.outputStream().use { zip.copyMember(metadata, it) }
            val json = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(metadataFile.readBytes())).toString()
            val backup = parseBackup(json)
            metadataFile.delete()

            val photos = mutableMapOf<String, File>()
            var total = metadata.size
            for ((path, member) in members) {
                if (path == METADATA_NAME) continue
                val name = path.removePrefix(PHOTO_PREFIX)
                if (name == path || !isPhotoFileName(name)) {
                    throw BackupFormatException("Invalid ZIP member name")
                }
                if (member.size !in 0..MAX_PHOTO_BYTES || total + member.size > MAX_PAYLOAD_BYTES) {
                    throw BackupFormatException("Backup exceeds size limits")
                }
                total += member.size
                // Staged paths are created by the app, independently of names in the ZIP.
                val photo = File(directory, "photo-${photos.size}")
                photo.outputStream().use { zip.copyMember(member, it) }
                photos[name] = photo
            }
            LoadedBackup(backup, directory, photos)
        }
        archive.delete()
        return loaded
    } catch (failure: Throwable) {
        directory.deleteRecursively()
        when (failure) {
            is ZipException, is CharacterCodingException -> throw BackupFormatException("Invalid backup archive", failure)
            else -> throw failure
        }
    }
}

private fun zipDirectoryCount(archive: File): Int = RandomAccessFile(archive, "r").use { file ->
    // Bound the central directory before ZipFile reads it into memory. Our writer uses a single-disk ZIP.
    val tailSize = minOf(file.length(), (ZIP_END_BYTES + ZIP_MAX_COMMENT_BYTES).toLong()).toInt()
    val tailStart = file.length() - tailSize
    val tail = ByteBuffer.allocate(tailSize).order(ByteOrder.LITTLE_ENDIAN)
    file.seek(tailStart)
    file.readFully(tail.array())
    for (index in (tailSize - ZIP_END_BYTES) downTo 0) {
        if (tail.getInt(index) != ZIP_END_SIGNATURE) continue
        val commentSize = tail.getShort(index + 20).toInt() and 0xffff
        if (index + ZIP_END_BYTES + commentSize != tailSize) continue
        val countOnDisk = tail.getShort(index + 8).toInt() and 0xffff
        val count = tail.getShort(index + 10).toInt() and 0xffff
        val size = tail.getInt(index + 12).toLong() and 0xffff_ffffL
        val start = tail.getInt(index + 16).toLong() and 0xffff_ffffL
        if (tail.getInt(index + 4) != 0 || countOnDisk != count || count !in 1..MAX_MEMBERS ||
            size !in 1..MAX_DIRECTORY_BYTES || start + size != tailStart + index
        ) break
        return@use count
    }
    throw BackupFormatException("Invalid or oversized ZIP directory")
}

private fun ZipFile.copyMember(member: ZipEntry, out: OutputStream) {
    CheckedInputStream(getInputStream(member), CRC32()).use { input ->
        if (input.copyBounded(out, member.size) != member.size || input.checksum.value != member.crc) {
            throw BackupFormatException("Corrupt ZIP member")
        }
    }
}

private fun InputStream.copyBounded(out: OutputStream, limit: Long): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) return total
        total += count
        if (total > limit) throw BackupFormatException("Backup exceeds size limits")
        out.write(buffer, 0, count)
    }
}
