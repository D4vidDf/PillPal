package com.d4viddf.medicationreminder.common

object NotificationConstants {
    // Channel IDs
    const val REMINDER_CHANNEL_ID = "medication_reminder_channel"
    const val PRE_REMINDER_CHANNEL_ID = "pre_medication_reminder_channel"

    // Notification Actions (also see IntentActionConstants for broader actions)
    // ACTION_MARK_AS_TAKEN is used by notifications, so it's relevant here too.
    // It will also be in IntentActionConstants for central access.
    const val ACTION_MARK_AS_TAKEN = "com.d4viddf.medicationreminder.ACTION_MARK_AS_TAKEN"

    // Notification IDs
    const val PRE_REMINDER_NOTIFICATION_ID_OFFSET = 2000000 // Used in PreReminderForegroundService

    // Intent Extras for Notifications
    const val EXTRA_NOTIFICATION_TAP_REMINDER_ID = "notification_tap_reminder_id" // Used in NotificationHelper for contentIntent
    const val EXTRA_NOTIFICATION_TAP_PREREMINDER_ID = "notification_tap_prereminder_id" // Used in PreReminderForegroundService for tapIntent

    // Extras for FullScreenNotificationActivity (related to notification display)
    const val EXTRA_MED_COLOR_HEX = "extra_med_color_hex"
    const val EXTRA_MED_TYPE_NAME = "extra_med_type_name"
    // Note: EXTRA_REMINDER_ID, EXTRA_MED_NAME, EXTRA_MED_DOSAGE for FullScreenNotificationActivity
    // will be handled by the general IntentExtraConstants.kt as they are more broadly used.
}
