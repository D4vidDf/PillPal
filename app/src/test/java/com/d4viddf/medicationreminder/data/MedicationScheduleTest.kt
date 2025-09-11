package com.d4viddf.medicationreminder.data

import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.ScheduleType
import com.d4viddf.medicationreminder.data.model.getFormattedSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class MedicationScheduleTest {

    // Helper to format time consistently with the getFormattedSchedule function
    private fun formatTime(time: LocalTime): String {
        return time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
    }

    @Test
    fun testGetFormattedSchedule_daily_singleTime() {
        val time = LocalTime.of(8, 0)
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.DAILY,
            specificTimes = listOf(time),
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        assertEquals("Daily at ${formatTime(time)}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_daily_multipleTimes() {
        val times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 30), LocalTime.of(20, 15))
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.DAILY,
            specificTimes = times,
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        val expectedTimes = times.joinToString { formatTime(it) }
        assertEquals("Daily at $expectedTimes", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_daily_nullTimes() {
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.DAILY,
            specificTimes = null,
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        assertEquals("Daily at N/A", schedule.getFormattedSchedule())
    }


    @Test
    fun testGetFormattedSchedule_weekly_validData() {
        val time = LocalTime.of(10, 0)
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.WEEKLY,
            specificTimes = listOf(time),
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            intervalHours = null,
            intervalMinutes = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        val expectedDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            .joinToString { it.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) }
        assertEquals("Weekly on $expectedDays at ${formatTime(time)}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_weekly_allDays() {
        val time = LocalTime.of(9, 0)
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.WEEKLY,
            specificTimes = listOf(time),
            daysOfWeek = DayOfWeek.values().toList(),
            intervalHours = null,
            intervalMinutes = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        val expectedDays = DayOfWeek.values()
            .joinToString { it.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) }
        assertEquals("Weekly on $expectedDays at ${formatTime(time)}", schedule.getFormattedSchedule())
    }


    @Test
    fun testGetFormattedSchedule_weekly_nullDays() {
        val time = LocalTime.of(11, 0)
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.WEEKLY,
            specificTimes = listOf(time),
            daysOfWeek = null,
            intervalHours = null,
            intervalMinutes = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        assertEquals("Weekly on N/A at ${formatTime(time)}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_weekly_nullTimes() {
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.WEEKLY,
            specificTimes = null,
            daysOfWeek = listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            intervalHours = null,
            intervalMinutes = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        val expectedDays = listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
            .joinToString { it.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) }
        assertEquals("Weekly on $expectedDays at N/A", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_weekly_emptyDays() {
        val time = LocalTime.of(11, 0)
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.WEEKLY,
            specificTimes = listOf(time),
            daysOfWeek = emptyList(),
            intervalHours = null,
            intervalMinutes = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        assertEquals("Weekly on N/A at ${formatTime(time)}", schedule.getFormattedSchedule())
    }

    // This test might need to be removed or rethought as "invalidDayFormat" isn't possible with List<DayOfWeek>
    // @Test
    // fun testGetFormattedSchedule_weekly_invalidDayFormat() {
    //     // Current implementation's mapNotNull will filter out "invalid" or ""
    //     val schedule = MedicationSchedule(
    //         medicationId = 1, scheduleType = ScheduleType.WEEKLY, specificTimes = "12:00", daysOfWeek = "1,invalid,3,,7",
    //         intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null
    //     )
    //     assertEquals("Weekly on Mon, Wed, Sun at ${formatTime("12:00")}", schedule.getFormattedSchedule())
    // }

    @Test
    fun testGetFormattedSchedule_interval_validData() {
        val startTime = LocalTime.of(8,0)
        val endTime = LocalTime.of(22,0)
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 6,
            intervalMinutes = 30,
            intervalStartTime = startTime.toString(),
            intervalEndTime = endTime.toString(),
            specificTimes = null,
            daysOfWeek = null
        )
        assertEquals("Every 6h 30m from ${formatTime(startTime)} to ${formatTime(endTime)}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_interval_nullHoursMinutes() {
        val startTime = LocalTime.of(9,0)
        val endTime = LocalTime.of(21,0)
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = null,
            intervalMinutes = null,
            intervalStartTime = startTime.toString(),
            intervalEndTime = endTime.toString(),
            specificTimes = null,
            daysOfWeek = null
        )
        assertEquals("Every 0h 0m from ${formatTime(startTime)} to ${formatTime(endTime)}", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_interval_nullStartEndTime() {
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 4,
            intervalMinutes = 0,
            intervalStartTime = null,
            intervalEndTime = null,
            specificTimes = null,
            daysOfWeek = null
        )
        assertEquals("Every 4h 0m from N/A to N/A", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_interval_zeroHoursZeroMinutes() {
        val startTime = LocalTime.of(9,0)
        val endTime = LocalTime.of(21,0)
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 0,
            intervalMinutes = 0,
            intervalStartTime = startTime.toString(),
            intervalEndTime = endTime.toString(),
            specificTimes = null,
            daysOfWeek = null
        )
        assertEquals("Every 0h 0m from ${formatTime(startTime)} to ${formatTime(endTime)}", schedule.getFormattedSchedule())
    }


    @Test
    fun testGetFormattedSchedule_asNeeded() {
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.AS_NEEDED,
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = null,
            specificTimes = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        assertEquals("As needed", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_customAlarms_validTimes() {
        val times = listOf(LocalTime.of(7,0), LocalTime.of(19,0))
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.CUSTOM_ALARMS,
            specificTimes = times,
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        val expectedTimes = times.joinToString { formatTime(it) }
        assertEquals("Custom alarms at $expectedTimes", schedule.getFormattedSchedule())
    }

    @Test
    fun testGetFormattedSchedule_customAlarms_nullTimes() {
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.CUSTOM_ALARMS,
            specificTimes = null,
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        assertEquals("Custom alarms at N/A", schedule.getFormattedSchedule())
    }
}
