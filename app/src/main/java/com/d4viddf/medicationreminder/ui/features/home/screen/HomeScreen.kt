package com.d4viddf.medicationreminder.ui.features.home.screen

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.CloudOff // For not connected
import androidx.compose.material.icons.outlined.Download // For app not installed
import androidx.compose.material.icons.outlined.Watch // For connected app installed (optional variant)
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.FirebaseSync
import com.d4viddf.medicationreminder.data.FirebaseSyncDao
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationDao
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderDao
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.data.MedicationTypeDao
import com.d4viddf.medicationreminder.data.SyncStatus
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.features.home.components.NextDoseCard
import com.d4viddf.medicationreminder.ui.features.home.components.TodayScheduleItem
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.WatchStatus // Import WatchStatus
import com.d4viddf.medicationreminder.ui.features.home.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Collect navigation events
    LaunchedEffect(key1 = Unit) { // Use Unit or a key that doesn't change often
        viewModel.navigationEvents.collectLatest { route ->
            navController.navigate(route)
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onMarkAsTaken = viewModel::markAsTaken,
        onRefresh = viewModel::refreshData, // Pass refresh lambda
        navController = navController, // Pass navController for other internal navigations if any
        onWatchIconClick = { viewModel.handleWatchIconClick() } // Pass the handler
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class
)
@Composable
internal fun HomeScreenContent(
    uiState: HomeViewModel.HomeState,
    onMarkAsTaken: (MedicationReminder) -> Unit,
    onRefresh: () -> Unit, // Added onRefresh callback
    navController: NavController, // Keep for existing internal navigations
    onWatchIconClick: () -> Unit // Added callback for watch icon click
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp > 600 // Example threshold for tablets, comparing Dp.value to Int

    // Initialize all parts of day to be expanded by default
    val sectionExpandedStates = remember {
        mutableStateMapOf(
            "Morning" to true,
            "Afternoon" to true,
            "Evening" to true,
            "Night" to true
        )
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val partOfDayHeaderIndices = remember { mutableMapOf<String, Int>() }

    // Moved LaunchedEffect to a higher level (HomeScreenContent scope)
    LaunchedEffect(
        uiState.nextDoseGroup,
        uiState.todaysReminders,
        sectionExpandedStates.toMap()
    ) {
        var idx = 0
        partOfDayHeaderIndices.clear()

        if (uiState.nextDoseGroup.isNotEmpty()) {
            idx++ // Next Dose Carousel
        } else {
            idx++ // "No upcoming doses" message
        }
        idx++ // "TODAY'S SCHEDULE" sticky header

        uiState.todaysReminders.forEach { (partOfDay, remindersInPart) ->
            if (remindersInPart.isNotEmpty()) {
                partOfDayHeaderIndices[partOfDay] = idx
                idx++ // Part of Day Sticky Header
                if (sectionExpandedStates[partOfDay] == true) {
                    idx += remindersInPart.size // Items in this part of day
                }
            }
        }
    }

    // Define card width for tablets using HorizontalPager
    val tabletPagerItemWidth = screenWidthDp * 0.7f // Example: 70% of screen width for tablet items

    // Define peeking amount and card width for phones
    val phonePeekWidth = 80
    val phonePagerItemWidth =
        screenWidthDp - (2 * phonePeekWidth) // Central item takes screen width minus peek areas

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.currentGreeting,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    val context = LocalContext.current

                    // Watch Icon Button
                    IconButton(onClick = onWatchIconClick) { // Use the passed callback
                        val watchIcon = painterResource(R.drawable.rounded_watch_24)
                        val iconTint = when (uiState.watchStatus) {
                            WatchStatus.NOT_CONNECTED -> Color.Gray
                            WatchStatus.CONNECTED_APP_NOT_INSTALLED -> MaterialTheme.colorScheme.error // Or a warning color
                            WatchStatus.CONNECTED_APP_INSTALLED -> MaterialTheme.colorScheme.primary
                            WatchStatus.UNKNOWN -> LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                        }
                        val contentDescRes = when (uiState.watchStatus) {
                            WatchStatus.NOT_CONNECTED -> R.string.home_button_cd_watch_not_connected
                            WatchStatus.CONNECTED_APP_NOT_INSTALLED -> R.string.home_button_cd_watch_app_not_installed
                            WatchStatus.CONNECTED_APP_INSTALLED -> R.string.home_button_cd_watch_connected
                            WatchStatus.UNKNOWN -> R.string.home_button_cd_connect_wear_os // Default
                        }

                        Icon(
                            watchIcon,
                            contentDescription = context.getString(contentDescRes),
                            tint = iconTint
                        )
                    }

                    // Notifications Icon Button
                    IconButton(onClick = { /* TODO: navController.navigate(...) */ }) {
                        val notificationsCd = if (uiState.hasUnreadAlerts) {
                            context.getString(R.string.home_button_cd_notifications_unread)
                        } else {
                            context.getString(R.string.home_button_cd_notifications_read)
                        }
                        BadgedBox(
                            badge = { if (uiState.hasUnreadAlerts) Badge() },
                            modifier = Modifier.semantics { contentDescription = notificationsCd }
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = null
                            ) // Description handled by BadgedBox
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, onRefresh = onRefresh)
        val context = LocalContext.current // For string resources

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && !uiState.isRefreshing) { // Show full screen loader only on initial load
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // partOfDayHeaderIndices is now defined and calculated in HomeScreenContent scope

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize(),
                            // Horizontal padding is now applied to sticky header Surface and items individually
                            // .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy (16.dp) // This adds space *between* items
                ) {
                    // Next Dose Carousel
                    if (uiState.nextDoseGroup.isNotEmpty()) {
                        item {
                            Column {
                                val nextDoseAtTime = uiState.nextDoseAtTime
                                val timeRemaining = uiState.nextDoseTimeRemaining

                                // Hoist stringResource calls to be direct children of the composable scope
                                val strNextDoseTimeAtIn = if (nextDoseAtTime != null && timeRemaining != null) {
                                    stringResource(R.string.next_dose_time_at_in, nextDoseAtTime, timeRemaining)
                                } else null

                                val strNextDoseTimeIn = if (timeRemaining != null) {
                                    stringResource(R.string.next_dose_time_in, timeRemaining)
                                } else null

                                val strNextDoseAt = if (nextDoseAtTime != null) {
                                    stringResource(R.string.next_dose_at, nextDoseAtTime)
                                } else null

                                val strNoUpcomingDoses = stringResource(R.string.no_upcoming_doses_at_all)

                                val textToShow =
                                    if (nextDoseAtTime != null && timeRemaining != null && strNextDoseTimeAtIn != null && strNextDoseTimeIn != null) {
                                        val startIndex = strNextDoseTimeAtIn.indexOf(strNextDoseTimeIn)
                                        if (startIndex != -1) {
                                            buildAnnotatedString {
                                                append(strNextDoseTimeAtIn.substring(0, startIndex))
                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append(strNextDoseTimeIn)
                                                }
                                                append(strNextDoseTimeAtIn.substring(startIndex + strNextDoseTimeIn.length))
                                            }
                                        } else {
                                            AnnotatedString(strNextDoseTimeAtIn) // Fallback
                                        }
                                    } else if (nextDoseAtTime != null && strNextDoseAt != null) {
                                        AnnotatedString(strNextDoseAt)
                                    } else {
                                        AnnotatedString(strNoUpcomingDoses)
                                    }

                                Text(
                                    text = textToShow,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp,).padding(horizontal = 16.dp)
                                )
                                // Common PagerState for both phone and tablet
                                val pagerState =
                                    rememberPagerState(pageCount = { uiState.nextDoseGroup.size })

                                val currentItemWidth =
                                    if (isTablet) tabletPagerItemWidth else phonePagerItemWidth

                                val calculatedHorizontalPadding = if (isTablet) {
                                    // For tablets, padding to center the (currentItemWidth) card
                                    ((screenWidthDp - currentItemWidth.toInt()) / 2).coerceAtLeast(0)
                                } else {
                                    // For phones, padding is the peekWidth itself
                                    phonePeekWidth
                                }

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth()
                                        .height(220.dp), // Consistent height
                                    contentPadding = PaddingValues(horizontal = calculatedHorizontalPadding.dp),
                                    pageSpacing = if (isTablet) 16.dp else 4.dp // Adjust spacing based on device
                                ) { pageIndex ->
                                    val item = uiState.nextDoseGroup[pageIndex]
                                    val pageOffset = (
                                            (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                                            ).absoluteValue.coerceIn(0f, 1f)

                                    // Adjust scale factor based on device type if needed, or use a common one
                                    val scaleFactor =
                                        if (isTablet) 0.90f else 0.85f // Slightly less scaling on tablets
                                    val alphaFactor = if (isTablet) 0.6f else 0.5f

                                    NextDoseCard(
                                        item = item,
                                        modifier = Modifier
                                            .width(currentItemWidth.toInt().dp) // Use the determined width for the current device
                                            .graphicsLayer {
                                                // Apply transformations: scale and alpha
                                                // Items further from the center will be smaller and more transparent
                                                val scale = lerp(1f, 0.85f, pageOffset)
                                                scaleX = scale
                                                scaleY = scale
                                                alpha = lerp(1f, 0.5f, pageOffset)
                                            },
                                        onNavigateToDetails = { medicationId ->
                                            navController.navigate(
                                                com.d4viddf.medicationreminder.ui.navigation.Screen.MedicationDetails.createRoute(
                                                    medicationId
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            // The ViewModel currently sets nextDoseTimeRemaining and nextDoseAtTime to null
                            // if nextDoseGroup is empty.
                            // A future enhancement in ViewModel could fetch the *absolute* next dose
                            // even if it's not today, and populate a specific field for that.
                            // For now, we'll use a general message.
                            Text(
                                text = LocalContext.current.getString(R.string.no_upcoming_doses_at_all), // Or a more specific "no doses today"
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    // Today's Schedule
                    stickyHeader {
                        Surface(modifier = Modifier.fillParentMaxWidth()) { // Surface to provide a background
                            Text(
                                "TODAY'S SCHEDULE", // This could also be a string resource
                                style = MaterialTheme.typography.titleLarge, // Increased font size
                                modifier = Modifier
                                    .padding(top = 16.dp, bottom = 8.dp)
                                    .fillMaxWidth() // Ensure text background spans width
                                    .background(MaterialTheme.colorScheme.surface) // Match screen background
                                    .padding(horizontal = 16.dp) // Re-apply horizontal padding if consumed by Surface
                            )
                        }
                    }

                    uiState.todaysReminders.forEach { (partOfDay, remindersInPart) ->
                        if (remindersInPart.isNotEmpty()) {
                            val isExpanded = sectionExpandedStates[partOfDay]
                                ?: true // Default to expanded if not found
                            val headerIndex = partOfDayHeaderIndices[partOfDay]

                            // Clickable Header for each section (Morning, Afternoon, etc.)
                            stickyHeader(key = "header-$partOfDay") {
                                Surface(modifier = Modifier.fillParentMaxWidth()) { // Surface for background
                                    val partOfDayTime = try {
                                        LocalDateTime.parse(
                                            remindersInPart.first().formattedReminderTime,
                                            ReminderCalculator.storableDateTimeFormatter
                                        )
                                            .format(timeFormatter)
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    val expandedStateText =
                                        if (isExpanded) context.getString(R.string.state_expanded) else context.getString(
                                            R.string.state_collapsed
                                        )
                                    val actionText =
                                        if (isExpanded) context.getString(R.string.action_collapse) else context.getString(
                                            R.string.action_expand
                                        )
                                    val headerCd = context.getString(
                                        R.string.home_section_header_cd,
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (headerIndex != null) {
                                                    coroutineScope.launch {
                                                        lazyListState.animateScrollToItem(index = headerIndex)
                                                    }
                                                }
                                                // Toggle expansion state
                                                sectionExpandedStates[partOfDay] = !isExpanded
                                            }
                                            .semantics { contentDescription = headerCd }
                                            .background(MaterialTheme.colorScheme.surface) // Match screen background
                                            .padding(
                                                vertical = 8.dp,
                                                horizontal = 16.dp
                                            ), // Re-apply horizontal padding
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween // Pushes icon to the end
                                    ) {
                                        Text(
                                            text = "$partOfDay ($partOfDayTime)",
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        // Icon's CD is good as is, it will be part of the Row's description
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                            contentDescription = null // Described by the Row's CD
                                            // Original: if (isExpanded) "Collapse $partOfDay" else "Expand $partOfDay"
                                        )
                                    }
                                }
                            }

                            // Conditionally display items based on expanded state
                            if (isExpanded) {
                                items(
                                    remindersInPart,
                                    key = { "schedule-${it.reminder.id}" }) { scheduleItem ->
                                    // Apply horizontal padding to items if not already handled by their own content
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        TodayScheduleItem(
                                            item = scheduleItem, // Pass the TodayScheduleUiItem
                                            onMarkAsTaken = { reminder -> onMarkAsTaken(reminder) }, // ViewModel expects MedicationReminder
                                            onNavigateToDetails = { medicationId ->
                                                navController.navigate(
                                                    com.d4viddf.medicationreminder.ui.navigation.Screen.MedicationDetails.createRoute(
                                                        medicationId
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    } // Add some space at the bottom
                }

                PullRefreshIndicator(
                    refreshing = uiState.isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
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
                TodayScheduleUiItem(
                    MedicationReminder(3, 101, 3, afternoonRawTime, false, null, null),
                    medicationName = "Ibuprofeno",
                    medicationDosage = "200mg",
                    medicationColorName = "LIGHT_BLUE",
                    medicationIconUrl = null,
                    medicationTypeName = "Pill",
                    formattedReminderTime = "08:00",
                )
            ),
            "Afternoon" to listOf(
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
            onRefresh = {}, // No-op for preview
            navController = rememberNavController(), // Pass a dummy NavController
            onWatchIconClick = {} // Add dummy handler for preview
        )
    }
}

// Linear interpolation helper for Float
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

// Linear interpolation helper for Dp
private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp {
    return Dp(lerp(start.value, stop.value, fraction))
}


// --- Fake Repositories and DAOs for Preview (Still defined for completeness, though HomeScreenNewPreview is decoupled) ---

private class FakeNotificationSchedulerPreview : NotificationScheduler() {
    // Minimal fake, NotificationScheduler has an empty constructor.
}

private class FakeMedicationReminderRepositoryPreview(
    private val context: Context, // Context first
    medicationReminderDao: MedicationReminderDao,
    firebaseSyncDao: FirebaseSyncDao
) : MedicationReminderRepository(
    context, // Pass context
    medicationReminderDao,
    firebaseSyncDao
) {
    override fun getRemindersForDay(startOfDayString: String, endOfDayString: String): Flow<List<MedicationReminder>> {
        val now = LocalDateTime.now()
        val sampleRemindersList = listOf(
            MedicationReminder(1, 101,1, now.withHour(9).format(ReminderCalculator.storableDateTimeFormatter), false, null, null),
            MedicationReminder(5, 104,5, now.withHour(8).format(ReminderCalculator.storableDateTimeFormatter), true, now.withHour(8).format(ReminderCalculator.storableDateTimeFormatter), null)
        )
        return flowOf(sampleRemindersList)
    }
}

private class FakeMedicationRepositoryPreview(context: Context) : MedicationRepository(
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

private class FakeMedicationReminderDaoPreview : MedicationReminderDao {
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

private class FakeFirebaseSyncDaoPreview : FirebaseSyncDao {
    override suspend fun insertSyncRecord(syncRecord: FirebaseSync) {}
    override suspend fun updateSyncRecord(syncRecord: FirebaseSync) {}
    override suspend fun deleteSyncRecord(syncRecord: FirebaseSync) {}
    override fun getPendingSyncRecords(status: SyncStatus): Flow<List<FirebaseSync>> = flowOf(emptyList())
}

private class FakeMedicationDaoPreview : MedicationDao {
    override fun getAllMedications(): Flow<List<Medication>> = flowOf(emptyList())
    override suspend fun getMedicationById(id: Int): Medication? = null
    override suspend fun insertMedication(medication: Medication): Long = 0
    override suspend fun updateMedication(medication: Medication) {}
    override suspend fun deleteMedication(medication: Medication) {}
    suspend fun getMedicationIdByName(name: String): Int? = null
}

private class FakeMedicationTypeDaoPreview : MedicationTypeDao {
    override fun getAllMedicationTypes(): Flow<List<MedicationType>> = flowOf(emptyList())
    override suspend fun getMedicationTypeById(id: Int): MedicationType? = null
    override suspend fun insertMedicationType(medicationType: MedicationType) {}
    override suspend fun updateMedicationType(medicationType: MedicationType) {}
    override suspend fun deleteMedicationType(medicationType: MedicationType) {}
    override suspend fun deleteAllMedicationTypes() {} // Added
    override suspend fun getMedicationTypeByName(name: String): MedicationType? = null // Added
    override suspend fun getMedicationTypesByIds(ids: List<Int>): List<MedicationType> = emptyList() // Added
}