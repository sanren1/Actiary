package com.actiary.data

import com.actiary.data.photo.PhotoStore
import java.io.File

internal data class RecordStorageUsage(val databaseBytes: Long, val photoBytes: Long) {
    val totalBytes: Long get() = databaseBytes + photoBytes
}

internal fun measureRecordStorage(database: File, photos: PhotoStore): RecordStorageUsage {
    val databaseBytes = listOf("", "-wal", "-shm", "-journal").sumOf { suffix ->
        File(database.path + suffix).length()
    }
    val photoBytes = photos.listFileNames().sumOf { photos.file(it).length() }
    return RecordStorageUsage(databaseBytes, photoBytes)
}
