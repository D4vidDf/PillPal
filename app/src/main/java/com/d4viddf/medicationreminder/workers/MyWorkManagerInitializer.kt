package com.d4viddf.medicationreminder.workers

import androidx.work.Configuration
import androidx.work.WorkManager

class MyWorkManagerInitializer : DummyContentProvider() {
    override fun onCreate(): Boolean {
        WorkManager.initialize(context!!, Configuration.Builder().build())
        //run your tasks here
        return true
    }
}