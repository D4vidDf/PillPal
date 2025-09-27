package com.d4viddf.medicationreminder.data.converters

import com.d4viddf.medicationreminder.data.source.local.DateTimeConverters
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class DateTimeConvertersTest {

    private val converters = DateTimeConverters()

    // DayOfWeekListConverter Tests
    @Test
    fun `fromDayOfWeekList returns correct string`() {
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals("1,3,5", converters.fromDayOfWeekList(days))
    }

    @Test
    fun `fromDayOfWeekList with empty list returns empty string`() {
        assertEquals("", converters.fromDayOfWeekList(emptyList()))
    }

    @Test
    fun `fromDayOfWeekList with null returns null`() {
        assertNull(converters.fromDayOfWeekList(null))
    }

    @Test
    fun `toDayOfWeekList returns correct list`() {
        val data = "1,3,5"
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), converters.toDayOfWeekList(data))
    }

    @Test
    fun `toDayOfWeekList with empty string returns empty list`() {
        assertEquals(emptyList<DayOfWeek>(), converters.toDayOfWeekList(""))
    }

    @Test
    fun `toDayOfWeekList with null returns null`() {
        assertNull(converters.toDayOfWeekList(null))
    }

    @Test
    fun `toDayOfWeekList with invalid data returns filtered list`() {
        val data = "1,invalid,3,8,5" // 8 is invalid day
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), converters.toDayOfWeekList(data))
    }

    @Test
    fun `toDayOfWeekList with duplicate data returns distinct list`() {
        val data = "1,3,3,5,1"
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), converters.toDayOfWeekList(data))
    }

    // LocalTimeListConverter Tests
    @Test
    fun `fromLocalTimeList returns correct string`() {
        val times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 30), LocalTime.of(20, 15, 5))
        assertEquals("08:00,14:30,20:15:05", converters.fromLocalTimeList(times))
    }

    @Test
    fun `fromLocalTimeList with empty list returns empty string`() {
        assertEquals("", converters.fromLocalTimeList(emptyList()))
    }

    @Test
    fun `fromLocalTimeList with null returns null`() {
        assertNull(converters.fromLocalTimeList(null))
    }

    @Test
    fun `toLocalTimeList returns correct list`() {
        val data = "08:00,14:30,20:15:05"
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(14, 30), LocalTime.of(20, 15, 5)), converters.toLocalTimeList(data))
    }

    @Test
    fun `toLocalTimeList with empty string returns empty list`() {
        assertEquals(emptyList<LocalTime>(), converters.toLocalTimeList(""))
    }

    @Test
    fun `toLocalTimeList with null returns null`() {
        assertNull(converters.toLocalTimeList(null))
    }

    @Test
    fun `toLocalTimeList with invalid data returns filtered list`() {
        val data = "08:00,invalid,14:30,25:00" // 25:00 is invalid time
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(14, 30)), converters.toLocalTimeList(data))
    }

    @Test
    fun `toLocalTimeList with duplicate data returns distinct list`() {
        val data = "08:00,14:30,14:30,08:00:00" // 08:00:00 is same as 08:00
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(14, 30)), converters.toLocalTimeList(data))
    }
}
