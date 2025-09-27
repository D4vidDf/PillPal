package com.d4viddf.medicationreminder.data.model

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

data class MedicationInfoSyncItem(
    val medicationId: Int,
    val notes: String?,
    val instructions: String?
)

data class MedicationTypeSyncItem(
    val id: Int,
    val name: String,
    val iconUrl: String?
)

data class MedicationReminderSyncItem(
    val id: Long,
    val medicationId: Int,
    val reminderTime: String,
    val isTaken: Boolean,
    val takenAt: String?
)

// Represents a medication with its full schedule details for syncing
data class MedicationFullSyncItem(
    val medicationId: Int,
    val name: String,
    val dosage: String?,
    val color: String?,
    val type: MedicationTypeSyncItem?,
    val info: MedicationInfoSyncItem?,
    val schedules: List<MedicationScheduleDetailSyncItem>,
    val reminders: List<MedicationReminderSyncItem>,
    val startDate: String?, // "dd/MM/yyyy"
    val endDate: String? // "dd/MM/yyyy"
)
