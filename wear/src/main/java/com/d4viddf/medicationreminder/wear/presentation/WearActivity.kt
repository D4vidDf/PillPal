package com.d4viddf.medicationreminder.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.app.Application // Needed for Preview ViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Keep for tinting
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // Ensure this import is correct
// Corrected: Add the missing import
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


class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Ensure Application context is correctly passed if needed by factory, or use default Hilt/Lifecycle ways
            val wearViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application))
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
                            items(remindersForTime, key = { it.id }) { reminder ->
                                MedicationReminderChip(
                                    reminder = reminder,
                                    onChipClick = { viewModel.markReminderAsTaken(reminder.underlyingReminderId) }
                                )
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
                        MedicationReminderChip(
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

@Composable
fun ConnectionStatusIcon(isConnected: Boolean) {
    // USER: Please provide ic_watch_connected.xml and ic_watch_disconnected.xml drawables
    // For now, using a placeholder if R.drawable.ic_watch_connected is not available.
    // Using medication_filled as a stand-in for both and tinting it.
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
            if (isTakenDisplay || reminder.isTaken) {
                // USER: Please provide ic_check_circle.xml drawable
                Icon(
                    painter = painterResource(id = R.drawable.medication_filled /* R.drawable.ic_check_circle */),
                    contentDescription = "Taken",
                    modifier = Modifier.size(ChipDefaults.IconSize),
                    tint = Color.Green // Keeping tint for placeholder
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
fun DefaultPreview() {
    PreviewMainAppScreen(reminders = sampleWearReminders, isConnected = true)
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
