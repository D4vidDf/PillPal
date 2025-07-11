package com.d4viddf.medicationreminder.wear.presentation

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
// Import M3 components where needed - will replace M2 ones
import androidx.wear.compose.material3.Button as M3Button
import androidx.wear.compose.material3.Text as M3Text
import androidx.wear.compose.material.TimeText // Keep M2 TimeText for now unless M3 version is specifically required by design
import androidx.wear.compose.material.MaterialTheme as M2MaterialTheme // Alias M2 theme if still used by other parts
import androidx.wear.compose.material.Chip as M2Chip // Alias M2 Chip
import androidx.wear.compose.material.ChipDefaults as M2ChipDefaults // Alias M2 ChipDefaults
import androidx.wear.compose.material.Icon as M2Icon // Alias M2 Icon
import androidx.wear.compose.material.ScalingLazyColumn as M2ScalingLazyColumn // Alias M2 ScalingLazyColumn
import androidx.wear.compose.material.items as m2Items // Alias M2 items
import androidx.wear.compose.material.rememberScalingLazyListState as rememberM2ScalingLazyListState // Alias M2 rememberScalingLazyListState


import androidx.wear.compose.foundation.lazy.ScalingLazyColumn // M3-compatible foundation component
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState // M3-compatible foundation component


import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import com.d4viddf.medicationreminder.wear.data.WearReminder // Import for WearReminder data class

// Added missing imports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.wear.presentation.components.ConnectionStatusIcon
import com.d4viddf.medicationreminder.wear.presentation.components.MedicationReminderChip
import com.d4viddf.medicationreminder.wear.presentation.components.OnboardingScreen
import com.d4viddf.medicationreminder.wear.presentation.components.DeviceNotConnectedScreen
import com.d4viddf.medicationreminder.wear.presentation.components.RemindersContent
import com.d4viddf.medicationreminder.wear.presentation.components.WearApp // Added import


const val PREFS_NAME = "MedicationReminderPrefs"
const val KEY_FIRST_LAUNCH = "isFirstLaunch"

class MainActivity : ComponentActivity() {
    private lateinit var wearViewModel: WearViewModel // Declare ViewModel instance

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        // Initialize ViewModel here
        wearViewModel = viewModel(factory = WearViewModelFactory(application))

        setContent {
            WearApp(wearViewModel = wearViewModel) // Pass only the ViewModel
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