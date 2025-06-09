package com.d4viddf.medicationreminder.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class MedicationHistoryEntry(
    val id: String, // Can be derived from MedicationReminder.id or a combination
    val medicationName: String,
    val dateTaken: LocalDate,
    val timeTaken: LocalTime,
    val originalDateTimeTaken: LocalDateTime // For precise sorting before formatting
)
