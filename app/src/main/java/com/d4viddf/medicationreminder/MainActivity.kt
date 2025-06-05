package com.d4viddf.medicationreminder

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // Added
import androidx.lifecycle.lifecycleScope // Added
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.utils.PermissionUtils // Added import
import com.d4viddf.medicationreminder.data.ThemeKeys
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.ui.MedicationReminderApp
import com.d4viddf.medicationreminder.workers.TestSimpleWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow // Added
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update // Added
import kotlinx.coroutines.launch // Added (though lifecycleScope.launch is specific)
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    // Removed requestPermissionLauncher
    // Removed requestFullScreenIntentLauncher

    companion object {
        private const val TAG_MAIN_ACTIVITY = "MainActivity" // For logging
    }
    // Tracks the locale tag this Activity instance last successfully applied or attempted to apply.
    private var localeTagSetByThisInstance: String? = null
    private val isLoadingOnboardingStatus = MutableStateFlow(true)

    @SuppressLint("FlowOperatorInvokedInComposition")
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen() // Before super.onCreate()

        enableEdgeToEdge() // Added this line
        super.onCreate(savedInstanceState)

        // Initialize PermissionUtils
        PermissionUtils.init(this)

        // Logic to set isLoadingOnboardingStatus (new)
        lifecycleScope.launch {
            Log.d(TAG_MAIN_ACTIVITY, "Waiting for onboarding status from DataStore...")
            userPreferencesRepository.onboardingCompletedFlow.first() // Wait for the first emission
            Log.d(TAG_MAIN_ACTIVITY, "Onboarding status loaded.")
            isLoadingOnboardingStatus.update { false } // Set to false once loaded
        }

        // Set the condition for the splash screen (new)
        splashScreen.setKeepOnScreenCondition {
            val isLoading = isLoadingOnboardingStatus.value
            Log.d(TAG_MAIN_ACTIVITY, "setKeepOnScreenCondition check: isLoading = $isLoading")
            isLoading
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
        // REMOVE THESE LINES:
        // PermissionUtils.requestPostNotificationPermission(this)
        // PermissionUtils.checkAndRequestExactAlarmPermission(this)
        // PermissionUtils.checkAndRequestFullScreenIntentPermission(this)


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

            // Collect the actual onboarding status here, once isLoadingOnboardingStatus is false.
            val finalOnboardingCompletedState by userPreferencesRepository.onboardingCompletedFlow
                .collectAsState(initial = false) // Default to false for initial route calculation

            MedicationReminderApp(
                themePreference = themePreference,
                widthSizeClass = windowSizeClass.widthSizeClass,
                userPreferencesRepository = userPreferencesRepository,
                onboardingCompleted = finalOnboardingCompletedState // Pass the loaded boolean
            )
        }
    }
    // Removed checkAndRequestFullScreenIntentPermission
    // Removed requestPostNotificationPermission
    // Removed checkAndRequestExactAlarmPermission
}