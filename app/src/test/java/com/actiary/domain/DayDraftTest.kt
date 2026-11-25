package com.actiary.domain

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.EntryWithStrengthLogs
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.StrengthLogItemEntity
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

    @Test fun movingPhotosPreservesMixedReferencesAndMarksTheDraftDirty() {
        val first = DraftPhoto.Saved("first.jpg")
        val second = DraftPhoto.Staged("second.jpg")
        val third = DraftPhoto.Picked("content://third")
        val original = empty.copy(photos = listOf(first, second, third))

        val moved = original.movePhoto(0, 2)
        assertEquals(listOf(second, third, first), moved.photos)
        assertTrue(moved.isChangedFrom(original))
        assertEquals(original, moved.movePhoto(2, 0))
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

    @Test fun applyPlanToEntryReplacesAndAppendsItems() {
        val base = empty.toggleType(9)
        val entryId = base.entries.single().id
        val pushItems = listOf(
            DraftStrengthItem(exerciseId = 10, sets = 4, reps = 10, weightKg = 60f),
            DraftStrengthItem(exerciseId = 11, sets = 3, reps = 12, weightKg = null),
        )
        val applied = base.applyPlanToEntry(entryId, planId = 1L, items = pushItems, replace = true)
        val appliedEntry = applied.entries.single()
        assertEquals(1L, appliedEntry.planId)
        assertEquals(
            listOf(
                DraftStrengthItem(10, 4, 10, 60f, localId = 1L),
                DraftStrengthItem(11, 3, 12, null, localId = 2L),
            ),
            appliedEntry.strengthItems,
        )

        val extraItems = listOf(DraftStrengthItem(exerciseId = 12, sets = 3, reps = 15, weightKg = 22.5f))
        val appended = applied.applyPlanToEntry(entryId, planId = 2L, items = extraItems, replace = false)
        val appendedEntry = appended.entries.single()
        assertEquals(1L, appendedEntry.planId)
        assertEquals(
            listOf(
                DraftStrengthItem(10, 4, 10, 60f, localId = 1L),
                DraftStrengthItem(11, 3, 12, null, localId = 2L),
                DraftStrengthItem(12, 3, 15, 22.5f, localId = 3L),
            ),
            appendedEntry.strengthItems,
        )

        val appendedFromNoPlan = base.applyPlanToEntry(entryId, planId = 2L, items = extraItems, replace = false)
        assertEquals(2L, appendedFromNoPlan.entries.single().planId)

        val replaced = appended.applyPlanToEntry(entryId, planId = 3L, items = extraItems, replace = true)
        val replacedEntry = replaced.entries.single()
        assertEquals(3L, replacedEntry.planId)
        assertEquals(listOf(DraftStrengthItem(12, 3, 15, 22.5f, localId = 1L)), replacedEntry.strengthItems)
    }

    @Test fun addUpdateAndRemoveStrengthItems() {
        val base = empty.toggleType(9)
        val entryId = base.entries.single().id
        val added = base.addStrengthItems(
            entryId,
            listOf(
                DraftStrengthItem(exerciseId = 1, sets = 4, reps = 10, weightKg = 50f),
                DraftStrengthItem(exerciseId = 2, sets = 3, reps = 12, weightKg = null),
            ),
        )
        assertEquals(listOf(1L, 2L), added.entries.single().strengthItems.map { it.localId })

        val updated = added.updateStrengthItem(
            entryId,
            DraftStrengthItem(exerciseId = 1, sets = 5, reps = 8, weightKg = 62.5f, localId = 1L),
        )
        assertEquals(5, updated.entries.single().strengthItems[0].sets)
        assertEquals(8, updated.entries.single().strengthItems[0].reps)
        assertEquals(62.5f, updated.entries.single().strengthItems[0].weightKg)

        val removed = updated.removeStrengthItem(entryId, localId = 1L)
        assertEquals(
            listOf(DraftStrengthItem(exerciseId = 2, sets = 3, reps = 12, weightKg = null, localId = 2L)),
            removed.entries.single().strengthItems,
        )

        val addedMore = removed.addStrengthItems(entryId, listOf(DraftStrengthItem(exerciseId = 3, sets = 4, reps = 10)))
        assertEquals(listOf(2L, 3L), addedMore.entries.single().strengthItems.map { it.localId })
    }

    @Test fun fromLoadsPlanIdAndSortedStrengthLogs() {
        val epochDay = date.toEpochDay()
        val entry = ExerciseEntryEntity(id = 7, epochDay = epochDay, typeId = 9, durationMinutes = 50, distanceMeters = null, calories = 300, avgHeartRate = 130, sortOrder = 0, planId = 2L)
        val logs = listOf(
            StrengthLogItemEntity(id = 101, entryId = 7, exerciseId = 20, sets = 3, reps = 12, weightKg = null, sortOrder = 1),
            StrengthLogItemEntity(id = 100, entryId = 7, exerciseId = 10, sets = 4, reps = 10, weightKg = 60f, sortOrder = 0),
        )
        val day = DayWithDetails(
            record = DailyRecordEntity(epochDay, DayStatus.EXERCISED, "push", 1),
            entriesWithLogs = listOf(EntryWithStrengthLogs(entry, logs)),
            photos = emptyList(),
        )

        val loaded = DayDraft.from(date, day).entries.single()
        assertEquals(2L, loaded.planId)
        assertEquals(
            listOf(
                DraftStrengthItem(exerciseId = 10, sets = 4, reps = 10, weightKg = 60f, localId = 1L),
                DraftStrengthItem(exerciseId = 20, sets = 3, reps = 12, weightKg = null, localId = 2L),
            ),
            loaded.strengthItems,
        )
    }

    @Test fun isChangedFromIgnoresLocalIdButDetectsStrengthAndPlanChanges() {
        val original = DayDraft(
            date = date,
            status = DayStatus.EXERCISED,
            entries = listOf(
                DraftEntry(
                    typeId = 9,
                    id = 5,
                    planId = 1L,
                    strengthItems = listOf(
                        DraftStrengthItem(exerciseId = 10, sets = 4, reps = 10, weightKg = 60f, localId = 1L),
                    ),
                ),
            ),
            note = "",
            photos = emptyList(),
        )
        val sameWithDifferentIds = original.copy(
            entries = listOf(
                original.entries.single().copy(
                    id = 99,
                    strengthItems = listOf(
                        DraftStrengthItem(exerciseId = 10, sets = 4, reps = 10, weightKg = 60f, localId = 42L),
                    ),
                ),
            ),
        )
        assertFalse(sameWithDifferentIds.isChangedFrom(original))

        assertTrue(original.copy(entries = listOf(original.entries.single().copy(planId = 2L))).isChangedFrom(original))
        assertTrue(original.copy(entries = listOf(original.entries.single().copy(planId = null))).isChangedFrom(original))
        assertTrue(original.updateStrengthItem(5, DraftStrengthItem(11, 4, 10, 60f, localId = 1L)).isChangedFrom(original))
        assertTrue(original.updateStrengthItem(5, DraftStrengthItem(10, 5, 10, 60f, localId = 1L)).isChangedFrom(original))
        assertTrue(original.updateStrengthItem(5, DraftStrengthItem(10, 4, 12, 60f, localId = 1L)).isChangedFrom(original))
        assertTrue(original.updateStrengthItem(5, DraftStrengthItem(10, 4, 10, 62.5f, localId = 1L)).isChangedFrom(original))
        assertTrue(original.updateStrengthItem(5, DraftStrengthItem(10, 4, 10, null, localId = 1L)).isChangedFrom(original))

        val withSourcePlanId = original.copy(
            entries = listOf(
                original.entries.single().copy(
                    strengthItems = listOf(
                        DraftStrengthItem(exerciseId = 10, sets = 4, reps = 10, weightKg = 60f, localId = 1L, sourcePlanId = 1L),
                    ),
                ),
            ),
        )
        assertFalse(withSourcePlanId.isChangedFrom(original))
    }

    @Test fun togglePlanOnEntryMultiSelectsDedupsDeselectsAndClears() {
        val base = empty.toggleType(9)
        val entryId = base.entries.single().id
        val plan1Items = listOf(
            DraftStrengthItem(exerciseId = 10, sets = 4, reps = 10, weightKg = 60f),
            DraftStrengthItem(exerciseId = 11, sets = 3, reps = 12),
        )
        val plan2Items = listOf(
            DraftStrengthItem(exerciseId = 11, sets = 4, reps = 8), // overlaps with plan1
            DraftStrengthItem(exerciseId = 12, sets = 3, reps = 15),
        )

        // Select plan 1
        val afterPlan1 = base.togglePlanOnEntry(entryId, planId = 1L, items = plan1Items)
        val entry1 = afterPlan1.entries.single()
        assertEquals(1L, entry1.planId)
        assertEquals(
            listOf(
                DraftStrengthItem(10, 4, 10, 60f, localId = 1L, sourcePlanId = 1L),
                DraftStrengthItem(11, 3, 12, null, localId = 2L, sourcePlanId = 1L),
            ),
            entry1.strengthItems,
        )

        // Multi-select plan 2: skips overlapping exerciseId=11, tags exerciseId=12 with sourcePlanId=2L
        val afterPlan2 = afterPlan1.togglePlanOnEntry(entryId, planId = 2L, items = plan2Items)
        val entry2 = afterPlan2.entries.single()
        assertEquals(2L, entry2.planId)
        assertEquals(
            listOf(
                DraftStrengthItem(10, 4, 10, 60f, localId = 1L, sourcePlanId = 1L),
                DraftStrengthItem(11, 3, 12, null, localId = 2L, sourcePlanId = 1L),
                DraftStrengthItem(12, 3, 15, null, localId = 3L, sourcePlanId = 2L),
            ),
            entry2.strengthItems,
        )

        // Toggle off plan 2: removes only plan 2 items, restores planId to last remaining sourcePlanId (1L)
        val deselectedPlan2 = afterPlan2.togglePlanOnEntry(entryId, planId = 2L, items = plan2Items)
        val entryDeselected2 = deselectedPlan2.entries.single()
        assertEquals(1L, entryDeselected2.planId)
        assertEquals(
            listOf(
                DraftStrengthItem(10, 4, 10, 60f, localId = 1L, sourcePlanId = 1L),
                DraftStrengthItem(11, 3, 12, null, localId = 2L, sourcePlanId = 1L),
            ),
            entryDeselected2.strengthItems,
        )

        // Toggle off plan 1: clears remaining items and planId becomes null
        val deselectedPlan1 = deselectedPlan2.togglePlanOnEntry(entryId, planId = 1L, items = plan1Items)
        val entryDeselected1 = deselectedPlan1.entries.single()
        assertNull(entryDeselected1.planId)
        assertTrue(entryDeselected1.strengthItems.isEmpty())

        // Clear strength items removes all items and clears planId
        val cleared = afterPlan2.clearStrengthItems(entryId)
        assertNull(cleared.entries.single().planId)
        assertTrue(cleared.entries.single().strengthItems.isEmpty())
    }

    @Test fun togglePlanOnEntryDeselectsLegacySavedPlanItems() {
        val legacyDraft = DayDraft(
            date = date,
            status = DayStatus.EXERCISED,
            entries = listOf(
                DraftEntry(
                    typeId = 9,
                    id = 5,
                    planId = 1L,
                    strengthItems = listOf(
                        DraftStrengthItem(exerciseId = 10, sets = 4, reps = 10, localId = 1L, sourcePlanId = null),
                        DraftStrengthItem(exerciseId = 11, sets = 3, reps = 12, localId = 2L, sourcePlanId = null),
                        DraftStrengthItem(exerciseId = 99, sets = 3, reps = 15, localId = 3L, sourcePlanId = null),
                    ),
                ),
            ),
            note = "",
            photos = emptyList(),
        )
        val plan1Items = listOf(
            DraftStrengthItem(exerciseId = 10, sets = 4, reps = 10),
            DraftStrengthItem(exerciseId = 11, sets = 3, reps = 12),
        )

        val toggledOff = legacyDraft.togglePlanOnEntry(5L, planId = 1L, items = plan1Items)
        val updatedEntry = toggledOff.entries.single()
        assertNull(updatedEntry.planId)
        assertEquals(
            listOf(DraftStrengthItem(exerciseId = 99, sets = 3, reps = 15, localId = 3L, sourcePlanId = null)),
            updatedEntry.strengthItems,
        )
    }

    @Test fun moveStrengthItemReordersHandlesBoundsAndMarksDirty() {
        val original = DayDraft(
            date = date,
            status = DayStatus.EXERCISED,
            entries = listOf(
                DraftEntry(
                    typeId = 9,
                    id = 5,
                    strengthItems = listOf(
                        DraftStrengthItem(exerciseId = 10, sets = 4, reps = 10, localId = 1L),
                        DraftStrengthItem(exerciseId = 20, sets = 3, reps = 12, localId = 2L),
                        DraftStrengthItem(exerciseId = 30, sets = 5, reps = 8, localId = 3L),
                    ),
                ),
            ),
            note = "",
            photos = emptyList(),
        )

        val moved = original.moveStrengthItem(entryId = 5L, fromIndex = 0, toIndex = 2)
        assertEquals(listOf(20L, 30L, 10L), moved.entries.single().strengthItems.map { it.exerciseId })
        assertTrue(moved.isChangedFrom(original))
        assertFalse(moved.moveStrengthItem(entryId = 5L, fromIndex = 2, toIndex = 0).isChangedFrom(original))

        // Same-index and out-of-bounds moves are no-ops
        assertEquals(original, original.moveStrengthItem(entryId = 5L, fromIndex = 1, toIndex = 1))
        assertEquals(original, original.moveStrengthItem(entryId = 5L, fromIndex = -1, toIndex = 1))
        assertEquals(original, original.moveStrengthItem(entryId = 5L, fromIndex = 0, toIndex = 3))
        assertEquals(original, original.moveStrengthItem(entryId = 999L, fromIndex = 0, toIndex = 1))
    }
}
