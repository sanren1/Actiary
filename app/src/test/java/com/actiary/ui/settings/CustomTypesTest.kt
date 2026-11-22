package com.actiary.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomTypesTest {
    @Test
    fun typeDragDestinationMovesDownAcrossMultipleRows() {
        val sizes = listOf(100, 100, 100, 100)
        // Threshold for 1 step = (100 + 100) / 2 + 16 = 116
        assertEquals(0, typeDragDestination(0, sizes, 16f, 115f))
        assertEquals(1, typeDragDestination(0, sizes, 16f, 116f))
        assertEquals(2, typeDragDestination(0, sizes, 16f, 232f))
        assertEquals(3, typeDragDestination(0, sizes, 16f, 500f))
    }

    @Test
    fun typeDragDestinationMovesUpAndClampsAtZero() {
        val sizes = listOf(80, 120, 100)
        // Step 2 -> 1 threshold = (100 + 120) / 2 + 10 = 120
        // Step 1 -> 0 threshold = (120 + 80) / 2 + 10 = 110
        assertEquals(2, typeDragDestination(2, sizes, 10f, -119f))
        assertEquals(1, typeDragDestination(2, sizes, 10f, -120f))
        assertEquals(1, typeDragDestination(2, sizes, 10f, -229f))
        assertEquals(0, typeDragDestination(2, sizes, 10f, -230f))
        assertEquals(0, typeDragDestination(2, sizes, 10f, -999f))
    }

    @Test
    fun validateTypeNameEnforcesBlankLengthAndCaseInsensitiveUniqueness() {
        val existing = listOf(
            1L to "跑步",
            2L to "Running",
            3L to "Yoga",
        )
        assertEquals(TypeNameError.BLANK, validateTypeName("   ", existing, currentId = null))
        assertNull(validateTypeName("   ", existing, currentId = 1L, allowBlank = true))

        val maxLen = "a".repeat(MAX_TYPE_NAME_LENGTH)
        assertNull(validateTypeName("  $maxLen  ", existing, currentId = null))
        assertEquals(
            TypeNameError.TOO_LONG,
            validateTypeName("a".repeat(MAX_TYPE_NAME_LENGTH + 1), existing, currentId = null),
        )

        assertEquals(TypeNameError.DUPLICATE, validateTypeName("yoga", existing, currentId = null))
        assertEquals(TypeNameError.DUPLICATE, validateTypeName("  RUNNING ", existing, currentId = null))
        assertEquals(TypeNameError.DUPLICATE, validateTypeName("跑步", existing, currentId = 3L))
        // Editing the same type keeps its own name valid.
        assertNull(validateTypeName("Yoga", existing, currentId = 3L))
        assertNull(validateTypeName("YOGA", existing, currentId = 3L))
        assertNull(validateTypeName("Pilates", existing, currentId = null))
    }
}
