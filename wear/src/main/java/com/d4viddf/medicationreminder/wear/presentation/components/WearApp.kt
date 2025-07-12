package com.d4viddf.medicationreminder.wear.presentation.components

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Correct import for getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.*
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.presentation.*
import com.google.android.gms.wearable.Wearable

@Composable
fun WearApp(wearViewModel: WearViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var isFirstLaunch by remember { mutableStateOf(sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)) }
    var hasAlarmPermission by remember { mutableStateOf(checkAlarmPermission(context)) }

    val reminders by wearViewModel.reminders.collectAsStateWithLifecycle()
    val isLoading by wearViewModel.isLoading.collectAsStateWithLifecycle()
    val phoneAppStatus by wearViewModel.phoneAppStatus.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasAlarmPermission = isGranted
        if (!isGranted) {
            Log.w("WearApp", "Schedule exact alarm permission denied by user.")
            // TODO: Show a message to the user that permission is important for reminders
            // Toast.makeText(context, R.string.permission_denied_alarm, Toast.LENGTH_LONG).show()
        }
    }

    MedicationReminderTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()

            LaunchedEffect(Unit) {
                wearViewModel.checkPhoneAppInstallation(
                    Wearable.getCapabilityClient(context),
                    RemoteActivityHelper(context),
                    Wearable.getMessageClient(context)
                )
            }

            if (isFirstLaunch) {
                OnboardingScreen(
                    onDismiss = {
                        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                        isFirstLaunch = false
                        if (!hasAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
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
                when (phoneAppStatus) {
                    PhoneAppStatus.UNKNOWN, PhoneAppStatus.CHECKING -> {
                        CircularProgressIndicator()
                    }
                    PhoneAppStatus.NOT_INSTALLED_ANDROID -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.phone_app_not_installed_android),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Button(onClick = {
                                wearViewModel.openPlayStoreOnPhone(RemoteActivityHelper(context))
                            }) {
                                Text(stringResource(R.string.open_play_store))
                            }
                        }
                    }
                    PhoneAppStatus.INSTALLED_NO_DATA, PhoneAppStatus.INSTALLED_DATA_REQUESTED -> {
                        if (isLoading && reminders.isEmpty()) {
                            CircularProgressIndicator()
                        } else  { // Show message or list even if loading but some old data exists
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (reminders.isEmpty()) stringResource(R.string.no_reminders_today_syncing) else stringResource(R.string.checking_for_updates),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 8.dp)
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
                                if (reminders.isNotEmpty()){ // Show stale list if available
                                     Spacer(modifier = Modifier.height(8.dp))
                                     RemindersContent(
                                        reminders = reminders,
                                        onMarkAsTaken = { reminderToTake ->
                                            wearViewModel.markReminderAsTakenOnWatch(reminderToTake)
                                        }
                                    )
                                }
                            }
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
                                onMarkAsTaken = { reminderToTake ->
                                    wearViewModel.markReminderAsTakenOnWatch(reminderToTake)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// RemindersContent and MedicationListItem are now expected to be in their own files if they were separate.
// If they were inline, their M3 versions are assumed here. Let's put their correct M3 versions here for clarity.

@Composable
fun RemindersContent(reminders: List<WearReminder>, onMarkAsTaken: (WearReminder) -> Unit) {
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally // Added for centering items like headers if any
    ) {
        // Optional: Header for the list
        if (reminders.isNotEmpty()) {
            item {
                ListHeader { // M3 ListHeader
                    Text(stringResource(R.string.todays_reminders_header))
                }
            }
        }
        items(items = reminders, key = { it.id }) { reminder ->
            MedicationListItem(reminder = reminder, onMarkAsTaken = {
                onMarkAsTaken(reminder)
            })
        }
    }
}

@Composable
fun MedicationListItem(reminder: WearReminder, onMarkAsTaken: () -> Unit) {
    Card(
        onClick = { /* TODO: Decide if item click does something, e.g. details */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp), // Reduced vertical padding
        colors = CardDefaults.cardColors(
            containerColor = if(reminder.isTaken) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp), // Adjusted padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = reminder.medicationName,
                    style = MaterialTheme.typography.titleSmall, // Adjusted for less emphasis than bodyLarge
                    color = if (reminder.isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.reminder_time_prefix, reminder.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (reminder.isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reminder.dosage?.isNotBlank() == true) {
                    Text(
                        text = stringResource(R.string.reminder_dosage_prefix, reminder.dosage),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reminder.isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!reminder.isTaken) {
                Button(
                    onClick = onMarkAsTaken,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.mark_as_taken),
                        modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.already_taken),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultWearAppPreview() {
    MedicationReminderTheme {
        val context = LocalContext.current.applicationContext as Application
        val previewViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application = context))
        WearApp(wearViewModel = previewViewModel)
    }
}
