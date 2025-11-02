package com.d4viddf.medicationreminder.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

    fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    fun parseDate(dateString: String): LocalDate {
        return LocalDate.parse(dateString, dateFormatter)
    }
}
