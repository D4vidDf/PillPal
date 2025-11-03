package com.d4viddf.medicationreminder.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import java.time.format.DateTimeParseException

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())


    fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    fun parseDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, dateFormatter)
        } catch (e: DateTimeParseException) {
            LocalDate.parse(dateString, isoDateFormatter)
        }
    }
}
