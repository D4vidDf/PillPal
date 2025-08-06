package com.d4viddf.medicationreminder.ui.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
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
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.ui.common.component.ConfirmationDialog
import com.d4viddf.medicationreminder.ui.features.home.components.NextDoseCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.*
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.WatchStatus
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeItem
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeSection
import com.d4viddf.medicationreminder.ui.features.todayschedules.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeLayout by viewModel.homeLayout.collectAsState()

    LaunchedEffect(key1 = Unit) {
        viewModel.navigationEvents.collectLatest { route ->
            navController.navigate(route)
        }
    }

    HomeScreenContent(
        uiState = uiState,
        homeLayout = homeLayout,
        viewModel = viewModel,
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
    homeLayout: List<HomeSection>,
    viewModel: HomeViewModel,
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
        val hasVisibleItems = homeLayout.any { section -> section.items.any { it.isVisible } }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && !uiState.isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1: Carousel / No Meds / No Schedule
                    item {
                        when {
                            !uiState.hasRegisteredMedications -> NoMedicationsCard(
                                onAddMedication = { navController.navigate(Screen.AddMedication.route) }
                            )
                            uiState.todaysReminders.values.all { it.isEmpty() } -> NoScheduleTodayCard()
                            uiState.nextDoseGroup.isEmpty() -> NoMoreSchedulesTodayCard()
                            else -> {
                                val carouselState = rememberCarouselState { uiState.nextDoseGroup.size }
                                HorizontalMultiBrowseCarousel(
                                    state = carouselState,
                                    modifier = Modifier.fillMaxWidth().height(180.dp),
                                    preferredItemWidth = if (screenWidthDp < 440.dp) 400.dp else screenWidthDp * 0.4f,
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

                    // Item 2: "Show All" button (only if meds exist)
                    if (uiState.hasRegisteredMedications) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { navController.navigate(Screen.TodaySchedules.route) }) {
                                    Text(stringResource(id = R.string.show_all_button))
                                }
                            }
                        }
                    }

                    // Items 3+: Dynamic Sections
                    if (homeLayout.isEmpty() && uiState.hasRegisteredMedications) {
                        item {
                            EmptyHomeState(
                                onNavigateToPersonalize = { navController.navigate(Screen.PersonalizeHome.route) }
                            )
                        }
                    } else if (!hasVisibleItems && homeLayout.isNotEmpty()) {
                        item {
                            EmptyHomeState(
                                onNavigateToPersonalize = { navController.navigate(Screen.PersonalizeHome.route) }
                            )
                        }
                    }
                    else {
                        homeLayout.forEach { section ->
                            val visibleItems = section.items.filter { it.isVisible }
                            if (visibleItems.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = section.name,
                                        onEditClick = { navController.navigate(Screen.PersonalizeHome.route) }
                                    )
                                }
                                items(visibleItems, key = { it.id }) { item ->
                                    when (item.id) {
                                        "today_progress" -> TodayProgressCard(
                                            taken = viewModel.todayProgressTaken,
                                            total = viewModel.todayProgressTotal
                                        )
                                        "next_dose" -> viewModel.nextDoseTimeInSeconds?.let {
                                            NextDoseTimeCard(
                                                timeToNextDoseInSeconds = it,
                                                displayUnit = item.displayUnit ?: "minutes"
                                            )
                                        }
                                        "heart_rate" -> HeartRateCard(heartRate = viewModel.heartRate)
                                        "weight" -> WeightCard(weight = viewModel.weight)
                                        "missed_reminders" -> MissedRemindersCard(
                                            missedDoses = viewModel.missedDoses,
                                            lastMissedMedication = viewModel.lastMissedMedication
                                        )
                                    }
                                }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyHomeState(modifier: Modifier = Modifier, onNavigateToPersonalize: () -> Unit) {
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Your Home Screen is Empty!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You can add and arrange cards to personalize your view.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNavigateToPersonalize,
                shapes = ButtonDefaults.shapes()) {
                Text("Personalize Home")
            }
        }
    }
}

@Composable
fun NoMedicationsCard(onAddMedication: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp),
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
        modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp),
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