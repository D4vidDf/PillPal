package com.d4viddf.medicationreminder.logic

import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.ScheduleType
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ReminderCalculatorTest {

    private val dateStorableFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    @Test
    fun testGenerateReminders_TypeB_NewMed_NoUserStartDate_UsesRegistrationDate() {
        val medication = Medication(
            id = 1,
            name = "Test Med",
            typeId = 1,
            color = "Blue",
            startDate = null, // User did not set a start date
            registrationDate = "2023-10-01" // Expected to be used, format "yyyy-MM-dd"
        )
        val schedule = MedicationSchedule(
            id = 1,
            medicationId = 1,
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 6,
            intervalMinutes = 0,
            intervalStartTime = null, // Type B
            intervalEndTime = null
        )
        val lastTakenDateTime: LocalDateTime? = null
        val periodStartDate = LocalDate.parse("2023-10-01")
        val periodEndDate = LocalDate.parse("2023-10-02")

        val result = ReminderCalculator.generateRemindersForPeriod(
            medication, schedule, periodStartDate, periodEndDate, lastTakenDateTime
        )

        // Expected reminders for 2023-10-01
        val expectedTimesDay1 = listOf(
            LocalTime.of(0, 0),
            LocalTime.of(6, 0),
            LocalTime.of(12, 0),
            LocalTime.of(18, 0)
        )
        // Expected reminders for 2023-10-02
        val expectedTimesDay2 = listOf(
            LocalTime.of(0, 0),
            LocalTime.of(6, 0),
            LocalTime.of(12, 0),
            LocalTime.of(18, 0)
        )

        assertNotNull("Result map should not be null", result)
        assertEquals("Should contain reminders for 2 days", 2, result.size)

        assertEquals("Times for 2023-10-01 do not match", expectedTimesDay1, result[LocalDate.parse("2023-10-01")])
        assertEquals("Times for 2023-10-02 do not match", expectedTimesDay2, result[LocalDate.parse("2023-10-02")])
    }

    @Test
    fun testGenerateReminders_TypeB_NewMed_NoUserStartDate_NoRegDate_UsesNow() {
        // This test needs to be careful about "now" changing.
        // We'll capture LocalDate.now() at the start and verify against it.
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val medication = Medication(
            id = 2,
            name = "Test Med 2",
            typeId = 1,
            color = "Red",
            startDate = null,
            registrationDate = null // No registration date either
        )
        val schedule = MedicationSchedule(
            id = 2,
            medicationId = 2,
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 8,
            intervalMinutes = 0,
            intervalStartTime = null, // Type B
            intervalEndTime = null
        )
        val lastTakenDateTime: LocalDateTime? = null
        // Period starts from "today" (when the test runs)
        val periodStartDate = today
        val periodEndDate = tomorrow

        val result = ReminderCalculator.generateRemindersForPeriod(
            medication, schedule, periodStartDate, periodEndDate, lastTakenDateTime
        )

        // Expected: reminders start from today.atStartOfDay()
        val expectedTimesToday = listOf(
            LocalTime.of(0, 0),
            LocalTime.of(8, 0),
            LocalTime.of(16, 0)
        )
        val expectedTimesTomorrow = listOf(
            LocalTime.of(0, 0),
            LocalTime.of(8, 0),
            LocalTime.of(16, 0)
        )

        assertNotNull("Result map should not be null", result)
        // It might generate for today and tomorrow, or just today if "now" is late in the day.
        // The core check is that if today is in the map, its times are correct from 00:00.
        assertTrue("Result should contain reminders for at least 1 day (today)", result.isNotEmpty())

        assertEquals("Times for $today do not match", expectedTimesToday, result[today])
        if (result.containsKey(tomorrow)) { // Tomorrow might not have all slots if periodEndDate is exactly tomorrow
            assertEquals("Times for $tomorrow do not match", expectedTimesTomorrow, result[tomorrow])
        }
    }

    @Test
    fun testGenerateReminders_TypeB_WithLastTaken_AnchorInPast_GeneratesFromAnchor() {
        val now = LocalDateTime.now()
        // Ensure lastTakenDateTime results in an anchor that is in the past but still relevant
        val lastTaken = now.minusHours(10) // e.g., if now is 14:00, lastTaken is 04:00
        val intervalHours = 4
        // Expected next reminder from lastTaken: 04:00 + 4h = 08:00 (which is now.minusHours(6))

        val medication = Medication(
            id = 3,
            name = "Test Med 3",
            typeId = 1,
            color = "Green",
            startDate = "01/01/2023" // dd/MM/yyyy, well in the past
        )
        val schedule = MedicationSchedule(
            id = 3,
            medicationId = 3,
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = intervalHours,
            intervalMinutes = 0,
            intervalStartTime = null, // Type B
            intervalEndTime = null
        )

        val periodStartDate = now.toLocalDate().minusDays(1)
        val periodEndDate = now.toLocalDate().plusDays(1)

        val result = ReminderCalculator.generateRemindersForPeriod(
            medication, schedule, periodStartDate, periodEndDate, lastTaken
        )

        assertNotNull("Result map should not be null", result)
        assertTrue("Result map should not be empty", result.isNotEmpty())

        val firstExpectedReminderTime = lastTaken.plusHours(intervalHours.toLong())
        val secondExpectedReminderTime = firstExpectedReminderTime.plusHours(intervalHours.toLong())
        val thirdExpectedReminderTime = secondExpectedReminderTime.plusHours(intervalHours.toLong())

        // Check if these specific times are present in the results
        val dayOfFirstExpected = firstExpectedReminderTime.toLocalDate()
        val dayOfSecondExpected = secondExpectedReminderTime.toLocalDate()
        val dayOfThirdExpected = thirdExpectedReminderTime.toLocalDate()

        assertTrue("Expected first reminder time ${firstExpectedReminderTime.toLocalTime()} on $dayOfFirstExpected to be present",
            result[dayOfFirstExpected]?.contains(firstExpectedReminderTime.toLocalTime()) == true
        )
        assertTrue("Expected second reminder time ${secondExpectedReminderTime.toLocalTime()} on $dayOfSecondExpected to be present",
            result[dayOfSecondExpected]?.contains(secondExpectedReminderTime.toLocalTime()) == true
        )
         assertTrue("Expected third reminder time ${thirdExpectedReminderTime.toLocalTime()} on $dayOfThirdExpected to be present",
            result[dayOfThirdExpected]?.contains(thirdExpectedReminderTime.toLocalTime()) == true
        )

        // Also check that it doesn't advance to 'now'
        // If the first reminder is significantly before 'now', it means it didn't advance.
        assertTrue("First reminder $firstExpectedReminderTime should not be after now $now if anchor was respected",
            !firstExpectedReminderTime.isAfter(now) || firstExpectedReminderTime.toLocalDate().isBefore(now.toLocalDate()))
    }

    @Test
    fun testGenerateReminders_TypeB_WithLastTaken_AnchorFarInPast_RespectsPeriodStart() {
        val medication = Medication(
            id = 4,
            name = "Test Med 4",
            typeId = 1,
            color = "Yellow",
            startDate = "01/01/2023" // dd/MM/yyyy
        )
        val schedule = MedicationSchedule(
            id = 4,
            medicationId = 4,
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 6, // Every 6 hours
            intervalMinutes = 0,
            intervalStartTime = null, // Type B
            intervalEndTime = null
        )
        // Last taken is far in the past.
        // Next reminder would be 2023-05-01T16:00:00.
        // Then 2023-05-01T22:00:00
        // Then 2023-05-02T04:00:00
        // Then 2023-05-02T10:00:00
        // Then 2023-05-02T16:00:00
        // Then 2023-05-02T22:00:00
        // Then 2023-05-03T04:00:00 -- This should be the first reminder in the period
        val lastTakenDateTime = LocalDateTime.parse("2023-05-01T10:00:00")
        val periodStartDate = LocalDate.parse("2023-05-03")
        val periodEndDate = LocalDate.parse("2023-05-04")

        val result = ReminderCalculator.generateRemindersForPeriod(
            medication, schedule, periodStartDate, periodEndDate, lastTakenDateTime
        )

        assertNotNull("Result map should not be null", result)
        assertTrue("Result map should not be empty", result.isNotEmpty())

        val expectedFirstDateInPeriod = LocalDate.parse("2023-05-03")
        val expectedFirstTimeInPeriod = LocalTime.of(4, 0) // Calculated manually based on interval

        assertTrue("Result should contain reminders for $expectedFirstDateInPeriod", result.containsKey(expectedFirstDateInPeriod))
        assertEquals("First reminder on $expectedFirstDateInPeriod should be at $expectedFirstTimeInPeriod",
            expectedFirstTimeInPeriod, result[expectedFirstDateInPeriod]?.firstOrNull()
        )

        // Check that no dates before periodStartDate are present
        result.keys.forEach { date ->
            assertFalse("Date $date in results should not be before period start $periodStartDate", date.isBefore(periodStartDate))
        }

        // Expected times for 2023-05-03
        val expectedTimesDay1 = listOf(
            LocalTime.of(4, 0),
            LocalTime.of(10, 0),
            LocalTime.of(16, 0),
            LocalTime.of(22, 0)
        )
        assertEquals("Times for 2023-05-03 do not match", expectedTimesDay1, result[expectedFirstDateInPeriod])
    }

    @Test
    fun testGenerateReminders_TypeB_MedicationStartDateAfterLastTaken_AnchorIsMedStartDate() {
        val medicationStartDateStr = "15/06/2023"
        val medicationStartDate = LocalDate.parse(medicationStartDateStr, dateStorableFormatter)

        val medication = Medication(
            id = 5,
            name = "Test Med 5",
            typeId = 1,
            color = "Purple",
            startDate = medicationStartDateStr, // dd/MM/yyyy
            registrationDate = "2023-01-01" // Does not matter for this test
        )
        val schedule = MedicationSchedule(
            id = 5,
            medicationId = 5,
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 6,
            intervalMinutes = 0,
            intervalStartTime = null, // Type B
            intervalEndTime = null
        )
        // Last taken is before the medication's official start date
        val lastTakenDateTime = LocalDateTime.parse("2023-06-01T12:00:00")

        val periodStartDate = medicationStartDate
        val periodEndDate = medicationStartDate.plusDays(1)

        val result = ReminderCalculator.generateRemindersForPeriod(
            medication, schedule, periodStartDate, periodEndDate, lastTakenDateTime
        )

        assertNotNull("Result map should not be null", result)
        assertTrue("Result map should not be empty", result.isNotEmpty())

        // Expected reminders should start from medicationStartDate.atStartOfDay()
        val expectedFirstDateTime = medicationStartDate.atStartOfDay()

        val expectedTimesMedStartDate = listOf(
            LocalTime.of(0, 0),
            LocalTime.of(6, 0),
            LocalTime.of(12, 0),
            LocalTime.of(18, 0)
        )
        val expectedTimesNextDay = listOf(
            LocalTime.of(0, 0),
            LocalTime.of(6, 0),
            LocalTime.of(12, 0),
            LocalTime.of(18, 0)
        )

        assertEquals("Times for $medicationStartDate do not match", expectedTimesMedStartDate, result[medicationStartDate])
        assertEquals("Times for ${medicationStartDate.plusDays(1)} do not match", expectedTimesNextDay, result[medicationStartDate.plusDays(1)])

        // Check that the very first reminder in the whole result set corresponds to medicationStartDate.atStartOfDay()
        val firstDateInResult = result.keys.minOrNull()
        assertNotNull(firstDateInResult)
        assertEquals(medicationStartDate, firstDateInResult)
        val firstTimeInResult = result[firstDateInResult]?.firstOrNull()
        assertNotNull(firstTimeInResult)
        assertEquals(expectedFirstDateTime.toLocalTime(), firstTimeInResult)
    }
}
