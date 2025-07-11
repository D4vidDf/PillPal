package com.d4viddf.medicationreminder.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.wear.protolayout.material.Text
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable

// Import M3 theme and components
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import com.d4viddf.medicationreminder.wear.presentation.components.ConnectionStatusIcon
import com.d4viddf.medicationreminder.wear.presentation.components.MedicationReminderChip
// M3 Text if needed, though current Text calls might resolve to M2 Text if not fully qualified
// M2 MaterialTheme is used by Scaffold, TimeText etc. If Scaffold becomes M3, this will change.
// For now, let M2 MaterialTheme be the one for the overall screen, components use M3 theme internally if they are M3.
// This can lead to inconsistencies, ideal solution is full M3 screen.


class WearActivity : ComponentActivity() {

    // 1. Declare CapabilityClient and the capability name
    private lateinit var capabilityClient: CapabilityClient
    private val CAPABILITY_NAME = "medication_reminder_wear_app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Initialize the CapabilityClient
        capabilityClient = Wearable.getCapabilityClient(this)

        setContent {
            val wearViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application))
            MainAppScreen(wearViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // 3. Add the capability when the app is in the foreground
        capabilityClient.addLocalCapability(CAPABILITY_NAME)
    }

    override fun onPause() {
        super.onPause()
        // 4. Remove the capability when the app is not in the foreground
        capabilityClient.removeLocalCapability(CAPABILITY_NAME)
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

    MedicationReminderTheme { // Use M3 Theme
        Scaffold( // This is M2 Scaffold. For full M3, this would be androidx.wear.compose.material3.Scaffold
            timeText = { TimeText() }, // M2 TimeText
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }, // M2 Vignette
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
                        Text( // Use M3Text
                            text = "Next: ${nextDoseGroup.first().time}",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(nextDoseGroup, key = { it.id }) { reminder ->
                        MedicationReminderChip( // This is now the imported M3 version
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
                                Text( // Use M3Text
                                    text = "Later: $time",
                                    style = MaterialTheme.typography.body1,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                                        .fillMaxWidth()
                                )
                            }
                            items(remindersForTime, key = { it.id }) { reminder ->
                                MedicationReminderChip( // Imported M3 version
                                    reminder = reminder,
                                    onChipClick = { viewModel.markReminderAsTaken(reminder.underlyingReminderId) }
                                )
                            }
                        }
                    }
                }

                if (upcomingReminders.isEmpty()) {
                    item {
                        Text( // Use M3Text
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
                        Text( // Use M3Text
                            text = "Taken Today",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp).fillMaxWidth()
                        )
                    }
                    items(takenReminders, key = { "taken_${it.id}" }) { reminder ->
                        MedicationReminderChip( // Imported M3 version
                            reminder = reminder,
                            onChipClick = { /* No action for already taken items */ },
                            isTakenDisplay = true
                        )
                    }
                }
            }
        }
    }
}

// REMOVE local ConnectionStatusIcon definition - it's now imported
// @Composable
// fun ConnectionStatusIcon(isConnected: Boolean) { ... }


// REMOVE local MedicationReminderChip definition - it's now imported
// @Composable
// fun MedicationReminderChip( ... ) { ... }

// Sample data for preview and initial testing - using WearReminder
val sampleWearReminders = listOf(
    WearReminder("item1_9am",1L, "Mestinon", "09:00", false, "1 tablet"),
    WearReminder("item2_9am",2L, "Valsartan", "09:00", false, "1 tablet"),
    WearReminder("item3_3pm",3L, "Mestinon", "15:00", false, "1 tablet"),
    WearReminder("item4_1030am",4L, "Vitamin D", "10:30", false, "1 capsule"),
    WearReminder("item5_1030am_taken",5L, "Aspirin", "10:30", true, "1 tablet")
)

// Simplified MainAppScreen for Previews that takes data directly
@Composable
fun PreviewMainAppScreen(reminders: List<WearReminder>, isConnected: Boolean) {
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
                item { ConnectionStatusIcon(isConnected = isConnected) }

                if (nextDoseGroup.isNotEmpty()) {
                    item { Text("Next: ${nextDoseGroup.first().time}", style = MaterialTheme.typography.title3, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(nextDoseGroup, key = { "next_${it.id}" }) { reminder ->
                        MedicationReminderChip(reminder = reminder, onChipClick = { /* Preview no action */ })
                    }
                }
                // ... (rest of the UI logic similar to MainAppScreen but using passed in reminders) ...
                 if (laterDoses.isNotEmpty()) {
                    val uniqueLaterTimes = laterDoses.map { it.time }.distinct().sorted()
                    uniqueLaterTimes.forEach { time ->
                        val remindersForTime = laterDoses.filter { it.time == time }
                        if (remindersForTime.isNotEmpty()) {
                            item { Text("Later: $time", style = MaterialTheme.typography.title3, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp).fillMaxWidth()) }
                            items(remindersForTime, key = { "later_${it.id}" }) { reminder ->
                                MedicationReminderChip(reminder = reminder, onChipClick = { /* Preview no action */ })
                            }
                        }
                    }
                }
                if (upcomingReminders.isEmpty()) {
                    item { Text(if (isConnected) "No upcoming medications." else "Disconnected.", style = MaterialTheme.typography.body1, modifier = Modifier.padding(16.dp)) }
                }
                val takenReminders = reminders.filter { it.isTaken }.sortedByDescending { it.time }
                if (takenReminders.isNotEmpty()) {
                    item { Text("Taken Today", style = MaterialTheme.typography.title3, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp).fillMaxWidth()) }
                    items(takenReminders, key = { "taken_${it.id}" }) { reminder ->
                        MedicationReminderChip(reminder = reminder, onChipClick = { /* Preview no action */ }, isTakenDisplay = true)
                    }
                }
            }
        }
    }
}




@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun EmptyStatePreview() {
    PreviewMainAppScreen(reminders = emptyList(), isConnected = true)
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun DisconnectedPreview() {
    PreviewMainAppScreen(reminders = emptyList(), isConnected = false)
}
