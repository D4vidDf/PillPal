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
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import com.d4viddf.medicationreminder.wear.data.WearReminder // Import for WearReminder data class

// Added missing imports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


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

@Composable
fun WearApp(wearViewModel: WearViewModel) { // Removed mainActivity parameter
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

    MedicationReminderTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()

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
                            wearViewModel.requestPhoneToOpenPlayStore() // Call ViewModel method directly
                        }
                    )
                } else {
                    RemindersContent(reminders = reminders, viewModel = wearViewModel)
                }
            }
        }
    }
}


@Composable
fun RemindersContent(reminders: List<WearReminder>, viewModel: WearViewModel) {
    val isConnectedState by viewModel.isConnectedToPhone.collectAsState()
    ConnectionStatusIcon(isConnected = isConnectedState) // Display connection status icon

    val upcomingReminders = reminders.filter { !it.isTaken }.sortedBy { it.time }

    if (upcomingReminders.isEmpty()) {
        Text(
            text = "No upcoming medications.",
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center
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
            // Consider adding taken reminders section if desired
        }
    }
}

@Composable
fun ConnectionStatusIcon(isConnected: Boolean) {
    val iconResId = R.drawable.medication_filled // Replace with actual icons
    val tint = if (isConnected) Color.Green else Color.Red
    val contentDesc = if (isConnected) "Connected to phone" else "Disconnected from phone"

    Icon(
        painter = painterResource(id = iconResId),
        contentDescription = contentDesc,
        tint = tint,
        modifier = Modifier
            .size(24.dp)
            .padding(top = 4.dp)
    )
}

@Composable
fun MedicationReminderChip(
    reminder: WearReminder,
    onChipClick: () -> Unit,
    isTakenDisplay: Boolean = false
) {
    Chip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        icon = {
            val iconResId = R.drawable.medication_filled // Replace
            val iconTint = if (isTakenDisplay || reminder.isTaken) Color.Green else MaterialTheme.colors.primary
            val iconDesc = if (isTakenDisplay || reminder.isTaken) "Taken" else "Medication icon"

            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = iconDesc,
                modifier = Modifier.size(ChipDefaults.IconSize),
                tint = iconTint
            )
        },
        label = {
            Column {
                Text(text = reminder.medicationName, fontWeight = FontWeight.Bold)
                reminder.dosage?.let { Text(text = it, fontSize = 12.sp) }
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

// Greeting function is likely unused now but kept for reference / safety.
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
        true
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MedicationReminderTheme {
        // For preview, we need a Context to create an Application instance for WearViewModel.
        val context = LocalContext.current
        WearApp(
            wearViewModel = WearViewModel(context.applicationContext as Application)
            // Removed mainActivity parameter
        )
    }
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

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "WearApp Connected Preview")
@Composable
fun WearAppConnectedPreview() {
    val application = LocalContext.current.applicationContext as Application
    val previewViewModel = WearViewModel(application)
    // To show connected state with reminders, you'd ideally have a way to set
    // the ViewModel's state here, or use a fake ViewModel.
    // e.g., previewViewModel._isConnectedToPhone.value = true (if not private)
    MedicationReminderTheme {
        WearApp(
            wearViewModel = previewViewModel
            // Removed mainActivity parameter from this preview as well
        )
    }
}