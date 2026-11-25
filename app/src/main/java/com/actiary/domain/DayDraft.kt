package com.actiary.domain

import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import java.time.LocalDate

sealed interface DraftPhoto {
    data class Saved(val fileName: String) : DraftPhoto
    data class Picked(val uri: String) : DraftPhoto
    data class Staged(val fileName: String) : DraftPhoto
}

data class DraftStrengthItem(
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val weightKg: Float? = null,
    val localId: Long = 0,
    val sourcePlanId: Long? = null,
)

data class DraftEntry(
    val typeId: Long,
    val durationMinutes: Int? = null,
    val distanceMeters: Int? = null,
    val calories: Int? = null,
    val avgHeartRate: Int? = null,
    val id: Long = 0,
    val metrics: WorkoutMetrics = WorkoutMetrics(),
    val planId: Long? = null,
    val strengthItems: List<DraftStrengthItem> = emptyList(),
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

    fun togglePlanOnEntry(entryId: Long, planId: Long, items: List<DraftStrengthItem>): DayDraft =
        copy(
            entries = entries.map { entry ->
                if (entry.id != entryId) entry
                else {
                    val hasTaggedForPlan = entry.strengthItems.any { it.sourcePlanId == planId }
                    val hasLegacyForPlan = entry.planId == planId && entry.strengthItems.any { it.sourcePlanId == null }
                    if (hasTaggedForPlan || hasLegacyForPlan) {
                        val remaining = if (hasTaggedForPlan) {
                            entry.strengthItems.filterNot { it.sourcePlanId == planId }
                        } else {
                            val planExerciseIds = items.mapTo(HashSet()) { it.exerciseId }
                            entry.strengthItems.filterNot { it.sourcePlanId == null && it.exerciseId in planExerciseIds }
                        }
                        entry.copy(
                            planId = remaining.mapNotNull { it.sourcePlanId }.lastOrNull(),
                            strengthItems = remaining,
                        )
                    } else {
                        val existingExerciseIds = entry.strengthItems.mapTo(HashSet()) { it.exerciseId }
                        val base = entry.strengthItems.maxOfOrNull { it.localId } ?: 0L
                        val appended = items
                            .filter { it.exerciseId !in existingExerciseIds }
                            .mapIndexed { idx, item ->
                                item.copy(localId = base + idx + 1L, sourcePlanId = planId)
                            }
                        entry.copy(
                            planId = planId,
                            strengthItems = entry.strengthItems + appended,
                        )
                    }
                }
            },
        )

    fun clearStrengthItems(entryId: Long): DayDraft =
        copy(
            entries = entries.map { entry ->
                if (entry.id != entryId) entry
                else entry.copy(planId = null, strengthItems = emptyList())
            },
        )

    fun applyPlanToEntry(entryId: Long, planId: Long, items: List<DraftStrengthItem>, replace: Boolean): DayDraft =
        copy(
            entries = entries.map { entry ->
                if (entry.id != entryId) entry
                else if (replace) {
                    entry.copy(
                        planId = planId,
                        strengthItems = items.mapIndexed { idx, item -> item.copy(localId = idx + 1L) },
                    )
                } else {
                    val base = entry.strengthItems.maxOfOrNull { it.localId } ?: 0L
                    entry.copy(
                        planId = entry.planId ?: planId,
                        strengthItems = entry.strengthItems + items.mapIndexed { idx, item -> item.copy(localId = base + idx + 1L) },
                    )
                }
            },
        )

    fun addStrengthItems(entryId: Long, items: List<DraftStrengthItem>): DayDraft {
        if (items.isEmpty()) return this
        return copy(
            entries = entries.map { entry ->
                if (entry.id != entryId) entry
                else {
                    val base = entry.strengthItems.maxOfOrNull { it.localId } ?: 0L
                    entry.copy(
                        strengthItems = entry.strengthItems + items.mapIndexed { idx, item -> item.copy(localId = base + idx + 1L) },
                    )
                }
            },
        )
    }

    fun updateStrengthItem(entryId: Long, item: DraftStrengthItem): DayDraft =
        copy(
            entries = entries.map { entry ->
                if (entry.id != entryId) entry
                else entry.copy(
                    strengthItems = entry.strengthItems.map { if (it.localId == item.localId) item else it },
                )
            },
        )

    fun removeStrengthItem(entryId: Long, localId: Long): DayDraft =
        copy(
            entries = entries.map { entry ->
                if (entry.id != entryId) entry
                else entry.copy(strengthItems = entry.strengthItems.filterNot { it.localId == localId })
            },
        )

    fun removeEntry(id: Long): DayDraft = copy(entries = entries.filterNot { it.id == id })

    fun withNote(text: String): DayDraft = copy(note = text.take(NOTE_LIMIT))

    fun addPhotos(uris: List<String>): DayDraft =
        copy(photos = (photos + uris.map { DraftPhoto.Picked(it) }).take(PHOTO_LIMIT))

    fun removePhoto(index: Int): DayDraft = copy(photos = photos.filterIndexed { i, _ -> i != index })

    fun movePhoto(from: Int, to: Int): DayDraft {
        if (from == to) return this
        return copy(photos = photos.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun markRest(): DayDraft = copy(status = DayStatus.REST, entries = emptyList())

    fun markExercised(): DayDraft = copy(status = DayStatus.EXERCISED)

    val needsRestConfirm: Boolean get() = status == DayStatus.EXERCISED && entries.isNotEmpty()

    private fun normalizedForDiff(): DayDraft = copy(
        entries = entries.mapIndexed { entryIndex, entry ->
            entry.copy(
                id = entryIndex.toLong(),
                strengthItems = entry.strengthItems.mapIndexed { itemIndex, item ->
                    item.copy(localId = itemIndex.toLong(), sourcePlanId = null)
                },
            )
        },
    )

    fun isChangedFrom(original: DayDraft): Boolean =
        normalizedForDiff() != original.normalizedForDiff()

    companion object {
        const val NOTE_LIMIT = 200
        const val PHOTO_LIMIT = 9

        fun from(date: LocalDate, day: DayWithDetails?): DayDraft =
            if (day == null) DayDraft(date, DayStatus.EXERCISED, emptyList(), "", emptyList())
            else DayDraft(
                date = date,
                status = day.record.status,
                entries = day.entriesWithLogs.sortedBy { it.entry.sortOrder }.map { e ->
                    DraftEntry(
                        typeId = e.entry.typeId,
                        durationMinutes = e.entry.durationMinutes,
                        distanceMeters = e.entry.distanceMeters,
                        calories = e.entry.calories,
                        avgHeartRate = e.entry.avgHeartRate,
                        id = e.entry.id,
                        metrics = e.entry.metrics,
                        planId = e.entry.planId,
                        strengthItems = e.strengthLogs.sortedBy { it.sortOrder }.mapIndexed { idx, log ->
                            DraftStrengthItem(
                                exerciseId = log.exerciseId,
                                sets = log.sets,
                                reps = log.reps,
                                weightKg = log.weightKg,
                                localId = idx + 1L,
                            )
                        },
                    )
                },
                note = day.record.note,
                photos = day.photos.sortedBy { it.sortOrder }.map { DraftPhoto.Saved(it.fileName) },
            )
    }
}
