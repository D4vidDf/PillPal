package com.d4viddf.medicationreminder.wear.presentation.components

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.TimeText
// Use M3 MaterialTheme
import androidx.wear.compose.material3.MaterialTheme
import com.d4viddf.medicationreminder.wear.presentation.KEY_FIRST_LAUNCH
import com.d4viddf.medicationreminder.wear.presentation.PREFS_NAME
import com.d4viddf.medicationreminder.wear.presentation.WearViewModel
import com.d4viddf.medicationreminder.wear.presentation.checkAlarmPermission
import androidx.compose.ui.tooling.preview.Preview // Added
import androidx.wear.tooling.preview.devices.WearDevices // Added
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme // Added
import android.app.Application // Added for preview ViewModel instantiation

// Removed MedicationReminderTheme import from here, will use M3 theme from presentation.theme

@Composable
fun WearApp(wearViewModel: WearViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var isFirstLaunch by remember { mutableStateOf(sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)) }
    var hasAlarmPermission by remember { mutableStateOf(checkAlarmPermission(context)) }

    val isConnected by wearViewModel.isConnectedToPhone.collectAsState()
    val reminders by wearViewModel.reminders.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            hasAlarmPermission = true
        }
    }

    // Ensure this uses the M3 theme defined in theme.Theme.kt
    com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText() // M2 TimeText, generally okay to mix for this element

            if (isFirstLaunch) {
                OnboardingScreen(
                    onDismiss = {
                        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                        isFirstLaunch = false
                        if (!hasAlarmPermission) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                permissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                            }
                        }
                    },
                    hasAlarmPermission = hasAlarmPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                        }
                    }
                )
            } else {
                if (!isConnected) {
                    DeviceNotConnectedScreen(
                        onTryConnection = {
                            wearViewModel.requestPhoneToOpenPlayStore()
                        }
                    )
                } else {
                    RemindersContent(reminders = reminders, viewModel = wearViewModel)
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MedicationReminderTheme {
        val context = LocalContext.current
        WearApp(
            wearViewModel = WearViewModel(context.applicationContext as Application)
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "WearApp Connected Preview")
@Composable
fun WearAppConnectedPreview() {
    val application = LocalContext.current.applicationContext as Application
    val previewViewModel = WearViewModel(application)
    // To show connected state with reminders, you'd ideally have a way to set
    // the ViewModel's state here, or use a fake ViewModel.
    MedicationReminderTheme {
        WearApp(
            wearViewModel = previewViewModel
        )
    }
}
