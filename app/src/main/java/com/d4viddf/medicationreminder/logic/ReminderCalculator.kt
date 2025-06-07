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
        targetDate: LocalDate, // The specific date for which to calculate reminders
        lastTakenOnDate: LocalTime? = null // New parameter for Type A adjustment
    ): List<LocalDateTime> {
        val funcTag = "calculateReminderDateTimes[MedID: ${medication.id}, SchedID: ${schedule.id}, Date: $targetDate]"
        Log.d("ReminderCalculator", "$funcTag: Called with lastTakenOnDate: $lastTakenOnDate")
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

                Log.d("ReminderCalculator", "$funcTag: Type A interval. actualDailyStart: $actualDailyStart, actualDailyEnd: $actualDailyEnd, interval: $totalIntervalMinutes min.")

                var loopStartAnchorTime = actualDailyStart
                if (lastTakenOnDate != null) {
                    Log.i("ReminderCalculator", "$funcTag: lastTakenOnDate is provided: $lastTakenOnDate.")
                    val potentialStartTimeAfterTaken = lastTakenOnDate.plusMinutes(totalIntervalMinutes.toLong())
                    Log.d("ReminderCalculator", "$funcTag: potentialStartTimeAfterTaken from lastTakenOnDate: $potentialStartTimeAfterTaken")

                    if (!potentialStartTimeAfterTaken.isAfter(actualDailyEnd) && !potentialStartTimeAfterTaken.isBefore(actualDailyStart)) {
                        loopStartAnchorTime = potentialStartTimeAfterTaken
                        Log.i("ReminderCalculator", "$funcTag: Adjusted loopStartAnchorTime to $loopStartAnchorTime based on lastTakenOnDate.")
                    } else if (potentialStartTimeAfterTaken.isBefore(actualDailyStart)) {
                        // If the next interval from lastTaken is before the window, but lastTaken was valid,
                        // the next dose should still be at the window start.
                        loopStartAnchorTime = actualDailyStart
                        Log.i("ReminderCalculator", "$funcTag: potentialStartTimeAfterTaken is before window. Using actualDailyStart ($actualDailyStart) as loopStartAnchorTime.")
                    } else { // potentialStartTimeAfterTaken is after actualDailyEnd
                        Log.i("ReminderCalculator", "$funcTag: potentialStartTimeAfterTaken ($potentialStartTimeAfterTaken) is after actualDailyEnd ($actualDailyEnd). No reminders for this day from lastTakenOnDate.")
                        return emptyList() // No reminders possible for this day based on last taken.
                    }
                } else {
                    Log.d("ReminderCalculator", "$funcTag: No lastTakenOnDate provided. Using actualDailyStart ($actualDailyStart) as loopStartAnchorTime.")
                }

                if (loopStartAnchorTime.isAfter(actualDailyEnd)) {
                     Log.i("ReminderCalculator", "$funcTag: loopStartAnchorTime ($loopStartAnchorTime) is already after actualDailyEnd ($actualDailyEnd). No reminders to generate.")
                     return emptyList()
                }

                var loopTime = loopStartAnchorTime
                var iterations = 0
                val MAX_ITERATIONS_PER_DAY = (24 * 60 / totalIntervalMinutes.coerceAtLeast(1)) + 5 // Dynamic safeguard based on interval

                Log.d("ReminderCalculator", "$funcTag: Starting Type A loop. loopTime: $loopTime, actualDailyEnd: $actualDailyEnd")

                while (iterations < MAX_ITERATIONS_PER_DAY && !loopTime.isAfter(actualDailyEnd)) {
                    reminders.add(LocalDateTime.of(targetDate, loopTime))
                    Log.d("ReminderCalculatorLoop", "$funcTag Iter ${iterations}: Added: ${LocalDateTime.of(targetDate, loopTime)}")

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
        periodEndDate: LocalDate,
        lastTakenDateTime: LocalDateTime? = null // New optional parameter
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
                Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, medicationActualStartDateStr = $medicationActualStartDateStr")
                if (medicationActualStartDateStr.isNullOrBlank()) {
                    Log.e("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Medication start date is null/blank. Cannot proceed.")
                    return emptyMap()
                }

                val medicationStartDateOnly: LocalDate = try {
                    LocalDate.parse(medicationActualStartDateStr, dateStorableFormatter)
                } catch (e: Exception) {
                    Log.e("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Error parsing medication start date string: $medicationActualStartDateStr.", e)
                    return emptyMap()
                }

                val currentDateTime = LocalDateTime.now()
                var anchorDateTime: LocalDateTime

                if (lastTakenDateTime != null) {
                    Log.i("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, lastTakenDateTime is provided: $lastTakenDateTime")
                    val nextReminderAfterTaken = lastTakenDateTime.plusMinutes(totalIntervalMinutes.toLong())
                    Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, calculated nextReminderAfterTaken: $nextReminderAfterTaken")

                    // Ensure anchor is not before medication start date
                    val medicationStartAtDay = medicationStartDateOnly.atStartOfDay()
                    anchorDateTime = if (nextReminderAfterTaken.isBefore(medicationStartAtDay)) {
                        Log.w("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, nextReminderAfterTaken ($nextReminderAfterTaken) is before medication start date ($medicationStartAtDay). Using medication start date as anchor.")
                        medicationStartAtDay
                    } else {
                        nextReminderAfterTaken
                    }
                    Log.i("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Using lastTakenDateTime. Adjusted anchorDateTime: $anchorDateTime")
                } else {
                    // Original logic if lastTakenDateTime is null
                    anchorDateTime = if (medicationStartDateOnly.isEqual(currentDateTime.toLocalDate()) && !currentDateTime.toLocalTime().isBefore(LocalTime.MIN.plusSeconds(1))) { // Ensure current time is not midnight start
                         // If med start is today, AND current time is not effectively 00:00 (implying we want to start from now, not from a historical 00:00)
                        Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, No lastTaken. Medication start date is today. Anchoring to currentDateTime: $currentDateTime")
                        currentDateTime
                    } else {
                        // If med start is in the past, or it's today but effectively very early (e.g. worker runs at 00:01 for a med starting today)
                        // then anchor to the start of the medication day.
                        Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, No lastTaken. Medication start date is not today ($medicationStartDateOnly) or it is today but very early. Anchoring to start of medication day: ${medicationStartDateOnly.atStartOfDay()}")
                        medicationStartDateOnly.atStartOfDay()
                    }
                    Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, No lastTaken. Original anchor logic. Final anchorDateTime = $anchorDateTime")
                }


                // Ensure anchorDateTime is not in the future beyond what's sensible for calculation start.
                // If the calculated anchor (especially from lastTaken) is way past the current time,
                // it might be more logical to start calculations from 'now' if 'now' is later than medication start.
                // However, the current primary logic is to respect lastTakenDateTime's projection.
                // A secondary check: if anchorDateTime is before 'now' but medication started earlier, 'now' could be a floor.
                val effectiveCalculationStartAnchor = if (anchorDateTime.isBefore(currentDateTime) && medicationStartDateOnly.isBefore(currentDateTime.toLocalDate())) {
                    // If the anchor (potentially from an old lastTaken) is in the past, and med started in past,
                    // we should start generating from 'now' to avoid historical reminders, but only if 'now' is after the original anchor.
                    // This ensures that if lastTaken results in an anchor in the future, we use that.
                    // If lastTaken results in an anchor in the past, we advance it to 'now' (if 'now' is also past medication start)
                    if (currentDateTime.isAfter(anchorDateTime)) {
                         Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Anchor $anchorDateTime was in past. Med started in past. Advancing anchor to currentDateTime: $currentDateTime for generation start.")
                         currentDateTime
                    } else {
                         anchorDateTime
                    }
                } else {
                  anchorDateTime
                }
                // Re-assign anchorDateTime to the potentially adjusted one for clarity in subsequent logs
                anchorDateTime = effectiveCalculationStartAnchor
                Log.i("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, final effective anchorDateTime for generation = $anchorDateTime")


                val parsedMedicationEndDate: LocalDate? = medication.endDate?.let {
                    try { LocalDate.parse(it, dateStorableFormatter) } catch (e: Exception) { null }
                }

                var currentReminderTime = anchorDateTime // Start generating from the determined anchorDateTime
                val allGeneratedDateTimes = mutableListOf<LocalDateTime>()
                val MAX_CONTINUOUS_ITERATIONS = 10000 // Safeguard: Max 10,000 reminders in raw sequence
                var continuousIterations = 0
                val longStopDate = periodStartDate.plusYears(5) // Long-stop safeguard for loop

                Log.i("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Starting generation from $currentReminderTime (derived from anchorDateTime). Interval: $totalIntervalMinutes mins. MedEndDate: $parsedMedicationEndDate. Period: $periodStartDate to $periodEndDate.")

                while (continuousIterations++ < MAX_CONTINUOUS_ITERATIONS) {
                    if (continuousIterations <= 5) { // Log first 5 generated currentReminderTime values
                        Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Iteration $continuousIterations, currentReminderTime candidate = $currentReminderTime")
                    }

                    // Add reminder candidate if it's not before the medication start date's time (anchor could be med start day 00:00)
                    // and also not before the overall periodStartDate (though subsequent filtering handles periodStartDate)
                    // This initial check ensures we don't add times from before the medication was supposed to start if anchor was adjusted.
                    if (!currentReminderTime.isBefore(medicationStartDateOnly.atStartOfDay())) {
                         allGeneratedDateTimes.add(currentReminderTime)
                    } else {
                        Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Iteration $continuousIterations, currentReminderTime candidate $currentReminderTime is before medication start day ${medicationStartDateOnly.atStartOfDay()}. Skipping add.")
                    }

                    val oldTimeForCheck = currentReminderTime
                    currentReminderTime = currentReminderTime.plusMinutes(totalIntervalMinutes.toLong())

                    if (currentReminderTime == oldTimeForCheck) { // Robustness for zero interval that might pass initial check
                        Log.e("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, currentReminderTime did not advance. Aborting. Interval: $totalIntervalMinutes.")
                        break
                    }
                    if (parsedMedicationEndDate != null && currentReminderTime.toLocalDate().isAfter(parsedMedicationEndDate)) {
                        Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, next currentReminderTime ($currentReminderTime) would be after medication end date ($parsedMedicationEndDate). Stopping generation.")
                        break
                    }
                    if (currentReminderTime.toLocalDate().isAfter(longStopDate)) {
                        Log.w("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, generation exceeded 5 years from period start ($longStopDate). Aborting.")
                        break
                    }
                }
                Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Generated ${allGeneratedDateTimes.size} raw dateTimes. Now filtering for period.")

                var filteredRemindersCount = 0
                allGeneratedDateTimes.forEach { dt ->
                    val reminderDate = dt.toLocalDate()
                    if (!reminderDate.isBefore(periodStartDate) && !reminderDate.isAfter(periodEndDate)) {
                        if (parsedMedicationEndDate == null || !reminderDate.isAfter(parsedMedicationEndDate)) {
                    // Ensure reminder is not before the medication's actual start date on that day.
                    // The primary anchorDateTime logic should handle the starting point correctly,
                    // but this is a safeguard for the generated list.
                    if (!dt.toLocalDate().isBefore(medicationStartDateOnly)) {
                         if (!dt.isBefore(anchorDateTime) || dt.toLocalDate().isAfter(anchorDateTime.toLocalDate())) { // Accept if on same day but at/after anchor time, or any time on subsequent days
                            allRemindersResult.getOrPut(reminderDate) { mutableListOf() }
                                .add(dt.toLocalTime())
                            filteredRemindersCount++
                        } else {
                             Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Filtering out $dt as it's before effective anchor time $anchorDateTime on the same day.")
                        }
                    } else {
                        Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Filtering out $dt as it's before medication start date $medicationStartDateOnly.")
                            }
                        }
                    }
                }
                Log.d("ReminderCalculator", "Continuous Interval: Med ID ${medication.id}, Number of reminders after filtering for period $periodStartDate to $periodEndDate: $filteredRemindersCount.")

            } else { // Type A: Daily Repeating Interval (intervalStartTime is present)
                Log.i("ReminderCalculator", "Daily Repeating Interval (Type A) detected for Med ID: ${medication.id}, Schedule ID: ${schedule.id}")
                var currentDateIter = periodStartDate
                while (!currentDateIter.isAfter(periodEndDate)) {
                    // Call calculateReminderDateTimes for each day, passing lastTakenOnDate if applicable
                    val lastTakenOnCurrentDateIter: LocalTime? = if (lastTakenDateTime != null && lastTakenDateTime.toLocalDate().isEqual(currentDateIter)) {
                        Log.d("ReminderCalculator", "generateRemindersForPeriod: Passing lastTakenDateTime.toLocalTime() (${lastTakenDateTime.toLocalTime()}) for date $currentDateIter to calculateReminderDateTimes.")
                        lastTakenDateTime.toLocalTime()
                    } else {
                        null
                    }
                    // Ensure parsedIntervalStartTime (which defines this as Type A) is correctly passed if not null
                    val scheduleForDailyCalc = if(parsedIntervalStartTime != null) schedule.copy(intervalStartTime = parsedIntervalStartTime.format(timeStorableFormatter)) else schedule

                    val dailyTimes = calculateReminderDateTimes(medication, scheduleForDailyCalc, currentDateIter, lastTakenOnCurrentDateIter)
                    if (dailyTimes.isNotEmpty()) {
                        Log.d("ReminderCalculator", "generateRemindersForPeriod: For $currentDateIter (Type A), got dailyTimes: ${dailyTimes.map { it.toLocalTime() }} using lastTakenOnDate: $lastTakenOnCurrentDateIter")
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
                // For non-INTERVAL types, lastTakenOnDate is not applicable for calculateReminderDateTimes's current logic
                val dailyTimes = calculateReminderDateTimes(medication, schedule, currentDateIter, null)
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