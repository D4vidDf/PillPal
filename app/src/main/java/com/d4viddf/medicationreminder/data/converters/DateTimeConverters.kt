package com.d4viddf.medicationreminder.data.converters

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.stream.Collectors

class DateTimeConverters {
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
            } catch (e: java.time.DateTimeException) {
                null // Handle cases where Int is not a valid DayOfWeek
            }
        }?.distinct() // Ensure no duplicate days if input was messy
    }

    @TypeConverter
    fun fromLocalTimeList(times: List<LocalTime>?): String? {
        return times?.map { it.toString() }?.joinToString(",")
    }

    @TypeConverter
    fun toLocalTimeList(data: String?): List<LocalTime>? {
        return data?.split(',')?.mapNotNull {
            try {
                LocalTime.parse(it.trim())
            } catch (e: java.time.format.DateTimeParseException) {
                null // Handle parsing errors
            }
        }?.distinct() // Ensure no duplicate times
    }
}
