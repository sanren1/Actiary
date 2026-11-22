package com.actiary.ui.record

import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.ui.records.entryMetrics

internal data class DetailEntry(val typeId: Long, val metrics: List<Pair<Int, String>>)

internal data class DetailModel(
    val status: DayStatus,
    val entries: List<DetailEntry>,
    val note: String,
    val photos: List<String>,
    val empty: Boolean,
)

internal fun buildDetailModel(
    day: DayWithDetails,
    types: Map<Long, ExerciseTypeEntity>,
    format: (Int, Any) -> String,
): DetailModel = DetailModel(
    status = day.record.status,
    entries = day.entries.sortedBy { it.sortOrder }.mapNotNull { entry ->
        types[entry.typeId]?.let { DetailEntry(entry.typeId, entryMetrics(entry, it, includeOptional = true, format = format)) }
    },
    note = day.record.note,
    photos = day.photos.sortedBy { it.sortOrder }.map { it.fileName },
    empty = day.record.status == DayStatus.EXERCISED &&
        day.entries.isEmpty() && day.record.note.isBlank() && day.photos.isEmpty(),
)
