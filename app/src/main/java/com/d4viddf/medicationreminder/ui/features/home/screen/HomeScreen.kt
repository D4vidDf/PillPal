package com.d4viddf.medicationreminder.ui.features.home.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.features.home.viewmodel.HomeViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.currentGreeting, style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { /* TODO: Navigate to Wear OS connect screen */ }) {
                        Icon(Icons.Filled.Watch, contentDescription = "Connect Wear OS")
                    }
                    IconButton(onClick = { /* TODO: Navigate to Notifications Center */ }) {
                        BadgedBox(badge = { if (uiState.hasUnreadAlerts) Badge() }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Next Dose Carousel
                if (uiState.nextDoseGroup.isNotEmpty()) {
                    item {
                        Column {
                            val nextDoseTime = uiState.nextDoseGroup.first().reminderTime
                            val formattedTime = try {
                                LocalDateTime.parse(nextDoseTime, ReminderCalculator.storableDateTimeFormatter).format(timeFormatter)
                            } catch (e: Exception) { "Future" }
                            Text(
                                "NEXT DOSE (at $formattedTime)",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(uiState.nextDoseGroup, key = { "next-${it.id}" }) { reminder ->
                                    NextDoseCard(reminder)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "No upcoming doses for the rest of the day.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }

                // Today's Schedule
                item {
                    Text(
                        "TODAY'S SCHEDULE",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                uiState.todaysReminders.forEach { (partOfDay, remindersInPart) ->
                    if (remindersInPart.isNotEmpty()) {
                        item {
                            Text(
                                text = "$partOfDay (${
                                    try {
                                        LocalDateTime.parse(remindersInPart.first().reminderTime, ReminderCalculator.storableDateTimeFormatter)
                                            .format(timeFormatter)
                                    } catch (e: Exception) { "" }
                                })",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                            )
                        }
                        items(remindersInPart, key = { "schedule-${it.id}" }) { reminder ->
                            TodayScheduleItem(
                                reminder = reminder,
                                onMarkAsTaken = { viewModel.markAsTaken(reminder) },
                                timeFormatter = timeFormatter
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(64.dp)) } // Space for FAB or bottom nav
            }
        }
    }
}

@Composable
fun NextDoseCard(reminder: MedicationReminder) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.width(150.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Med ID: ${reminder.medicationId}", fontWeight = FontWeight.Bold) // Placeholder for Med Name
            Text("1 tablet") // Placeholder for Dosage
            val time = try {
                LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                "N/A"
            }
            Text(time, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TodayScheduleItem(
    reminder: MedicationReminder,
    onMarkAsTaken: () -> Unit,
    timeFormatter: DateTimeFormatter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = if (reminder.isTaken) BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isTaken) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = reminder.isTaken,
                    onCheckedChange = { if (it) onMarkAsTaken() },
                    enabled = !reminder.isTaken,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Medication ID: ${reminder.medicationId}", // Placeholder
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (reminder.isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "1 tablet at ${
                            try {
                                LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter).format(timeFormatter)
                            } catch (e: Exception) { "" }
                        }", // Placeholder for dosage
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (reminder.isTaken) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Taken",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "HomeScreen Preview (New Design)")
@Composable
fun HomeScreenNewPreview() {
    // Simplified ViewModel for preview
    val previewViewModel = object : HomeViewModel(medicationReminderRepository = médicamentReminderRepositoryPreview) {
        override val uiState = MutableStateFlow(
            HomeViewModel.HomeState(
                nextDoseGroup = listOf(
                    MedicationReminder(1, 101, 1, LocalDateTime.now().plusHours(1).format(ReminderCalculator.storableDateTimeFormatter), false, null, null),
                    MedicationReminder(2, 102, 2, LocalDateTime.now().plusHours(1).format(ReminderCalculator.storableDateTimeFormatter), false, null, null)
                ),
                todaysReminders = mapOf(
                    "Morning" to listOf(
                        MedicationReminder(5, 104, 5, LocalDateTime.now().withHour(8).format(ReminderCalculator.storableDateTimeFormatter), true, LocalDateTime.now().withHour(8).format(ReminderCalculator.storableDateTimeFormatter), null),
                        MedicationReminder(1, 101, 1, LocalDateTime.now().withHour(9).format(ReminderCalculator.storableDateTimeFormatter), false, null, null)
                    ),
                    "Afternoon" to listOf(
                        MedicationReminder(3, 101, 3, LocalDateTime.now().withHour(15).format(ReminderCalculator.storableDateTimeFormatter), false, null, null)
                    ),
                    "Evening" to listOf(
                        MedicationReminder(4, 103, 4, LocalDateTime.now().withHour(21).format(ReminderCalculator.storableDateTimeFormatter), false, null, null)
                    ),
                    "Night" to emptyList()
                ),
                hasUnreadAlerts = true,
                isLoading = false,
                currentGreeting = "Good morning! 🌤️"
            )
        )
    }

    AppTheme {
        HomeScreen(
            navController = rememberNavController(),
            viewModel = previewViewModel
        )
    }
}

// Fake repository for preview (can be shared or defined per preview if needed)
private val médicamentReminderRepositoryPreview = object : com.d4viddf.medicationreminder.data.MedicationReminderRepository {
    override suspend fun insertReminder(reminder: MedicationReminder): Long = 0L
    override suspend fun updateReminder(reminder: MedicationReminder) {}
    override suspend fun deleteReminder(reminder: MedicationReminder) {}
    override suspend fun getReminderById(id: Int): MedicationReminder? = null
    override fun getRemindersForMedication(medicationId: Int): kotlinx.coroutines.flow.Flow<List<MedicationReminder>> = flowOf(emptyList())
    override fun getRemindersByStatus(isTaken: Boolean): kotlinx.coroutines.flow.Flow<List<MedicationReminder>> = flowOf(emptyList())
    override fun getFutureRemindersForMedication(medicationId: Int, currentTimeIso: String): kotlinx.coroutines.flow.Flow<List<MedicationReminder>> = flowOf(emptyList())
    override suspend fun deleteFutureUntakenRemindersForMedication(medicationId: Int, currentTimeIso: String) {}
    override fun getFutureUntakenRemindersForMedication(medicationId: Int, currentTimeIsoString: String): kotlinx.coroutines.flow.Flow<List<MedicationReminder>> = flowOf(emptyList())
    override suspend fun getRemindersForMedicationInWindow(medicationId: Int, startTime: String, endTime: String): List<MedicationReminder> = emptyList()
    override suspend fun deleteReminderById(reminderId: Int) {}
    override suspend fun getMostRecentTakenReminder(medicationId: Int): MedicationReminder? = null
    override fun getRemindersForDay(startOfDayString: String, endOfDayString: String): kotlinx.coroutines.flow.Flow<List<MedicationReminder>> {
        val now = LocalDateTime.now()
        val sampleRemindersList = listOf(
            MedicationReminder(1, 101,1, now.withHour(9).format(ReminderCalculator.storableDateTimeFormatter), false, null, null),
            MedicationReminder(2, 102,2, now.withHour(9).format(ReminderCalculator.storableDateTimeFormatter), false, null, null),
            MedicationReminder(3, 101,3, now.withHour(15).format(ReminderCalculator.storableDateTimeFormatter), false, null, null),
            MedicationReminder(4, 103,4, now.withHour(21).format(ReminderCalculator.storableDateTimeFormatter), false, null, null),
            MedicationReminder(5, 104,5, now.withHour(8).format(ReminderCalculator.storableDateTimeFormatter), true, now.withHour(8).format(ReminderCalculator.storableDateTimeFormatter), null)
        )
        return flowOf(sampleRemindersList.filter {
            val reminderDateTime = LocalDateTime.parse(it.reminderTime, ReminderCalculator.storableDateTimeFormatter)
            reminderDateTime.toLocalDate().isEqual(now.toLocalDate())
        }.sortedBy { it.reminderTime })
    }
    override suspend fun getAllReminders(): kotlinx.coroutines.flow.Flow<List<MedicationReminder>> = flowOf(emptyList())
    override suspend fun getReminderTimeForNextDose(medicationId: Int, currentTimeIso: String): String? = null
}

// Minimal HomeViewModel for preview
private open class PreviewHomeViewModel(repo: com.d4viddf.medicationreminder.data.MedicationReminderRepository) : HomeViewModel(repo) {
    // Can override specific things for preview if needed, or just use as is
}