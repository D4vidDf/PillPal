package com.d4viddf.medicationreminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity // Using ComponentActivity is fine
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.data.ThemeKeys
import com.d4viddf.medicationreminder.data.UserPreferencesRepository
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.ui.MedicationReminderApp
import com.d4viddf.medicationreminder.workers.TestSimpleWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "POST_NOTIFICATIONS permission granted.")
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS permission denied.")
            }
        }

    // Variable to store the language tag that this MainActivity instance last attempted to apply.
    // This helps LaunchedEffect determine if a DataStore emission is a "new" change
    // or just a re-emission of the already applied value after recreation.
    private var localeTagSetByThisInstance: String? = null

    @SuppressLint("FlowOperatorInvokedInComposition")
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Fetch the stored language preference.
        // Your UserPreferencesRepository.languageTagFlow already defaults to system locale if nothing is stored.
        val storedLocaleTag = runBlocking { userPreferencesRepository.languageTagFlow.first() }
        Log.d("MainActivity", "onCreate: Initial locale tag from DataStore: '$storedLocaleTag'")

        // Step 2: Apply this locale using AppCompatDelegate.
        // This ensures the app attempts to set its preferred language early.
        // AppCompatDelegate should handle if the locale is already the current one.
        val localeListToApply = LocaleListCompat.forLanguageTags(storedLocaleTag)
        AppCompatDelegate.setApplicationLocales(localeListToApply)
        localeTagSetByThisInstance = storedLocaleTag // Track the tag we just instructed AppCompat to use.
        Log.d("MainActivity", "onCreate: Called AppCompatDelegate.setApplicationLocales with '${localeListToApply.toLanguageTags()}'. Tracking as '$localeTagSetByThisInstance'.")


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannels(this)
        }
        requestPostNotificationPermission()
        checkAndRequestExactAlarmPermission()

        val testWorkRequest = OneTimeWorkRequestBuilder<TestSimpleWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(testWorkRequest)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themePreference by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeKeys.SYSTEM)

            // Collect the language tag from DataStore.
            // Use distinctUntilChanged() to only react to actual value changes from DataStore.
            val userPreferenceTagFromFlow by userPreferencesRepository.languageTagFlow
                .distinctUntilChanged()
                .collectAsState(initial = storedLocaleTag) // Initialize with the tag read in onCreate

            LaunchedEffect(userPreferenceTagFromFlow) {
                Log.d("MainActivity", "LanguageEffect: DataStore emitted '$userPreferenceTagFromFlow'. Locale last set by this instance: '$localeTagSetByThisInstance'.")

                // Only act if the new value from DataStore is different from what this instance last set.
                // This is key to prevent loops after recreation if the flow re-emits the same value.
                if (userPreferenceTagFromFlow != localeTagSetByThisInstance) {
                    Log.i("MainActivity", "LanguageEffect: User preference changed in DataStore to '$userPreferenceTagFromFlow'. Last set was '$localeTagSetByThisInstance'. Applying and Recreating.")

                    val newLocaleList = LocaleListCompat.forLanguageTags(userPreferenceTagFromFlow)
                    AppCompatDelegate.setApplicationLocales(newLocaleList)
                    localeTagSetByThisInstance = userPreferenceTagFromFlow // Update tracker
                    recreate() // Recreate to apply the new language setting throughout the UI
                } else {
                    Log.d("MainActivity", "LanguageEffect: DataStore value '$userPreferenceTagFromFlow' is the same as what was last set by this instance ('$localeTagSetByThisInstance'). No action needed.")
                }
            }

            MedicationReminderApp(
                themePreference = themePreference,
                widthSizeClass = windowSizeClass.widthSizeClass
            )
        }
    }

    // Permission methods remain the same
    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Log.i("MainActivity", "POST_NOTIFICATIONS permission already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Log.i("MainActivity", "Showing rationale for POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Log.i("MainActivity", "Requesting POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Log.w("MainActivity", "SCHEDULE_EXACT_ALARM permission not granted. Requesting...")
                Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }.also {
                    try {
                        startActivity(it)
                    } catch (e: Exception) {
                        // Log.e("MainActivity", "Could not open ACTION_REQUEST_SCHEDULE_EXACT_ALARM settings", e)
                    }
                }
            } else {
                // Log.d("MainActivity", "SCHEDULE_EXACT_ALARM permission already granted.")
            }
        }
    }
}