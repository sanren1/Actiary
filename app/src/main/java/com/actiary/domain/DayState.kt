package com.actiary.domain

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus

enum class DayState { EXERCISED, REST, UNRECORDED }

fun DailyRecordEntity?.toDayState(): DayState = when (this?.status) {
    DayStatus.EXERCISED -> DayState.EXERCISED
    DayStatus.REST -> DayState.REST
    null -> DayState.UNRECORDED
}
