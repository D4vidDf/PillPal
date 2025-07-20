package com.d4viddf.medicationreminder.wear.logic

import android.util.Log
import com.d4viddf.medicationreminder.wear.persistence.MedicationSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.ScheduleDetailSyncEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ReminderCalculator {
    private const val TAG = "WearReminderCalculator"

    val timeStorableFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateStorableFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun generateRemindersForPeriod(
        medication: MedicationSyncEntity,
        schedule: ScheduleDetailSyncEntity,
        periodStartDate: LocalDate,
        periodEndDate: LocalDate,
    ): Map<LocalDate, List<LocalTime>> {
        val allRemindersResult = mutableMapOf<LocalDate, MutableList<LocalTime>>()

        val medicationOverallStartDate = medication.startDate?.let { try { LocalDate.parse(it, dateStorableFormatter) } catch (e: DateTimeParseException) { null } }
        val medicationOverallEndDate = medication.endDate?.let { try { LocalDate.parse(it, dateStorableFormatter) } catch (e: DateTimeParseException) { null } }

        if (medicationOverallStartDate != null && periodEndDate.isBefore(medicationOverallStartDate)) {
            return emptyMap()
        }
        if (medicationOverallEndDate != null && periodStartDate.isAfter(medicationOverallEndDate)) {
            return emptyMap()
        }

        var currentDateIter = periodStartDate
        while (!currentDateIter.isAfter(periodEndDate)) {
            if (medicationOverallStartDate != null && currentDateIter.isBefore(medicationOverallStartDate)) {
                currentDateIter = currentDateIter.plusDays(1)
                continue
            }
            if (medicationOverallEndDate != null && currentDateIter.isAfter(medicationOverallEndDate)) {
                break
            }

            val dailyTimes = getAllPotentialSlotsForDay(schedule, currentDateIter)

            if (dailyTimes.isNotEmpty()) {
                allRemindersResult.getOrPut(currentDateIter) { mutableListOf() }.addAll(dailyTimes)
            }
            currentDateIter = currentDateIter.plusDays(1)
        }
        return allRemindersResult.mapValues { entry -> entry.value.distinct().sorted() }
    }

    private fun getAllPotentialSlotsForDay(
        schedule: ScheduleDetailSyncEntity,
        targetDate: LocalDate
    ): List<LocalTime> {
        val slots = mutableListOf<LocalTime>()
        val scheduleType = try {
            ScheduleType.valueOf(schedule.scheduleType)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Unknown schedule type: ${schedule.scheduleType}")
            return emptyList()
        }

        when (scheduleType) {
            ScheduleType.DAILY, ScheduleType.CUSTOM_ALARMS -> {
                schedule.specificTimesJson?.let { json ->
                    val typeToken = object : TypeToken<List<String>>() {}.type
                    val times = Gson().fromJson<List<String>>(json, typeToken)
                    times.forEach { timeStr ->
                        try {
                            slots.add(LocalTime.parse(timeStr, timeStorableFormatter))
                        } catch (e: DateTimeParseException) {
                            Log.e(TAG, "Error parsing time: $timeStr", e)
                        }
                    }
                }
            }
            ScheduleType.INTERVAL -> {
                val intervalHours = schedule.intervalHours ?: 0
                val intervalMinutes = schedule.intervalMinutes ?: 0
                val totalIntervalMinutes = (intervalHours * 60) + intervalMinutes

                if (totalIntervalMinutes > 0 && !schedule.intervalStartTime.isNullOrBlank()) {
                    val actualDailyStart = try {
                        LocalTime.parse(schedule.intervalStartTime, timeStorableFormatter)
                    } catch (e: Exception) {
                        LocalTime.MIN
                    }
                    val actualDailyEnd = schedule.intervalEndTime?.takeIf { it.isNotBlank() }?.let {
                        try { LocalTime.parse(it, timeStorableFormatter) } catch (e: Exception) { LocalTime.MAX }
                    } ?: LocalTime.MAX

                    var loopTime = actualDailyStart
                    val MAX_ITERATIONS_PER_DAY = (24 * 60 / totalIntervalMinutes.coerceAtLeast(1)) + 5
                    var iterations = 0

                    while (iterations < MAX_ITERATIONS_PER_DAY && !loopTime.isAfter(actualDailyEnd)) {
                        slots.add(loopTime)
                        val nextLoopTimeCandidate = loopTime.plusMinutes(totalIntervalMinutes.toLong())
                        if (nextLoopTimeCandidate.isBefore(loopTime) && actualDailyEnd != LocalTime.MAX) {
                            break
                        }
                        loopTime = nextLoopTimeCandidate
                        iterations++
                    }
                }
            }
            else -> {
                // Not implemented for other types
            }
        }
        return slots.distinct().sorted()
    }
}
