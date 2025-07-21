package com.d4viddf.medicationreminder.ui.features.home.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.ui.common.components.ConfirmationDialog
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.features.home.components.NextDoseCard
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.WatchStatus // Import WatchStatus
import com.d4viddf.medicationreminder.ui.features.home.viewmodel.HomeViewModel
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

    // Collect navigation events
    LaunchedEffect(key1 = Unit) { // Use Unit or a key that doesn't change often
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
        onWatchIconClick = { viewModel.handleWatchIconClick() } // Pass the handler
    )
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
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
    onWatchIconClick: () -> Unit // Added callback for watch icon click
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
                    val context = LocalContext.current

                    // Watch Icon Button
                    IconButton(onClick = onWatchIconClick) {
                        val isWatchConnected = uiState.watchStatus == WatchStatus.CONNECTED_APP_INSTALLED ||
                                uiState.watchStatus == WatchStatus.CONNECTED_APP_NOT_INSTALLED

                        val iconTint = if (isWatchConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current.copy(alpha = LocalContentAlpha.current) // Muted if not connected
                        }

                        val contentDesc = if (isWatchConnected) {
                            stringResource(R.string.home_button_cd_watch_connected_settings) // New string resource needed
                        } else {
                            stringResource(R.string.home_button_cd_watch_disconnected_settings) // New string resource needed
                        }

                        BadgedBox(
                            badge = {
                                if (isWatchConnected) {
                                    Badge() // Shows a small dot
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = contentDesc }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Watch, // Static Watch Icon
                                contentDescription = null, // Content description handled by BadgedBox
                                tint = iconTint
                            )
                        }
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
                            Icon(Icons.Filled.Notifications, contentDescription = null)
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
                    if (uiState.nextDoseGroup.isNotEmpty()) {
                        item {
                            val carouselState = rememberCarouselState { uiState.nextDoseGroup.size }
                            val largeItemWidth = if (screenWidthDp * 0.75f >=300.dp) 340.dp else screenWidthDp * 0.75f
                            HorizontalMultiBrowseCarousel(
                                state = carouselState,
                                modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 16.dp),
                                preferredItemWidth = largeItemWidth,
                                itemSpacing = 8.dp,
                            ) { itemIndex ->
                                val item = uiState.nextDoseGroup[itemIndex]
                                NextDoseCard(
                                    item = item,
                                    modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                                    onNavigateToDetails = { medicationId ->
                                        navController.navigate(
                                            Screen.MedicationDetails.createRoute(medicationId)
                                        )
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { /* TODO: Navigate */ }) {
                                    Text(stringResource(id = R.string.show_all_button))
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = context.getString(R.string.no_upcoming_doses_at_all),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    // --- New Progress Section ---
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

// --- HEALTH MOCKUP SECTIONS ---

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

// --- NEW PROGRESS MOCKUP SECTIONS ---

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
                    text = "2", // Mock data for missed doses
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stringResource(id = R.string.last_missed_label)} Ibuprofen", // Mock data
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
                    text = "9/12", // Mock data for progress
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            // Using a standard CircularProgressIndicator as Wavy is experimental
            CircularProgressIndicator(
                progress = { 9f / 12f }, // Mock progress
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
        }
    }
}


@Preview(showBackground = true, name = "HomeScreen Preview")
@Composable
fun HomeScreenNewPreview() {
    val sampleTime = LocalDateTime.now()
    val morningRawTime = sampleTime.withHour(8).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    val previewState = HomeViewModel.HomeState(
        currentGreeting = "Good morning!",
        nextDoseGroup = listOf(
            NextDoseUiItem(1, 101, "Amoxicillin", "250mg", "LIGHT_BLUE", null, morningRawTime, "08:00"),
        ),
        todaysReminders = emptyMap(),
        hasUnreadAlerts = true,
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