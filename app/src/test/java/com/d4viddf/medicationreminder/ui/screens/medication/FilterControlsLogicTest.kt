package com.d4viddf.medicationreminder.ui.screens.medication

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class FilterControlsLogicTest {

    @Test
    fun convertMillisToLocalDate_handlesStartOfDayCorrectly() {
        // Test with a specific date at the start of the day in UTC
        val testDate = LocalDate.of(2023, 10, 26)
        val startOfDayMillisUtc = testDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        // Logic under test (simulating conversion in system's default zone)
        val convertedDate = Instant.ofEpochMilli(startOfDayMillisUtc)
            .atZone(ZoneId.systemDefault()) // Conversion happens using system default
            .toLocalDate()

        // The assertion depends on whether the test JVM's ZoneId.systemDefault() is UTC or not.
        // For consistency in tests, it's often better to test with a specific zone
        // or ensure the test environment's default zone is controlled.
        // However, the actual code uses ZoneId.systemDefault(), so we test that behavior.
        // If systemDefault is UTC, convertedDate will be testDate.
        // If systemDefault is, e.g., UTC-5, startOfDayMillisUtc (00:00 UTC) would be
        // 19:00 on the previous day (Oct 25) in that zone.
        // So, the result of toLocalDate() would be Oct 25.

        // To make the test robust irrespective of where it runs, let's be explicit:
        // What is 00:00 UTC on Oct 26, 2023, when viewed as a LocalDate in the system's default zone?
        val expectedDateInSystemZone = Instant.ofEpochMilli(startOfDayMillisUtc)
                                           .atZone(ZoneId.systemDefault())
                                           .toLocalDate()

        assertEquals(expectedDateInSystemZone, convertedDate)
    }

    @Test
    fun convertMillisToLocalDate_handlesEndOfDayCorrectly() {
        // Test with a specific date towards the end of the day in UTC
        val testDate = LocalDate.of(2023, 11, 15)
        // 23:59:59.999 UTC
        val endOfDayMillisUtc = testDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

        val convertedDate = Instant.ofEpochMilli(endOfDayMillisUtc)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        // Similar to above, what is 23:59:59 UTC on Nov 15, 2023, as a LocalDate in system default?
        // If systemDefault is UTC, it's Nov 15.
        // If systemDefault is UTC+3, it would be Nov 16, 02:59:59. So LocalDate is Nov 16.
        // If systemDefault is UTC-3, it would be Nov 15, 20:59:59. So LocalDate is Nov 15.
        val expectedDateInSystemZone = Instant.ofEpochMilli(endOfDayMillisUtc)
                                           .atZone(ZoneId.systemDefault())
                                           .toLocalDate()

        assertEquals(expectedDateInSystemZone, convertedDate)
    }

    @Test
    fun convertMillisToLocalDate_specificExample_NewYork() {
        // Example: A millisecond timestamp representing midnight UTC on Jan 1, 2024
        val midnightUtcJan12024 = LocalDate.of(2024, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        // Simulate conversion if system default was America/New_York (UTC-5 during standard time)
        val newYorkZoneId = ZoneId.of("America/New_York")
        val convertedDateInNewYork = Instant.ofEpochMilli(midnightUtcJan12024)
            .atZone(newYorkZoneId)
            .toLocalDate()

        // Midnight UTC on Jan 1 is Dec 31, 7 PM in New York (EST, UTC-5)
        assertEquals(LocalDate.of(2023, 12, 31), convertedDateInNewYork)
    }

     @Test
    fun convertMillisToLocalDate_specificExample_Tokyo() {
        // Example: A millisecond timestamp representing midnight UTC on Jan 1, 2024
        val midnightUtcJan12024 = LocalDate.of(2024, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        // Simulate conversion if system default was Asia/Tokyo (UTC+9)
        val tokyoZoneId = ZoneId.of("Asia/Tokyo")
        val convertedDateInTokyo = Instant.ofEpochMilli(midnightUtcJan12024)
            .atZone(tokyoZoneId)
            .toLocalDate()

        // Midnight UTC on Jan 1 is Jan 1, 9 AM in Tokyo
        assertEquals(LocalDate.of(2024, 1, 1), convertedDateInTokyo)
    }
}
