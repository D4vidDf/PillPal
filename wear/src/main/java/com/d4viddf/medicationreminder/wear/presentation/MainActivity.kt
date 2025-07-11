/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.d4viddf.medicationreminder.wear.presentation

import android.Manifest
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
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
// Removed Node and Wearable imports from here, should be handled by ViewModel
import kotlinx.coroutines.launch
// Removed tasks.await from here

const val PREFS_NAME = "MedicationReminderPrefs"
const val KEY_FIRST_LAUNCH = "isFirstLaunch"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            // Initialize ViewModel here
            val wearViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application))
            WearApp(wearViewModel = wearViewModel)
        }
    }

    // openPlayStoreOnPhone might be better in ViewModel or a helper if it needs context beyond activity
    // For now, let it be here if WearApp calls it directly.
    // However, the "Try Connection" button should now be more nuanced.
    // If disconnected, it might mean "Try to connect" or "Open settings" or "Open on phone".
    // The WearViewModel's requestInitialSync might be relevant here.
    private fun openPlayStoreOnPhone() {
        // This specific implementation sends a message to the phone.
        // It should be triggered by the ViewModel based on its state.
        // For now, this function can be called by the ViewModel.
        val wearViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application))
        wearViewModel.requestPhoneToOpenPlayStore() // New method in ViewModel
    }
}

@Composable
fun WearApp(wearViewModel: WearViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var isFirstLaunch by remember { mutableStateOf(sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)) }
    var hasAlarmPermission by remember { mutableStateOf(checkAlarmPermission(context)) }

    // Get connection status from ViewModel
    val isConnected by wearViewModel.isConnectedToPhone.collectAsState()
    // Get reminders from ViewModel
    val reminders by wearViewModel.reminders.collectAsState()


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            hasAlarmPermission = true
        }
    }

    MedicationReminderTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText() // Keep TimeText, common for Wear OS screens

            if (isFirstLaunch) {
                OnboardingScreen(
                    onDismiss = {
                        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                        isFirstLaunch = false
                        if (!hasAlarmPermission) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                permissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                            }
                            // After onboarding, the ViewModel should already be trying to connect/sync
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
                // After onboarding, show main content driven by ViewModel's state
                // This combines logic from the original WearApp and MainAppScreen from WearActivity
                if (!isConnected) {
                    DeviceNotConnectedScreen(
                        onTryConnection = {
                            // This should trigger the ViewModel to attempt connection or guide user
                            // For now, let's assume it means "ask phone to open play store to guide setup"
                            // This would be a good candidate for a ViewModel action.
                            wearViewModel.requestPhoneToOpenPlayStore()
                        }
                    )
                } else {
                    // If connected, display reminders content (simplified from MainAppScreen)
                    // Or, if MainActivity is just for onboarding, it should navigate to WearActivity
                    // For this fix, let's integrate reminder display here.
                    RemindersContent(reminders = reminders, viewModel = wearViewModel)
                }
            }
        }
    }
}

// Copied from WearActivity.kt
@Composable
fun ConnectionStatusIcon(isConnected: Boolean) {
    val iconRes = if (isConnected) R.drawable.medication_filled /* R.drawable.ic_watch_connected */ else R.drawable.medication_filled /* R.drawable.ic_watch_disconnected */
    val tintColor = if (isConnected) Color.Green else Color.Red
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = if (isConnected) "Connected to phone" else "Disconnected from phone",
        tint = tintColor,
        modifier = Modifier
            .size(24.dp)
            .padding(top = 4.dp)
    )
}

