package com.actiary.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FreeformFocusNoteTest {
    private val zhLabels = mapOf(
        FreeformFocusPart.CHEST to "胸",
        FreeformFocusPart.BACK to "背",
        FreeformFocusPart.SHOULDERS to "肩",
        FreeformFocusPart.ARMS to "臂",
        FreeformFocusPart.LEGS to "腿",
        FreeformFocusPart.ABS to "腹",
    )

    @Test
    fun selectingPartsAppendsLinesAndPreservesExistingNote() {
        val afterChest = FreeformFocusNote.togglePartInNote("", "胸", selected = true, colon = "：")
        assertEquals("胸：", afterChest)

        val afterBack = FreeformFocusNote.togglePartInNote(afterChest, "背", selected = true, colon = "：")
        assertEquals("胸：\n背：", afterBack)

        val withUserText = "热身10分钟\n$afterBack"
        val afterLegs = FreeformFocusNote.togglePartInNote(withUserText, "腿", selected = true, colon = "：")
        assertEquals("热身10分钟\n胸：\n背：\n腿：", afterLegs)
    }

    @Test
    fun unselectingRemovesEmptyPlaceholderButKeepsEditedLine() {
        val note = "胸：杠铃卧推 4x8\n背：\n臂："
        val afterUnselectBack = FreeformFocusNote.togglePartInNote(note, "背", selected = false, colon = "：")
        assertEquals("胸：杠铃卧推 4x8\n臂：", afterUnselectBack)

        val afterUnselectChest = FreeformFocusNote.togglePartInNote(afterUnselectBack, "胸", selected = false, colon = "：")
        assertEquals("胸：杠铃卧推 4x8\n臂：", afterUnselectChest)
    }

    @Test
    fun detectSelectedRecognizesBothChineseAndAsciiColons() {
        val note = "热身\n胸：卧推\n背:\n腿："
        val detected = FreeformFocusNote.detectSelected(note, zhLabels)
        assertEquals(
            setOf(FreeformFocusPart.CHEST, FreeformFocusPart.BACK, FreeformFocusPart.LEGS),
            detected,
        )
    }

    @Test
    fun selectingAlreadyPresentPartDoesNotDuplicate() {
        val note = "胸：哑铃推举"
        val again = FreeformFocusNote.togglePartInNote(note, "胸", selected = true, colon = "：")
        assertEquals(note, again)
    }
}
