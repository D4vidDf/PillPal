package com.d4viddf.medicationreminder.common

object IntentActionConstants {
    // Actions for ReminderBroadcastReceiver
    const val ACTION_SHOW_REMINDER = "com.d4viddf.medicationreminder.ACTION_SHOW_REMINDER"
    const val ACTION_TRIGGER_PRE_REMINDER_SERVICE = "com.d4viddf.medicationreminder.ACTION_TRIGGER_PRE_REMINDER_SERVICE"
    const val ACTION_MARK_AS_TAKEN = "com.d4viddf.medicationreminder.ACTION_MARK_AS_TAKEN" // Also used by NotificationHelper
    const val ACTION_SNOOZE_REMINDER = "com.d4viddf.medicationreminder.ACTION_SNOOZE_REMINDER" // Referenced in AndroidManifest.xml

    // Actions for PreReminderForegroundService
    const val ACTION_STOP_PRE_REMINDER = "com.d4viddf.medicationreminder.ACTION_STOP_PRE_REMINDER"

    // Action for data sync
    const val ACTION_DATA_CHANGED = "com.d4viddf.medicationreminder.ACTION_DATA_CHANGED"
}