// Copied from WearActivity.kt
@Composable
fun MedicationReminderChip(
    reminder: WearReminder,
    onChipClick: () -> Unit,
    isTakenDisplay: Boolean = false // Added to match WearActivity's version for consistency
) {
    Chip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        icon = {
            if (isTakenDisplay || reminder.isTaken) {
                Icon(
                    painter = painterResource(id = R.drawable.medication_filled /* R.drawable.ic_check_circle */),
                    contentDescription = "Taken",
                    modifier = Modifier.size(ChipDefaults.IconSize),
                    tint = Color.Green
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.medication_filled),
                    contentDescription = "Medication icon",
                    modifier = Modifier.size(ChipDefaults.IconSize),
                    tint = MaterialTheme.colors.primary
                )
            }
        },
        label = {
            Column {
                Text(text = reminder.medicationName, fontWeight = FontWeight.Bold)
                if (reminder.dosage != null) {
                    Text(text = reminder.dosage, fontSize = 12.sp)
                }
            }
        },
        secondaryLabel = {
             Text(text = reminder.time, fontSize = 12.sp)
        },
        onClick = {
            if (!reminder.isTaken && !isTakenDisplay) {
                onChipClick()
            }
        },
        colors = if (isTakenDisplay || reminder.isTaken) {
            ChipDefaults.secondaryChipColors()
        } else {
            ChipDefaults.primaryChipColors(
                backgroundColor = MaterialTheme.colors.surface
            )
        }
    )
}


@Composable
fun RemindersContent(reminders: List<WearReminder>, viewModel: WearViewModel) {
    // This is a simplified version of MainAppScreen's content display logic
    // It should be adapted from WearActivity.MainAppScreen
    ConnectionStatusIcon(isConnected = viewModel.isConnectedToPhone.collectAsState().value) // Added status icon
    val upcomingReminders = reminders.filter { !it.isTaken }.sortedBy { it.time }

    if (upcomingReminders.isEmpty()) {
        Text(
            text = "No upcoming medications.", // Assuming connected if this screen is shown
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        val listState = rememberScalingLazyListState()
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val nextDoseTime = upcomingReminders.firstOrNull()?.time
            val nextDoseGroup = upcomingReminders.filter { it.time == nextDoseTime }
            val laterDoses = upcomingReminders.filter { it.time != nextDoseTime && it.time > (nextDoseTime ?: "00:00") }

            if (nextDoseGroup.isNotEmpty()) {
                item {
                    Text(
                        text = "Next: ${nextDoseGroup.first().time}",
                        style = MaterialTheme.typography.title3,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(nextDoseGroup, key = { "next_${it.id}" }) { reminder ->
                    MedicationReminderChip(
                        reminder = reminder,
                        onChipClick = { viewModel.markReminderAsTaken(reminder.underlyingReminderId) }
                    )
                }
            }
            // Add logic for laterDoses and takenReminders if needed, similar to WearActivity.MainAppScreen
             if (laterDoses.isNotEmpty()) {
                val uniqueLaterTimes = laterDoses.map { it.time }.distinct().sorted()
                uniqueLaterTimes.forEach { time ->
                    val remindersForTime = laterDoses.filter { it.time == time }
                    if (remindersForTime.isNotEmpty()) {
                        item {
                            Text(
                                text = "Later: $time",
                                style = MaterialTheme.typography.title3,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                                    .fillMaxWidth()
                            )
                        }
                        items(remindersForTime, key = { "later_${it.id}" }) { reminder ->
                            MedicationReminderChip(
                                reminder = reminder,
                                onChipClick = { viewModel.markReminderAsTaken(reminder.underlyingReminderId) }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun OnboardingScreen(onDismiss: () -> Unit, hasAlarmPermission: Boolean, onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_message_title),
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_message_body),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (!hasAlarmPermission) {
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.enable_alarms_permission))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = onDismiss) {
            Text(stringResource(R.string.got_it))
        }
    }
}

@Composable
fun DeviceNotConnectedScreen(onTryConnection: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.device_not_connected),
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onTryConnection) {
            Text(stringResource(R.string.try_connection))
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

fun checkAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SCHEDULE_EXACT_ALARM
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // For older versions, permission is granted by default or not needed
    }
}


@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun OnboardingPreview() {
    MedicationReminderTheme {
        OnboardingScreen({}, false, {})
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun NotConnectedPreview() {
    MedicationReminderTheme {
        DeviceNotConnectedScreen({})
    }
}