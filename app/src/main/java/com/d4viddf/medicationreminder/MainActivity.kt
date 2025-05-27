package com.d4viddf.medicationreminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
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

    private lateinit var requestFullScreenIntentLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG_MAIN_ACTIVITY = "MainActivity" // For logging
    }
    // Tracks the locale tag this Activity instance last successfully applied or attempted to apply.
    private var localeTagSetByThisInstance: String? = null

    @SuppressLint("FlowOperatorInvokedInComposition")
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestFullScreenIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // After returning from settings, re-check the permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+ for canUseFullScreenIntent
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.canUseFullScreenIntent()) {
                    Log.i(TAG_MAIN_ACTIVITY, "USE_FULL_SCREEN_INTENT permission is now granted after returning from settings.")
                } else {
                    Log.w(TAG_MAIN_ACTIVITY, "USE_FULL_SCREEN_INTENT permission is still NOT granted after returning from settings.")
                }
            }
        }
        // Step 1: Fetch the stored language preference.
        // Your UserPreferencesRepository defaults to system language if DataStore is empty/key not found.
        val storedLocaleTag = runBlocking { userPreferencesRepository.languageTagFlow.first() }
        Log.d("MainActivity", "onCreate: Initial locale tag from DataStore: '$storedLocaleTag'")

        // Step 2: Apply this locale using AppCompatDelegate.
        // This ensures the app attempts to set its preferred language early.
        // If storedLocaleTag is empty (which it shouldn't be with your current repo default),
        // forLanguageTags("") would result in an empty LocaleListCompat, effectively system default.
        val localeListToApply = LocaleListCompat.forLanguageTags(storedLocaleTag)

        // Only call setApplicationLocales if it's actually different from what AppCompat currently has,
        // OR if localeTagSetByThisInstance is null (first time after a full app kill, for example).
        // This is an attempt to reduce redundant calls if the system already matches.
        // However, given AppCompatDelegate.getApplicationLocales() returns '', this check might be tricky.
        // The more direct approach is to always set it from our source of truth (DataStore)
        // and rely on localeTagSetByThisInstance to break loops in LaunchedEffect.
        AppCompatDelegate.setApplicationLocales(localeListToApply)
        localeTagSetByThisInstance = storedLocaleTag // Track the tag we just instructed AppCompat to use.
        Log.d("MainActivity", "onCreate: Called AppCompatDelegate.setApplicationLocales with '${localeListToApply.toLanguageTags()}'. Tracking as '$localeTagSetByThisInstance'.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannels(this)
        }
        requestPostNotificationPermission()
        checkAndRequestExactAlarmPermission()
        checkAndRequestFullScreenIntentPermission() // Call the new check method


        val testWorkRequest = OneTimeWorkRequestBuilder<TestSimpleWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(testWorkRequest)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themePreference by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeKeys.SYSTEM)

            val userPreferenceTagFromFlow by userPreferencesRepository.languageTagFlow
                .distinctUntilChanged()
                .collectAsState(initial = storedLocaleTag)

            LaunchedEffect(userPreferenceTagFromFlow) {
                Log.d("MainActivity", "LanguageEffect: DataStore emitted '$userPreferenceTagFromFlow'. Locale last set by this instance: '$localeTagSetByThisInstance'.")

                // Critical check: Only act if the preference from DataStore has genuinely changed
                // from what this Activity instance last knew it set.
                if (userPreferenceTagFromFlow != localeTagSetByThisInstance) {
                    Log.i("MainActivity", "LanguageEffect: User preference changed in DataStore to '$userPreferenceTagFromFlow'. Last set by this instance was '$localeTagSetByThisInstance'. Applying and Recreating.")

                    val newLocaleList = LocaleListCompat.forLanguageTags(userPreferenceTagFromFlow)
                    AppCompatDelegate.setApplicationLocales(newLocaleList)
                    localeTagSetByThisInstance = userPreferenceTagFromFlow // Update tracker
                    recreate() // Recreate to apply the new language setting
                } else {
                    Log.d("MainActivity", "LanguageEffect: DataStore value '$userPreferenceTagFromFlow' matches what was last set by this instance ('$localeTagSetByThisInstance'). No action needed from LaunchedEffect.")
                }
            }

            MedicationReminderApp(
                themePreference = themePreference,
                widthSizeClass = windowSizeClass.widthSizeClass
            )
        }
    }
    private fun checkAndRequestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // NotificationManager.canUseFullScreenIntent() is API 34
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.canUseFullScreenIntent()) {
                Log.w(TAG_MAIN_ACTIVITY, "App cannot use full-screen intents. Sending user to settings.")
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    requestFullScreenIntentLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e(TAG_MAIN_ACTIVITY, "Could not open ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT settings", e)
                }
            } else {
                Log.i(TAG_MAIN_ACTIVITY, "App can use full-screen intents.")
            }
        } else {
            // For API < 34, the USE_FULL_SCREEN_INTENT permission declared in manifest is usually sufficient,
            // though behavior might vary by OEM. No special runtime check via NotificationManager.
            Log.d(TAG_MAIN_ACTIVITY, "Full-screen intent permission check not applicable for API < 34 via NotificationManager.")
        }
    }
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