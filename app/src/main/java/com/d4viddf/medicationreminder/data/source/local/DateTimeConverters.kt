package com.d4viddf.medicationreminder.data.source.local

import androidx.room.TypeConverter
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException

class DateTimeConverters {
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.toString()
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            try {
                LocalDateTime.parse(it)
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }

    @TypeConverter
    fun fromDayOfWeekList(daysOfWeek: List<DayOfWeek>?): String? {
        return daysOfWeek?.map { it.value }?.joinToString(",")
    }

    @TypeConverter
    fun toDayOfWeekList(data: String?): List<DayOfWeek>? {
        return data?.split(',')?.mapNotNull {
            try {
                DayOfWeek.of(it.trim().toInt())
            } catch (e: NumberFormatException) {
                null // Handle cases where parsing to Int fails
            } catch (e: DateTimeException) {
                null // Handle cases where Int is not a valid DayOfWeek
            }
        }?.distinct() // Ensure no duplicate days if input was messy
    }

    @TypeConverter
    fun fromLocalTimeList(times: List<LocalTime>?): String? {
        return times?.joinToString(",") { it.toString() }
    }

    @TypeConverter
    fun toLocalTimeList(data: String?): List<LocalTime>? {
        return data?.split(',')?.mapNotNull {
            try {
                LocalTime.parse(it.trim())
            } catch (e: DateTimeParseException) {
                null // Handle parsing errors
            }
        }?.distinct() // Ensure no duplicate times
    }
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}