package com.actiary.domain

import com.actiary.data.db.DayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
