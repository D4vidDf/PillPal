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
import java.time.format.DateTimeParseException // Added for safety in new function

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

    // New function to get all potential slots for a day, irrespective of taken status
    fun getAllPotentialSlotsForDay(
        schedule: MedicationSchedule,
        targetDate: LocalDate
    ): List<LocalTime> {
        val slots = mutableListOf<LocalTime>()
        // Note: Medication start/end date checks are ideally done by the caller (ViewModel)
        // before calling this, or Medication object should be passed here.
        // This function focuses purely on the schedule's definition for the targetDate.

        when (schedule.scheduleType) {
            ScheduleType.DAILY -> {
                // For DAILY, daysOfWeek should be checked if present.
                // If daysOfWeek is null/empty, it implies every day.
                val dayOfWeekForTarget = targetDate.dayOfWeek.value
                val scheduledDays = schedule.daysOfWeek?.split(',')?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()

                if (scheduledDays.isEmpty() || scheduledDays.contains(dayOfWeekForTarget)) {
                    schedule.specificTimes?.split(',')?.firstOrNull()?.let { timeStr -> // DAILY usually has one time
                        try {
                            slots.add(LocalTime.parse(timeStr, timeStorableFormatter))
                        } catch (e: DateTimeParseException) {
                            Log.e(TAG, "getAllPotentialSlotsForDay (DAILY): Error parsing timeStr '$timeStr'. ScheduleId: ${schedule.id}", e)
                        }
                    }
                }
            }
            ScheduleType.CUSTOM_ALARMS -> {
                schedule.specificTimes?.split(',')?.forEach { timeStr ->
                    try {
                        slots.add(LocalTime.parse(timeStr, timeStorableFormatter))
                    } catch (e: DateTimeParseException) {
                        Log.e(TAG, "getAllPotentialSlotsForDay (CUSTOM_ALARMS): Error parsing timeStr '$timeStr'. ScheduleId: ${schedule.id}", e)
                    }
                }
            }
            ScheduleType.WEEKLY -> {
                val dayOfWeekForTarget = targetDate.dayOfWeek.value
                val scheduledDays = schedule.daysOfWeek?.split(',')?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
                if (scheduledDays.contains(dayOfWeekForTarget)) {
                    schedule.specificTimes?.split(',')?.forEach { timeStr -> // Weekly can have multiple times for selected days
                        try {
                            slots.add(LocalTime.parse(timeStr, timeStorableFormatter))
                        } catch (e: DateTimeParseException) {
                             Log.e(TAG, "getAllPotentialSlotsForDay (WEEKLY): Error parsing timeStr '$timeStr'. ScheduleId: ${schedule.id}", e)
                        }
                    }
                }
            }
            ScheduleType.INTERVAL -> {
                // This generates all potential slots for a given day based on interval parameters,
                // ignoring lastTakenTime for this specific function's purpose.
                val intervalHours = schedule.intervalHours ?: 0
                val intervalMinutes = schedule.intervalMinutes ?: 0
                val totalIntervalMinutes = (intervalHours * 60) + intervalMinutes

                if (totalIntervalMinutes > 0 && !schedule.intervalStartTime.isNullOrBlank()) { // Only for Type A intervals
                    val actualDailyStart = try {
                        LocalTime.parse(schedule.intervalStartTime, timeStorableFormatter)
                    } catch (e: Exception) {
                        Log.e(TAG, "getAllPotentialSlotsForDay (INTERVAL): Error parsing intervalStartTime '${schedule.intervalStartTime}'. ScheduleId: ${schedule.id}", e)
                        LocalTime.MIN // Fallback, though this state implies malformed schedule data
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
                            break // Wrapped around midnight with a specific end time
                        }
                        loopTime = nextLoopTimeCandidate
                        iterations++
                    }
                } else if (totalIntervalMinutes <=0) {
                    Log.e(TAG, "getAllPotentialSlotsForDay (INTERVAL): Invalid totalIntervalMinutes (<=0). ScheduleId: ${schedule.id}")
                }
                // Continuous intervals (Type B - no intervalStartTime) are not handled by this function for daily slot generation,
                // as their nature is continuous from a specific anchor, not tied to daily start/end times for slot definition.
                // The ViewModel will use generateRemindersForPeriod for Type B.
            }
            ScheduleType.AS_NEEDED -> {
                // No specific slots for AS_NEEDED
            }
        }
        return slots.distinct().sorted()
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

        // Check medication start/end dates against the period first
        val medicationOverallStartDate = medication.startDate?.let { try { LocalDate.parse(it, dateStorableFormatter) } catch (e: DateTimeParseException) { null } }
        val medicationOverallEndDate = medication.endDate?.let { try { LocalDate.parse(it, dateStorableFormatter) } catch (e: DateTimeParseException) { null } }

        if (medicationOverallStartDate != null && periodEndDate.isBefore(medicationOverallStartDate)) {
            Log.d(TAG, "generateRemindersForPeriod: Entire period is before medication start date. Med ID: ${medication.id}")
            return emptyMap()
        }
        if (medicationOverallEndDate != null && periodStartDate.isAfter(medicationOverallEndDate)) {
            Log.d(TAG, "generateRemindersForPeriod: Entire period is after medication end date. Med ID: ${medication.id}")
            return emptyMap()
        }


        if (schedule.scheduleType == ScheduleType.INTERVAL && schedule.intervalStartTime.isNullOrBlank()) {
            // Type B: Continuous Interval (intervalStartTime is null or blank)
            // This logic remains largely the same as it depends on lastTakenDateTime for its anchor.
            val typeBLog = "generateRemindersForPeriod: Processing Continuous INTERVAL (Type B). Med ID: ${medication.id}, Schedule ID: ${schedule.id}"
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
                } else { // lastTakenDateTime IS NULL
                    // NEW LOGIC: Always anchor to the very beginning of the medication's start day.
                    Log.d(TAG, "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, No lastTaken. Anchoring to start of medication day: ${medicationStartDateOnly.atStartOfDay()}")
                    anchorDateTime = medicationStartDateOnly.atStartOfDay()
                    // End of NEW LOGIC for lastTakenDateTime == null
                }

                // The effectiveCalculationStartAnchor logic below seems to handle advancing the anchor if it's in the past.
                // This part might need review in context of the new anchorDateTime, but the primary change is above.
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

                var currentReminderTime = anchorDateTime
                val allGeneratedDateTimes = mutableListOf<LocalDateTime>()
                val MAX_CONTINUOUS_ITERATIONS = 10000
                var continuousIterations = 0
                val longStopDate = periodStartDate.plusYears(5)

                val startGenLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Starting generation from $currentReminderTime (derived from anchorDateTime). Interval: $totalIntervalMinutes mins. MedEndDate: $medicationOverallEndDate. Period: $periodStartDate to $periodEndDate."
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
                    if (medicationOverallEndDate != null && currentReminderTime.toLocalDate().isAfter(medicationOverallEndDate)) {
                        val afterEndDateLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, next currentReminderTime ($currentReminderTime) would be after medication end date ($medicationOverallEndDate). Stopping generation."
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
                    if (!reminderDate.isBefore(periodStartDate) && !reminderDate.isAfter(periodEndDate)) { // Within the overall requested period
                        if (medicationOverallEndDate == null || !reminderDate.isAfter(medicationOverallEndDate)) { // Not after med's own end date
                            if (!dt.toLocalDate().isBefore(medicationStartDateOnly)) { // Not before med's own start date
                                 if (!dt.isBefore(anchorDateTime) || dt.toLocalDate().isAfter(anchorDateTime.toLocalDate())) { // After or on anchor
                                    allRemindersResult.getOrPut(reminderDate) { mutableListOf() }.add(dt.toLocalTime())
                                    filteredRemindersCount++
                                 }
                            }
                        }
                    }
                }
                val finalFilterLog = "generateRemindersForPeriod: Continuous Interval: Med ID ${medication.id}, Total allGeneratedDateTimes count: ${allGeneratedDateTimes.size}, Filtered reminders count for period $periodStartDate to $periodEndDate: $filteredRemindersCount."
                Log.d(TAG, finalFilterLog)
                FileLogger.log(TAG, finalFilterLog)

        } else { // DAILY, WEEKLY, CUSTOM_ALARMS, AS_NEEDED, or Type A INTERVAL (intervalStartTime is NOT blank)
            val otherTypeLog = "generateRemindersForPeriod: Processing non-continuous schedule (DAILY, WEEKLY, CUSTOM_ALARMS, AS_NEEDED, or Type A INTERVAL). Med ID: ${medication.id}, Schedule Type: ${schedule.scheduleType}"
            Log.d(TAG, otherTypeLog)
            FileLogger.log(TAG, otherTypeLog)

            var currentDateIter = periodStartDate
            while (!currentDateIter.isAfter(periodEndDate)) {
                // Skip if current date is outside medication's overall start/end dates
                if (medicationOverallStartDate != null && currentDateIter.isBefore(medicationOverallStartDate)) {
                    currentDateIter = currentDateIter.plusDays(1)
                    continue
                }
                if (medicationOverallEndDate != null && currentDateIter.isAfter(medicationOverallEndDate)) {
                    break // No further dates will be valid
                }

                // For Type A interval, the `lastTakenDateTime` is still relevant if we were to use
                // the old `calculateReminderDateTimes` which adjusted slots based on it.
                // However, the new approach is to get ALL potential slots for the day first.
                // So, `lastTakenDateTime` isn't directly used here for slot generation anymore,
                // but it was part of the original logic and might be used by the ViewModel.
                // For now, `getAllPotentialSlotsForDay` doesn't take it.
                val dailyTimes = getAllPotentialSlotsForDay(schedule, currentDateIter)

                if (dailyTimes.isNotEmpty()) {
                    allRemindersResult.getOrPut(currentDateIter) { mutableListOf() }.addAll(dailyTimes)
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