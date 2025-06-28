package com.d4viddf.medicationreminder.common

object WorkerConstants {
    // For ReminderSchedulingWorker
    const val WORK_NAME_PREFIX = "ReminderSchedulingWorker_"
    const val KEY_MEDICATION_ID = "medication_id"
    const val KEY_IS_DAILY_REFRESH = "is_daily_refresh"

    // Feature flags/configurations related to workers or background tasks
    const val ENABLE_PRE_REMINDER_NOTIFICATION_FEATURE = true
    const val PRE_REMINDER_OFFSET_MINUTES = 60L // Configurable: 60 minutes antes
}
