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
import androidx.wear.compose.material3.Button // M3
import androidx.wear.compose.material3.CircularProgressIndicator // M3
import androidx.wear.compose.material3.MaterialTheme // M3
import androidx.wear.compose.material3.Text // M3
import androidx.wear.compose.material3.TimeText // M3 (or keep M2 if preferred for this specific component)
// Use M3 MaterialTheme
import com.d4viddf.medicationreminder.wear.presentation.KEY_FIRST_LAUNCH
import com.d4viddf.medicationreminder.wear.presentation.PREFS_NAME
import com.d4viddf.medicationreminder.wear.presentation.WearViewModel
import com.d4viddf.medicationreminder.wear.presentation.checkAlarmPermission
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn // M3
import androidx.wear.compose.foundation.lazy.items // M3
import androidx.wear.compose.material3.Icon
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.google.android.gms.wearable.Wearable
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.d4viddf.medicationreminder.wear.presentation.PhoneAppStatus


@Composable
fun WearApp(wearViewModel: WearViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var isFirstLaunch by remember { mutableStateOf(sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)) }
    var hasAlarmPermission by remember { mutableStateOf(checkAlarmPermission(context)) }

    // val isConnected by wearViewModel.isConnectedToPhone.collectAsState() // Replaced by phoneAppStatus
    val reminders by wearViewModel.reminders.collectAsStateWithLifecycle()
    val isLoading by wearViewModel.isLoading.collectAsStateWithLifecycle()
    val phoneAppStatus by wearViewModel.phoneAppStatus.collectAsStateWithLifecycle()


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            hasAlarmPermission = true
        }
    }

    // Ensure this uses the M3 theme defined in theme.Theme.kt
    MedicationReminderTheme { // This is now the M3 Theme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background), // M3 color
            contentAlignment = Alignment.Center
        ) {
            TimeText() // M3 TimeText

            // LaunchedEffect to trigger initial phone app status check
            LaunchedEffect(Unit) {
                wearViewModel.checkPhoneAppInstallation(
                    Wearable.getCapabilityClient(context),
                    RemoteActivityHelper(context),
                    Wearable.getMessageClient(context)
                )
            }

            if (isFirstLaunch) {
                OnboardingScreen( // This will also be M3 styled
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
                // Main content based on phoneAppStatus
                when (phoneAppStatus) {
                    PhoneAppStatus.UNKNOWN, PhoneAppStatus.CHECKING -> {
                        CircularProgressIndicator()
                    }
                    PhoneAppStatus.NOT_INSTALLED_ANDROID -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(
                                text = stringResource(R.string.phone_app_not_installed_android),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(onClick = {
                                wearViewModel.openPlayStoreOnPhone(RemoteActivityHelper(context))
                            }) {
                                Text(stringResource(R.string.open_play_store))
                            }
                        }
                    }
                    PhoneAppStatus.NOT_INSTALLED_IOS -> {
                        Text(
                            text = stringResource(R.string.phone_app_not_installed_ios),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    PhoneAppStatus.INSTALLED_NO_DATA, PhoneAppStatus.INSTALLED_DATA_REQUESTED -> {
                        if (isLoading && reminders.isEmpty()) {
                            CircularProgressIndicator()
                        } else if (reminders.isEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = stringResource(R.string.no_reminders_today_syncing),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Button(onClick = {
                                    wearViewModel.checkPhoneAppInstallation(
                                        Wearable.getCapabilityClient(context),
                                        RemoteActivityHelper(context),
                                        Wearable.getMessageClient(context)
                                    )
                                }) {
                                    Text(stringResource(R.string.retry_sync))
                                }
                            }
                        } else {
                             // Data is available, show the list
                            RemindersContent(
                                reminders = reminders,
                                onMarkAsTaken = { reminderId -> // reminderId here is underlyingReminderId as string
                                    wearViewModel.markReminderAsTakenOnPhone(reminderId, Wearable.getMessageClient(context))
                                }
                            )
                        }
                    }
                    PhoneAppStatus.INSTALLED_WITH_DATA -> {
                        if (reminders.isEmpty()){
                            Text(
                                text = stringResource(R.string.no_reminders_today),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        } else {
                            RemindersContent(
                                reminders = reminders,
                                onMarkAsTaken = { reminderId ->
                                    wearViewModel.markReminderAsTakenOnPhone(reminderId, Wearable.getMessageClient(context))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RemindersContent(reminders: List<WearReminder>, onMarkAsTaken: (WearReminder) -> Unit) { // Pass full WearReminder
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(reminders, key = { it.id }) { reminder ->
            MedicationListItem(reminder = reminder, onMarkAsTaken = {
                onMarkAsTaken(reminder) // Pass the full reminder object
            })
        }
    }
}

@Composable
fun MedicationListItem(reminder: WearReminder, onMarkAsTaken: () -> Unit) {
    androidx.wear.compose.material3.Card(
        onClick = { /* TODO: Decide if item click does something, e.g. details */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = reminder.medicationName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Time: ${reminder.time}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reminder.dosage?.isNotBlank() == true) {
                    Text(
                        text = "Dosage: ${reminder.dosage}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!reminder.isTaken) {
                Button(
                    onClick = onMarkAsTaken,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize) // M3 standard size
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.mark_as_taken),
                        modifier = Modifier.size(ButtonDefaults.SmallIconSize) // M3 standard icon size
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Close, // Or Icons.Filled.CheckCircle for "taken"
                    contentDescription = stringResource(R.string.already_taken),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize) // Keep consistent sizing
                )
            }
        }
    }
}


@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MedicationReminderTheme {
        val context = LocalContext.current.applicationContext as Application
        // For previews, it's better to create a WearViewModel with mocked dependencies or a fake implementation
        // For now, just instantiating, but this won't show real data or states effectively.
        val previewViewModel = WearViewModel(context)
        WearApp(
            wearViewModel = previewViewModel
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "WearApp Connected Preview")
@Composable
fun WearAppConnectedPreview() {
    val application = LocalContext.current.applicationContext as Application
    val previewViewModel = WearViewModel(application) // Real VM, state needs to be manipulated for preview if possible
    // To truly preview states, you'd ideally have a way to set StateFlow values in the ViewModel for previewing,
    // or use a fake ViewModel designed for previews.
    // previewViewModel._phoneAppStatus.value = PhoneAppStatus.INSTALLED_WITH_DATA // This is not ideal as it accesses private state
    // previewViewModel._reminders.value = listOf(...)
    MedicationReminderTheme {
         WearApp(wearViewModel = previewViewModel) // Shows default (likely loading or no connection)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "WearApp Not Installed Preview")
@Composable
fun WearAppNotInstalledPreview() {
    MedicationReminderTheme {
        // This preview will likely show the "Checking" state or "Unknown"
        // as the ViewModel's default state without specific mocking.
        WearApp(WearViewModel(LocalContext.current.applicationContext as Application))
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "WearApp Loading Preview")
@Composable
fun WearAppLoadingPreview() {
     MedicationReminderTheme {
        // This preview will likely show the "Checking" state or "Unknown"
        WearApp(WearViewModel(LocalContext.current.applicationContext as Application))
    }
}
