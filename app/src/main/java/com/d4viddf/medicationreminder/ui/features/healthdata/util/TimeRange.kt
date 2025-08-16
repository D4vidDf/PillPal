package com.d4viddf.medicationreminder.ui.features.healthdata.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

enum class TimeRange {
    DAY,
    WEEK,
    MONTH,
    THREE_MONTHS,
    YEAR;

    fun getStartAndEndTimes(date: LocalDate): Pair<Instant, Instant> {
        val start: LocalDate
        val end: LocalDate
        when (this) {
            DAY -> {
                start = date
                end = date
            }
            WEEK -> {
                val weekFields = WeekFields.of(Locale.getDefault())
                start = date.with(weekFields.dayOfWeek(), 1)
                end = start.plusDays(6)
            }
            MONTH -> {
                val yearMonth = YearMonth.from(date)
                start = yearMonth.atDay(1)
                end = yearMonth.atEndOfMonth()
            }
            THREE_MONTHS -> {
                start = date.withDayOfMonth(1).minusMonths(2)
                end = date.withDayOfMonth(1).plusMonths(1).minusDays(1)
            }
            YEAR -> {
                start = date.withDayOfYear(1)
                end = date.withDayOfYear(date.lengthOfYear())
            }
        }
        val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusNanos(1)
        return Pair(startInstant, endInstant)
    }
}
