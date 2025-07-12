package com.d4viddf.medicationreminder.wear.presentation.components

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle // For taken state
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.*
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.presentation.* // Import PhoneAppStatus, WearViewModelFactory etc.
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
            // TODO: Optionally show a message explaining why the permission is important
            Log.w("WearApp", "Schedule exact alarm permission denied by user.")
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
                    // NOT_INSTALLED_IOS case removed as enum was updated

                    PhoneAppStatus.INSTALLED_NO_DATA, PhoneAppStatus.INSTALLED_DATA_REQUESTED -> {
                        if (isLoading && reminders.isEmpty()) {
                            CircularProgressIndicator()
                        } else if (reminders.isEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.no_reminders_today_syncing),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Button(onClick = {
                                    wearViewModel.checkPhoneAppInstallation( // Re-trigger check and sync attempt
                                        Wearable.getCapabilityClient(context),
                                        RemoteActivityHelper(context),
                                        Wearable.getMessageClient(context)
                                    )
                                }) {
                                    Text(stringResource(R.string.retry_sync))
                                }
                            }
                        } else {
                            RemindersContent(
                                reminders = reminders,
                                onMarkAsTaken = { reminderToTake -> // Correctly call the ViewModel function
                                    wearViewModel.markReminderAsTakenOnWatch(reminderToTake)
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
                                onMarkAsTaken = { reminderToTake -> // Correctly call the ViewModel function
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

@Composable
fun RemindersContent(reminders: List<WearReminder>, onMarkAsTaken: (WearReminder) -> Unit) {
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(reminders, key = { it.id }) { reminder ->
            MedicationListItem(reminder = reminder, onMarkAsTaken = { // Pass the specific reminder object
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
                    text = stringResource(R.string.reminder_time_prefix, reminder.time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reminder.dosage?.isNotBlank() == true) {
                    Text(
                        text = stringResource(R.string.reminder_dosage_prefix, reminder.dosage),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!reminder.isTaken) {
                Button(
                    onClick = onMarkAsTaken,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.mark_as_taken),
                        modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle, // Using CheckCircle for taken state
                    contentDescription = stringResource(R.string.already_taken),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                )
            }
        }
    }
}

// Simplified Previews
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultWearAppPreview() {
    MedicationReminderTheme {
        val context = LocalContext.current.applicationContext as Application
        val previewViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application = context))
        WearApp(wearViewModel = previewViewModel)
    }
}
