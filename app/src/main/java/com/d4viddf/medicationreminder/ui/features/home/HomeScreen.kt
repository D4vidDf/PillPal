package com.d4viddf.medicationreminder.ui.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.ui.common.component.ConfirmationDialog
import com.d4viddf.medicationreminder.ui.common.component.bottomsheet.getBottomSheetData
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.features.home.components.NextDoseCard
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import com.d4viddf.medicationreminder.ui.features.todayschedules.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.WatchStatus
import com.d4viddf.medicationreminder.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Get the dynamic data configuration for the bottom sheet
    val bottomSheetData = getBottomSheetData(navController)
    LaunchedEffect(key1 = Unit) {
        viewModel.navigationEvents.collectLatest { route ->
            navController.navigate(route)
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onMarkAsTaken = viewModel::markAsTaken,
        onSkip = viewModel::skipDose,
        isFutureDose = viewModel::isFutureDose,
        onRefresh = viewModel::refreshData,
        onDismissConfirmationDialog = viewModel::dismissConfirmationDialog,
        navController = navController,
        onWatchIconClick = { viewModel.handleWatchIconClick() }
    )
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class, ExperimentalMaterial3ExpressiveApi::class
)
@Composable
internal fun HomeScreenContent(
    uiState: HomeViewModel.HomeState,
    onMarkAsTaken: (MedicationReminder) -> Unit,
    onSkip: (MedicationReminder) -> Unit,
    isFutureDose: (String) -> Boolean,
    onRefresh: () -> Unit,
    onDismissConfirmationDialog: () -> Unit,
    navController: NavController,
    onWatchIconClick: () -> Unit
) {
    if (uiState.showConfirmationDialog) {
        ConfirmationDialog(
            onDismissRequest = onDismissConfirmationDialog,
            onConfirmation = uiState.confirmationAction,
            dialogTitle = uiState.confirmationDialogTitle,
            dialogText = uiState.confirmationDialogText
        )
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    var topBarTitle by remember(uiState.currentGreeting) { mutableStateOf(uiState.currentGreeting) }
    val nextDosageTitle = stringResource(id = R.string.next_dosage_title)

    LaunchedEffect(uiState.nextDoseGroup, uiState.currentGreeting) {
        if (uiState.nextDoseGroup.isNotEmpty()) {
            delay(3000L)
            topBarTitle = nextDosageTitle
        } else {
            topBarTitle = uiState.currentGreeting
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = topBarTitle,
                        transitionSpec = {
                            slideInVertically { height -> height } togetherWith
                                    slideOutVertically { height -> -height }
                        }, label = "TopBarTitleAnimation"
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onWatchIconClick, shapes = IconButtonDefaults.shapes()) {
                        val isWatchConnected = uiState.watchStatus == WatchStatus.CONNECTED_APP_INSTALLED
                        val iconTint = if (isWatchConnected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        val contentDesc = if (isWatchConnected) stringResource(R.string.home_button_cd_watch_connected_settings) else stringResource(R.string.home_button_cd_watch_disconnected_settings)

                        BadgedBox(
                            badge = { if (isWatchConnected) Badge() },
                            modifier = Modifier.semantics { contentDescription = contentDesc }
                        ) {
                            Icon(painterResource(R.drawable.ic_rounded_devices_wearables_24), contentDescription = null, tint = iconTint)
                        }
                    }
                    IconButton(onClick = { /* TODO */ }, shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors()) {
                        val notificationsCd = if (uiState.hasUnreadAlerts) context.getString(R.string.home_button_cd_notifications_unread) else context.getString(R.string.home_button_cd_notifications_read)
                        BadgedBox(
                            badge = { if (uiState.hasUnreadAlerts) Badge() },
                            modifier = Modifier.semantics { contentDescription = notificationsCd }
                        ) {
                            Icon(painterResource(R.drawable.rounded_notifications_24), contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, onRefresh = onRefresh)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && !uiState.isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        when {
                            !uiState.hasRegisteredMedications -> NoMedicationsCard(
                                onAddMedication = { navController.navigate(Screen.AddMedication.route) }
                            )
                            uiState.todaysReminders.values.all { it.isEmpty() } -> NoScheduleTodayCard()
                            uiState.nextDoseGroup.isEmpty() -> NoMoreSchedulesTodayCard()
                            else -> {
                                val carouselState = rememberCarouselState { uiState.nextDoseGroup.size }
                                val largeItemWidth = if (screenWidthDp<440.dp) 400.dp else screenWidthDp * 0.4f

                                HorizontalMultiBrowseCarousel(
                                    state = carouselState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp),
                                    preferredItemWidth = largeItemWidth,
                                    itemSpacing = 8.dp,
                                ) { itemIndex ->
                                    val item = uiState.nextDoseGroup[itemIndex]
                                    NextDoseCard(
                                        item = item,
                                        modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                                        onNavigateToDetails = { medicationId ->
                                            navController.navigate(Screen.MedicationDetails.createRoute(medicationId))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.hasRegisteredMedications) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { navController.navigate(Screen.TodaySchedules.route) }) {
                                    Text(stringResource(id = R.string.show_all_button))
                                }
                            }
                        }

                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = stringResource(id = R.string.progress_section_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                MissedRemindersCard()
                                Spacer(modifier = Modifier.height(16.dp))
                                TodayProgressCard()
                            }
                        }

                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = stringResource(id = R.string.health_section_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                HeartRateCard()
                                Spacer(modifier = Modifier.height(16.dp))
                                WeightCard()
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
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

// --- INFORMATIONAL & MOCKUP SECTIONS ---

@Composable
fun NoMedicationsCard(onAddMedication: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Medication,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.no_medications_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.no_medications_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddMedication) {
                Text(stringResource(id = R.string.add_medication_button))
            }
        }
    }
}

@Composable
fun NoScheduleTodayCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Updated Height
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.no_schedule_today_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.no_schedule_today_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NoMoreSchedulesTodayCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Updated Height
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.no_more_schedules_today_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.no_more_schedules_today_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HeartRateCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.heart_rate_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "46-97",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(id = R.string.heart_rate_unit),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.today_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun WeightCard(modifier: Modifier = Modifier) {
    val lastDate = LocalDate.now().minusDays(3).format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.weight_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "80 kg",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(id = R.string.last_registered_label)} $lastDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun MissedRemindersCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.missed_reminders_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "2",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stringResource(id = R.string.last_missed_label)} Ibuprofen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun TodayProgressCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.today_progress_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "9/12",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            CircularProgressIndicator(
                progress = { 9f / 12f },
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "No Registered Medications")
@Composable
fun HomeScreenNoMedsPreview() {
    val previewState = HomeViewModel.HomeState(
        currentGreeting = "Good morning!",
        hasRegisteredMedications = false,
        isLoading = false
    )
    AppTheme {
        HomeScreenContent(
            uiState = previewState,
            onMarkAsTaken = {}, onSkip = {}, isFutureDose = { false },
            onRefresh = {}, onDismissConfirmationDialog = {},
            navController = rememberNavController(),
            onWatchIconClick = {}
        )
    }
}

@Preview(showBackground = true, name = "No Schedule Today")
@Composable
fun HomeScreenNoSchedulePreview() {
    val previewState = HomeViewModel.HomeState(
        currentGreeting = "Good afternoon!",
        hasRegisteredMedications = true,
        todaysReminders = emptyMap(),
        isLoading = false
    )
    AppTheme {
        HomeScreenContent(
            uiState = previewState,
            onMarkAsTaken = {}, onSkip = {}, isFutureDose = { false },
            onRefresh = {}, onDismissConfirmationDialog = {},
            navController = rememberNavController(),
            onWatchIconClick = {}
        )
    }
}

@Preview(showBackground = true, name = "No More Doses Today")
@Composable
fun HomeScreenNoMoreDosesPreview() {
    val previewState = HomeViewModel.HomeState(
        currentGreeting = "Good evening!",
        hasRegisteredMedications = true,
        todaysReminders = mapOf("Morning" to listOf(mockTodayScheduleUiItem())),
        nextDoseGroup = emptyList(),
        isLoading = false
    )
    AppTheme {
        HomeScreenContent(
            uiState = previewState,
            onMarkAsTaken = {}, onSkip = {}, isFutureDose = { false },
            onRefresh = {}, onDismissConfirmationDialog = {},
            navController = rememberNavController(),
            onWatchIconClick = {}
        )
    }
}

@Preview(showBackground = true, name = "One Dose in Carousel")
@Composable
fun HomeScreenOneDosePreview() {
    val previewState = HomeViewModel.HomeState(
        currentGreeting = "Next Dosage",
        hasRegisteredMedications = true,
        nextDoseGroup = listOf(mockNextDoseUiItem(1, "Amoxicillin")),
        todaysReminders = mapOf("Morning" to listOf(mockTodayScheduleUiItem())),
        isLoading = false
    )
    AppTheme {
        HomeScreenContent(
            uiState = previewState,
            onMarkAsTaken = {}, onSkip = {}, isFutureDose = { false },
            onRefresh = {}, onDismissConfirmationDialog = {},
            navController = rememberNavController(),
            onWatchIconClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Two Doses in Carousel")
@Composable
fun HomeScreenTwoDosesPreview() {
    val previewState = HomeViewModel.HomeState(
        currentGreeting = "Next Dosage",
        hasRegisteredMedications = true,
        nextDoseGroup = listOf(
            mockNextDoseUiItem(1, "Amoxicillin"),
            mockNextDoseUiItem(2, "Ibuprofen", "LIGHT_RED")
        ),
        todaysReminders = mapOf("Morning" to listOf(mockTodayScheduleUiItem())),
        isLoading = false
    )
    AppTheme {
        HomeScreenContent(
            uiState = previewState,
            onMarkAsTaken = {}, onSkip = {}, isFutureDose = { false },
            onRefresh = {}, onDismissConfirmationDialog = {},
            navController = rememberNavController(),
            onWatchIconClick = {}
        )
    }
}


// --- Helper functions for Previews ---
private fun mockNextDoseUiItem(id: Int, name: String, color: String = "LIGHT_BLUE"): NextDoseUiItem {
    val time = LocalDateTime.now().withHour(8).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    return NextDoseUiItem(id, 100 + id, name, "250mg", color, null, time, "08:00")
}

private fun mockTodayScheduleUiItem(): TodayScheduleUiItem {
    val time = LocalDateTime.now().withHour(15).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    return TodayScheduleUiItem(
        MedicationReminder(1, 101, 1, time, false, null, null),
        medicationName = "Ibuprofeno",
        medicationDosage = "200mg",
        medicationColorName = "LIGHT_BLUE",
        medicationIconUrl = null,
        medicationTypeName = "Pill",
        formattedReminderTime = "15:00"
    )
}