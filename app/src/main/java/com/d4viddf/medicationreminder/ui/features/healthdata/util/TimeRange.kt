package com.d4viddf.medicationreminder.ui.features.healthdata.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import androidx.annotation.StringRes
import com.d4viddf.medicationreminder.R
import java.time.temporal.WeekFields
import java.util.Locale

enum class TimeRange(@StringRes val titleResId: Int) {
    DAY(R.string.time_range_day),
    WEEK(R.string.time_range_week),
    MONTH(R.string.time_range_month),
    YEAR(R.string.time_range_year);

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
