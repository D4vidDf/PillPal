package com.d4viddf.medicationreminder

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
// import androidx.appcompat.app.AppCompatDelegate // Removed
import androidx.compose.foundation.layout.Box // Added
import androidx.compose.foundation.layout.fillMaxSize // Added
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
// import androidx.compose.runtime.LaunchedEffect // Removed
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
// import androidx.core.os.LocaleListCompat // Removed
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
// import kotlinx.coroutines.flow.distinctUntilChanged // Removed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update // Added
import kotlinx.coroutines.launch // Added (though lifecycleScope.launch is specific)
// import kotlinx.coroutines.runBlocking // Removed as it was only used by locale code
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository


    companion object {
        private const val TAG_MAIN_ACTIVITY = "MainActivity" // For logging
    }
    // Tracks the locale tag this Activity instance last successfully applied or attempted to apply.
    // private var localeTagSetByThisInstance: String? = null // Remove this line
    private val onboardingStatusHolder = MutableStateFlow<Boolean?>(null)

    @SuppressLint("FlowOperatorInvokedInComposition")
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen() // 1.
        enableEdgeToEdge() // 2.

        Log.i(TAG_MAIN_ACTIVITY, "onCreate: Calling super.onCreate()") // 3.
        super.onCreate(savedInstanceState) // 4.
        Log.i(TAG_MAIN_ACTIVITY, "onCreate: super.onCreate() finished") // 5.

        // userPreferencesRepository is now available after super.onCreate()
        // Log.i(TAG_MAIN_ACTIVITY, "onCreate: Initializing locale settings...") // 6.
        // val storedLocaleTag = runBlocking { userPreferencesRepository.languageTagFlow.first() } // 7.
        // Log.i(TAG_MAIN_ACTIVITY, "onCreate: Fetched storedLocaleTag from repository: '$storedLocaleTag'") // 8.
        // val localeListToApply = LocaleListCompat.forLanguageTags(storedLocaleTag) // 9.
        // Log.i(TAG_MAIN_ACTIVITY, "onCreate: Applying localeList: '${localeListToApply.toLanguageTags()}' with AppCompatDelegate.setApplicationLocales()") // 10.
        // AppCompatDelegate.setApplicationLocales(localeListToApply) // 11.
        // localeTagSetByThisInstance = storedLocaleTag // 12.
        // Log.i(TAG_MAIN_ACTIVITY, "onCreate: localeTagSetByThisInstance initialized to: '$localeTagSetByThisInstance'") // 13.
        // Log.i(TAG_MAIN_ACTIVITY, "onCreate: Locale settings initialized.") // 14.

        Log.i(TAG_MAIN_ACTIVITY, "onCreate: Initializing other utilities (PermissionUtils, NotificationHelper, WorkManager)...") // 15.
        PermissionUtils.init(this) // 16.
        NotificationHelper.createNotificationChannels(this) // 17.
        val testWorkRequest = OneTimeWorkRequestBuilder<TestSimpleWorker>().build() // 18.
        WorkManager.getInstance(applicationContext).enqueue(testWorkRequest) // 19.
        Log.i(TAG_MAIN_ACTIVITY, "onCreate: Other utilities initialized.") // 20.

        Log.i(TAG_MAIN_ACTIVITY, "onCreate: Setting up onboarding status observer...") // 21.
        lifecycleScope.launch { // 22.
            Log.d(TAG_MAIN_ACTIVITY, "Waiting for onboarding status from DataStore...")
            val status = userPreferencesRepository.onboardingCompletedFlow.first()
            Log.d(TAG_MAIN_ACTIVITY, "Onboarding status loaded: $status")
            onboardingStatusHolder.value = status
        }

        splashScreen.setKeepOnScreenCondition { // 23.
            val isLoading = onboardingStatusHolder.value == null
            Log.d(TAG_MAIN_ACTIVITY, "setKeepOnScreenCondition check: isLoading = $isLoading (status is null: ${onboardingStatusHolder.value == null})")
            isLoading
        }

        Log.i(TAG_MAIN_ACTIVITY, "onCreate: Calling setContent { ... }") // 24.
        setContent { // 25.
            val loadedOnboardingCompletedStatus by onboardingStatusHolder.collectAsState()

            if (loadedOnboardingCompletedStatus != null) {
                val windowSizeClass = calculateWindowSizeClass(this)
                val themePreference by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeKeys.SYSTEM)
                // val userPreferenceTagFromFlow by userPreferencesRepository.languageTagFlow
                //     .distinctUntilChanged()
                //     .collectAsState(initial = storedLocaleTag) // Ensure storedLocaleTag is available here

                // LaunchedEffect(userPreferenceTagFromFlow) {
                //     Log.d(TAG_MAIN_ACTIVITY, "LanguageEffect: DataStore emitted '$userPreferenceTagFromFlow'. Locale last set by this instance: '$localeTagSetByThisInstance'.")
                //     if (userPreferenceTagFromFlow != localeTagSetByThisInstance) {
                //         Log.i(TAG_MAIN_ACTIVITY, "LanguageEffect: User preference changed in DataStore to '$userPreferenceTagFromFlow'. Last set by this instance was '$localeTagSetByThisInstance'. Recreating.")
                //         recreate()
                //     } else {
                //         Log.d(TAG_MAIN_ACTIVITY, "LanguageEffect: DataStore value '$userPreferenceTagFromFlow' matches what was last set by this instance ('$localeTagSetByThisInstance'). No action needed from LaunchedEffect.")
                //     }
                // }

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