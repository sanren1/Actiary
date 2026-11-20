package com.actiary.domain

import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import java.time.LocalDate

sealed interface DraftPhoto {
    data class Saved(val fileName: String) : DraftPhoto
    data class Picked(val uri: String) : DraftPhoto
}

data class DraftEntry(
    val typeId: Long,
    val durationMinutes: Int? = null,
    val distanceMeters: Int? = null,
    val calories: Int? = null,
    val avgHeartRate: Int? = null,
    val id: Long = 0,
)

data class DayDraft(
    val date: LocalDate,
    val status: DayStatus,
    val entries: List<DraftEntry>,
    val note: String,
    val photos: List<DraftPhoto>,
) {
    fun toggleType(typeId: Long): DayDraft {
        if (status == DayStatus.REST) return this
        val matching = entries.filter { it.typeId == typeId }
        return when (matching.size) {
            0 -> copy(entries = entries + DraftEntry(typeId, id = (entries.maxOfOrNull { it.id } ?: 0L) + 1))
            1 -> removeEntry(matching.single().id)
            else -> this // Multiple sessions of this type must be removed individually.
        }
    }

    fun withEntry(entry: DraftEntry): DayDraft = copy(entries = entries.map { if (it.id == entry.id) entry else it })

    fun removeEntry(id: Long): DayDraft = copy(entries = entries.filterNot { it.id == id })

    fun withNote(text: String): DayDraft = copy(note = text.take(NOTE_LIMIT))

    fun addPhotos(uris: List<String>): DayDraft =
        copy(photos = (photos + uris.map { DraftPhoto.Picked(it) }).take(PHOTO_LIMIT))

    fun removePhoto(index: Int): DayDraft = copy(photos = photos.filterIndexed { i, _ -> i != index })

    fun markRest(): DayDraft = copy(status = DayStatus.REST, entries = emptyList())

    fun markExercised(): DayDraft = copy(status = DayStatus.EXERCISED)

    val needsRestConfirm: Boolean get() = status == DayStatus.EXERCISED && entries.isNotEmpty()

    fun isChangedFrom(original: DayDraft): Boolean =
        copy(entries = entries.mapIndexed { i, entry -> entry.copy(id = original.entries.getOrNull(i)?.id ?: entry.id) }) != original

    companion object {
        const val NOTE_LIMIT = 200
        const val PHOTO_LIMIT = 9

        fun from(date: LocalDate, day: DayWithDetails?): DayDraft =
            if (day == null) DayDraft(date, DayStatus.EXERCISED, emptyList(), "", emptyList())
            else DayDraft(
                date = date,
                status = day.record.status,
                entries = day.entries.sortedBy { it.sortOrder }
                    .map { DraftEntry(it.typeId, it.durationMinutes, it.distanceMeters, it.calories, it.avgHeartRate, it.id) },
                note = day.record.note,
                photos = day.photos.sortedBy { it.sortOrder }.map { DraftPhoto.Saved(it.fileName) },
            )
    }
}
