package com.d4viddf.medicationreminder

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.utils.constants.WorkerConstants
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.utils.FileLogger
import com.d4viddf.medicationreminder.workers.CustomWorkerFactory
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MedicationReminderApplication : Application(), Configuration.Provider {

    @Inject // Hilt inyectará una instancia de CustomWorkerFasctory (si su constructor es @Inject o tiene un @Provides)
    lateinit var customWorkerFactory: CustomWorkerFactory

    private var originalUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    override val workManagerConfiguration: Configuration
        get() {
            Log.i("MedicationReminderApp", "WorkManager CONFIGURATION GETTER CALLED. Using CustomWorkerFasctory: $customWorkerFactory")
            if (!::customWorkerFactory.isInitialized) {
                Log.e("MedicationReminderApp", "CRITICAL: CustomWorkerFasctory NOT INITIALIZED when workManagerConfiguration was called!")
            }
            Log.d("MedicationReminderApp", "Providing WorkManager Configuration via property getter with CustomWorkerFasctory")
            return Configuration.Builder()
                .setWorkerFactory(customWorkerFactory)
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        FileLogger.initialize(this)

        // Initialize FileLogger
        FileLogger.log("MedicationReminderApp", "Application onCreate - File logging active.")


        Log.d("MedicationReminderApp", "Application onCreate called.")
        FileLogger.log("MedicationReminderApp", "Standard Log.d: Application onCreate called.")


        try {
            // Hilt inyecta customWorkerFactory aquí
            if (::customWorkerFactory.isInitialized) {
                Log.d("MedicationReminderApp", "CustomWorkerFasctory is initialized in onCreate.")
                FileLogger.log("MedicationReminderApp", "Standard Log.d: CustomWorkerFasctory is initialized in onCreate.")
            } else {
                Log.e("MedicationReminderApp", "CustomWorkerFasctory IS NOT initialized in onCreate!")
                FileLogger.log("MedicationReminderAppError", "CustomWorkerFasctory IS NOT initialized in onCreate!")
            }


            NotificationHelper.createNotificationChannels(this)
            Log.d("MedicationReminderApp", "Notification channels created.")
            FileLogger.log("MedicationReminderApp", "Standard Log.d: Notification channels created.")

            setupDailyReminderRefreshWorker()
        } catch (e: Exception) {
            Log.e("MedicationReminderApp", "Error in onCreate: ${e.message}", e)
            FileLogger.log("MedicationReminderAppError", "Error in onCreate: ${e.message}", e)
        }
    }

    private fun setupDailyReminderRefreshWorker() {
        Log.d("MedicationReminderApp", "Attempting to get WorkManager instance.")
        FileLogger.log("MedicationReminderApp", "Attempting to get WorkManager instance.")
        val workManager = WorkManager.getInstance(applicationContext)
        Log.d("MedicationReminderApp", "Successfully got WorkManager instance: $workManager")
        FileLogger.log("MedicationReminderApp", "Successfully got WorkManager instance: $workManager")

        val data = Data.Builder()
            .putBoolean(WorkerConstants.KEY_IS_DAILY_REFRESH, true)
            .build()

        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delayUntilNextRun = calendar.timeInMillis - currentTimeMillis

        val dailyRefreshWorkRequest = PeriodicWorkRequestBuilder<ReminderSchedulingWorker>(
            1, TimeUnit.DAYS
        )
            .setInputData(data)
            .setInitialDelay(delayUntilNextRun, TimeUnit.MILLISECONDS)
            .addTag("DailyReminderRefreshWorker")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailyReminderRefreshWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyRefreshWorkRequest
        )
        val hoursUntilRun = TimeUnit.MILLISECONDS.toHours(delayUntilNextRun)
        val minutesUntilRun = TimeUnit.MILLISECONDS.toMinutes(delayUntilNextRun) % 60
        val logMsg = "Enqueued ReminderSchedulingWorker (as DailyReminderRefreshWorker) to run in approx ${hoursUntilRun}h ${minutesUntilRun}m (using CustomFactory)."
        Log.i("MedicationReminderApp", logMsg)
        FileLogger.log("MedicationReminderApp", logMsg)
    }
}