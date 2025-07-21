package com.d4viddf.medicationreminder.wear.data

data class WearReminder(
    val id: String, // Unique ID for this reminder instance on the watch (e.g., medId_scheduleId_day_timeKey)
    val medicationId: Int,
    val scheduleId: Long, // The ID of the schedule this reminder instance belongs to
    val underlyingReminderId: Long, // The actual ID from the phone's database `reminders` table if this instance was synced from such a record; 0L otherwise.
    val medicationName: String,
    val time: String, // Expected HH:mm format (this is the reminderTimeKey)
    var isTaken: Boolean, // Made var to be updatable in the list if needed, though ViewModel should drive this
    val dosage: String?,
    val takenAt: String? = null // ISO 8601 timestamp string when taken
)
