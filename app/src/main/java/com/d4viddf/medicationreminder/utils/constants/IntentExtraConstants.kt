package com.d4viddf.medicationreminder.utils.constants

object IntentExtraConstants {
    // General Extras (used by ReminderBroadcastReceiver, FullScreenNotificationActivity, etc.)
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_MEDICATION_NAME = "extra_medication_name" // Corresponds to EXTRA_MED_NAME in FullScreenNotificationActivity
    const val EXTRA_MEDICATION_DOSAGE = "extra_medication_dosage" // Corresponds to EXTRA_MED_DOSAGE in FullScreenNotificationActivity
    const val EXTRA_ACTUAL_REMINDER_TIME_MILLIS = "extra_actual_reminder_time_millis"
    const val EXTRA_IS_INTERVAL = "extra_is_interval"
    const val EXTRA_NEXT_DOSE_TIME_MILLIS = "extra_next_dose_time_millis"

    // Extras for PreReminderForegroundService (could be prefixed if shared, but seem specific enough)
    // Using more descriptive names to avoid clashes if these were ever less specific.
    // For now, keeping original names as they are distinct.
    const val EXTRA_SERVICE_REMINDER_ID = "extra_service_reminder_id"
    const val EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS = "extra_service_actual_scheduled_time_millis"
    const val EXTRA_SERVICE_MEDICATION_NAME = "extra_service_medication_name"

    // Note: Extras specific to Notification display like EXTRA_MED_COLOR_HEX, EXTRA_MED_TYPE_NAME
    // are in NotificationConstants.kt
}
