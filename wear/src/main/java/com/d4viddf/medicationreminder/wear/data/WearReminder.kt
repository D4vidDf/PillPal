package com.d4viddf.medicationreminder.wear.data

data class WearReminder(
    val id: String, // Unique ID for the list item on Wear (from phone's TodayScheduleItem.id)
    val underlyingReminderId: Long, // The actual ID from the phone's database MedicationReminder.id
    val medicationName: String,
    val time: String, // Expected HH:mm format from phone
    val isTaken: Boolean,
    val dosage: String? = null // Optional: if sent from phone; phone currently doesn't send dosage in TodayScheduleItem directly
    // val medicationScheduleId: Int? = null // from phone's TodayScheduleItem
    // val takenAt: String? = null // from phone's TodayScheduleItem
)
