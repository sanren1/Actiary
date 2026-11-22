package com.actiary.data

import com.actiary.data.photo.PhotoStore
import java.io.File

internal data class RecordStorageUsage(
    val databaseBytes: Long,
    val photoBytes: Long,
    val photoCount: Int = 0,
    val largestPhotoBytes: Long = 0L,
    val cacheBytes: Long = 0L,
) {
    val totalBytes: Long get() = databaseBytes + photoBytes + cacheBytes
    val averagePhotoBytes: Long get() = if (photoCount > 0) photoBytes / photoCount else 0L
}

internal fun measureRecordStorage(
    database: File,
    photos: PhotoStore,
    cacheDir: File? = null,
): RecordStorageUsage {
    val databaseBytes = listOf("", "-wal", "-shm", "-journal").sumOf { suffix ->
        File(database.path + suffix).length()
    }
    val photoFiles = photos.listFileNames().map { photos.file(it) }
    val photoBytes = photoFiles.sumOf { it.length() }
    val largestPhotoBytes = photoFiles.maxOfOrNull { it.length() } ?: 0L
    val cacheBytes = cacheDir?.let(::directorySizeBytes) ?: 0L
    return RecordStorageUsage(
        databaseBytes = databaseBytes,
        photoBytes = photoBytes,
        photoCount = photoFiles.size,
        largestPhotoBytes = largestPhotoBytes,
        cacheBytes = cacheBytes,
    )
}

internal fun directorySizeBytes(dir: File): Long {
    if (!dir.exists()) return 0L
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

internal fun clearDirectoryContents(dir: File) {
    dir.listFiles()?.forEach { it.deleteRecursively() }
}
