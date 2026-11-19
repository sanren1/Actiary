package com.actiary.domain

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DayStateTest {
    private fun row(s: DayStatus) = DailyRecordEntity(1, s, "", 1)

    @Test fun mapsStatus() {
        assertEquals(DayState.UNRECORDED, (null as DailyRecordEntity?).toDayState())
        assertEquals(DayState.EXERCISED, row(DayStatus.EXERCISED).toDayState())
        assertEquals(DayState.REST, row(DayStatus.REST).toDayState())
    }
}
