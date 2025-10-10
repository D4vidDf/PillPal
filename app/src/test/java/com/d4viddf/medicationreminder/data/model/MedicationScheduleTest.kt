package com.d4viddf.medicationreminder.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class MedicationScheduleTest {

    @Test
    fun `createMedicationSchedule with all properties`() {
        val schedule = MedicationSchedule(
            id = 1,
            medicationId = 1,
            scheduleType = ScheduleType.WEEKLY,
            startDate = "2024-01-01",
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            specificTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            intervalStartTime = null,
            intervalEndTime = null
        )

        assertEquals(1, schedule.id)
        assertEquals(1, schedule.medicationId)
        assertEquals(ScheduleType.WEEKLY, schedule.scheduleType)
        assertEquals("2024-01-01", schedule.startDate)
        assertEquals(null, schedule.intervalHours)
        assertEquals(null, schedule.intervalMinutes)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), schedule.daysOfWeek)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), schedule.specificTimes)
        assertEquals(null, schedule.intervalStartTime)
        assertEquals(null, schedule.intervalEndTime)
    }

    @Test
    fun `createMedicationSchedule for interval`() {
        val schedule = MedicationSchedule(
            id = 2,
            medicationId = 2,
            scheduleType = ScheduleType.INTERVAL,
            startDate = "2024-01-01",
            intervalHours = 8,
            intervalMinutes = 30,
            daysOfWeek = null,
            specificTimes = null,
            intervalStartTime = "09:00",
            intervalEndTime = "21:00"
        )

        assertEquals(2, schedule.id)
        assertEquals(2, schedule.medicationId)
        assertEquals(ScheduleType.INTERVAL, schedule.scheduleType)
        assertEquals("2024-01-01", schedule.startDate)
        assertEquals(8, schedule.intervalHours)
        assertEquals(30, schedule.intervalMinutes)
        assertEquals(null, schedule.daysOfWeek)
        assertEquals(null, schedule.specificTimes)
        assertEquals("09:00", schedule.intervalStartTime)
        assertEquals("21:00", schedule.intervalEndTime)
    }
}