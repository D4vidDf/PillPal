package com.d4viddf.medicationreminder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MedicationReminderApplication : Application() {
    // This class is required by Hilt to generate components for dependency injection
}
