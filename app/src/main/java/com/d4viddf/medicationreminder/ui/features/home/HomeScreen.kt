package com.d4viddf.medicationreminder.ui.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.HeartRate
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.ui.common.component.ConfirmationDialog
import com.d4viddf.medicationreminder.ui.common.model.UiItemState
import com.d4viddf.medicationreminder.ui.features.home.components.NextDoseCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.MissedRemindersCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.NewHealthStatCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.NextDoseTimeCard
import com.d4viddf.medicationreminder.ui.features.home.components.cards.ValueUnitRow
import com.d4viddf.medicationreminder.ui.features.home.components.cards.SectionHeader
import com.d4viddf.medicationreminder.ui.features.home.components.cards.TodayProgressCard
import com.d4viddf.medicationreminder.ui.features.home.components.skeletons.HealthStatCardSkeleton
import com.d4viddf.medicationreminder.ui.features.home.components.skeletons.MissedRemindersCardSkeleton
import com.d4viddf.medicationreminder.ui.features.home.components.skeletons.NextDoseCardSkeleton
import com.d4viddf.medicationreminder.ui.features.home.components.skeletons.NextDoseTimeCardSkeleton
import com.d4viddf.medicationreminder.ui.features.home.components.skeletons.SectionHeaderSkeleton
import com.d4viddf.medicationreminder.ui.features.home.components.skeletons.TemperatureCardSkeleton
import com.d4viddf.medicationreminder.ui.features.home.components.skeletons.TodayProgressCardSkeleton
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.WatchStatus
// ** ADDED IMPORTS FOR PREVIEW **
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeItem
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeSection
import com.d4viddf.medicationreminder.ui.features.todayschedules.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // --- Collect individual reactive states from the ViewModel ---
    val dialogState by viewModel.dialogState.collectAsState()
    val greeting by viewModel.greeting.collectAsState()
    val watchStatus by viewModel.watchStatus.collectAsState()
    val hasRegisteredMedications by viewModel.hasRegisteredMedications.collectAsState()
    val nextDoseGroupState by viewModel.nextDoseGroup.collectAsState()
    val nextDoseTimeInSeconds by viewModel.nextDoseTimeInSeconds.collectAsState()
    val todayProgressState by viewModel.todayProgress.collectAsState()
    val missedRemindersState by viewModel.missedReminders.collectAsState()
    val homeLayoutState by viewModel.homeLayout.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Health Data States
    val latestWeightState by viewModel.latestWeight.collectAsState()
    val latestTemperatureState by viewModel.latestTemperature.collectAsState()
    val waterIntakeTodayState by viewModel.waterIntakeToday.collectAsState()
    val heartRateRangeState by viewModel.heartRateRange.collectAsState()
    val latestHeartRateState by viewModel.latestHeartRate.collectAsState()
    val waterIntakeProgress by viewModel.waterIntakeProgress.collectAsState()
    val weightProgress by viewModel.weightProgress.collectAsState()
    val heartRateProgress by viewModel.heartRateProgress.collectAsState()
    val isHealthConnectEnabled by viewModel.isHealthConnectEnabled.collectAsState()
    val weightGoal by viewModel.weightGoal.collectAsState()
    val waterIntakeGoal by viewModel.waterIntakeGoal.collectAsState()


    // A flag to indicate if there are unread alerts (you can wire this to a real data source)
    val hasUnreadAlerts = false // TODO: Replace with a flow from the ViewModel

    LaunchedEffect(key1 = Unit) {
        viewModel.navigationEvents.collectLatest { route ->
            navController.navigate(route)
        }
    }

    HomeScreenContent(
        dialogState = dialogState,
        greeting = greeting,
        watchStatus = watchStatus,
        hasRegisteredMedications = hasRegisteredMedications,
        nextDoseGroupState = nextDoseGroupState,
        nextDoseTimeInSeconds = nextDoseTimeInSeconds,
        todayProgressState = todayProgressState,
        missedRemindersState = missedRemindersState,
        homeLayoutState = homeLayoutState,
        widthSizeClass = widthSizeClass,
        isRefreshing = isRefreshing,
        hasUnreadAlerts = hasUnreadAlerts,
        latestWeightState = latestWeightState,
        latestTemperatureState = latestTemperatureState,
        waterIntakeTodayState = waterIntakeTodayState,
        waterIntakeProgress = waterIntakeProgress,
        weightProgress = weightProgress,
        heartRateProgress = heartRateProgress,
        weightGoal = weightGoal,
        waterIntakeGoal = waterIntakeGoal,
        heartRateRangeState = heartRateRangeState,
        latestHeartRateState = latestHeartRateState,
        isHealthConnectEnabled = isHealthConnectEnabled,
        onRefresh = viewModel::refreshData,
        onDismissConfirmationDialog = viewModel::dismissConfirmationDialog,
        onWatchIconClick = viewModel::handleWatchIconClick,
        navController = navController
    )
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class, ExperimentalMaterial3ExpressiveApi::class
)
@Composable
internal fun HomeScreenContent(
    dialogState: HomeViewModel.DialogState?,
    greeting: String,
    watchStatus: WatchStatus,
    hasRegisteredMedications: Boolean,
    nextDoseGroupState: UiItemState<List<NextDoseUiItem>>,
    nextDoseTimeInSeconds: Long?,
    todayProgressState: UiItemState<Pair<Int, Int>>,
    missedRemindersState: UiItemState<List<TodayScheduleUiItem>>,
    homeLayoutState: UiItemState<List<HomeSection>>,
    widthSizeClass: WindowWidthSizeClass,
    isRefreshing: Boolean,
    hasUnreadAlerts: Boolean,
    latestWeightState: UiItemState<Weight?>,
    latestTemperatureState: UiItemState<BodyTemperature?>,
    waterIntakeTodayState: UiItemState<Pair<Double?, Boolean>>,
    waterIntakeProgress: Float,
    weightProgress: Float,
    heartRateProgress: Float,
    heartRateRangeState: UiItemState<Pair<Long, Long>?>,
    latestHeartRateState: UiItemState<HeartRate?>,
    isHealthConnectEnabled: Boolean,
    weightGoal: Float,
    waterIntakeGoal: Int,
    onRefresh: () -> Unit,
    onDismissConfirmationDialog: () -> Unit,
    navController: NavController,
    onWatchIconClick: () -> Unit,
) {
    if (dialogState != null) {
        ConfirmationDialog(
            onDismissRequest = onDismissConfirmationDialog,
            onConfirmation = dialogState.onConfirm,
            dialogTitle = dialogState.title,
            dialogText = dialogState.text
        )
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    var topBarTitle by remember(greeting) { mutableStateOf(greeting) }
    val nextDosageTitle = stringResource(id = R.string.next_dosage_title)

    LaunchedEffect(nextDoseGroupState, greeting) {
        if (nextDoseGroupState is UiItemState.Success && nextDoseGroupState.data.isNotEmpty()) {
            delay(3000L)
            topBarTitle = nextDosageTitle
        } else {
            topBarTitle = greeting
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
                    IconButton(
                        onClick = onWatchIconClick,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        val isWatchConnected = watchStatus == WatchStatus.CONNECTED_APP_INSTALLED
                        val iconTint =
                            if (isWatchConnected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        val contentDesc =
                            if (isWatchConnected) stringResource(R.string.home_button_cd_watch_connected_settings) else stringResource(
                                R.string.home_button_cd_watch_disconnected_settings
                            )

                        BadgedBox(
                            badge = { if (isWatchConnected) Badge() },
                            modifier = Modifier.semantics { contentDescription = contentDesc }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_rounded_devices_wearables_24),
                                contentDescription = null,
                                tint = iconTint
                            )
                        }
                    }
                    IconButton(
                        onClick = { /* TODO */ },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        val notificationsCd =
                            if (hasUnreadAlerts) context.getString(R.string.home_button_cd_notifications_unread) else context.getString(
                                R.string.home_button_cd_notifications_read
                            )
                        BadgedBox(
                            badge = { if (hasUnreadAlerts) Badge() },
                            modifier = Modifier.semantics { contentDescription = notificationsCd }
                        ) {
                            Icon(
                                painterResource(R.drawable.rounded_notifications_24),
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = onRefresh)
        val isLoading = homeLayoutState is UiItemState.Loading

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
                .padding(paddingValues)
        ) {
            val columns = if (widthSizeClass == WindowWidthSizeClass.Compact) 1 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Crossfade(
                        targetState = nextDoseGroupState,
                        label = "NextDoseCrossfade"
                    ) { state ->
                        when (state) {
                            is UiItemState.Loading -> {
                                NextDoseCardSkeleton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }

                            is UiItemState.Success -> {
                                val nextDoseGroup = state.data
                                when {
                                    !hasRegisteredMedications -> NoMedicationsCard(
                                        onAddMedication = { navController.navigate(Screen.AddMedicationChoice.route) }
                                    )
                                    // This logic is key: if we have meds but no upcoming doses, show the "all done" card.
                                    nextDoseGroup.isEmpty() && hasRegisteredMedications && ((todayProgressState as? UiItemState.Success)?.data?.second
                                        ?: 0) > 0 -> NoMoreSchedulesTodayCard()
                                    nextDoseGroup.isEmpty() && hasRegisteredMedications && ((todayProgressState as? UiItemState.Success)?.data?.second
                                        ?: 0) == 0 -> NoScheduleTodayCard()
                                    else -> {
                                        val carouselState =
                                            rememberCarouselState { nextDoseGroup.size }
                                        HorizontalMultiBrowseCarousel(
                                            state = carouselState,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            preferredItemWidth = if (screenWidthDp < 550.dp) 400.dp else screenWidthDp * 0.5f,
                                            itemSpacing = 8.dp,
                                        ) { itemIndex ->
                                            val item = nextDoseGroup[itemIndex]
                                            NextDoseCard(
                                                item = item,
                                                modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                                                onNavigateToDetails = { medicationId ->
                                                    navController.navigate(
                                                        Screen.MedicationDetails.createRoute(
                                                            medicationId
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            is UiItemState.Error -> {
                                // TODO: Show error state for the dose card
                            }
                        }
                    }
                }

                if (hasRegisteredMedications) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { navController.navigate(Screen.TodaySchedules.createRoute()) },
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(stringResource(id = R.string.show_all_button))
                            }
                        }
                    }
                }

                when (homeLayoutState) {
                    is UiItemState.Loading -> {
                        // Show skeleton for sections
                        items(2) {
                            SectionHeaderSkeleton()
                            HealthStatCardSkeleton()
                        }
                    }

                    is UiItemState.Success -> {
                        val homeLayout = homeLayoutState.data
                        val hasVisibleItems = homeLayout.any { section -> section.items.any { it.isVisible } }
                        if (homeLayout.isEmpty() || !hasVisibleItems) {
                            if (hasRegisteredMedications) { // Only show if they have meds but no layout
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyHomeState(
                                        onNavigateToPersonalize = { navController.navigate(Screen.PersonalizeHome.route) }
                                    )
                                }
                            }
                        } else {
                            homeLayout.forEach { section ->
                                val visibleItems = section.items.filter { it.isVisible }
                                if (visibleItems.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        SectionHeader(
                                            title = stringResource(id = section.nameRes),
                                            onEditClick = { navController.navigate(Screen.PersonalizeHome.route) }
                                        )
                                    }
                                    items(visibleItems, key = { it.id }) { item ->
                                        when (item.id) {
                                            "today_progress" -> Crossfade(
                                                targetState = todayProgressState,
                                                label = "TodayProgressCrossfade"
                                            ) { state ->
                                                when (state) {
                                                    is UiItemState.Loading -> TodayProgressCardSkeleton()
                                                    is UiItemState.Success -> TodayProgressCard(
                                                        taken = state.data.first,
                                                        total = state.data.second
                                                    )

                                                    is UiItemState.Error -> {} // Optional: show error
                                                }
                                            }

                                            "missed_reminders" -> Crossfade(
                                                targetState = missedRemindersState,
                                                label = "MissedRemindersCrossfade"
                                            ) { state ->
                                                when (state) {
                                                    is UiItemState.Loading -> MissedRemindersCardSkeleton()
                                                    is UiItemState.Success -> MissedRemindersCard(
                                                        missedDoses = state.data.size,
                                                        lastMissedMedication = state.data.firstOrNull()?.medicationName,
                                                        onClick = {
                                                            navController.navigate(
                                                                Screen.TodaySchedules.createRoute(
                                                                    showMissed = true
                                                                )
                                                            )
                                                        }
                                                    )

                                                    is UiItemState.Error -> {}
                                                }
                                            }

                                            "next_dose" -> Crossfade(
                                                targetState = nextDoseGroupState,
                                                label = "NextDoseTimeCrossfade"
                                            ) { state ->
                                                when (state) {
                                                    is UiItemState.Loading -> NextDoseTimeCardSkeleton()
                                                    is UiItemState.Success -> nextDoseTimeInSeconds?.let {
                                                        NextDoseTimeCard(
                                                            timeToNextDoseInSeconds = it,
                                                            displayUnit = item.displayUnit
                                                                ?: "minutes"
                                                        )
                                                    }

                                                    is UiItemState.Error -> {}
                                                }
                                            }

                                            "heart_rate" -> Crossfade(
                                                targetState = heartRateRangeState,
                                                label = "HeartRateCrossfade"
                                            ) { state ->
                                                when (state) {
                                                    is UiItemState.Loading -> HealthStatCardSkeleton()
                                                    is UiItemState.Success -> {
                                                        val heartRateRange = state.data
                                                        val latestHeartRate = (latestHeartRateState as? UiItemState.Success)?.data
                                                        NewHealthStatCard(
                                                            title = stringResource(id = R.string.heart_rate),
                                                            value = {
                                                                ValueUnitRow(
                                                                    value = if (heartRateRange != null) "${heartRateRange.first}-${heartRateRange.second}" else "-",
                                                                    unit = stringResource(id = R.string.bpm)
                                                                )
                                                            },
                                                            subtitle = if (latestHeartRate != null) "${stringResource(id = R.string.last_record)}${
                                                                DateTimeFormatter.ofLocalizedTime(
                                                                    FormatStyle.SHORT
                                                                ).format(latestHeartRate.time.atZone(ZoneId.systemDefault()))
                                                            }" else stringResource(id = R.string.no_data),
                                                            progress = heartRateProgress,
                                                            icon = painterResource(id = R.drawable.cardio_load),
                                                            isHealthConnectData = isHealthConnectEnabled && latestHeartRate?.sourceApp != LocalContext.current.packageName,
                                                            onClick = { navController.navigate(Screen.HeartRate.route) }
                                                        )
                                                    }
                                                    is UiItemState.Error -> {}
                                                }
                                            }

                                            "weight" -> Crossfade(
                                                targetState = latestWeightState,
                                                label = "WeightCrossfade"
                                            ) { state ->
                                                when (state) {
                                                    is UiItemState.Loading -> HealthStatCardSkeleton()
                                                    is UiItemState.Success -> {
                                                        val weight = state.data
                                                        NewHealthStatCard(
                                                            title = stringResource(id = R.string.weight),
                                                            value = {
                                                                ValueUnitRow(
                                                                    value = if (weight != null) "%.1f".format(weight.weightKilograms) else "-",
                                                                    unit = stringResource(id = R.string.kg)
                                                                )
                                                            },
                                                            subtitle = if (weight != null) "${stringResource(id = R.string.last_record)}${
                                                                DateTimeFormatter.ofLocalizedDate(
                                                                    FormatStyle.MEDIUM
                                                                ).format(weight.time.atZone(ZoneId.systemDefault()))
                                                            }" else stringResource(id = R.string.no_data),
                                                            progress = weightProgress,
                                                            icon = painterResource(id = R.drawable.monitor_weight),
                                                            isHealthConnectData = isHealthConnectEnabled,
                                                            onClick = { navController.navigate(Screen.Weight.route) },
                                                            showProgress = weightGoal > 0f
                                                        )
                                                    }
                                                    is UiItemState.Error -> {}
                                                }
                                            }

                                            "water" -> Crossfade(
                                                targetState = waterIntakeTodayState,
                                                label = "WaterCrossfade"
                                            ) { state ->
                                                when (state) {
                                                    is UiItemState.Loading -> HealthStatCardSkeleton()
                                                    is UiItemState.Success -> {
                                                        val waterIntakeData = state.data
                                                        val waterIntake = waterIntakeData.first
                                                        val fromHealthConnect = waterIntakeData.second
                                                        NewHealthStatCard(
                                                            title = stringResource(id = R.string.water_card_title),
                                                            value = {
                                                                ValueUnitRow(
                                                                    value = if (waterIntake != null) "%.0f".format(waterIntake * 1000) else "0",
                                                                    unit = stringResource(id = R.string.ml)
                                                                )
                                                            },
                                                            subtitle = "${stringResource(id = R.string.today_progress)}${"%.0f".format(waterIntakeProgress * 100)}${stringResource(id = R.string.percentage)}",
                                                            progress = waterIntakeProgress,
                                                            icon = painterResource(id = R.drawable.water_full),
                                                            isHealthConnectData = isHealthConnectEnabled,
                                                            onClick = { navController.navigate(Screen.WaterIntake.route) },
                                                            showProgress = waterIntakeProgress > 0f
                                                        )
                                                    }
                                                    is UiItemState.Error -> {}
                                                }
                                            }

                                            "temperature" -> Crossfade(
                                                targetState = latestTemperatureState,
                                                label = "TemperatureCrossfade"
                                            ) { state ->
                                                when (state) {
                                                    is UiItemState.Loading -> TemperatureCardSkeleton()
                                                    is UiItemState.Success -> {
                                                        val temperature = state.data
                                                        NewHealthStatCard(
                                                            title = stringResource(id = R.string.temperature),
                                                            value = {
                                                                ValueUnitRow(
                                                                    value = if (temperature != null) "%.1f".format(temperature.temperatureCelsius) else "-",
                                                                    unit = stringResource(id = R.string.celsius)
                                                                )
                                                            },
                                                            subtitle = if (temperature != null) "${stringResource(id = R.string.last_record)}${
                                                                DateTimeFormatter.ofLocalizedTime(
                                                                    FormatStyle.SHORT
                                                                ).format(temperature.time.atZone(ZoneId.systemDefault()))
                                                            }" else stringResource(id = R.string.no_data),
                                                            progress = 0f,
                                                            icon = painterResource(id = R.drawable.thermometer),
                                                            isHealthConnectData = isHealthConnectEnabled,
                                                            onClick = { navController.navigate(Screen.BodyTemperature.route) },
                                                            showProgress = false
                                                        )
                                                    }
                                                    is UiItemState.Error -> {}
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is UiItemState.Error -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("Error loading home layout.")
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// region Non-stateful Cards and Placeholders

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyHomeState(modifier: Modifier = Modifier, onNavigateToPersonalize: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
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
            Button(onClick = onNavigateToPersonalize, shapes = ButtonDefaults.shapes()) {
                Text("Personalize Home")
            }
        }
    }
}

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
            .height(180.dp)
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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
            .height(180.dp)
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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
// endregion

// region Preview Section
private val mockHomeLayout = listOf(
    HomeSection(
        "insights", R.string.home_section_progress, true, items = listOf(
            HomeItem("today_progress", R.string.home_item_today_progress, true, "doses"),
            HomeItem("missed_reminders", R.string.home_item_missed_reminders, true, null)
        )
    ),
    HomeSection(
        "vitals", R.string.home_section_health, false, items = listOf(
            HomeItem("next_dose", R.string.home_item_next_dose, true, "minutes"),
            HomeItem("heart_rate", R.string.home_item_heart_rate, true, null),
            HomeItem("weight", R.string.home_item_weight, true, null),
            HomeItem("water", R.string.home_item_water_intake, true, null),
            HomeItem("temperature", R.string.home_item_temperature, true, null)
        )
    )
)

private val mockNextDoseGroup = listOf(
    NextDoseUiItem(1, 1, "Ibuprofen", "200mg", "LIGHT_RED", null, "22:00", "22:00"),
    NextDoseUiItem(2, 2, "Lisinopril", "10mg", "LIGHT_BLUE", null, "22:00", "22:00")
)

private val mockMissedReminders = listOf(
    TodayScheduleUiItem(
        MedicationReminder(3, 3, 3, "08:00", isTaken = false, null, null),
        "Metformin", "500mg", "LIGHT_ORANGE", null, null, "08:00"
    )
)

@Preview(name = "Normal State", showBackground = true)
@Composable
private fun HomeScreenNormalPreview() {
    AppTheme {
        HomeScreenContent(
            dialogState = null,
            greeting = "Good Evening",
            watchStatus = WatchStatus.CONNECTED_APP_INSTALLED,
            hasRegisteredMedications = true,
            nextDoseGroupState = UiItemState.Success(mockNextDoseGroup),
            nextDoseTimeInSeconds = 1234,
            todayProgressState = UiItemState.Success(Pair(5, 8)),
            missedRemindersState = UiItemState.Success(mockMissedReminders),
            homeLayoutState = UiItemState.Success(mockHomeLayout),
            widthSizeClass = WindowWidthSizeClass.Compact,
            isRefreshing = false,
            hasUnreadAlerts = true,
            latestWeightState = UiItemState.Success(Weight(1, Instant.now(), 85.5)),
            latestTemperatureState = UiItemState.Success(BodyTemperature(1, Instant.now(), 36.8)),
            waterIntakeTodayState = UiItemState.Success(Pair(1250.0, true)),
            heartRateRangeState = UiItemState.Success(Pair(60, 100)),
            latestHeartRateState = UiItemState.Success(HeartRate(1, Instant.now(), 68)),
            waterIntakeProgress = 0.5f,
            weightProgress = 0.8f,
            heartRateProgress = 0.6f,
            isHealthConnectEnabled = true,
            weightGoal = 80f,
            waterIntakeGoal = 2000,
            onRefresh = {},
            onDismissConfirmationDialog = {},
            navController = rememberNavController(),
            onWatchIconClick = {}
        )
    }
}

@Preview(name = "No Medications State", showBackground = true)
@Composable
private fun HomeScreenNoMedicationsPreview() {
    AppTheme {
        HomeScreenContent(
            dialogState = null,
            greeting = "Good Morning",
            watchStatus = WatchStatus.NOT_CONNECTED,
            hasRegisteredMedications = false,
            nextDoseGroupState = UiItemState.Success(emptyList()),
            nextDoseTimeInSeconds = null,
            todayProgressState = UiItemState.Success(Pair(0, 0)),
            missedRemindersState = UiItemState.Success(emptyList()),
            homeLayoutState = UiItemState.Success(emptyList()),
            widthSizeClass = WindowWidthSizeClass.Compact,
            isRefreshing = false,
            hasUnreadAlerts = false,
            latestWeightState = UiItemState.Success(null),
            latestTemperatureState = UiItemState.Success(null),
            waterIntakeTodayState = UiItemState.Success(Pair(null, false)),
            heartRateRangeState = UiItemState.Success(null),
            latestHeartRateState = UiItemState.Success(null),
            waterIntakeProgress = 0f,
            weightProgress = 0f,
            heartRateProgress = 0f,
            isHealthConnectEnabled = false,
            weightGoal = 0f,
            waterIntakeGoal = 0,
            onRefresh = {},
            onDismissConfirmationDialog = {},
            navController = rememberNavController(),
            onWatchIconClick = {}
        )
    }
}

@Preview(name = "No More Doses Today State", showBackground = true)
@Composable
private fun HomeScreenNoMoreDosesPreview() {
    AppTheme {
        HomeScreenContent(
            dialogState = null,
            greeting = "Good Afternoon",
            watchStatus = WatchStatus.CONNECTED_APP_NOT_INSTALLED,
            hasRegisteredMedications = true,
            nextDoseGroupState = UiItemState.Success(emptyList()), // The key for this state
            nextDoseTimeInSeconds = null,
            todayProgressState = UiItemState.Success(Pair(8, 8)),
            missedRemindersState = UiItemState.Success(emptyList()),
            homeLayoutState = UiItemState.Success(mockHomeLayout),
            widthSizeClass = WindowWidthSizeClass.Compact,
            isRefreshing = false,
            hasUnreadAlerts = false,
            latestWeightState = UiItemState.Success(Weight(1, Instant.now(), 85.5)),
            latestTemperatureState = UiItemState.Success(null),
            waterIntakeTodayState = UiItemState.Success(Pair(750.0, false)),
            heartRateRangeState = UiItemState.Success(Pair(65, 110)),
            latestHeartRateState = UiItemState.Success(HeartRate(1, Instant.now(), 72)),
            waterIntakeProgress = 0.3f,
            weightProgress = 0.7f,
            heartRateProgress = 0.5f,
            isHealthConnectEnabled = true,
            weightGoal = 90f,
            waterIntakeGoal = 2500,
            onRefresh = {},
            onDismissConfirmationDialog = {},
            navController = rememberNavController(),
            onWatchIconClick = {}
        )
    }
}

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    AppTheme {
        HomeScreenContent(
            dialogState = null,
            greeting = "Good Morning",
            watchStatus = WatchStatus.UNKNOWN,
            hasRegisteredMedications = true,
            nextDoseGroupState = UiItemState.Loading,
            nextDoseTimeInSeconds = null,
            todayProgressState = UiItemState.Loading,
            missedRemindersState = UiItemState.Loading,
            homeLayoutState = UiItemState.Loading,
            widthSizeClass = WindowWidthSizeClass.Compact,
            isRefreshing = false,
            hasUnreadAlerts = false,
            latestWeightState = UiItemState.Loading,
            latestTemperatureState = UiItemState.Loading,
            waterIntakeTodayState = UiItemState.Loading,
            heartRateRangeState = UiItemState.Loading,
            latestHeartRateState = UiItemState.Loading,
            waterIntakeProgress = 0f,
            weightProgress = 0f,
            heartRateProgress = 0f,
            isHealthConnectEnabled = false,
            weightGoal = 0f,
            waterIntakeGoal = 0,
            onRefresh = {},
            onDismissConfirmationDialog = {},
            navController = rememberNavController(),
            onWatchIconClick = {}
        )
    }
}