package com.d4viddf.medicationreminder.ui.features.healthdata.util

import java.time.Instant
import java.time.temporal.ChronoUnit

enum class TimeRange {
    DAY,
    WEEK,
    MONTH,
    THREE_MONTHS,
    YEAR;

    fun getStartAndEndTimes(): Pair<Instant, Instant> {
        val endTime = Instant.now()
        val startTime = when (this) {
            DAY -> endTime.minus(1, ChronoUnit.DAYS)
            WEEK -> endTime.minus(7, ChronoUnit.DAYS)
            MONTH -> endTime.minus(30, ChronoUnit.DAYS)
            THREE_MONTHS -> endTime.minus(90, ChronoUnit.DAYS)
            YEAR -> endTime.minus(365, ChronoUnit.DAYS)
        }
        return Pair(startTime, endTime)
    }
}
