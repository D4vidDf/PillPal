package com.d4viddf.medicationreminder.ui.features.home.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyRow // Replaced by Carousel
//import androidx.compose.foundation.lazy.items // Replaced by Carousel items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CarouselState // Added for Carousel
import androidx.compose.material3.HorizontalMultiBrowseCarousel // Added for Carousel
import androidx.compose.material3.rememberCarouselState // Added for Carousel
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
import com.d4viddf.medicationreminder.ui.features.home.components.NextDoseCard
import com.d4viddf.medicationreminder.ui.features.home.components.TodayScheduleItem
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
                            // Use formattedReminderTime directly from the first item in the already processed NextDoseUiItem list
                            val formattedTime = uiState.nextDoseGroup.firstOrNull()?.formattedReminderTime ?: "Future"
                            Text(
                                "NEXT DOSE (at $formattedTime)",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val carouselState = rememberCarouselState { uiState.nextDoseGroup.count() }
                            HorizontalMultiBrowseCarousel(
                                state = carouselState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp), // Increased height to accommodate taller card + some padding
                                preferredItemWidth = 160.dp, // Matches NextDoseCard width
                                itemSpacing = 12.dp, // Increased spacing slightly
                                contentPadding = PaddingValues(horizontal = 8.dp) // Add some horizontal padding for the carousel itself
                            ) {carouselIndex ->
                                val item = uiState.nextDoseGroup[carouselIndex] // Now NextDoseUiItem
                                NextDoseCard(item = item) // Pass NextDoseUiItem
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

// NextDoseCard and TodayScheduleItem are now imported from their own files.
// Their definitions have been removed from this file.

@Preview(showBackground = true, name = "HomeScreen Preview (New Design)")
@Composable
fun HomeScreenNewPreview() {
    val sampleTime = LocalDateTime.now()
    val morningRawTime = sampleTime.withHour(8).format(ReminderCalculator.storableDateTimeFormatter)
    val afternoonRawTime = sampleTime.withHour(15).format(ReminderCalculator.storableDateTimeFormatter)

    val previewState = HomeViewModel.HomeState(
        currentGreeting = "Good morning! 🌤️",
        nextDoseGroup = listOf(
            NextDoseUiItem(1,101,"Amoxicillin","250mg","LIGHT_BLUE",null, morningRawTime, "08:00"),
            NextDoseUiItem(2,102,"Ibuprofen","200mg","LIGHT_RED", "http://example.com/ibu.png", morningRawTime, "08:00")
        ),
        todaysReminders = mapOf(
            "Morning" to listOf(
                MedicationReminder(5, 104, 5, morningRawTime, true, morningRawTime, null),
                MedicationReminder(1, 101, 1, morningRawTime, false, null, null)
            ),
            "Afternoon" to listOf(
                MedicationReminder(3, 101, 3, afternoonRawTime, false, null, null)
            ),
            "Evening" to emptyList(),
            "Night" to emptyList()
        ),
        hasUnreadAlerts = true,
        isLoading = false
    )

    // Dummy ViewModel for preview that directly exposes the state
    val previewViewModel = object : HomeViewModel(
        FakeMedicationReminderRepositoryPreview(),
        FakeMedicationRepositoryPreview(),
        FakeMedicationTypeRepositoryPreview()
    ) {
        override val uiState: StateFlow<HomeState> = MutableStateFlow(previewState)
        override fun markAsTaken(reminder: MedicationReminder) {} // No-op for preview
    }

    AppTheme {
        HomeScreen(
            navController = rememberNavController(),
            viewModel = previewViewModel
        )
    }
}


// --- Fake Repositories and DAOs for Preview ---

private class FakeMedicationReminderRepositoryPreview : com.d4viddf.medicationreminder.data.MedicationReminderRepository(
    FakeMedicationReminderDaoPreview(),
    FakeFirebaseSyncDaoPreview()
) {
    override fun getRemindersForDay(startOfDayString: String, endOfDayString: String): kotlinx.coroutines.flow.Flow<List<MedicationReminder>> {
        val now = LocalDateTime.now()
        val sampleRemindersList = listOf(
            MedicationReminder(1, 101,1, now.withHour(9).format(ReminderCalculator.storableDateTimeFormatter), false, null, null),
            MedicationReminder(5, 104,5, now.withHour(8).format(ReminderCalculator.storableDateTimeFormatter), true, now.withHour(8).format(ReminderCalculator.storableDateTimeFormatter), null)
        )
        return flowOf(sampleRemindersList)
    }
}

private class FakeMedicationRepositoryPreview : com.d4viddf.medicationreminder.repository.MedicationRepository(
    FakeMedicationDaoPreview(),
    FakeFirebaseSyncDaoPreview()
) {
    override suspend fun getMedicationById(id: Int): com.d4viddf.medicationreminder.data.Medication? {
        return com.d4viddf.medicationreminder.data.Medication(
            id = id,
            name = if (id == 101) "Amoxicillin" else "Ibuprofen",
            dosage = if (id == 101) "250mg" else "200mg",
            color = if (id == 101) "LIGHT_BLUE" else "LIGHT_RED",
            typeId = 1, // Example typeId
            packageSize = 20, remainingDoses = 10, startDate = null, endDate = null, reminderTime = null,
            nregistro = "12345"
        )
    }
}

private class FakeMedicationTypeRepositoryPreview : com.d4viddf.medicationreminder.data.MedicationTypeRepository(
    FakeMedicationTypeDaoPreview()
) {
    override suspend fun getMedicationTypeById(id: Int): com.d4viddf.medicationreminder.data.MedicationType? {
        return com.d4viddf.medicationreminder.data.MedicationType(
            id = id,
            name = "PILL", // Example type name
            imageUrl = if (id == 1) "http://example.com/pill.png" else null // Example imageUrl
        )
    }
}

private class FakeMedicationReminderDaoPreview : com.d4viddf.medicationreminder.data.MedicationReminderDao {
    override suspend fun insertReminder(reminder: MedicationReminder): Long = 0
    override suspend fun updateReminder(reminder: MedicationReminder): Int = 0
    override suspend fun deleteReminder(reminder: MedicationReminder) {}
    override suspend fun getReminderById(id: Int): MedicationReminder? = null
    override fun getRemindersForMedication(medicationId: Int): Flow<List<MedicationReminder>> = flowOf(emptyList())
    override fun getRemindersByStatus(isTaken: Boolean): Flow<List<MedicationReminder>> = flowOf(emptyList())
    override fun getFutureRemindersForMedication(medicationId: Int, currentTimeIso: String): Flow<List<MedicationReminder>> = flowOf(emptyList())
    override suspend fun deleteFutureUntakenRemindersForMedication(medicationId: Int, currentTimeIso: String) {}
    override fun getFutureUntakenRemindersForMedication(medicationId: Int, currentTimeIsoString: String): Flow<List<MedicationReminder>> = flowOf(emptyList())
    override suspend fun getRemindersForMedicationInWindow(medicationId: Int, startTime: String, endTime: String): List<MedicationReminder> = emptyList()
    override suspend fun deleteReminderById(reminderId: Int) {}
    override suspend fun getMostRecentTakenReminder(medicationId: Int): MedicationReminder? = null
    override fun getRemindersForDay(startOfDayString: String, endOfDayString: String): Flow<List<MedicationReminder>> = flowOf(emptyList())
}

private class FakeFirebaseSyncDaoPreview : com.d4viddf.medicationreminder.data.FirebaseSyncDao {
    override suspend fun insertSyncRecord(syncRecord: com.d4viddf.medicationreminder.data.FirebaseSync) {}
    override suspend fun updateSyncRecord(syncRecord: com.d4viddf.medicationreminder.data.FirebaseSync) {}
    override suspend fun deleteSyncRecord(syncRecord: com.d4viddf.medicationreminder.data.FirebaseSync) {}
    override fun getPendingSyncRecords(status: com.d4viddf.medicationreminder.data.SyncStatus): kotlinx.coroutines.flow.Flow<List<com.d4viddf.medicationreminder.data.FirebaseSync>> = flowOf(emptyList())
}

private class FakeMedicationDaoPreview : com.d4viddf.medicationreminder.data.MedicationDao {
    override fun getAllMedications(): Flow<List<com.d4viddf.medicationreminder.data.Medication>> = flowOf(emptyList())
    override suspend fun getMedicationById(id: Int): com.d4viddf.medicationreminder.data.Medication? = null
    override suspend fun insertMedication(medication: com.d4viddf.medicationreminder.data.Medication): Long = 0
    override suspend fun updateMedication(medication: com.d4viddf.medicationreminder.data.Medication) {}
    override suspend fun deleteMedication(medication: com.d4viddf.medicationreminder.data.Medication) {}
    override suspend fun getMedicationIdByName(name: String): Int? = null
}

private class FakeMedicationTypeDaoPreview : com.d4viddf.medicationreminder.data.MedicationTypeDao {
    override fun getAllMedicationTypes(): Flow<List<com.d4viddf.medicationreminder.data.MedicationType>> = flowOf(emptyList())
    override suspend fun getMedicationTypeById(id: Int): com.d4viddf.medicationreminder.data.MedicationType? = null
    override suspend fun insertMedicationType(medicationType: com.d4viddf.medicationreminder.data.MedicationType) {}
    override suspend fun updateMedicationType(medicationType: com.d4viddf.medicationreminder.data.MedicationType) {}
    override suspend fun deleteMedicationType(medicationType: com.d4viddf.medicationreminder.data.MedicationType) {}
}