package com.d4viddf.medicationreminder.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class MedicationScheduleTest {

    // Helper to format time consistently with the getFormattedSchedule function
    private fun formatTime(timeStr: String): String {
        return LocalTime.parse(timeStr).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
    }

    @Test
    fun testGetFormattedSchedule_daily_singleTime() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = "08:00",
            intervalHours = null, intervalMinutes = null, daysOfWeek = null, intervalStartTime = null, intervalEndTime = null
        )
        assertEquals("Daily at ${formatTime("08:00")}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_daily_multipleTimes() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = "08:00,14:30,20:15",
            intervalHours = null, intervalMinutes = null, daysOfWeek = null, intervalStartTime = null, intervalEndTime = null
        )
        val expectedTimes = "08:00,14:30,20:15".split(",").joinToString { formatTime(it) }
        assertEquals("Daily at $expectedTimes", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_daily_nullTimes() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = null,
            intervalHours = null, intervalMinutes = null, daysOfWeek = null, intervalStartTime = null, intervalEndTime = null
        )
        assertEquals("Daily at N/A", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_weekly_validData() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.WEEKLY, specificTimes = "10:00", daysOfWeek = "1,3,5", // Mon, Wed, Fri
            intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null
        )
        assertEquals("Weekly on Mon, Wed, Fri at ${formatTime("10:00")}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_weekly_allDays() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.WEEKLY, specificTimes = "09:00", daysOfWeek = "1,2,3,4,5,6,7",
            intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null
        )
        assertEquals("Weekly on Mon, Tue, Wed, Thu, Fri, Sat, Sun at ${formatTime("09:00")}", schedule.getFormattedSchedule())
    }


    @Test
    fun testGetFormattedSchedule_weekly_nullDays() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.WEEKLY, specificTimes = "11:00", daysOfWeek = null,
            intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null
        )
        assertEquals("Weekly on N/A at ${formatTime("11:00")}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_weekly_nullTimes() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.WEEKLY, specificTimes = null, daysOfWeek = "2,4",
            intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null
        )
        assertEquals("Weekly on Tue, Thu at N/A", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_weekly_emptyDays() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.WEEKLY, specificTimes = "11:00", daysOfWeek = "",
            intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null
        )
        // Assuming empty string for days results in "N/A" or similar, based on joinToString behavior on empty list
        assertEquals("Weekly on N/A at ${formatTime("11:00")}", schedule.getFormattedSchedule())
    }


    @Test
    fun testGetFormattedSchedule_weekly_invalidDayFormat() {
        // Current implementation's mapNotNull will filter out "invalid" or ""
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.WEEKLY, specificTimes = "12:00", daysOfWeek = "1,invalid,3,,7",
            intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null
        )
        assertEquals("Weekly on Mon, Wed, Sun at ${formatTime("12:00")}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_interval_validData() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.INTERVAL, intervalHours = 6, intervalMinutes = 30,
            intervalStartTime = "08:00", intervalEndTime = "22:00",
            specificTimes = null, daysOfWeek = null
        )
        assertEquals("Every 6h 30m from ${formatTime("08:00")} to ${formatTime("22:00")}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_interval_nullHoursMinutes() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.INTERVAL, intervalHours = null, intervalMinutes = null,
            intervalStartTime = "09:00", intervalEndTime = "21:00",
            specificTimes = null, daysOfWeek = null
        )
        assertEquals("Every 0h 0m from ${formatTime("09:00")} to ${formatTime("21:00")}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_interval_nullStartEndTime() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.INTERVAL, intervalHours = 4, intervalMinutes = 0,
            intervalStartTime = null, intervalEndTime = null,
            specificTimes = null, daysOfWeek = null
        )
        assertEquals("Every 4h 0m from N/A to N/A", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_interval_zeroHoursZeroMinutes() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.INTERVAL, intervalHours = 0, intervalMinutes = 0,
            intervalStartTime = "09:00", intervalEndTime = "21:00",
            specificTimes = null, daysOfWeek = null
        )
        assertEquals("Every 0h 0m from ${formatTime("09:00")} to ${formatTime("21:00")}", schedule.getFormattedSchedule())
    }


    @Test
    fun testGetFormattedSchedule_asNeeded() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.AS_NEEDED,
            intervalHours = null, intervalMinutes = null, daysOfWeek = null, specificTimes = null, intervalStartTime = null, intervalEndTime = null
        )
        assertEquals("As needed", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_customAlarms_validTimes() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.CUSTOM_ALARMS, specificTimes = "07:00,19:00",
            intervalHours = null, intervalMinutes = null, daysOfWeek = null, intervalStartTime = null, intervalEndTime = null
        )
        val expectedTimes = "07:00,19:00".split(",").joinToString { formatTime(it) }
        assertEquals("Custom alarms at $expectedTimes", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_customAlarms_nullTimes() {
        val schedule = MedicationSchedule(
            medicationId = 1, scheduleType = ScheduleType.CUSTOM_ALARMS, specificTimes = null,
            intervalHours = null, intervalMinutes = null, daysOfWeek = null, intervalStartTime = null, intervalEndTime = null
        )
        assertEquals("Custom alarms at N/A", schedule.getFormattedSchedule())
    }
}
