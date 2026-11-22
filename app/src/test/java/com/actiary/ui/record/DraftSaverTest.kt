package com.actiary.ui.record

import androidx.compose.runtime.saveable.SaverScope
import com.actiary.data.db.DayStatus
import com.actiary.domain.DayDraft
import com.actiary.domain.DraftEntry
import com.actiary.domain.DraftPhoto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private object TestSaverScope : SaverScope {
    override fun canBeSaved(value: Any): Boolean = true
}

class DraftSaverTest {
    private val date = LocalDate.of(2024, 2, 10)

    private fun roundTrip(draft: DayDraft?): DayDraft? {
        val saved = with(DraftSaver) { TestSaverScope.save(draft) } ?: return null
        return DraftSaver.restore(saved)
    }

    @Test fun preservesCaloriesAndHeartRate() {
        val draft = DayDraft(
            date = date,
            status = DayStatus.EXERCISED,
            entries = listOf(
                DraftEntry(1, 30, 5000, 320, 145, id = 4),
                DraftEntry(1, 45, null, null, null, id = 5),
                DraftEntry(3, null, 1200, 1, 230, id = 6),
            ),
            note = "note",
            photos = listOf(DraftPhoto.Picked("content://capture/1"), DraftPhoto.Saved("a.jpg")),
        )
        assertEquals(draft, roundTrip(draft))
    }

    @Test fun stagedPhotoReferencesSurviveProcessRecreation() {
        val draft = DayDraft(date, DayStatus.EXERCISED, emptyList(), "", listOf(DraftPhoto.Staged("draft.jpg")))
        assertEquals(draft, roundTrip(draft))
    }

    @Test fun nullsStayNull() {
        val draft = DayDraft(date, DayStatus.REST, listOf(DraftEntry(9)), "", emptyList())
        val restored = roundTrip(draft)!!
        assertEquals(null, restored.entries.single().calories)
        assertEquals(null, restored.entries.single().avgHeartRate)
    }

    @Test fun emptyDraftRestoresToNull() {
        assertEquals(null, roundTrip(null))
    }
}
