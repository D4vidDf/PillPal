package com.d4viddf.medicationreminder.logic // Or any other appropriate package

import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ReminderCalculator {

     val timeStorableFormatter = DateTimeFormatter.ISO_LOCAL_TIME
     val dateStorableFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy") // Matches AddMedicationScreen
    val storableDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME


    fun calculateReminderDateTimes(
        medication: Medication,
        schedule: MedicationSchedule,
        targetDate: LocalDate // The specific date for which to calculate reminders
    ): List<LocalDateTime> {
        val reminders = mutableListOf<LocalDateTime>()

        // Check if medication is active on the targetDate
        val medicationStartDate = medication.startDate?.let { LocalDate.parse(it, dateStorableFormatter) }
        val medicationEndDate = medication.endDate?.let { LocalDate.parse(it, dateStorableFormatter) }

        if (medicationStartDate != null && targetDate.isBefore(medicationStartDate)) {
            return emptyList() // Medication hasn't started yet
        }
        if (medicationEndDate != null && targetDate.isAfter(medicationEndDate)) {
            return emptyList() // Medication has ended
        }

        when (schedule.scheduleType) {
            ScheduleType.DAILY -> { // "Once a day"
                val dayOfWeekForTarget = targetDate.dayOfWeek.value // Monday=1, Sunday=7
                val scheduledDays = schedule.daysOfWeek?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList()

                if (scheduledDays.isEmpty() || scheduledDays.contains(dayOfWeekForTarget)) {
                    schedule.specificTimes?.split(',')?.firstOrNull()?.let { timeStr ->
                        try {
                            val time = LocalTime.parse(timeStr, timeStorableFormatter)
                            reminders.add(LocalDateTime.of(targetDate, time))
                        } catch (e: Exception) {
                            // Log parsing error
                        }
                    }
                }
            }
            ScheduleType.CUSTOM_ALARMS -> { // "Multiple times a day"
                // For custom alarms, we assume they apply every day the medication is active,
                // unless daysOfWeek is also used in conjunction (which it isn't currently in your setup).
                schedule.specificTimes?.split(',')?.forEach { timeStr ->
                    try {
                        val time = LocalTime.parse(timeStr, timeStorableFormatter)
                        reminders.add(LocalDateTime.of(targetDate, time))
                    } catch (e: Exception) {
                        // Log parsing error
                    }
                }
            }
            ScheduleType.INTERVAL -> {
                val intervalHours = schedule.intervalHours ?: 0
                val intervalMinutes = schedule.intervalMinutes ?: 0
                val totalIntervalMinutes = (intervalHours * 60) + intervalMinutes

                if (totalIntervalMinutes > 0) {
                    val dailyStartTimeStr = schedule.intervalStartTime
                    val dailyEndTimeStr = schedule.intervalEndTime

                    val actualDailyStart = dailyStartTimeStr?.let { LocalTime.parse(it, timeStorableFormatter) } ?: LocalTime.MIN
                    val actualDailyEnd = dailyEndTimeStr?.let { LocalTime.parse(it, timeStorableFormatter) } ?: LocalTime.MAX

                    var currentTime = actualDailyStart
                    while (!currentTime.isAfter(actualDailyEnd)) {
                        reminders.add(LocalDateTime.of(targetDate, currentTime))
                        currentTime = currentTime.plusMinutes(totalIntervalMinutes.toLong())
                        if (currentTime == LocalTime.MIDNIGHT && totalIntervalMinutes > 0) { // Avoid infinite loop if interval is 0 and time is midnight
                            break // Handled edge case where interval might perfectly align to make it loop midnight to midnight
                        }
                    }
                }
            }
            ScheduleType.WEEKLY -> {
                // TODO: Implement if re-add "Weekly" frequency.
                // Would check if targetDate's dayOfWeek is in schedule.daysOfWeek
                // And then use specificTimes (likely one time for that day).
            }
            ScheduleType.AS_NEEDED -> {
                // "As Needed" typically doesn't generate proactive reminders.
                // Handled by user taking it when needed.
            }
        }
        return reminders.sorted()
    }

    // Generates reminders for a period, e.g., from medication start date to end date, or for next N days
    fun generateRemindersForPeriod(
        medication: Medication,
        schedule: MedicationSchedule,
        periodStartDate: LocalDate,
        periodEndDate: LocalDate
    ): Map<LocalDate, List<LocalTime>> {
        val allReminders = mutableMapOf<LocalDate, MutableList<LocalTime>>()
        var currentDate = periodStartDate
        while (!currentDate.isAfter(periodEndDate)) {
            val dailyTimes = calculateReminderDateTimes(medication, schedule, currentDate)
            if (dailyTimes.isNotEmpty()) {
                allReminders.getOrPut(currentDate) { mutableListOf() }
                    .addAll(dailyTimes.map { it.toLocalTime() })
            }
            currentDate = currentDate.plusDays(1)
        }
        return allReminders.mapValues { it.value.distinct().sorted() } // Ensure distinct and sorted times per day
    }
}