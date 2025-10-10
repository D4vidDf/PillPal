package com.d4viddf.medicationreminder.domain.usecase

import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderCalculatorTest {

    private val medication = Medication(
        id = 1,
        name = "Test Med",
        typeId = 1,
        color = "#FFFFFF",
        packageSize = 30,
        remainingDoses = 30,
        startDate = "01/01/2024",
        endDate = "31/12/2024"
    )

    @Test
    fun `calculate daily reminders`() {
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.DAILY,
            startDate = "01/01/2024",
            specificTimes = listOf(LocalTime.of(8, 0)),
            daysOfWeek = emptyList(),
            intervalHours = null,
            intervalMinutes = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        val targetDate = LocalDate.of(2024, 1, 15)
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule, targetDate)
        assertEquals(1, reminders.size)
        assertEquals(LocalDateTime.of(2024, 1, 15, 8, 0), reminders.first())
    }

    @Test
    fun `calculate custom alarms reminders`() {
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.CUSTOM_ALARMS,
            startDate = "01/01/2024",
            specificTimes = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)),
            daysOfWeek = null,
            intervalHours = null,
            intervalMinutes = null,
            intervalStartTime = null,
            intervalEndTime = null
        )
        val targetDate = LocalDate.of(2024, 2, 10)
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule, targetDate)
        assertEquals(2, reminders.size)
        assertEquals(LocalDateTime.of(2024, 2, 10, 9, 0), reminders[0])
        assertEquals(LocalDateTime.of(2024, 2, 10, 21, 0), reminders[1])
    }

    @Test
    fun `calculate interval reminders`() {
        val schedule = MedicationSchedule(
            medicationId = 1,
            scheduleType = ScheduleType.INTERVAL,
            startDate = "01/01/2024",
            intervalHours = 8,
            intervalMinutes = 0,
            intervalStartTime = "08:00",
            intervalEndTime = "22:00",
            daysOfWeek = null,
            specificTimes = null
        )
        val targetDate = LocalDate.of(2024, 3, 20)
        val reminders = ReminderCalculator.calculateReminderDateTimes(medication, schedule, targetDate)
        assertEquals(2, reminders.size)
        assertEquals(LocalDateTime.of(2024, 3, 20, 8, 0), reminders[0])
        assertEquals(LocalDateTime.of(2024, 3, 20, 16, 0), reminders[1])
    }
}