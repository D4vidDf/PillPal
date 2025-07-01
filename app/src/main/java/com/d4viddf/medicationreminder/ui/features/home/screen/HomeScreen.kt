package com.d4viddf.medicationreminder.ui.features.home.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi // Added for Pager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.carousel.CarouselState // Added for Carousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel // Changed for Uncontained Carousel
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel // Added back for tablets
import androidx.compose.material3.carousel.rememberCarouselState // Added for Carousel
// import androidx.compose.foundation.pager.HorizontalPager // Replaced by Carousel
// import androidx.compose.foundation.pager.rememberPagerState // Replaced by Carousel
import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.LazyRow // Replaced by Carousel
//import androidx.compose.foundation.lazy.items // Replaced by Carousel items
// import androidx.compose.foundation.pager.HorizontalPager // Replaced by Carousel
// import androidx.compose.foundation.pager.rememberPagerState // Replaced by Carousel
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.ui.platform.LocalConfiguration // Ensure this is imported
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
import androidx.compose.ui.platform.LocalContext // Added for Context
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.ui.features.home.components.NextDoseCard
import com.d4viddf.medicationreminder.ui.features.home.components.TodayScheduleItem
// Import NotificationScheduler if its definition is needed for the fake, assuming a path
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import com.d4viddf.medicationreminder.ui.features.home.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeScreenContent(
        uiState = uiState,
        onMarkAsTaken = viewModel::markAsTaken,
        navController = navController // Pass NavController if TopAppBar actions need it
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // Added ExperimentalFoundationApi
@Composable
internal fun HomeScreenContent(
    uiState: HomeViewModel.HomeState,
    onMarkAsTaken: (MedicationReminder) -> Unit,
    navController: NavController
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp.value > 600 // Example threshold for tablets, comparing Dp.value to Int

    // Define card width for tablets, e.g., a fraction of screen width or a larger fixed value
    val tabletCardWidth = (screenWidthDp * 0.3f).coerceIn(200.dp, 300.dp) // Example: 30% of screen, capped

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.currentGreeting, style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { /* TODO: navController.navigate(...) */ }) {
                        Icon(Icons.Filled.Watch, contentDescription = "Connect Wear OS")
                    }
                    IconButton(onClick = { /* TODO: navController.navigate(...) */ }) {
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
                            val formattedTime = uiState.nextDoseGroup.firstOrNull()?.formattedReminderTime ?: "Future"
                            Text(
                                "NEXT DOSE (at $formattedTime)",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val carouselState = rememberCarouselState { uiState.nextDoseGroup.count() }

                            if (isTablet) {
                                HorizontalMultiBrowseCarousel(
                                    state = carouselState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp), // Give carousel a bit more height on tablets
                                    preferredItemWidth = tabletCardWidth,
                                    itemSpacing = 16.dp, // Larger spacing for tablets
                                    contentPadding = PaddingValues(horizontal = 32.dp) // More padding for tablets
                                ) { pageIndex ->
                                    val item = uiState.nextDoseGroup[pageIndex]
                                    Box(modifier = Modifier.width(tabletCardWidth)) { // Ensure card takes the preferred width
                                        NextDoseCard(item = item, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            } else {
                                HorizontalUncontainedCarousel(
                                    state = carouselState,
                                    modifier = Modifier.fillMaxWidth(),
                                    itemWidth = 150.dp, // Fixed width for phones
                                    contentPadding = PaddingValues(horizontal = 24.dp), // Increased padding for phones
                                    itemSpacing = 8.dp
                                ) { pageIndex ->
                                    val item = uiState.nextDoseGroup[pageIndex]
                                    NextDoseCard(item = item, modifier = Modifier.width(140.dp)) // Reduced width for phone
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
                                onMarkAsTaken = { onMarkAsTaken(reminder) }, // Use passed lambda
                                timeFormatter = timeFormatter
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }
    }
}


@Preview(showBackground = true, name = "HomeScreen Preview (New Design)")
@Composable
fun HomeScreenNewPreview() {
    val sampleTime = LocalDateTime.now()
    val morningRawTime = sampleTime.withHour(8).format(ReminderCalculator.storableDateTimeFormatter)
    val afternoonRawTime = sampleTime.withHour(15).format(ReminderCalculator.storableDateTimeFormatter)

    val previewState = HomeViewModel.HomeState(
        currentGreeting = "Good morning! 🌤️",
        nextDoseGroup = listOf(
            NextDoseUiItem(
                1,
                101,
                "Amoxicillin",
                "250mg",
                "LIGHT_BLUE",
                null,
                morningRawTime,
                "08:00"
            ),
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

    AppTheme {
        HomeScreenContent(
            uiState = previewState,
            onMarkAsTaken = {}, // No-op for preview
            navController = rememberNavController() // Pass a dummy NavController
        )
    }
}

// Linear interpolation helper for Float
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

// Linear interpolation helper for Dp
private fun lerp(start: androidx.compose.ui.unit.Dp, stop: androidx.compose.ui.unit.Dp, fraction: Float): androidx.compose.ui.unit.Dp {
    return androidx.compose.ui.unit.Dp(lerp(start.value, stop.value, fraction))
}


// --- Fake Repositories and DAOs for Preview (Still defined for completeness, though HomeScreenNewPreview is decoupled) ---

private class FakeNotificationSchedulerPreview : NotificationScheduler() {
    // Minimal fake, NotificationScheduler has an empty constructor.
}

private class FakeMedicationReminderRepositoryPreview(
    medicationReminderDao: com.d4viddf.medicationreminder.data.MedicationReminderDao,
    firebaseSyncDao: com.d4viddf.medicationreminder.data.FirebaseSyncDao
) : com.d4viddf.medicationreminder.data.MedicationReminderRepository(
    medicationReminderDao,
    firebaseSyncDao
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

private class FakeMedicationRepositoryPreview(context: android.content.Context) : com.d4viddf.medicationreminder.repository.MedicationRepository(
    context,
    FakeMedicationDaoPreview(),
    FakeMedicationReminderDaoPreview(), // This needs its DAO dependencies
    FakeNotificationSchedulerPreview(), // Instantiated
    FakeFirebaseSyncDaoPreview()
) {
    // It's generally problematic to @Override methods of a final class.
    // The main goal here is to ensure the constructor of FakeMedicationRepositoryPreview
    // can successfully call the super constructor of the real MedicationRepository.
    // If HomeViewModel directly calls methods on this fake that are not part of an interface
    // and are not open in the real repository, it would lead to issues in tests,
    // but for previews where we directly set state, it might be less critical
    // as long as the object can be constructed.

    // Example: if getMedicationById was open in MedicationRepository
    // override suspend fun getMedicationById(id: Int): com.d4viddf.medicationreminder.data.Medication? {
    // return com.d4viddf.medicationreminder.data.Medication(
    // ...
    // )
    // }
    // Since it's likely not open, we cannot override it.
    // The previewViewModel directly sets state, so this fake's method implementations
    // are less critical *for the preview itself*, but constructor must be valid.
    // For the preview to work with `previewViewModel.loadTodaysSchedule()`, these fakes would need to be more robust or an interface used.
    // But since `HomeScreenNewPreview` calls `HomeScreenContent` with hardcoded state, this is okay.
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
    suspend fun getMedicationIdByName(name: String): Int? = null
}

private class FakeMedicationTypeDaoPreview : com.d4viddf.medicationreminder.data.MedicationTypeDao {
    override fun getAllMedicationTypes(): Flow<List<com.d4viddf.medicationreminder.data.MedicationType>> = flowOf(emptyList())
    override suspend fun getMedicationTypeById(id: Int): com.d4viddf.medicationreminder.data.MedicationType? = null
    override suspend fun insertMedicationType(medicationType: com.d4viddf.medicationreminder.data.MedicationType) {}
    override suspend fun updateMedicationType(medicationType: com.d4viddf.medicationreminder.data.MedicationType) {}
    override suspend fun deleteMedicationType(medicationType: com.d4viddf.medicationreminder.data.MedicationType) {}
    override suspend fun deleteAllMedicationTypes() {} // Added
    override suspend fun getMedicationTypeByName(name: String): com.d4viddf.medicationreminder.data.MedicationType? = null // Added
    override suspend fun getMedicationTypesByIds(ids: List<Int>): List<com.d4viddf.medicationreminder.data.MedicationType> = emptyList() // Added
}