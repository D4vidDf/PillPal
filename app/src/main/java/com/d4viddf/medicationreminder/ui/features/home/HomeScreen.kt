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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.ui.common.component.ConfirmationDialog
import com.d4viddf.medicationreminder.ui.features.home.components.NextDoseCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.HeartRateCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.MissedRemindersCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.NextDoseTimeCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.SectionHeader
import com.d4viddf.medicationreminder.ui.features.home.components.cards.TemperatureCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.TodayProgressCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.WaterCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.WeightCard
import com.d4viddf.medicationreminder.ui.features.home.model.WatchStatus
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeSection
import com.d4viddf.medicationreminder.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeLayout by viewModel.homeLayout.collectAsState()
    val latestWeight by viewModel.latestWeight.collectAsState()
    val latestTemperature by viewModel.latestTemperature.collectAsState()
    val waterIntakeToday by viewModel.waterIntakeToday.collectAsState()
    val todayProgress by viewModel.todayProgress.collectAsState()
    val missedReminders by viewModel.missedReminders.collectAsState()
    val lastMissedMedicationName by viewModel.lastMissedMedicationName.collectAsState()
    val nextDoseTimeInSeconds by viewModel.nextDoseTimeInSeconds.collectAsState() // Safely collect the state here

    LaunchedEffect(key1 = Unit) {
        viewModel.navigationEvents.collectLatest { route ->
            navController.navigate(route)
        }
    }

    HomeScreenContent(
        uiState = uiState,
        homeLayout = homeLayout,
        widthSizeClass= widthSizeClass,
        latestWeight = latestWeight,
        latestTemperature = latestTemperature,
        waterIntakeToday = waterIntakeToday,
        todayProgress = todayProgress,
        missedRemindersCount = missedReminders.size,
        lastMissedMedicationName = lastMissedMedicationName,
        nextDoseTimeInSeconds = nextDoseTimeInSeconds,
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
    widthSizeClass: WindowWidthSizeClass,
    latestWeight: Weight?,
    latestTemperature: BodyTemperature?,
    waterIntakeToday: Double?,
    todayProgress: Pair<Int, Int>,
    missedRemindersCount: Int,
    lastMissedMedicationName: String?,
    nextDoseTimeInSeconds: Long?,
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
                val columns = if (widthSizeClass == WindowWidthSizeClass.Compact) 1 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1: Carousel / No Meds / No Schedule
                    item (span = {  GridItemSpan(maxLineSpan) }){
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
                        item(span = {  GridItemSpan(maxLineSpan) }) {
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
                    if (homeLayout.isEmpty()) {
                        item(span = {  GridItemSpan(maxLineSpan) }) {
                            EmptyHomeState(
                                onNavigateToPersonalize = { navController.navigate(Screen.PersonalizeHome.route) }
                            )
                        }
                    } else if (!hasVisibleItems) {
                        item(span = {  GridItemSpan(maxLineSpan) }) {
                            EmptyHomeState(
                                onNavigateToPersonalize = { navController.navigate(Screen.PersonalizeHome.route) }
                            )
                        }
                    }
                    else {
                        homeLayout.forEach { section ->
                            val visibleItems = section.items.filter { it.isVisible }
                            if (visibleItems.isNotEmpty()) {
                                item (span = {  GridItemSpan(maxLineSpan) }){
                                    SectionHeader(
                                        title = section.name,
                                        onEditClick = { navController.navigate(Screen.PersonalizeHome.route) }
                                    )
                                }
                                items(visibleItems, key = { it.id }) { item ->
                                    when (item.id) {
                                        "today_progress" -> TodayProgressCard(
                                            taken = todayProgress.first,
                                            total = todayProgress.second
                                        )
                                        "missed_reminders" -> MissedRemindersCard(
                                            missedDoses = missedRemindersCount,
                                            lastMissedMedication = lastMissedMedicationName
                                        )
                                        "next_dose" -> {
                                            // *** FIX: Check if the value is not null before showing the card ***
                                            if (nextDoseTimeInSeconds != null) {
                                                NextDoseTimeCard(
                                                    timeToNextDoseInSeconds = nextDoseTimeInSeconds,
                                                    displayUnit = item.displayUnit ?: "minutes"
                                                )
                                            }
                                        }
                                        "heart_rate" -> HeartRateCard(heartRate = viewModel.heartRate)
                                        "weight" -> WeightCard(weight = latestWeight?.weightKilograms?.toString())
                                        "water" -> WaterCard(totalIntakeMl = waterIntakeToday)
                                        "temperature" -> TemperatureCard(temperatureRecord = latestTemperature)

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