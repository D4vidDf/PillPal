package com.d4viddf.medicationreminder.data

// Represents the detailed schedule information for a medication for syncing to wear
data class MedicationScheduleDetailSyncItem(
    val scheduleId: Long,
    val scheduleType: String, // e.g., "DAILY_SPECIFIC_TIMES", "INTERVAL"
    val specificTimes: List<String>?, // List of "HH:mm" strings for specific times
    val intervalHours: Int?,
    val intervalMinutes: Int?,
    val intervalStartTime: String?, // "HH:mm" for interval start
    val intervalEndTime: String?,   // "HH:mm" for interval end
    val dailyRepetitionDays: List<String>? // e.g., ["MONDAY", "TUESDAY"] for daily specific times
    // Add any other relevant fields from MedicationSchedule.kt
)

// Represents a medication with its full schedule details for syncing
data class MedicationFullSyncItem(
    val medicationId: Int,
    val name: String,
    val dosage: String?,
    val color: String?, // color name or hex
    val typeName: String?, // e.g., "Pill", "Syrup" from MedicationType
    val typeIconUrl: String?, // from MedicationType
    val schedules: List<MedicationScheduleDetailSyncItem>,
    val startDate: String?, // "dd/MM/yyyy"
    val endDate: String? // "dd/MM/yyyy"
)
