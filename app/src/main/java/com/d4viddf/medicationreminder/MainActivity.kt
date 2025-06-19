package com.d4viddf.medicationreminder

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box // Added
import androidx.compose.foundation.layout.fillMaxSize // Added
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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


    companion object {
        private const val TAG_MAIN_ACTIVITY = "MainActivity" // For logging
    }
    // Tracks the locale tag this Activity instance last successfully applied or attempted to apply.
    private var localeTagSetByThisInstance: String? = null
    private val onboardingStatusHolder = MutableStateFlow<Boolean?>(null)

    @SuppressLint("FlowOperatorInvokedInComposition")
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen() // Before super.onCreate()

        enableEdgeToEdge() // Added this line
        super.onCreate(savedInstanceState)

        // Initialize PermissionUtils
        PermissionUtils.init(this)

        // Logic to set isLoadingOnboardingStatus (new) -> onboardingStatusHolder
        lifecycleScope.launch {
            Log.d(TAG_MAIN_ACTIVITY, "Waiting for onboarding status from DataStore...")
            val status = userPreferencesRepository.onboardingCompletedFlow.first()
            Log.d(TAG_MAIN_ACTIVITY, "Onboarding status loaded: $status")
            onboardingStatusHolder.value = status
        }

        // Set the condition for the splash screen (new)
        splashScreen.setKeepOnScreenCondition {
            val isLoading = onboardingStatusHolder.value == null
            Log.d(TAG_MAIN_ACTIVITY, "setKeepOnScreenCondition check: isLoading = $isLoading (status is null: ${onboardingStatusHolder.value == null})")
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

        NotificationHelper.createNotificationChannels(this)


        val testWorkRequest = OneTimeWorkRequestBuilder<TestSimpleWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(testWorkRequest)

        setContent {
            val loadedOnboardingCompletedStatus by onboardingStatusHolder.collectAsState()

            if (loadedOnboardingCompletedStatus != null) {
                val windowSizeClass = calculateWindowSizeClass(this)
                val themePreference by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeKeys.SYSTEM)
                val userPreferenceTagFromFlow by userPreferencesRepository.languageTagFlow
                    .distinctUntilChanged()
                    .collectAsState(initial = storedLocaleTag)

                LaunchedEffect(userPreferenceTagFromFlow) {
                    Log.d(TAG_MAIN_ACTIVITY, "LanguageEffect: DataStore emitted '$userPreferenceTagFromFlow'. Locale last set by this instance: '$localeTagSetByThisInstance'.")
                    if (userPreferenceTagFromFlow != localeTagSetByThisInstance) {
                        Log.i(TAG_MAIN_ACTIVITY, "LanguageEffect: User preference changed in DataStore to '$userPreferenceTagFromFlow'. Last set by this instance was '$localeTagSetByThisInstance'. Recreating.")
                        recreate()
                    } else {
                        Log.d(TAG_MAIN_ACTIVITY, "LanguageEffect: DataStore value '$userPreferenceTagFromFlow' matches what was last set by this instance ('$localeTagSetByThisInstance'). No action needed from LaunchedEffect.")
                    }
                }

                MedicationReminderApp(
                    themePreference = themePreference,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                    userPreferencesRepository = userPreferencesRepository,
                    onboardingCompleted = loadedOnboardingCompletedStatus!! // Use non-null asserted value
                )
            } else {
                // While onboardingStatusHolder.value is null (splash screen is showing),
                // compose a minimal placeholder.
                Box(modifier = Modifier.fillMaxSize())
                Log.d(TAG_MAIN_ACTIVITY, "setContent: onboardingStatusHolder is null, showing placeholder Box.")
            }
        }
    }
    // Removed checkAndRequestFullScreenIntentPermission
    // Removed requestPostNotificationPermission
    // Removed checkAndRequestExactAlarmPermission
}