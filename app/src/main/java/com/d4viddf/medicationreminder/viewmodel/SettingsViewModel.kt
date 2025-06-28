package com.d4viddf.medicationreminder.viewmodel

import android.app.Application // Keep one import
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.d4viddf.medicationreminder.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.common.WorkerConstants // Added import
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker // Keep for class name
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Data
import com.d4viddf.medicationreminder.utils.FileLogger
import java.util.concurrent.TimeUnit
import java.util.Calendar
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val application: Application
) : ViewModel() {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _shareRequest = MutableSharedFlow<Intent>(replay = 0, extraBufferCapacity = 1)
    val shareRequest = _shareRequest.asSharedFlow()

    // Volume Control
    private val _currentVolume = MutableStateFlow(0)
    val currentVolume: StateFlow<Int> = _currentVolume.asStateFlow()

    private val _maxVolume = MutableStateFlow(0)
    val maxVolume: StateFlow<Int> = _maxVolume.asStateFlow()

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            Log.d("SettingsViewModel", "Volume observer onChange triggered. selfChange: $selfChange")
            val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            Log.d("SettingsViewModel", "New alarm volume read by observer: $newVolume")
            if (_currentVolume.value != newVolume) { // Only update if different to avoid potential loops if setVolume also triggers onChange
                _currentVolume.value = newVolume
                Log.d("SettingsViewModel", "_currentVolume updated to: $newVolume")
            }
        }
    }

    init {
        _maxVolume.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        _currentVolume.value = audioManager.getStreamVolume(AudioManager.STREAM_ALARM) // Initial fetch


    }

    fun setVolume(volume: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
        // Update StateFlow immediately for responsive UI.
        // If observer triggers too, the check in onChange should prevent issues.
        if (_currentVolume.value != volume) {
            _currentVolume.value = volume
        }
    }

    // This function might become redundant if the observer works reliably,
    // but can be kept for explicit UI-triggered refresh if desired.
    fun refreshCurrentVolume() {
        _currentVolume.value = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    }

    override fun onCleared() {
        super.onCleared()
        application.contentResolver.unregisterContentObserver(volumeObserver)
    }

    // Language Preference
    // val currentLanguageTag: StateFlow<String> = userPreferencesRepository.languageTagFlow
    //     .stateIn(
    //         scope = viewModelScope,
    //         started = SharingStarted.WhileSubscribed(5000),
    //         initialValue = ""
    //     ) // Remove this entire block

    // fun updateLanguageTag(newTag: String) {
    //     viewModelScope.launch {
    //         userPreferencesRepository.setLanguageTag(newTag)
    //     }
    // } // Remove this entire block

    // Theme Preference
    val currentTheme: StateFlow<String> = userPreferencesRepository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.d4viddf.medicationreminder.data.ThemeKeys.SYSTEM // Default to System
        )

    fun updateTheme(themeKey: String) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(themeKey)
        }
    }

    // Notification Sound Preference
    val currentNotificationSoundUri: StateFlow<String?> = userPreferencesRepository.notificationSoundUriFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // Default to null (no specific sound set)
        )

    fun updateNotificationSoundUri(uri: String?) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationSoundUri(uri)
        }
    }

    fun getNotificationSoundName(uriString: String?): String {
        val initialMessage = "getNotificationSoundName called with URI: $uriString"
        Log.d("SettingsViewModel", initialMessage)
        FileLogger.log("SettingsViewModel", initialMessage)

        return if (uriString.isNullOrEmpty()) {
            application.getString(R.string.settings_notification_sound_default)
        } else {
            try {
                val parseAttemptMessage = "Attempting to parse URI: $uriString"
                Log.d("SettingsViewModel", parseAttemptMessage)
                FileLogger.log("SettingsViewModel", parseAttemptMessage)

                val uri = Uri.parse(uriString) // This must be inside the try block
                val ringtone = RingtoneManager.getRingtone(application, uri)

                if (ringtone == null) {
                    val nullRingtoneMessage = "RingtoneManager.getRingtone returned null for URI: $uriString"
                    Log.w("SettingsViewModel", nullRingtoneMessage)
                    FileLogger.log("SettingsViewModelWarn", nullRingtoneMessage)
                    "Unknown or Invalid Sound"
                } else {
                    ringtone.getTitle(application) ?: run {
                        val nullTitleMessage = "Ringtone title is null for URI: $uriString"
                        Log.w("SettingsViewModel", nullTitleMessage)
                        FileLogger.log("SettingsViewModelWarn", nullTitleMessage)
                        "Unknown or Invalid Sound"
                    }
                }
            } catch (fnfe: java.io.FileNotFoundException) {
                val errorMessage = "File not found for ringtone URI: $uriString"
                Log.e("SettingsViewModel", errorMessage, fnfe)
                FileLogger.log("SettingsViewModelError", errorMessage, fnfe)
                "Unknown or Invalid Sound"
            } catch (se: SecurityException) {
                val errorMessage = "Security exception accessing ringtone URI: $uriString"
                Log.e("SettingsViewModel", errorMessage, se)
                FileLogger.log("SettingsViewModelError", errorMessage, se)
                "Unknown or Invalid Sound"
            } catch (e: Exception) { // Catch any other exceptions from Uri.parse or getTitle
                val errorMessage = "Error processing ringtone URI: $uriString"
                Log.e("SettingsViewModel", errorMessage, e)
                FileLogger.log("SettingsViewModelError", errorMessage, e)
                "Unknown or Invalid Sound"
            }
        }
    }

    fun restartDailyWorker() {
        viewModelScope.launch {
            val initialLog = "Initiating DailyReminderRefreshWorker restart..."
            Log.i("SettingsViewModel", initialLog)
            FileLogger.log("SettingsViewModel", initialLog)
            val workManager = WorkManager.getInstance(application)

            // Cancel the existing worker
            workManager.cancelUniqueWork("DailyReminderRefreshWorker")
            val cancelLog = "Cancelled existing DailyReminderRefreshWorker."
            Log.i("SettingsViewModel", cancelLog)
            FileLogger.log("SettingsViewModel", cancelLog)

            // Re-enqueue the worker
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
                .addTag("DailyReminderRefreshWorker") // Ensure the tag is consistent if used elsewhere for querying
                .build()

            workManager.enqueueUniquePeriodicWork(
                "DailyReminderRefreshWorker", // This name must match the one used for cancellation
                ExistingPeriodicWorkPolicy.REPLACE, // REPLACE ensures the new worker replaces the old one if somehow not cancelled properly
                dailyRefreshWorkRequest
            )

            val hoursUntilRun = TimeUnit.MILLISECONDS.toHours(delayUntilNextRun)
            val minutesUntilRun = TimeUnit.MILLISECONDS.toMinutes(delayUntilNextRun) % 60
            val enqueueLog = "Enqueued new DailyReminderRefreshWorker to run in approx ${hoursUntilRun}h ${minutesUntilRun}m. Delay: $delayUntilNextRun ms."
            Log.i("SettingsViewModel", enqueueLog)
            FileLogger.log("SettingsViewModel", enqueueLog)
            // Optionally, emit an event here for UI feedback (e.g., Toast)
        }
    }

    fun shareAppLogs() {
        viewModelScope.launch {
            FileLogger.log("SettingsViewModel", "shareAppLogs called")
            Log.i("SettingsViewModel", "Initiating app logs sharing...")
            try {
                val logDir = File(application.cacheDir, "logs")
                val logFile = File(logDir, "app_log.txt")

                if (logFile.exists()) {
                    val contentUri = FileProvider.getUriForFile(
                        application,
                        "${application.packageName}.fileprovider",
                        logFile
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Medication Reminder App Logs")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    val chooser = Intent.createChooser(shareIntent, "Share App Logs")
                    _shareRequest.tryEmit(chooser)
                    FileLogger.log("SettingsViewModel", "Log share intent emitted.")
                    Log.i("SettingsViewModel", "Log share intent emitted.")
                } else {
                    FileLogger.log("SettingsViewModelError", "Log file not found for sharing.")
                    Log.e("SettingsViewModel", "Log file not found for sharing.")
                    // Optionally, emit a different event/state to inform UI (e.g., Toast "Log file not found")
                    // For now, just logging. A simple Toast could be:
                    // _toastEvents.tryEmit("Log file not found.")
                }
            } catch (e: Exception) {
                FileLogger.log("SettingsViewModelError", "Error preparing log share intent", e)
                Log.e("SettingsViewModel", "Error preparing log share intent", e)
                // _toastEvents.tryEmit("Error sharing logs: ${e.message}")
            }
        }
    }
}
