package com.actiary.domain

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayDraftTest {
    private val date = LocalDate.of(2024, 2, 10)
    private val empty = DayDraft.from(date, null)

    @Test fun defaults() {
        assertEquals(DayDraft(date, DayStatus.EXERCISED, emptyList(), "", emptyList()), empty)
    }

    @Test fun toggleAddsRemovesAndIgnoredOnRest() {
        val one = empty.toggleType(1).toggleType(2)
        assertEquals(listOf(1L, 2L), one.entries.map { it.typeId })
        assertEquals(listOf(2L), one.toggleType(1).entries.map { it.typeId })
        val rest = empty.markRest()
        assertEquals(rest, rest.toggleType(1))
    }

    @Test fun noteTruncated() {
        assertEquals(200, empty.withNote("x".repeat(250)).note.length)
    }

    @Test fun photosCapped() {
        val d = empty.addPhotos(List(7) { "u$it" }).addPhotos(listOf("a", "b", "c"))
        assertEquals(9, d.photos.size)
        assertEquals(8, d.removePhoto(0).photos.size)
    }

    @Test fun markRestKeepsNoteAndPhotos() {
        val d = empty.toggleType(1).withNote("hi").addPhotos(listOf("u")).markRest()
        assertEquals(DayStatus.REST, d.status)
        assertTrue(d.entries.isEmpty())
        assertEquals("hi", d.note)
        assertEquals(1, d.photos.size)
    }

    @Test fun needsRestConfirmTable() {
        assertFalse(empty.needsRestConfirm)
        assertTrue(empty.toggleType(1).needsRestConfirm)
        assertFalse(empty.toggleType(1).markRest().needsRestConfirm)
        assertFalse(empty.markRest().needsRestConfirm)
    }

    @Test fun newFieldsRoundTripAndToggleDefaults() {
        val epochDay = date.toEpochDay()
        val day = DayWithDetails(
            DailyRecordEntity(epochDay, DayStatus.EXERCISED, "", 1),
            listOf(ExerciseEntryEntity(1, epochDay, 5, 30, 5000, 320, 145, 0)),
            emptyList(),
        )
        val loaded = DayDraft.from(date, day).entries.single()
        assertEquals(320, loaded.calories)
        assertEquals(145, loaded.avgHeartRate)
        val added = empty.toggleType(9).entries.single()
        assertNull(added.calories)
        assertNull(added.avgHeartRate)
    }

    @Test fun sameTypeEntriesCanBeEditedAndRemovedIndependently() {
        val epochDay = date.toEpochDay()
        val day = DayWithDetails(
            DailyRecordEntity(epochDay, DayStatus.EXERCISED, "", 1),
            listOf(
                ExerciseEntryEntity(21, epochDay, 8, 30, null, 320, 145, 0),
                ExerciseEntryEntity(22, epochDay, 8, 45, null, 200, 120, 1),
            ),
            emptyList(),
        )

        val draft = DayDraft.from(date, day)
        val (first, second) = draft.entries
        val editedFirst = first.copy(durationMinutes = 35)
        val edited = draft.withEntry(editedFirst)
        assertEquals(listOf(editedFirst, second), edited.entries)
        assertEquals(draft, draft.toggleType(8))
        assertEquals(listOf(second), edited.removeEntry(first.id).entries)
    }

    @Test fun screenshotUpdatesOnlyRecognizedFieldsWhenOneEntryMatches() {
        val running = ExerciseTypeEntity(id = 4, builtinKey = "running", name = null, sortOrder = 0, hasDistance = true)
        val old = DraftEntry(running.id, durationMinutes = 25, calories = 160, avgHeartRate = 123, id = 3)
        val draft = empty.copy(entries = listOf(old), note = "Keep this")
        val scan = ParsedWorkout(date, "running", 37, 4230, null, null)

        val imported = draft.withRecognizedWorkout(scan, running)

        assertEquals(draft.copy(entries = listOf(old.copy(durationMinutes = 37, distanceMeters = 4230))), imported)
    }

    @Test fun screenshotDoesNotOverwriteEitherEntryWhenTwoSessionsMatch() {
        val freeform = ExerciseTypeEntity(id = 9, builtinKey = "freeform", name = null, sortOrder = 1)
        val recorded = listOf(DraftEntry(freeform.id, 15, id = 3), DraftEntry(freeform.id, 25, id = 4))
        val scan = ParsedWorkout(date, "freeform", 20, null, 132, 124)

        val imported = empty.copy(entries = recorded).withRecognizedWorkout(scan, freeform)

        assertEquals(recorded + DraftEntry(freeform.id, 20, calories = 132, avgHeartRate = 124, id = 5), imported.entries)
    }
}
