package com.d4viddf.medicationreminder.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Correct imports for Material Icons
import androidx.compose.material.icons.Icons // General Icons
import androidx.compose.material.icons.filled.Vaccines // Specific icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import com.d4viddf.medreminder.wear.services.WearableCommunicationService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Data class to represent a medication reminder
data class MedicationReminder(
    val id: String, // Keep as String if it comes from a source that uses String IDs
    val name: String,
    val dosage: String,
    val time: Long, // Timestamp in milliseconds
    val isTaken: Boolean = false
)

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Initialize WearableCommunicationService here or inject it
            val communicationService = WearableCommunicationService(applicationContext)
            MainAppScreen(reminders = sampleReminders, communicationService = communicationService) // Use sample data for now
        }
    }
}

@Composable
fun MainAppScreen(reminders: List<MedicationReminder>, communicationService: WearableCommunicationService) {
    val sortedReminders = reminders.filter { !it.isTaken }.sortedBy { it.time }
    val nextDoseTime = sortedReminders.firstOrNull()?.time
    val nextDoseGroup = sortedReminders.filter { it.time == nextDoseTime }
    val laterDoses = sortedReminders.filter { it.time != nextDoseTime }

    MaterialTheme {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            if (nextDoseGroup.isNotEmpty()) {
                item {
                    Text(
                        text = "Next: ${formatTime(nextDoseTime ?: 0)}",
                        style = MaterialTheme.typography.title3,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(nextDoseGroup) { reminder ->
                    MedicationReminderChip(reminder = reminder, communicationService = communicationService)
                }
            }

            if (laterDoses.isNotEmpty()) {
                // Find unique times for later doses to group them
                val uniqueLaterTimes = laterDoses.map { it.time }.distinct()

                uniqueLaterTimes.forEach { time ->
                    val remindersForTime = laterDoses.filter { it.time == time }
                    if (remindersForTime.isNotEmpty()) {
                        item {
                            Text(
                                text = "Later: ${formatTime(time)}",
                                style = MaterialTheme.typography.title3,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                                    .fillMaxWidth()
                            )
                        }
                        items(remindersForTime) { reminder ->
                            MedicationReminderChip(reminder = reminder, communicationService = communicationService)
                        }
                    }
                }
            }

            if (nextDoseGroup.isEmpty() && laterDoses.isEmpty()) {
                item {
                    Text(
                        text = "No upcoming medications.",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MedicationReminderChip(reminder: MedicationReminder, communicationService: WearableCommunicationService) {
    Chip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        icon = {
            Icon(
                Icons.Filled.Vaccines, // Replace with actual medication icon later
                contentDescription = "Medication icon",
                modifier = Modifier.size(ChipDefaults.IconSize),
                tint = MaterialTheme.colors.primary
            )
        },
        label = {
            Column {
                Text(text = reminder.name, fontWeight = FontWeight.Bold)
                Text(text = reminder.dosage, fontSize = 12.sp)
            }
        },
        onClick = {
            // Send message to phone to mark as taken
            // Note: Reminder ID needs to be Int for the existing phone-side service
            reminder.id.toIntOrNull()?.let { reminderIdInt ->
                communicationService.sendMarkAsTakenMessage(reminderIdInt)
                // Optionally: Update UI locally, or wait for confirmation/data sync
            }
        },
        colors = ChipDefaults.primaryChipColors(
            backgroundColor = MaterialTheme.colors.surface
        )
    )
}

fun formatTime(timeInMillisValue: Long): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timeInMillisValue
    }
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(calendar.time)
}

// Sample data for preview and initial testing
val sampleReminders = listOf(
    MedicationReminder("1", "Mestinon", "1 tablet", getTimestamp(9, 0), false),
    MedicationReminder("2", "Valsartan", "1 tablet", getTimestamp(9, 0), false),
    MedicationReminder("3", "Mestinon", "1 tablet", getTimestamp(15, 0), false),
    MedicationReminder("4", "Vitamin D", "1 capsule", getTimestamp(10, 30), false),
    MedicationReminder("5", "Aspirin", "1 tablet", getTimestamp(10, 30), true) // Already taken
)

fun getTimestamp(hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun DefaultPreview() {
    MainAppScreen(reminders = sampleReminders, communicationService = WearableCommunicationService(androidx.compose.ui.platform.LocalContext.current))
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun EmptyStatePreview() {
    MainAppScreen(reminders = emptyList(), communicationService = WearableCommunicationService(androidx.compose.ui.platform.LocalContext.current))
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun OnlyNextDosePreview() {
    MainAppScreen(reminders = listOf(
        MedicationReminder("1", "Mestinon", "1 tablet", getTimestamp(9, 0), false),
        MedicationReminder("2", "Valsartan", "1 tablet", getTimestamp(9, 0), false)
    ), communicationService = WearableCommunicationService(androidx.compose.ui.platform.LocalContext.current))
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun OnlyLaterDosePreview() {
    MainAppScreen(reminders = listOf(
        MedicationReminder("3", "Mestinon", "1 tablet", getTimestamp(15, 0), false)
    ), communicationService = WearableCommunicationService(androidx.compose.ui.platform.LocalContext.current))
}
