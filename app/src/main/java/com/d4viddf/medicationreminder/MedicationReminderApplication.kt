package com.d4viddf.medicationreminder

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory // Correct import
import androidx.work.Configuration // Correct import
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MedicationReminderApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            Log.d("MedicationReminderApp", "Providing WorkManager Configuration via property getter with HiltWorkerFactory")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        Log.d("MedicationReminderApp", "Application onCreate called.")

        // 1. Create Notification Channels (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannels(this)
            Log.d("MedicationReminderApp", "Notification channels created.")
        }

        // 2. Setup Daily Reminder Refresh Worker
        setupDailyReminderRefreshWorker()
    }

    private fun setupDailyReminderRefreshWorker() {
        val workManager = WorkManager.getInstance(applicationContext)

        val data = Data.Builder()
            .putBoolean(ReminderSchedulingWorker.KEY_IS_DAILY_REFRESH, true)
            .build()

        // Calculate delay until approximately 00:01 AM next day
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            add(Calendar.DAY_OF_YEAR, 1) // Move to tomorrow
            set(Calendar.HOUR_OF_DAY, 0) // Midnight
            set(Calendar.MINUTE, 1)      // 1 minute past midnight
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var delayUntilNextRun = calendar.timeInMillis - currentTimeMillis
        // If it's already past 00:01 for today but before next day's 00:01,
        // this delay will be positive. If somehow current time is already past
        // today's 00:01 in a way that calendar is set to "tomorrow 00:01" but that's
        // less than 24h away (e.g. current time is 00:00:30), this works.
        // If the calculated time for tomorrow 00:01 somehow ends up in the past (shouldn't happen with add(DAY_OF_YEAR,1)),
        // this would make it run immediately, which is fine for a periodic worker's first run if miscalculated.

        val dailyRefreshWorkRequest = PeriodicWorkRequestBuilder<ReminderSchedulingWorker>(
            1, TimeUnit.DAYS
        )
            .setInputData(data)
            .setInitialDelay(delayUntilNextRun, TimeUnit.MILLISECONDS)
            .addTag("DailyReminderRefreshWorker")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailyReminderRefreshWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Or REPLACE if you want to always use the latest work request config
            dailyRefreshWorkRequest
        )
        val hoursUntilRun = TimeUnit.MILLISECONDS.toHours(delayUntilNextRun)
        val minutesUntilRun = TimeUnit.MILLISECONDS.toMinutes(delayUntilNextRun) % 60
        Log.i("MedicationReminderApp", "Enqueued DailyReminderRefreshWorker to run in approx ${hoursUntilRun}h ${minutesUntilRun}m.")
    }


}