package com.d4viddf.medicationreminder.logic

import android.util.Log // Added for logging
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.utils.FileLogger // Import FileLogger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ReminderCalculator {
    private const val TAG = "ReminderCalculatorLog"

    val timeStorableFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    val dateStorableFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy") // Matches AddMedicationScreen
    val storableDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME


    fun calculateReminderDateTimes(
        medication: Medication,
        schedule: MedicationSchedule,
        targetDate: LocalDate, // The specific date for which to calculate reminders
        lastTakenOnDate: LocalTime? = null // New parameter for Type A adjustment
    ): List<LocalDateTime> {
        val initialLogMsg = "calculateReminderDateTimes called with: medication.id=${medication.id}, medication.name='${medication.name}', schedule.id=${schedule.id}, schedule.scheduleType=${schedule.scheduleType}, targetDate=$targetDate, lastTakenOnDate=$lastTakenOnDate"
        Log.d(TAG, initialLogMsg)
        FileLogger.log(TAG, initialLogMsg)
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
                // Existing log for funcTag can be removed or kept if it provides additional unique value.
                // For now, I'll rely on the new comprehensive log at the function start.
                // val funcTag = "calculateReminderDateTimes[MedID: ${medication.id}, SchedID: ${schedule.id}, Date: $targetDate]"
                // Log.d("ReminderCalculator", "$funcTag: Called with lastTakenOnDate: $lastTakenOnDate")
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
                    val errorMsg = "calculateReminderDateTimes: Invalid totalIntervalMinutes (<=0) for medication ID: ${medication.id}, schedule ID: ${schedule.id}. Interval: $intervalHours hrs, $intervalMinutes mins."
                    Log.e(TAG, errorMsg)
                    FileLogger.log(TAG, errorMsg) // Log error to file
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
                        val errorMsg = "calculateReminderDateTimes: Error parsing actualDailyStart: $it for Type A. Defaulting to MIN. Med ID: ${medication.id}"
                        Log.e(TAG, errorMsg, e)
                        FileLogger.log(TAG, errorMsg, e)
                        LocalTime.MIN
                    }
                } ?: LocalTime.MIN

                val actualDailyEnd = parsedDailyEndTimeStr?.takeIf { it.isNotBlank() }?.let {
                    try { LocalTime.parse(it, timeStorableFormatter) } catch (e: Exception) {
                        val errorMsg = "calculateReminderDateTimes: Error parsing actualDailyEnd: $it for Type A. Defaulting to MAX. Med ID: ${medication.id}"
                        Log.e(TAG, errorMsg, e)
                        FileLogger.log(TAG, errorMsg, e)
                        LocalTime.MAX
                    }
                } ?: LocalTime.MAX

                val typeAIntervalLog = "calculateReminderDateTimes: Type A interval. totalIntervalMinutes=$totalIntervalMinutes, actualDailyStart=$actualDailyStart, actualDailyEnd=$actualDailyEnd"
                Log.d(TAG, typeAIntervalLog)
                FileLogger.log(TAG, typeAIntervalLog)

                var loopStartAnchorTime = actualDailyStart
                if (lastTakenOnDate != null) {
                    val lastTakenLog = "calculateReminderDateTimes: lastTakenOnDate is provided: $lastTakenOnDate."
                    Log.i(TAG, lastTakenLog)
                    FileLogger.log(TAG, lastTakenLog)
                    val potentialStartTimeAfterTaken = lastTakenOnDate.plusMinutes(totalIntervalMinutes.toLong())
                    val potentialStartLog = "calculateReminderDateTimes: potentialStartTimeAfterTaken from lastTakenOnDate: $potentialStartTimeAfterTaken"
                    Log.d(TAG, potentialStartLog)
                    FileLogger.log(TAG, potentialStartLog)

                    if (!potentialStartTimeAfterTaken.isAfter(actualDailyEnd) && !potentialStartTimeAfterTaken.isBefore(actualDailyStart)) {
                        loopStartAnchorTime = potentialStartTimeAfterTaken
                        val adjustedLoopStartLog = "calculateReminderDateTimes: Adjusted loopStartAnchorTime to $loopStartAnchorTime based on lastTakenOnDate."
                        Log.i(TAG, adjustedLoopStartLog)
                        FileLogger.log(TAG, adjustedLoopStartLog)
                    } else if (potentialStartTimeAfterTaken.isBefore(actualDailyStart)) {
                        loopStartAnchorTime = actualDailyStart
                        val beforeWindowLog = "calculateReminderDateTimes: potentialStartTimeAfterTaken is before window. Using actualDailyStart ($actualDailyStart) as loopStartAnchorTime."
                        Log.i(TAG, beforeWindowLog)
                        FileLogger.log(TAG, beforeWindowLog)
                    } else { // potentialStartTimeAfterTaken is after actualDailyEnd
                        val afterActualEndLog = "calculateReminderDateTimes: potentialStartTimeAfterTaken ($potentialStartTimeAfterTaken) is after actualDailyEnd ($actualDailyEnd). No reminders for this day from lastTakenOnDate."
                        Log.i(TAG, afterActualEndLog)
                        FileLogger.log(TAG, afterActualEndLog)
                        val returnRemindersLog = "calculateReminderDateTimes: Returning reminders: $reminders"
                        Log.d(TAG, returnRemindersLog)
                        FileLogger.log(TAG, returnRemindersLog)
                        return emptyList()
                    }
                } else {
                    val noLastTakenLog = "calculateReminderDateTimes: No lastTakenOnDate provided. Using actualDailyStart ($actualDailyStart) as loopStartAnchorTime."
                    Log.d(TAG, noLastTakenLog)
                    FileLogger.log(TAG, noLastTakenLog)
                }
                val determinedLoopStartLog = "calculateReminderDateTimes: Determined loopStartAnchorTime: $loopStartAnchorTime"
                Log.d(TAG, determinedLoopStartLog)
                FileLogger.log(TAG, determinedLoopStartLog)

                if (loopStartAnchorTime.isAfter(actualDailyEnd)) {
                     val loopStartAfterEndLog = "calculateReminderDateTimes: loopStartAnchorTime ($loopStartAnchorTime) is already after actualDailyEnd ($actualDailyEnd). No reminders to generate."
                     Log.i(TAG, loopStartAfterEndLog)
                     FileLogger.log(TAG, loopStartAfterEndLog)
                     val returnRemindersLog2 = "calculateReminderDateTimes: Returning reminders: $reminders"
                     Log.d(TAG, returnRemindersLog2)
                     FileLogger.log(TAG, returnRemindersLog2)
                     return emptyList()
                }

                var loopTime = loopStartAnchorTime
                var iterations = 0
                val MAX_ITERATIONS_PER_DAY = (24 * 60 / totalIntervalMinutes.coerceAtLeast(1)) + 5

                val startLoopLog = "calculateReminderDateTimes: Starting Type A loop. loopTime: $loopTime, actualDailyEnd: $actualDailyEnd"
                Log.d(TAG, startLoopLog)
                FileLogger.log(TAG, startLoopLog)

                while (iterations < MAX_ITERATIONS_PER_DAY && !loopTime.isAfter(actualDailyEnd)) {
                    val reminderToAdd = LocalDateTime.of(targetDate, loopTime)
                    reminders.add(reminderToAdd)
                    val loopIterLog = "calculateReminderDateTimes: Loop iteration $iterations: loopTime=$loopTime, Added reminder: $reminderToAdd"
                    Log.d(TAG, loopIterLog)
                    FileLogger.log(TAG, loopIterLog)

                    val nextLoopTimeCandidate = loopTime.plusMinutes(totalIntervalMinutes.toLong())

                    // If next candidate time wraps around midnight (i.e., it's earlier than current loopTime)
                    // AND actualDailyEnd was not LocalTime.MAX (meaning a specific end time was set for the day), then stop.
                    // This prevents reminders from spilling into the next conceptual day if a specific end time was set.
                    if (nextLoopTimeCandidate.isBefore(loopTime) && actualDailyEnd != LocalTime.MAX) {
                        val breakLog = "calculateReminderDateTimes: Iter ${iterations}: nextLoopTimeCandidate $nextLoopTimeCandidate is before loopTime $loopTime, and actualDailyEnd $actualDailyEnd is not MAX. Breaking. ScheduleId: ${schedule.id}"
                        Log.d(TAG, breakLog)
                        FileLogger.log(TAG, breakLog)
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
        val finalReturnLog = "calculateReminderDateTimes: Returning reminders: $reminders"
        Log.d(TAG, finalReturnLog)
        FileLogger.log(TAG, finalReturnLog)
        return reminders.sorted()
    }

    fun generateRemindersForPeriod(
        medication: Medication,
        schedule: MedicationSchedule,
        periodStartDate: LocalDate,
        periodEndDate: LocalDate,
        lastTakenDateTime: LocalDateTime? = null // New optional parameter
    ): Map<LocalDate, List<LocalTime>> {
        val initialGenLog = "generateRemindersForPeriod called with: medication.id=${medication.id}, medication.name='${medication.name}', schedule.id=${schedule.id}, schedule.scheduleType=${schedule.scheduleType}, periodStartDate=$periodStartDate, periodEndDate=$periodEndDate, lastTakenDateTime=$lastTakenDateTime"
        Log.d(TAG, initialGenLog)
        FileLogger.log(TAG, initialGenLog)
        val allRemindersResult = mutableMapOf<LocalDate, MutableList<LocalTime>>()

        if (schedule.scheduleType == ScheduleType.INTERVAL) {
            val intervalProcessingLog = "generateRemindersForPeriod: Processing INTERVAL schedule. Med ID: ${medication.id}, Schedule ID: ${schedule.id}"
            Log.d(TAG, intervalProcessingLog)
            FileLogger.log(TAG, intervalProcessingLog)

            val parsedIntervalStartTime: LocalTime? = try {
                if (schedule.intervalStartTime.isNullOrBlank()) null
                else LocalTime.parse(schedule.intervalStartTime, timeStorableFormatter)
            } catch (e: Exception) {
                val parseErrorMsg = "generateRemindersForPeriod: Error parsing intervalStartTime: ${schedule.intervalStartTime}. Treating as null. Med ID: ${medication.id}"
                Log.e(TAG, parseErrorMsg, e)
                FileLogger.log(TAG, parseErrorMsg, e)
                null
            }

            if (parsedIntervalStartTime == null) { // Type B: Continuous Interval
                val typeBLog = "generateRemindersForPeriod: Continuous Interval (Type B) detected. Med ID: ${medication.id}, Schedule ID: ${schedule.id}"
                Log.i(TAG, typeBLog)
                FileLogger.log(TAG, typeBLog)

                val intervalHours = schedule.intervalHours ?: 0
                val intervalMinutes = schedule.intervalMinutes ?: 0
                val totalIntervalMinutes = (intervalHours * 60) + intervalMinutes

                if (totalIntervalMinutes <= 0) {
                    val invalidIntervalMsg = "generateRemindersForPeriod: Continuous Interval: Invalid totalIntervalMinutes (<=0) for Med ID: ${medication.id}. Interval: $intervalHours hrs, $intervalMinutes mins."
                    Log.e(TAG, invalidIntervalMsg)
                    FileLogger.log(TAG, invalidIntervalMsg)
                    return emptyMap()
                }
                val totalIntervalMinLog = "generateRemindersForPeriod: Continuous Interval: totalIntervalMinutes=$totalIntervalMinutes"
                Log.d(TAG, totalIntervalMinLog)
                FileLogger.log(TAG, totalIntervalMinLog)


                val medicationActualStartDateStr = medication.startDate
                val medStartDateStrLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, medicationActualStartDateStr=$medicationActualStartDateStr"
                Log.d(TAG, medStartDateStrLog)
                FileLogger.log(TAG, medStartDateStrLog)
                if (medicationActualStartDateStr.isNullOrBlank()) {
                    val blankStartDateLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Medication start date is null/blank. Cannot proceed."
                    Log.e(TAG, blankStartDateLog)
                    FileLogger.log(TAG, blankStartDateLog)
                    return emptyMap()
                }

                val medicationStartDateOnly: LocalDate = try {
                    LocalDate.parse(medicationActualStartDateStr, dateStorableFormatter)
                } catch (e: Exception) {
                    val parseErrorDateStr = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Error parsing medication start date string: $medicationActualStartDateStr."
                    Log.e(TAG, parseErrorDateStr, e)
                    FileLogger.log(TAG, parseErrorDateStr, e)
                    return emptyMap()
                }

                val currentDateTime = LocalDateTime.now()
                var anchorDateTime: LocalDateTime

                if (lastTakenDateTime != null) {
                    val lastTakenContLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, lastTakenDateTime is provided: $lastTakenDateTime"
                    Log.i(TAG, lastTakenContLog)
                    FileLogger.log(TAG, lastTakenContLog)
                    val nextReminderAfterTaken = lastTakenDateTime.plusMinutes(totalIntervalMinutes.toLong())
                    val nextReminderLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, calculated nextReminderAfterTaken: $nextReminderAfterTaken"
                    Log.d(TAG, nextReminderLog)
                    FileLogger.log(TAG, nextReminderLog)

                    val medicationStartAtDay = medicationStartDateOnly.atStartOfDay()
                    anchorDateTime = if (nextReminderAfterTaken.isBefore(medicationStartAtDay)) {
                        val anchorAdjustLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, nextReminderAfterTaken ($nextReminderAfterTaken) is before medication start date ($medicationStartAtDay). Using medication start date as anchor."
                        Log.w(TAG, anchorAdjustLog)
                        FileLogger.log(TAG, anchorAdjustLog)
                        medicationStartAtDay
                    } else {
                        nextReminderAfterTaken
                    }
                    val adjustedAnchorLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Using lastTakenDateTime. Adjusted anchorDateTime: $anchorDateTime"
                    Log.i(TAG, adjustedAnchorLog)
                    FileLogger.log(TAG, adjustedAnchorLog)
                } else {
                    anchorDateTime = if (medicationStartDateOnly.isEqual(currentDateTime.toLocalDate()) && !currentDateTime.toLocalTime().isBefore(LocalTime.MIN.plusSeconds(1))) {
                        val anchorToCurrentLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, No lastTaken. Medication start date is today. Anchoring to currentDateTime: $currentDateTime"
                        Log.d(TAG, anchorToCurrentLog)
                        FileLogger.log(TAG, anchorToCurrentLog)
                        currentDateTime
                    } else {
                        val anchorToStartOfDayLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, No lastTaken. Medication start date is not today ($medicationStartDateOnly) or it is today but very early. Anchoring to start of medication day: ${medicationStartDateOnly.atStartOfDay()}"
                        Log.d(TAG, anchorToStartOfDayLog)
                        FileLogger.log(TAG, anchorToStartOfDayLog)
                        medicationStartDateOnly.atStartOfDay()
                    }
                    val finalAnchorLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, No lastTaken. Original anchor logic. Final anchorDateTime = $anchorDateTime"
                    Log.d(TAG, finalAnchorLog)
                    FileLogger.log(TAG, finalAnchorLog)
                }

                val effectiveCalculationStartAnchor = if (anchorDateTime.isBefore(currentDateTime) && medicationStartDateOnly.isBefore(currentDateTime.toLocalDate())) {
                    if (currentDateTime.isAfter(anchorDateTime)) {
                         val advanceAnchorLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Anchor $anchorDateTime was in past. Med started in past. Advancing anchor to currentDateTime: $currentDateTime for generation start."
                         Log.d(TAG, advanceAnchorLog)
                         FileLogger.log(TAG, advanceAnchorLog)
                         currentDateTime
                    } else {
                         anchorDateTime
                    }
                } else {
                  anchorDateTime
                }
                anchorDateTime = effectiveCalculationStartAnchor
                val finalEffectiveAnchorLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, anchorDateTime=$anchorDateTime, effectiveCalculationStartAnchor=$effectiveCalculationStartAnchor"
                Log.d(TAG, finalEffectiveAnchorLog)
                FileLogger.log(TAG, finalEffectiveAnchorLog)


                val parsedMedicationEndDate: LocalDate? = medication.endDate?.let {
                    try { LocalDate.parse(it, dateStorableFormatter) } catch (e: Exception) { null }
                }

                var currentReminderTime = anchorDateTime
                val allGeneratedDateTimes = mutableListOf<LocalDateTime>()
                val MAX_CONTINUOUS_ITERATIONS = 10000
                var continuousIterations = 0
                val longStopDate = periodStartDate.plusYears(5)

                val startGenLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Starting generation from $currentReminderTime (derived from anchorDateTime). Interval: $totalIntervalMinutes mins. MedEndDate: $parsedMedicationEndDate. Period: $periodStartDate to $periodEndDate."
                Log.i(TAG, startGenLog)
                FileLogger.log(TAG, startGenLog)

                while (continuousIterations++ < MAX_CONTINUOUS_ITERATIONS) {
                    if (continuousIterations <= 5) {
                        val iterCandidateLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Iteration $continuousIterations, currentReminderTime candidate = $currentReminderTime"
                        Log.d(TAG, iterCandidateLog)
                        FileLogger.log(TAG, iterCandidateLog)
                    }

                    if (!currentReminderTime.isBefore(medicationStartDateOnly.atStartOfDay())) {
                         allGeneratedDateTimes.add(currentReminderTime)
                    } else {
                        val skipAddLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Iteration $continuousIterations, currentReminderTime candidate $currentReminderTime is before medication start day ${medicationStartDateOnly.atStartOfDay()}. Skipping add."
                        Log.d(TAG, skipAddLog)
                        FileLogger.log(TAG, skipAddLog)
                    }

                    val oldTimeForCheck = currentReminderTime
                    currentReminderTime = currentReminderTime.plusMinutes(totalIntervalMinutes.toLong())

                    if (currentReminderTime == oldTimeForCheck) {
                        val noAdvanceLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, currentReminderTime did not advance. Aborting. Interval: $totalIntervalMinutes."
                        Log.e(TAG, noAdvanceLog)
                        FileLogger.log(TAG, noAdvanceLog)
                        break
                    }
                    if (parsedMedicationEndDate != null && currentReminderTime.toLocalDate().isAfter(parsedMedicationEndDate)) {
                        val afterEndDateLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, next currentReminderTime ($currentReminderTime) would be after medication end date ($parsedMedicationEndDate). Stopping generation."
                        Log.d(TAG, afterEndDateLog)
                        FileLogger.log(TAG, afterEndDateLog)
                        break
                    }
                    if (currentReminderTime.toLocalDate().isAfter(longStopDate)) {
                        val exceedLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, generation exceeded 5 years from period start ($longStopDate). Aborting."
                        Log.w(TAG, exceedLog)
                        FileLogger.log(TAG, exceedLog)
                        break
                    }
                }
                val generatedRawLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Generated ${allGeneratedDateTimes.size} raw dateTimes. Now filtering for period. First 5 generated (if any): ${allGeneratedDateTimes.take(5)}"
                Log.d(TAG, generatedRawLog)
                FileLogger.log(TAG, generatedRawLog)

                var filteredRemindersCount = 0
                allGeneratedDateTimes.forEach { dt ->
                    val reminderDate = dt.toLocalDate()
                    if (!reminderDate.isBefore(periodStartDate) && !reminderDate.isAfter(periodEndDate)) {
                        if (parsedMedicationEndDate == null || !reminderDate.isAfter(parsedMedicationEndDate)) {
                            if (!dt.toLocalDate().isBefore(medicationStartDateOnly)) {
                                 if (!dt.isBefore(anchorDateTime) || dt.toLocalDate().isAfter(anchorDateTime.toLocalDate())) {
                                    allRemindersResult.getOrPut(reminderDate) { mutableListOf() }
                                        .add(dt.toLocalTime())
                                    filteredRemindersCount++
                                } else {
                                     val filterAnchorLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Filtering out $dt as it's before effective anchor time $anchorDateTime on the same day."
                                     Log.d(TAG, filterAnchorLog)
                                     FileLogger.log(TAG, filterAnchorLog)
                                }
                            } else {
                                val filterStartDateLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Filtering out $dt as it's before medication start date $medicationStartDateOnly."
                                Log.d(TAG, filterStartDateLog)
                                FileLogger.log(TAG, filterStartDateLog)
                            }
                        }
                    }
                }
                val finalFilterLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Total allGeneratedDateTimes count: ${allGeneratedDateTimes.size}, Filtered reminders count for period $periodStartDate to $periodEndDate: $filteredRemindersCount."
                Log.d(TAG, finalFilterLog)
                FileLogger.log(TAG, finalFilterLog)

            } else { // Type A: Daily Repeating Interval (intervalStartTime is present)
                val typeALogRepeating = "generateRemindersForPeriod: Daily Repeating Interval (Type A) detected. Med ID: ${medication.id}, Schedule ID: ${schedule.id}, parsedIntervalStartTime=$parsedIntervalStartTime"
                Log.i(TAG, typeALogRepeating)
                FileLogger.log(TAG, typeALogRepeating)
                var currentDateIter = periodStartDate
                while (!currentDateIter.isAfter(periodEndDate)) {
                    val lastTakenOnCurrentDateIter: LocalTime? = if (lastTakenDateTime != null && lastTakenDateTime.toLocalDate().isEqual(currentDateIter)) {
                        val passingLastTakenLog = "generateRemindersForPeriod (Type A loop): Passing lastTakenDateTime.toLocalTime() (${lastTakenDateTime.toLocalTime()}) for date $currentDateIter to calculateReminderDateTimes."
                        Log.d(TAG, passingLastTakenLog)
                        FileLogger.log(TAG, passingLastTakenLog)
                        lastTakenDateTime.toLocalTime()
                    } else {
                        null
                    }
                    val forDateLog = "generateRemindersForPeriod (Type A loop): For date $currentDateIter, lastTakenOnCurrentDateIter being passed: $lastTakenOnCurrentDateIter"
                    Log.d(TAG, forDateLog)
                    FileLogger.log(TAG, forDateLog)
                    val scheduleForDailyCalc = if(parsedIntervalStartTime != null) schedule.copy(intervalStartTime = parsedIntervalStartTime.format(timeStorableFormatter)) else schedule

                    val dailyTimes = calculateReminderDateTimes(medication, scheduleForDailyCalc, currentDateIter, lastTakenOnCurrentDateIter)
                    if (dailyTimes.isNotEmpty()) {
                        val gotDailyTimesLog = "generateRemindersForPeriod (Type A loop): For $currentDateIter, got dailyTimes: ${dailyTimes.map { it.toLocalTime() }} using lastTakenOnDate: $lastTakenOnCurrentDateIter"
                        Log.d(TAG, gotDailyTimesLog)
                        FileLogger.log(TAG, gotDailyTimesLog)
                        allRemindersResult.getOrPut(currentDateIter) { mutableListOf() }
                            .addAll(dailyTimes.map { it.toLocalTime() })
                    }
                    currentDateIter = currentDateIter.plusDays(1)
                }
            }
        } else { // Not ScheduleType.INTERVAL (DAILY, CUSTOM_ALARMS, etc.)
            val nonIntervalLog = "generateRemindersForPeriod: Processing non-INTERVAL schedule type: ${schedule.scheduleType}. Med ID: ${medication.id}"
            Log.d(TAG, nonIntervalLog)
            FileLogger.log(TAG, nonIntervalLog)
            var currentDateIter = periodStartDate
            while (!currentDateIter.isAfter(periodEndDate)) {
                val dailyTimes = calculateReminderDateTimes(medication, schedule, currentDateIter, null)
                if (dailyTimes.isNotEmpty()) {
                    allRemindersResult.getOrPut(currentDateIter) { mutableListOf() }
                        .addAll(dailyTimes.map { it.toLocalTime() })
                }
                currentDateIter = currentDateIter.plusDays(1)
            }
        }
        val totalRemindersCount = allRemindersResult.values.sumOf { it.size }
        val returnAllLog = "generateRemindersForPeriod: Returning allRemindersResult. Number of days with reminders: ${allRemindersResult.keys.size}, Total number of reminders: $totalRemindersCount. Content: $allRemindersResult"
        Log.d(TAG, returnAllLog)
        FileLogger.log(TAG, returnAllLog)
        return allRemindersResult.mapValues { entry -> entry.value.distinct().sorted() }
    }
}