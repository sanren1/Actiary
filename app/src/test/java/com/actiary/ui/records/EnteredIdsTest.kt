package com.actiary.ui.records

import org.junit.Assert.assertEquals
import org.junit.Test

class EnteredIdsTest {
    @Test fun firstShowAnimatesAll() {
        assertEquals(setOf(3L, 2L) to listOf(3L, 2L), nextEntered(emptySet(), listOf(3L, 2L)))
    }

    @Test fun secondShowAnimatesNone() {
        assertEquals(setOf(3L, 2L) to emptyList<Long>(), nextEntered(setOf(3L, 2L), listOf(3L, 2L)))
    }

    @Test fun newItemAnimatesOnlyNew() {
        assertEquals(setOf(3L, 2L, 1L) to listOf(1L), nextEntered(setOf(3L, 2L), listOf(2L, 1L)))
    }

    @Test fun filteredOutThenBackDoesNotReplay() {
        val (seen, _) = nextEntered(emptySet(), listOf(3L, 2L))
        val (filtered, _) = nextEntered(seen, listOf(2L))
        assertEquals(seen to emptyList<Long>(), nextEntered(filtered, listOf(3L, 2L)))
    }
}
