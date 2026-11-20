package com.actiary.ui.records

import org.junit.Assert.assertEquals
import org.junit.Test

class MaxValueIndexTest {
    @Test
    fun emptyAndAllNullReturnMinusOne() {
        assertEquals(-1, maxValueIndex(emptyList()))
        assertEquals(-1, maxValueIndex(listOf(null, null)))
    }

    @Test
    fun indexOfFirstMaximum() {
        assertEquals(0, maxValueIndex(listOf(5)))
        assertEquals(1, maxValueIndex(listOf(1, 5, 3, 0, null)))
        assertEquals(3, maxValueIndex(listOf(null, 3, null, 7, 6)))
        assertEquals(0, maxValueIndex(listOf(-1, -5, -3)))
    }

    @Test
    fun tiesKeepEarliestBar() {
        assertEquals(0, maxValueIndex(listOf(5, 5, 2)))
        assertEquals(2, maxValueIndex(listOf(null, 3, 7, 7, 7)))
    }
}
