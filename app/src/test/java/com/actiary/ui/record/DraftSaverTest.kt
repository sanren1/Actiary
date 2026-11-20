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

    // Saver.save has a SaverScope receiver that is awkward to invoke from Kotlin source
    // in this Compose version; call its JVM method directly with a permissive scope.
    private fun roundTrip(draft: DayDraft?): DayDraft? {
        val method = DraftSaver.javaClass.getMethod("save", SaverScope::class.java, Any::class.java)
        val saved = method.invoke(DraftSaver, TestSaverScope, draft)
        return DraftSaver.restore(saved)
    }

    @Test fun preservesCaloriesAndHeartRate() {
        val draft = DayDraft(
            date = date,
            status = DayStatus.EXERCISED,
            entries = listOf(
                DraftEntry(1, 30, 5000, 320, 145),
                DraftEntry(2, 45, null, null, null),
                DraftEntry(3, null, 1200, 1, 230),
            ),
            note = "note",
            photos = listOf(DraftPhoto.Picked("content://capture/1"), DraftPhoto.Saved("a.jpg")),
        )
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
