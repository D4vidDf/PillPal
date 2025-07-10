package com.d4viddf.medicationreminder.wear.data

// NOTE: These are duplicates from the phone app's data package.
// Ideally, these would be in a shared module.

data class MedicationScheduleDetailSyncItem(
    val scheduleId: Long,
    val scheduleType: String, // e.g., "DAILY_SPECIFIC_TIMES", "INTERVAL"
    val specificTimes: List<String>?, // List of "HH:mm" strings
    val intervalHours: Int?,
    val intervalMinutes: Int?,
    val intervalStartTime: String?, // "HH:mm"
    val intervalEndTime: String?,   // "HH:mm"
    val dailyRepetitionDays: List<String>? // e.g., ["MONDAY", "TUESDAY"]
)

data class MedicationFullSyncItem(
    val medicationId: Int,
    val name: String,
    val dosage: String?,
    val color: String?,
    val typeName: String?,
    val typeIconUrl: String?,
    val schedules: List<MedicationScheduleDetailSyncItem>,
    val startDate: String?, // "dd/MM/yyyy"
    val endDate: String? // "dd/MM/yyyy"
)
