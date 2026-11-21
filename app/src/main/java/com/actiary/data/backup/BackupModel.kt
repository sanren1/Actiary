package com.actiary.data.backup

import com.actiary.data.db.DayStatus

const val BACKUP_FORMAT = 1

data class BackupData(
    val format: Int,
    val appVersion: String,
    val exportedAt: Long,
    val appearance: Map<String, String>,
    val types: List<BackupType>,
    val records: List<BackupRecord>,
    val entries: List<BackupEntry>,
    val photos: List<BackupPhoto>,
)

data class BackupType(
    val id: Long,
    val builtinKey: String?,
    val name: String?,
    val sortOrder: Int,
    val archived: Boolean,
    val hasDistance: Boolean,
    val iconKey: String?,
)

data class BackupRecord(val epochDay: Long, val status: DayStatus, val note: String, val updatedAt: Long)

data class BackupEntry(
    val id: Long,
    val epochDay: Long,
    val typeId: Long,
    val durationMinutes: Int?,
    val distanceMeters: Int?,
    val calories: Int?,
    val avgHeartRate: Int?,
    val sortOrder: Int,
)

data class BackupPhoto(val epochDay: Long, val fileName: String, val sortOrder: Int, val addedAt: Long)

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)
