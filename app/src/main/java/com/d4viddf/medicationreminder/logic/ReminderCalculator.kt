package com.d4viddf.medicationreminder.logic

import android.util.Log // Added for logging
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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

                // Stricter check for totalIntervalMinutes
                if (totalIntervalMinutes <= 0) {
                    Log.e("ReminderCalculator", "Invalid totalIntervalMinutes (<=0) for medication ID: ${medication.id}, schedule ID: ${schedule.id}. Interval: $intervalHours hrs, $intervalMinutes mins.")
                    return emptyList() // Return empty list if interval is non-positive
                }

                // The original 'if (totalIntervalMinutes > 0)' is now effectively an 'else' to the check above.
                // No need to re-wrap the following logic in an 'if (totalIntervalMinutes > 0)' block.
                val dailyStartTimeStr = schedule.intervalStartTime
                val dailyEndTimeStr = schedule.intervalEndTime

                // This function (calculateReminderDateTimes) is for Type A (daily repeating) intervals,
                // so schedule.intervalStartTime should ideally be present.
                val parsedDailyStartTimeStr = schedule.intervalStartTime
                val parsedDailyEndTimeStr = schedule.intervalEndTime

                // If intervalStartTime is missing for a Type A, default to MIN, but this might indicate a data setup issue.
                val actualDailyStart = parsedDailyStartTimeStr?.takeIf { it.isNotBlank() }?.let {
                    try { LocalTime.parse(it, timeStorableFormatter) } catch (e: Exception) {
                        Log.e("ReminderCalculator", "Error parsing actualDailyStart: $it for Type A. Defaulting to MIN. Med ID: ${medication.id}", e)
                        LocalTime.MIN
                    }
                } ?: LocalTime.MIN

                val actualDailyEnd = parsedDailyEndTimeStr?.takeIf { it.isNotBlank() }?.let {
                    try { LocalTime.parse(it, timeStorableFormatter) } catch (e: Exception) {
                        Log.e("ReminderCalculator", "Error parsing actualDailyEnd: $it for Type A. Defaulting to MAX. Med ID: ${medication.id}", e)
                        LocalTime.MAX
                    }
                } ?: LocalTime.MAX

                Log.d("ReminderCalculator", "Calculating Type A for $targetDate, start: $actualDailyStart, end: $actualDailyEnd, interval: $totalIntervalMinutes min, scheduleId: ${schedule.id}")

                var loopTime = actualDailyStart
                var iterations = 0
                // Using the static MAX_ITERATIONS as defined in previous versions for consistency of this safeguard's behavior.
                // A dynamic one like `(24 * 60 / totalIntervalMinutes.coerceAtLeast(1)) + 5` could also be used
                // if very fine-grained control per interval length is desired for the safeguard.
                val MAX_ITERATIONS_PER_DAY = (24 * 60) + 5

                while (iterations < MAX_ITERATIONS_PER_DAY) {
                    // Primary condition: if loopTime has passed actualDailyEnd, stop.
                    // This is especially important if actualDailyEnd is not LocalTime.MAX.
                    if (loopTime.isAfter(actualDailyEnd)) {
                        Log.d("ReminderCalculatorLoop", "Iter ${iterations}: loopTime $loopTime is after actualDailyEnd $actualDailyEnd. Breaking. ScheduleId: ${schedule.id}")
                        break
                    }

                    reminders.add(LocalDateTime.of(targetDate, loopTime))
                    Log.d("ReminderCalculatorLoop", "Iter ${iterations}: Added: ${LocalDateTime.of(targetDate, loopTime)}, scheduleId: ${schedule.id}")

                    val nextLoopTimeCandidate = loopTime.plusMinutes(totalIntervalMinutes.toLong())

                    // If next candidate time wraps around midnight (i.e., it's earlier than current loopTime)
                    // AND actualDailyEnd was not LocalTime.MAX (meaning a specific end time was set for the day), then stop.
                    // This prevents reminders from spilling into the next conceptual day if a specific end time was set.
                    if (nextLoopTimeCandidate.isBefore(loopTime) && actualDailyEnd != LocalTime.MAX) {
                        Log.d("ReminderCalculatorLoop", "Iter ${iterations}: nextLoopTimeCandidate $nextLoopTimeCandidate is before loopTime $loopTime, and actualDailyEnd $actualDailyEnd is not MAX. Breaking. ScheduleId: ${schedule.id}")
                        break
                    }

                    loopTime = nextLoopTimeCandidate
                    iterations++
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

    fun generateRemindersForPeriod(
        medication: Medication,
        schedule: MedicationSchedule,
        periodStartDate: LocalDate,
        periodEndDate: LocalDate
    ): Map<LocalDate, List<LocalTime>> {
        val allRemindersResult = mutableMapOf<LocalDate, MutableList<LocalTime>>()

        if (schedule.scheduleType == ScheduleType.INTERVAL) {
            Log.d("ReminderCalculator", "generateRemindersForPeriod for INTERVAL schedule. Med ID: ${medication.id}, Schedule ID: ${schedule.id}")

            val parsedIntervalStartTime: LocalTime? = try {
                if (schedule.intervalStartTime.isNullOrBlank()) null
                else LocalTime.parse(schedule.intervalStartTime, timeStorableFormatter)
            } catch (e: Exception) {
                Log.e("ReminderCalculator", "Error parsing intervalStartTime: ${schedule.intervalStartTime}. Treating as null. Med ID: ${medication.id}", e)
                null
            }

            if (parsedIntervalStartTime == null) { // Type B: Continuous Interval
                Log.i("ReminderCalculator", "Continuous Interval (Type B) detected for Med ID: ${medication.id}, Schedule ID: ${schedule.id}")

                val intervalHours = schedule.intervalHours ?: 0
                val intervalMinutes = schedule.intervalMinutes ?: 0
                val totalIntervalMinutes = (intervalHours * 60) + intervalMinutes

                if (totalIntervalMinutes <= 0) {
                    Log.e("ReminderCalculator", "Continuous Interval: Invalid totalIntervalMinutes (<=0) for Med ID: ${medication.id}. Interval: $intervalHours hrs, $intervalMinutes mins.")
                    return emptyMap()
                }

                val medicationActualStartDateStr = medication.startDate
                if (medicationActualStartDateStr.isNullOrBlank()) {
                    Log.e("ReminderCalculator", "Continuous Interval: Medication start date is null/blank. Cannot proceed. Med ID: ${medication.id}")
                    return emptyMap()
                }
                val anchorDateTime: LocalDateTime = try {
                    LocalDate.parse(medicationActualStartDateStr, dateStorableFormatter).atStartOfDay() // Anchor to 00:00 of medication start date
                } catch (e: Exception) {
                    Log.e("ReminderCalculator", "Continuous Interval: Error parsing medication start date: $medicationActualStartDateStr. Med ID: ${medication.id}", e)
                    return emptyMap()
                }

                val parsedMedicationEndDate: LocalDate? = medication.endDate?.let {
                    try { LocalDate.parse(it, dateStorableFormatter) } catch (e: Exception) { null }
                }

                var currentReminderTime = anchorDateTime
                val allGeneratedDateTimes = mutableListOf<LocalDateTime>()
                val MAX_CONTINUOUS_ITERATIONS = 10000 // Safeguard: Max 10,000 reminders in raw sequence
                var continuousIterations = 0
                val longStopDate = periodStartDate.plusYears(5) // Long-stop safeguard for loop

                Log.d("ReminderCalculator", "Continuous Interval: Starting generation from $anchorDateTime. Interval: $totalIntervalMinutes mins. MedEndDate: $parsedMedicationEndDate. Period: $periodStartDate to $periodEndDate.")

                while (continuousIterations++ < MAX_CONTINUOUS_ITERATIONS) {
                    allGeneratedDateTimes.add(currentReminderTime)
                    val oldTimeForCheck = currentReminderTime
                    currentReminderTime = currentReminderTime.plusMinutes(totalIntervalMinutes.toLong())

                    if (currentReminderTime == oldTimeForCheck) { // Robustness for zero interval that might pass initial check
                        Log.e("ReminderCalculator", "Continuous interval: currentTime did not advance. Aborting. Interval: $totalIntervalMinutes. Med ID: ${medication.id}")
                        break
                    }
                    if (parsedMedicationEndDate != null && currentReminderTime.toLocalDate().isAfter(parsedMedicationEndDate)) {
                        Log.d("ReminderCalculator", "Continuous interval: currentReminderTime ($currentReminderTime) is after medication end date ($parsedMedicationEndDate). Stopping generation.")
                        break
                    }
                    if (currentReminderTime.toLocalDate().isAfter(longStopDate)) {
                        Log.w("ReminderCalculator", "Continuous interval: generation exceeded 5 years from period start ($longStopDate). Aborting. Med ID: ${medication.id}")
                        break
                    }
                }
                Log.d("ReminderCalculator", "Continuous Interval: Generated ${allGeneratedDateTimes.size} raw dateTimes. Now filtering for period.")

                allGeneratedDateTimes.forEach { dt ->
                    val reminderDate = dt.toLocalDate()
                    if (!reminderDate.isBefore(periodStartDate) && !reminderDate.isAfter(periodEndDate)) {
                        if (parsedMedicationEndDate == null || !reminderDate.isAfter(parsedMedicationEndDate)) {
                            // Also ensure it's not before the medication's actual start date (already handled by anchorDateTime start)
                            if (!reminderDate.isBefore(anchorDateTime.toLocalDate())) {
                                allRemindersResult.getOrPut(reminderDate) { mutableListOf() }
                                    .add(dt.toLocalTime())
                            }
                        }
                    }
                }

            } else { // Type A: Daily Repeating Interval (intervalStartTime is present)
                Log.i("ReminderCalculator", "Daily Repeating Interval (Type A) detected for Med ID: ${medication.id}, Schedule ID: ${schedule.id}")
                var currentDateIter = periodStartDate
                while (!currentDateIter.isAfter(periodEndDate)) {
                    // Call the existing calculateReminderDateTimes for each day
                    // Pass schedule with parsedIntervalStartTime to ensure it's used
                    val scheduleForDailyCalc = schedule.copy(intervalStartTime = parsedIntervalStartTime.format(timeStorableFormatter))
                    val dailyTimes = calculateReminderDateTimes(medication, scheduleForDailyCalc, currentDateIter)
                    if (dailyTimes.isNotEmpty()) {
                        allRemindersResult.getOrPut(currentDateIter) { mutableListOf() }
                            .addAll(dailyTimes.map { it.toLocalTime() })
                    }
                    currentDateIter = currentDateIter.plusDays(1)
                }
            }
        } else { // Not ScheduleType.INTERVAL (DAILY, CUSTOM_ALARMS, etc.)
            Log.d("ReminderCalculator", "generateRemindersForPeriod for non-INTERVAL schedule type: ${schedule.scheduleType}. Med ID: ${medication.id}")
            var currentDateIter = periodStartDate
            while (!currentDateIter.isAfter(periodEndDate)) {
                val dailyTimes = calculateReminderDateTimes(medication, schedule, currentDateIter)
                if (dailyTimes.isNotEmpty()) {
                    allRemindersResult.getOrPut(currentDateIter) { mutableListOf() }
                        .addAll(dailyTimes.map { it.toLocalTime() })
                }
                currentDateIter = currentDateIter.plusDays(1)
            }
        }
        return allRemindersResult.mapValues { entry -> entry.value.distinct().sorted() }
    }
}