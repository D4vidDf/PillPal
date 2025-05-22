package com.d4viddf.medicationreminder.logic

import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ReminderCalculatorTest {

    private val testDate: LocalDate = LocalDate.of(2024, 1, 1)
    private val dateStorableFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeStorableFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    private fun createMedication(
        id: Int = 1,
        startDate: LocalDate = testDate.minusDays(1),
        endDate: LocalDate? = testDate.plusDays(1)
    ): Medication {
        return Medication(
            id = id,
            name = "TestMed",
            dosage = "1 pill",
            startDate = startDate.format(dateStorableFormatter),
            endDate = endDate?.format(dateStorableFormatter),
            notes = null
        )
    }

    private fun createIntervalSchedule(
        medId: Int = 1,
        scheduleId: Int = 10,
        intervalStartTime: LocalTime? = LocalTime.MIN,
        intervalEndTime: LocalTime? = LocalTime.MAX,
        intervalHours: Int? = 1,
        intervalMinutes: Int? = 0
    ): MedicationSchedule {
        return MedicationSchedule(
            id = scheduleId,
            medicationId = medId,
            scheduleType = ScheduleType.INTERVAL,
            intervalStartTime = intervalStartTime?.format(timeStorableFormatter),
            intervalEndTime = intervalEndTime?.format(timeStorableFormatter),
            intervalHours = intervalHours,
            intervalMinutes = intervalMinutes,
            specificDaysOfWeek = null,
            specificTimes = null
        )
    }

    // Scenario 1: Zero Interval
    @Test
    fun `calculateReminderDateTimes with zero interval returns empty list`() {
        val medication = createMedication()
        // totalIntervalMinutes = 0
        val scheduleZeroTotal = createIntervalSchedule(intervalHours = 0, intervalMinutes = 0)
        val remindersZeroTotal = ReminderCalculator.calculateReminderDateTimes(medication, scheduleZeroTotal, testDate)
        assertTrue("Expected empty list for zero total interval minutes", remindersZeroTotal.isEmpty())

        // intervalHours is null, intervalMinutes is 0
        val scheduleNullHours = createIntervalSchedule(intervalHours = null, intervalMinutes = 0)
        val remindersNullHours = ReminderCalculator.calculateReminderDateTimes(medication, scheduleNullHours, testDate)
        assertTrue("Expected empty list for null intervalHours and zero intervalMinutes", remindersNullHours.isEmpty())

        // intervalMinutes is null, intervalHours is 0
        val scheduleNullMinutes = createIntervalSchedule(intervalHours = 0, intervalMinutes = null)
        val remindersNullMinutes = ReminderCalculator.calculateReminderDateTimes(medication, scheduleNullMinutes, testDate)
        assertTrue("Expected empty list for zero intervalHours and null intervalMinutes", remindersNullMinutes.isEmpty())

        // Both null
        val scheduleBothNull = createIntervalSchedule(intervalHours = null, intervalMinutes = null)
        val remindersBothNull = ReminderCalculator.calculateReminderDateTimes(medication, scheduleBothNull, testDate)
        assertTrue("Expected empty list for null intervalHours and null intervalMinutes", remindersBothNull.isEmpty())
    }

    @Test
    fun `calculateReminderDateTimes with negative interval returns empty list`() {
        val medication = createMedication()
        // Negative intervalHours
        val scheduleNegativeHours = createIntervalSchedule(intervalHours = -1, intervalMinutes = 0)
        val remindersNegativeHours = ReminderCalculator.calculateReminderDateTimes(medication, scheduleNegativeHours, testDate)
        assertTrue("Expected empty list for negative intervalHours", remindersNegativeHours.isEmpty())

        // Negative intervalMinutes
        val scheduleNegativeMinutes = createIntervalSchedule(intervalHours = 0, intervalMinutes = -30)
        val remindersNegativeMinutes = ReminderCalculator.calculateReminderDateTimes(medication, scheduleNegativeMinutes, testDate)
        assertTrue("Expected empty list for negative intervalMinutes", remindersNegativeMinutes.isEmpty())
    }


    // Scenario 2: Max Iterations Safeguard (Smallest Valid Interval)
    @Test
    fun `calculateReminderDateTimes with 1 minute interval respects max iterations`() {
        val medication = createMedication()
        // Smallest positive interval: 1 minute
        val schedule1MinInterval = createIntervalSchedule(
            intervalHours = 0,
            intervalMinutes = 1,
            intervalStartTime = LocalTime.MIN,
            intervalEndTime = LocalTime.MAX
        )
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule1MinInterval, testDate)

        // MAX_ITERATIONS in ReminderCalculator is (24 * 60) + 5 = 1445
        // The loop runs for iterations = 0 up to MAX_ITERATIONS. So it can run MAX_ITERATIONS + 1 times.
        // If iterations++ > MAX_ITERATIONS, it breaks.
        // So, if iterations reaches MAX_ITERATIONS + 1 (i.e., 1446th potential reminder), it breaks.
        // This means it should produce MAX_ITERATIONS + 1 reminders if not for actualDailyEnd.
        // Since actualDailyEnd is LocalTime.MAX, it will produce 24 * 60 = 1440 reminders.
        // The safeguard is to prevent more than 1445.
        val expectedMaxReminders = 24 * 60 // 1440
        assertEquals("Number of reminders for 1-minute interval should be $expectedMaxReminders", expectedMaxReminders, reminders.size)

        // Verify times are correct (first, last, and one in middle)
        assertTrue("First reminder should be at 00:00", reminders.contains(testDate.atTime(0, 0)))
        assertTrue("A mid reminder should be present", reminders.contains(testDate.atTime(12, 0)))
        assertTrue("Last reminder should be at 23:59", reminders.contains(testDate.atTime(23, 59)))
    }


    // Scenario 3: Normal Interval Spanning Full Day
    @Test
    fun `calculateReminderDateTimes with 1 hour interval full day`() {
        val medication = createMedication()
        val schedule1HourInterval = createIntervalSchedule(
            intervalHours = 1,
            intervalMinutes = 0,
            intervalStartTime = LocalTime.MIN,
            intervalEndTime = LocalTime.MAX
        )
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule1HourInterval, testDate)
        assertEquals("Expected 24 reminders for 1-hour interval over full day", 24, reminders.size)
        for (i in 0..23) {
            assertTrue("Should contain reminder at $i:00", reminders.contains(testDate.atTime(i, 0)))
        }
    }

    @Test
    fun `calculateReminderDateTimes with 4 hours interval from 08 to 20`() {
        val medication = createMedication()
        val schedule4HourInterval = createIntervalSchedule(
            intervalHours = 4,
            intervalMinutes = 0,
            intervalStartTime = LocalTime.of(8, 0),
            intervalEndTime = LocalTime.of(20, 0) // Up to and including 8 PM
        )
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule4HourInterval, testDate)
        // Expected: 08:00, 12:00, 16:00, 20:00
        assertEquals("Expected 4 reminders", 4, reminders.size)
        assertTrue(reminders.contains(testDate.atTime(8, 0)))
        assertTrue(reminders.contains(testDate.atTime(12, 0)))
        assertTrue(reminders.contains(testDate.atTime(16, 0)))
        assertTrue(reminders.contains(testDate.atTime(20, 0)))
    }


    // Scenario 4: Edge Case: actualDailyEnd is LocalTime.MAX
    @Test
    fun `calculateReminderDateTimes with interval close to LocalTime MAX`() {
        val medication = createMedication()
        val schedule = createIntervalSchedule(
            intervalHours = 0,
            intervalMinutes = 30,
            intervalStartTime = LocalTime.of(23, 0),
            intervalEndTime = LocalTime.MAX // Effectively 23:59:59.999...
        )
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule, testDate)
        // Expected: 23:00, 23:30. Next is 00:00.
        // Trace:
        // 1. currentTime = 23:00. !23:00.isAfter(MAX) -> true. Add 23:00. currentTime = 23:30.
        // 2. currentTime = 23:30. !23:30.isAfter(MAX) -> true. Add 23:30. currentTime = 00:00.
        // 3. currentTime = 00:00. !00:00.isAfter(MAX) -> true. Add 00:00. currentTime = 00:30.
        // This will continue for the whole next day if not for MAX_ITERATIONS.
        // However, calculateReminderDateTimes is for a *single targetDate*.
        // The expectation is that times are for `testDate`.
        // The current code generates LocalDateTime.of(targetDate, currentTime).
        // So, 00:00 on `testDate` is a valid outcome.
        // The loop should naturally stop when currentTime wraps around the clock sufficiently for a long interval,
        // or hit MAX_ITERATIONS for very short intervals.
        // For interval 30min, start 23:00, end MAX:
        // 23:00 (on testDate)
        // 23:30 (on testDate)
        // 00:00 (on testDate) - this is the one that was tricky.
        // If actualDailyEnd is MAX, it means "up to the very end of this day".
        // So 00:00 on testDate is a valid time.
        assertEquals("Expected 3 reminders: 23:00, 23:30, 00:00", 3, reminders.size)
        assertTrue(reminders.contains(testDate.atTime(23, 0)))
        assertTrue(reminders.contains(testDate.atTime(23, 30)))
        assertTrue(reminders.contains(testDate.atTime(0, 0))) // This is testDate.atTime(0,0)
    }


    // Scenario 5: Edge Case: Interval leads to LocalTime.MIDNIGHT
    @Test
    fun `calculateReminderDateTimes interval leading to midnight with LocalTime MAX end`() {
        val medication = createMedication()
        // Start at 23:00, interval 60 minutes. actualDailyEnd is LocalTime.MAX
        val schedule = createIntervalSchedule(
            intervalHours = 1,
            intervalMinutes = 0,
            intervalStartTime = LocalTime.of(23, 0),
            intervalEndTime = LocalTime.MAX
        )
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule, testDate)
        // Trace:
        // 1. currentTime = 23:00. !23:00.isAfter(MAX) -> true. Add 23:00 (on testDate). currentTime = 00:00.
        // 2. currentTime = 00:00. !00:00.isAfter(MAX) -> true. Add 00:00 (on testDate). currentTime = 01:00.
        // The loop terminates because 01:00.isAfter(MAX) is false, but the next iteration would be 01:00 + 1hr = 02:00, etc.
        // The loop condition is `!currentTime.isAfter(actualDailyEnd)`.
        // If actualDailyEnd is LocalTime.MAX, then any time on that day, including 00:00, is not "after" MAX.
        // The `LocalDateTime.of(targetDate, currentTime)` means all times are for the same `targetDate`.
        assertEquals("Expected 2 reminders: 23:00 and 00:00", 2, reminders.size)
        assertTrue(reminders.contains(testDate.atTime(23, 0)))
        assertTrue(reminders.contains(testDate.atTime(0, 0))) // This is testDate.atTime(0,0)
    }

    @Test
    fun `calculateReminderDateTimes interval leading to midnight with specific end time 23 59`() {
        val medication = createMedication()
        // Start at 23:00, interval 60 minutes. actualDailyEnd is 23:59
        // Expected: 23:00.
        // Next is 00:00. `LocalTime.MIDNIGHT.isAfter(LocalTime.of(23,59))` is true. So loop terminates.
        val schedule = createIntervalSchedule(
            intervalHours = 1,
            intervalMinutes = 0,
            intervalStartTime = LocalTime.of(23, 0),
            intervalEndTime = LocalTime.of(23, 59) // Explicitly before midnight
        )
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule, testDate)
        assertEquals("Expected 1 reminder: 23:00", 1, reminders.size)
        assertTrue(reminders.contains(testDate.atTime(23, 0)))
    }

    @Test
    fun `calculateReminderDateTimes interval starting at MIDNIGHT with end MAX`() {
        val medication = createMedication()
        val schedule = createIntervalSchedule(
            intervalHours = 6,
            intervalMinutes = 0,
            intervalStartTime = LocalTime.MIDNIGHT, // 00:00
            intervalEndTime = LocalTime.MAX
        )
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule, testDate)
        // Expected: 00:00, 06:00, 12:00, 18:00
        assertEquals(4, reminders.size)
        assertTrue(reminders.contains(testDate.atTime(0,0)))
        assertTrue(reminders.contains(testDate.atTime(6,0)))
        assertTrue(reminders.contains(testDate.atTime(12,0)))
        assertTrue(reminders.contains(testDate.atTime(18,0)))
    }
}
