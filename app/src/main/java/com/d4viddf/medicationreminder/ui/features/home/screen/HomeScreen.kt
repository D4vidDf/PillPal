package com.d4viddf.medicationreminder.ui.features.home.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi // Added for Pager
import androidx.compose.foundation.layout.*
// import androidx.compose.material3.carousel.CarouselState // Replaced by PagerState
// import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel // Replaced by HorizontalPager
// import androidx.compose.material3.carousel.rememberCarouselState // Replaced by rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager // Added
import androidx.compose.foundation.pager.rememberPagerState // Added
import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.LazyRow // Replaced by Carousel
//import androidx.compose.foundation.lazy.items // Replaced by Carousel items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.ui.platform.LocalConfiguration // Ensure this is imported
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
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
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    // Dynamic Pager Padding
    val pagerHorizontalPadding = (screenWidthDp * 0.12f).coerceIn(72f, 2200f)

    // Dynamic Card Widths
    // Calculate available width for the pager's content area
    val pagerContentAreaWidth = screenWidthDp - (pagerHorizontalPadding * 3)

    // Max width for the centered card (e.g., 50% of content area, or a fixed large size on small screens)
    val maxCardWidth = (pagerContentAreaWidth * 0.55f).coerceIn(180f, 600f)
    // Min width for side cards (e.g., 70% of maxCardWidth)
    val minCardWidth = (maxCardWidth * 0.70f).coerceIn(140f, 300f)


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
                            val pagerState = rememberPagerState { uiState.nextDoseGroup.count() }
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = pagerHorizontalPadding.dp),
                                pageSpacing = 0.dp
                            ) { pageIndex ->
                                val item = uiState.nextDoseGroup[pageIndex]

                                val pageOffset = (
                                    (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                                ).absoluteValue

                                val currentCardTargetWidth = lerp(
                                    start = minCardWidth,
                                    stop = maxCardWidth,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                ).coerceIn(minCardWidth, maxCardWidth)

                                val scale = lerp(
                                    start = 0.95f,
                                    stop = 1.0f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                ).coerceIn(0.95f, 1.0f)

                                val alpha = lerp(
                                    start = 0.7f, // Slightly less aggressive alpha
                                    stop = 1f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                ).coerceIn(0.7f, 1.0f)

                                Box(
                                    modifier = Modifier
                                        .width(currentCardTargetWidth.dp)
                                        .fillMaxHeight()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            this.alpha = alpha
                                        }
                                ) {
                                    NextDoseCard(item = item)
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