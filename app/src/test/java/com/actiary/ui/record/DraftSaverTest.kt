package com.actiary.ui.record

import androidx.compose.runtime.saveable.SaverScope
import com.actiary.data.db.DayStatus
import com.actiary.domain.DayDraft
import com.actiary.domain.DraftEntry
import com.actiary.domain.DraftPhoto
import com.actiary.domain.WorkoutMetrics
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

    @Test fun optionalMetricsSurviveProcessRecreation() {
        val first = DraftEntry(1, 32, 6000, id = 1, metrics = WorkoutMetrics(
            durationSeconds = 1932, cadenceSpm = 172, stepCount = 5820, elevationGainCm = 2505,
        ))
        val second = DraftEntry(2, id = 2, metrics = WorkoutMetrics(poolLengthCm = 2500, lapCount = 60, strokeCount = 390))
        val draft = DayDraft(date, DayStatus.EXERCISED, listOf(first, second), "", emptyList())
        assertEquals(draft, roundTrip(draft))
    }

    @Test fun legacyDraftRestoresWithoutExtraMetrics() {
        val old = arrayListOf<Any>(
            date.toEpochDay(), DayStatus.EXERCISED.name, "", arrayListOf(1L),
            arrayListOf(30), arrayListOf(5000), arrayListOf<String>(),
            arrayListOf(95), arrayListOf(130), arrayListOf(9L),
        )
        assertEquals(DraftEntry(1, 30, 5000, 95, 130, id = 9), DraftSaver.restore(old)?.entries?.single())
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
