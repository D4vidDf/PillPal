package com.d4viddf.medicationreminder.wear.presentation


// Added missing imports
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.medicationreminder.wear.presentation.components.WearApp


const val PREFS_NAME = "MedicationReminderPrefs"
const val KEY_FIRST_LAUNCH = "isFirstLaunch"

class MainActivity : ComponentActivity() {
    // Removed: private lateinit var wearViewModel: WearViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            // ViewModel is initialized here, within a Composable context
            val wearViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application))
            WearApp(wearViewModel = wearViewModel)
        }
    }
    // Removed unused requestOpenPlayStoreOnPhone method from MainActivity
}

// Removed WearApp - moved to components/WearApp.kt

// Removed RemindersContent - moved to components/RemindersContent.kt

// Removed ConnectionStatusIcon - moved to components/ConnectionStatusIcon.kt

// Removed MedicationReminderChip - moved to components/MedicationReminderChip.kt

// Removed OnboardingScreen - moved to components/OnboardingScreen.kt

// Removed DeviceNotConnectedScreen - moved to components/DeviceNotConnectedScreen.kt

// Greeting function is unused and can be removed.
// Removed Greeting

fun checkAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SCHEDULE_EXACT_ALARM
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

// Removed DefaultPreview - moved to components/WearApp.kt

// Removed OnboardingPreview - moved to components/OnboardingScreen.kt

// Removed NotConnectedPreview - moved to components/DeviceNotConnectedScreen.kt

// Removed WearAppConnectedPreview - moved to components/WearApp.kt