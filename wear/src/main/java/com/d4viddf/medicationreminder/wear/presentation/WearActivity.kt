package com.d4viddf.medicationreminder.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Correct imports for Material Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle // Example for taken
import androidx.compose.material.icons.filled.Error // Example for disconnected
import androidx.compose.material.icons.filled.RadioButtonUnchecked // Example for not taken
import androidx.compose.material.icons.filled.Watch // Example for connected
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
// import com.d4viddf.medicationreminder.wear.services.WearableCommunicationService // ViewModel handles this
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val wearViewModel: WearViewModel = viewModel(
                factory = WearViewModelFactory(application)
            )
            MainAppScreen(wearViewModel)
        }
    }
}

@Composable
fun MainAppScreen(viewModel: WearViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    val isConnected by viewModel.isConnectedToPhone.collectAsState()

    // Filter out taken reminders and sort by time (HH:mm string comparison works for this format)
    val upcomingReminders = reminders.filter { !it.isTaken }.sortedBy { it.time }
    val nextDoseTime = upcomingReminders.firstOrNull()?.time
    val nextDoseGroup = upcomingReminders.filter { it.time == nextDoseTime }
    val laterDoses = upcomingReminders.filter { it.time != nextDoseTime && it.time > (nextDoseTime ?: "00:00") }

    val listState = rememberScalingLazyListState()

    MaterialTheme {
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ConnectionStatusIcon(isConnected = isConnected)
                }

                if (nextDoseGroup.isNotEmpty()) {
                    item {
                        Text(
                            text = "Next: ${nextDoseGroup.first().time}", // All in group have same time
                            style = MaterialTheme.typography.title3,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(nextDoseGroup, key = { it.id }) { reminder ->
                        MedicationReminderChip(reminder = reminder, viewModel = viewModel)
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
                            items(remindersForTime, key = { it.id }) { reminder ->
                                MedicationReminderChip(reminder = reminder, viewModel = viewModel)
                            }
                        }
                    }
                }

                if (upcomingReminders.isEmpty()) {
                    item {
                        Text(
                            text = if (isConnected) "No upcoming medications." else "Disconnected. Waiting for data...",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Optionally, show taken reminders at the end
                val takenReminders = reminders.filter { it.isTaken }.sortedByDescending { it.time }
                if (takenReminders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Taken Today",
                            style = MaterialTheme.typography.title3,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp).fillMaxWidth()
                        )
                    }
                    items(takenReminders, key = { "taken_${it.id}" }) { reminder ->
                        MedicationReminderChip(reminder = reminder, viewModel = viewModel, isTakenDisplay = true)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusIcon(isConnected: Boolean) {
    Icon(
        imageVector = if (isConnected) Icons.Default.Watch else Icons.Default.Error,
        contentDescription = if (isConnected) "Connected to phone" else "Disconnected from phone",
        tint = if (isConnected) Color.Green else Color.Red,
        modifier = Modifier
            .size(24.dp)
            .padding(top = 4.dp)
    )
}


@Composable
fun MedicationReminderChip(
    reminder: WearReminder,
    viewModel: WearViewModel,
    isTakenDisplay: Boolean = false // To differentiate display for already taken items
) {
    Chip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        icon = {
            if (isTakenDisplay || reminder.isTaken) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Taken",
                    modifier = Modifier.size(ChipDefaults.IconSize),
                    tint = Color.Green
                )
            } else {
                Icon(
                    painterResource(id = R.drawable.medication_filled), // Replace with actual medication icon later
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
        secondaryLabel = { // Display time here as well
             Text(text = reminder.time, fontSize = 12.sp)
        },
        onClick = {
            if (!reminder.isTaken && !isTakenDisplay) { // Only allow marking as taken if not already taken
                viewModel.markReminderAsTaken(reminder.underlyingReminderId)
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

// Sample data for preview and initial testing - using WearReminder
val sampleWearReminders = listOf(
    WearReminder("item1_9am",1L, "Mestinon", "09:00", false, "1 tablet"),
    WearReminder("item2_9am",2L, "Valsartan", "09:00", false, "1 tablet"),
    WearReminder("item3_3pm",3L, "Mestinon", "15:00", false, "1 tablet"),
    WearReminder("item4_1030am",4L, "Vitamin D", "10:30", false, "1 capsule"),
    WearReminder("item5_1030am_taken",5L, "Aspirin", "10:30", true, "1 tablet") // Already taken
)

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun DefaultPreview() {
    val app = LocalContext.current.applicationContext as Application
    val viewModel = WearViewModel(app) // Create a dummy ViewModel for preview
    // Populate with sample data for preview
    (viewModel.reminders as kotlinx.coroutines.flow.MutableStateFlow).value = sampleWearReminders
    MainAppScreen(viewModel = viewModel)
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun EmptyStatePreview() {
     val app = LocalContext.current.applicationContext as Application
    val viewModel = WearViewModel(app)
    (viewModel.reminders as kotlinx.coroutines.flow.MutableStateFlow).value = emptyList()
    MainAppScreen(viewModel = viewModel)
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun DisconnectedPreview() {
    val app = LocalContext.current.applicationContext as Application
    val viewModel = WearViewModel(app)
    (viewModel.reminders as kotlinx.coroutines.flow.MutableStateFlow).value = emptyList()
    (viewModel.isConnectedToPhone as kotlinx.coroutines.flow.MutableStateFlow).value = false
    MainAppScreen(viewModel = viewModel)
}
