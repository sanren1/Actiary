package com.actiary.ui.calendar

import java.time.LocalDate
import java.time.YearMonth

fun monthRange(month: YearMonth): ClosedRange<LocalDate> = month.atDay(1)..month.atEndOfMonth()

fun isFuture(date: LocalDate, today: LocalDate): Boolean = date.isAfter(today)
