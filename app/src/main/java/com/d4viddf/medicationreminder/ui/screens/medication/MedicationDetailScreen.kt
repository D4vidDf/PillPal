@file:OptIn(ExperimentalSharedTransitionApi::class) // Moved OptIn to file-level
package com.d4viddf.medicationreminder.ui.screens.medication

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
// import androidx.compose.material.icons.filled.Close // Import will be removed if not used elsewhere
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.data.TodayScheduleItem
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.AddPastMedicationDialog
import com.d4viddf.medicationreminder.ui.components.BarChartItem
import com.d4viddf.medicationreminder.ui.components.MedicationDetailCounters
import com.d4viddf.medicationreminder.ui.components.MedicationDetailHeader
import com.d4viddf.medicationreminder.ui.components.MedicationProgressDisplay
import com.d4viddf.medicationreminder.ui.components.ProgressDetails
import com.d4viddf.medicationreminder.ui.components.SimpleBarChart
import com.d4viddf.medicationreminder.ui.screens.Screen
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme
import com.d4viddf.medicationreminder.viewmodel.ChartyGraphEntry
import com.d4viddf.medicationreminder.viewmodel.MedicationGraphViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationReminderViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun Medication?.isPastEndDate(): Boolean {
    if (this?.endDate.isNullOrBlank()) return false
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val endDateValue = LocalDate.parse(this.endDate, formatter)
        endDateValue.isBefore(LocalDate.now())
    } catch (e: Exception) {
        Log.e("MedicationDetailScreen", "Error parsing endDate in isPastEndDate: ${this.endDate}", e)
        false // If parsing fails, assume not past or handle error appropriately
    }
}

// ScheduleItem Composable - Adapted
@Composable
fun ScheduleItem(
    time: String,
    label: String,
    isTaken: Boolean,
    onTakenChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Switch(
            checked = isTaken,
            onCheckedChange = onTakenChange,
            enabled = enabled
        )
    }
}

@Preview(showBackground = true, name = "Hosted Narrow Pane (Medium WC)", widthDp = 580)
@Composable
fun MedicationDetailsScreenHostedNarrowPreview() {
    AppTheme {
        Box(modifier = Modifier.width(580.dp)) {
            MedicationDetailsScreen(
                medicationId = 1,
                navController = rememberNavController(),
                onNavigateBack = {},
                sharedTransitionScope = null,
                animatedVisibilityScope = null,
                isHostedInPane = true,
                graphViewModel = null,
                onNavigateToAllSchedules = { _, _ -> },
                onNavigateToMedicationHistory = { _, _ -> },
                onNavigateToMedicationGraph = { _, _ -> },
                onNavigateToMedicationInfo = { _, _ -> },
                widthSizeClass = WindowWidthSizeClass.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MedicationDetailsScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    viewModel: MedicationViewModel = hiltViewModel(),
    scheduleViewModel: MedicationScheduleViewModel = hiltViewModel(),
    medicationTypeViewModel: MedicationTypeViewModel = hiltViewModel(),
    medicationReminderViewModel: MedicationReminderViewModel = hiltViewModel(),
    graphViewModel: MedicationGraphViewModel? = hiltViewModel(),
    isHostedInPane: Boolean,
    onNavigateToAllSchedules: (medicationId: Int, colorName: String) -> Unit,
    onNavigateToMedicationHistory: (medicationId: Int, colorName: String) -> Unit,
    onNavigateToMedicationGraph: (medicationId: Int, colorName: String) -> Unit,
    onNavigateToMedicationInfo: (medicationId: Int, colorName: String) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
    navController: NavController
) {
    var medicationState by remember { mutableStateOf<Medication?>(null) }
    var scheduleState by remember { mutableStateOf<MedicationSchedule?>(null) }
    var medicationTypeState by remember { mutableStateOf<MedicationType?>(null) }
    var internalShowTwoPanesState by remember { mutableStateOf(false) } // Added state variable

    val progressDetails by viewModel.medicationProgressDetails.collectAsState()
    val todayScheduleItems by medicationReminderViewModel.todayScheduleItems.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val chartEntries: List<ChartyGraphEntry> by (graphViewModel?.chartyGraphData?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val weeklyMaxYForChart by (graphViewModel?.weeklyMaxYValue?.collectAsState() ?: remember { mutableStateOf(5f) }) // Collect new max Y value
    val currentWeekDaysForChart by (graphViewModel?.currentWeekDaysForWeeklyChart?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val isGraphLoading by graphViewModel?.isLoading?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }


    LaunchedEffect(medicationId, graphViewModel) {
        viewModel.getMedicationById(medicationId)?.let { med ->
            medicationState = med
            scheduleState = scheduleViewModel.getActiveScheduleForMedication(med.id)
            viewModel.observeMedicationAndRemindersForDailyProgress(med.id)
            medicationReminderViewModel.loadTodaySchedule(medicationId)

            med.typeId?.let { typeId ->
                medicationTypeViewModel.medicationTypes.collect { types ->
                    medicationTypeState = types.find { it.id == typeId }
                }
            }
        }
    }

    LaunchedEffect(medicationId, graphViewModel) {
        if (medicationId > 0 && graphViewModel != null) {
            val today = LocalDate.now()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val currentWeekDays = List(7) { i -> monday.plusDays(i.toLong()) }
            graphViewModel.loadWeeklyGraphData(medicationId, currentWeekDays)
        }
    }

    val color = remember(medicationState?.color) {
        try {
            MedicationColor.valueOf(medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name)
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_ORANGE
        }
    }
    val makeAppBarTransparent = isHostedInPane ||
            widthSizeClass == WindowWidthSizeClass.Medium ||
            widthSizeClass == WindowWidthSizeClass.Expanded
    MedicationSpecificTheme(medicationColor = color) {
        if (medicationState == null && progressDetails == null && todayScheduleItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            if (!isHostedInPane) { // Only show icon if not hosted in pane
                                Box(modifier = Modifier.padding(start = 10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            .clickable { onNavigateBack() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                            contentDescription = stringResource(id = R.string.back),
                                            modifier = Modifier.size(28.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                            // No else block, so nothing is rendered if isHostedInPane is true
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { /* TODO: Handle edit action */ }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.edit),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (makeAppBarTransparent) Color.Transparent else color.backgroundColor,
                            scrolledContainerColor = if (makeAppBarTransparent) Color.Transparent else color.backgroundColor,
                            navigationIconContentColor = Color.White, // Assuming these remain white or adapt as needed
                            actionIconContentColor = Color.White,    // Assuming these remain white or adapt as needed
                            titleContentColor = Color.White          // Assuming these remain white or adapt as needed
                        )
                    )
                }
            ) { innerPadding ->
                // The variable 'internalShowTwoPanesState' might still be used for layout decisions (like showing two columns of content),
                // but it will no longer directly control the AppBar's transparency.
                val outerContentModifier = Modifier.fillMaxSize().then(if (!isHostedInPane) Modifier.padding(innerPadding) else Modifier.padding(top = innerPadding.calculateTopPadding()))
                val minWidthForTwoPanes = 600.dp

                BoxWithConstraints(modifier = outerContentModifier) {
                    val showTwoPanes = when (widthSizeClass) {
                        WindowWidthSizeClass.Compact -> false
                        else -> {
                            if (isHostedInPane) this.maxWidth >= minWidthForTwoPanes else true
                        }
                    }

                    LaunchedEffect(showTwoPanes) {
                        if (internalShowTwoPanesState != showTwoPanes) {
                            internalShowTwoPanesState = showTwoPanes
                        }
                    }

                    val actualContentModifier = Modifier.fillMaxSize()

                    if (showTwoPanes) {
                        LazyColumn(modifier = actualContentModifier
                            .then(Modifier.padding(horizontal = 16.dp)) // Keep horizontal padding for two-pane
                            .then(if (!isHostedInPane) Modifier.padding(top = innerPadding.calculateTopPadding()) else Modifier) // Add conditional top padding
                        ) {
                            item {
                                MedicationHeaderAndProgress(
                                    medicationState = medicationState,
                                    progressDetails = progressDetails,
                                    medicationTypeState = medicationTypeState,
                                    color = color,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    medicationId = medicationId,
                                     scheduleState = scheduleState,
                                     makeAppBarTransparent = makeAppBarTransparent // Pass the variable
                                )
                            }
                            item {
                                Row(Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        TodayScheduleContent(
                                            todayScheduleItems = todayScheduleItems,
                                            medicationState = medicationState,
                                            onShowMoreClick = {
                                                onNavigateToAllSchedules(
                                                    medicationId,
                                                    medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name
                                                )
                                            },
                                            onAddPastDoseClick = { showDialog = true },
                                            medicationReminderViewModel = medicationReminderViewModel,
                                            medicationId = medicationId,
                                            isTwoPane = true
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                        MedicationHistoryContent(
                                    navController = navController,
                                    medicationId = medicationId,
                                    colorName = color.name
                                )
                                WeekProgressContent(
                                    navController = navController,
                                    chartEntries = chartEntries,
                                    medicationState = medicationState,
                                    medicationId = medicationId,
                                    color = color,
                                    maxYValue = weeklyMaxYForChart, // Pass it here
                                    currentWeekDays = currentWeekDaysForChart,
                                    isGraphLoading = isGraphLoading // Pass loading state
                                )
                                MedicationInformationContent(
                                    navController = navController,
                                    medicationState = medicationState,
                                    medicationId = medicationId,
                                    colorName = color.name
                                )
                                Spacer(modifier = Modifier.height(48.dp))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = actualContentModifier
                    .then(if (makeAppBarTransparent) Modifier.padding(horizontal = 16.dp) else Modifier)
                ) {
                    item {
                        MedicationHeaderAndProgress(
                            medicationState = medicationState,
                            progressDetails = progressDetails,
                            medicationTypeState = medicationTypeState,
                            color = color,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            medicationId = medicationId,
                                     scheduleState = scheduleState,
                                     makeAppBarTransparent = makeAppBarTransparent // Pass the variable
                        )
                    }
                    item {
                        TodayScheduleContent(
                            todayScheduleItems = todayScheduleItems,
                            medicationState = medicationState,
                            onShowMoreClick = {
                                onNavigateToAllSchedules(
                                    medicationId,
                                    medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name
                                )
                            },
                            onAddPastDoseClick = { showDialog = true },
                            medicationReminderViewModel = medicationReminderViewModel,
                            medicationId = medicationId,
                            isTwoPane = false
                        )
                    }
                    item {
                        MedicationHistoryContent(
                            navController = navController,
                            medicationId = medicationId,
                            colorName = color.name
                        )
                    }
                    item {
                        WeekProgressContent(
                            navController = navController,
                            chartEntries = chartEntries,
                            medicationState = medicationState,
                            medicationId = medicationId,
                            color = color,
                            maxYValue = weeklyMaxYForChart, // Pass it here
                            currentWeekDays = currentWeekDaysForChart,
                            isGraphLoading = isGraphLoading // Pass loading state
                        )
                    }
                    item {
                        MedicationInformationContent(
                            navController = navController,
                            medicationState = medicationState,
                            medicationId = medicationId,
                            colorName = color.name
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
        if (showDialog) {
            AddPastMedicationDialog(
                        medicationNameDisplay = medicationState?.name
                            ?: stringResource(id = R.string.medication_name_placeholder),
                        onDismissRequest = { showDialog = false },
                        onSave = { date, time ->
                            medicationReminderViewModel.addPastMedicationTaken(
                                medicationId = medicationId,
                                date = date,
                                time = time,
                                medicationNameParam = medicationState?.name ?: ""
                            )
                            showDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicationHeaderAndProgress(
    medicationState: Medication?,
    progressDetails: ProgressDetails?,
    medicationTypeState: MedicationType?,
    color: MedicationColor,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    medicationId: Int,
    scheduleState: MedicationSchedule?,
    makeAppBarTransparent: Boolean // New parameter
) {
    val displayProgressDetails = if (medicationState.isPastEndDate()) {
        Log.d("MedDetailScreen", "Medication ${medicationState?.name} has ended. Displaying completed progress.")
        ProgressDetails(taken = 1, totalFromPackage = 1, remaining = 0, progressFraction = 1.0f, displayText = stringResource(id = R.string.medication_progress_completed))
    } else {
        progressDetails
    }

    val minWidthForHeaderTwoPanes = 500.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val showTwoPanesForHeader = this.maxWidth >= minWidthForHeaderTwoPanes
        // NEW SHAPE LOGIC:
        val headerShape = if (makeAppBarTransparent) {
            RoundedCornerShape(36.dp)
        } else {
            RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = color.backgroundColor, shape = headerShape)
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                rememberSharedContentState(key = "medication-background-${medicationId}"),
                                animatedVisibilityScope
                            )
                        }
                    } else Modifier
                )
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 16.dp)
        ) {
            if (medicationState != null && progressDetails != null) {
                if (showTwoPanesForHeader) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.height(212.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = medicationTypeState?.imageUrl ?: "https://placehold.co/100x100.png"),
                                    contentDescription = stringResource(id = R.string.medication_detail_header_image_acc),
                                    modifier = Modifier.size(180.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                val medicationNameString = medicationState.name
                                val words = medicationNameString.split(" ")
                                val displayName = if (words.size > 3) {
                                    words.take(3).joinToString(" ") + "..."
                                } else {
                                    medicationNameString
                                }
                                Text(
                                    text = displayName,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color.textColor,
                                    lineHeight = 34.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center, // Changed to Center
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = medicationState.dosage.takeIf { !it.isNullOrBlank() } ?: stringResource(id = R.string.medication_detail_header_no_dosage),
                                    fontSize = 20.sp,
                                    color = color.textColor,
                                    textAlign = TextAlign.Center, // Changed to Center
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                            // Removed verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.height(212.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                MedicationProgressDisplay(
                                    progressDetails = displayProgressDetails, // Use potentially overridden details
                                    colorScheme = color,
                                    indicatorSizeDp = 180.dp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            MedicationDetailCounters(
                                colorScheme = color,
                                medication = medicationState,
                                schedule = scheduleState
                            )
                        }
                    }
                } else {
                    // Original single-column layout
                    MedicationDetailHeader(
                        medicationId = medicationId,
                        medicationName = medicationState.name,
                        medicationDosage = medicationState.dosage,
                        medicationImageUrl = medicationTypeState?.imageUrl,
                        colorScheme = color,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MedicationProgressDisplay(
                        progressDetails = displayProgressDetails, // Use potentially overridden details
                        colorScheme = color,
                        indicatorSizeDp = 220.dp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MedicationDetailCounters(
                        colorScheme = color,
                        medication = medicationState,
                        schedule = scheduleState,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TodayScheduleContent(
    todayScheduleItems: List<TodayScheduleItem>,
    medicationState: Medication?,
    onShowMoreClick: () -> Unit,
    onAddPastDoseClick: () -> Unit,
    medicationReminderViewModel: MedicationReminderViewModel,
    medicationId: Int,
    isTwoPane: Boolean = false
) {
    // Removed the early return block for medicationState.isPastEndDate()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.medication_detail_today_title),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
        )
        // IconButton is now always present
        IconButton(
            onClick = onAddPastDoseClick,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_add_24),
                contentDescription = stringResource(id = R.string.content_desc_add_past_dose),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
            )
        }
    }

    if (medicationState.isPastEndDate()) {
        Text(
            text = stringResource(id = R.string.medication_period_ended),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp), // Or just top padding if preferred
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    } else if (todayScheduleItems.isEmpty() && medicationState != null) {
        Text(
            text = stringResource(id = R.string.medication_detail_no_reminders_today),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    var futureRemindersStarted = false
    // itemsToShow will always take 5, as "Show More" navigates away.
    val itemsToShow = todayScheduleItems.take(5)
    itemsToShow.forEach { todayItem ->
        val isActuallyPast = todayItem.time.isBefore(LocalTime.now())

        if (!isActuallyPast && !futureRemindersStarted) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = 3.dp,
                color = MaterialTheme.colorScheme.onBackground
            )
            futureRemindersStarted = true
        }
        ScheduleItem(
            time = todayItem.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
            label = todayItem.medicationName,
            isTaken = todayItem.isTaken,
            onTakenChange = { newState ->
                medicationReminderViewModel.updateReminderStatus(
                    todayItem.id,
                    newState,
                    medicationId
                )
            },
            enabled = (isActuallyPast || todayItem.isTaken) && !medicationState.isPastEndDate()
        )
    }

    // Unified "Show More" button logic
    if (todayScheduleItems.size > 5) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Adjust padding: no horizontal padding for isTwoPane to allow centering of a half-width button
                .padding(vertical = 8.dp, horizontal = if (isTwoPane) 0.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onShowMoreClick, // Always navigates
                modifier = if (isTwoPane) Modifier.fillMaxWidth(0.5f) else Modifier.fillMaxWidth(), // Centered for twoPane, full width otherwise
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(id = R.string.show_more)) // Text is always "Show More"
            }
        }
    }
}

@Composable
private fun MedicationHistoryContent(
    navController: NavController,
    medicationId: Int,
    colorName: String
) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.MedicationHistory.createRoute(medicationId, colorName)) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "History",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(id = R.drawable.rounded_arrow_forward_ios_24),
            contentDescription = "History",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun WeekProgressContent(
    navController: NavController,
    chartEntries: List<ChartyGraphEntry>,
    medicationState: Medication?,
    medicationId: Int,
    color: MedicationColor,
    maxYValue: Float, // New parameter
    currentWeekDays: List<LocalDate>,
    isGraphLoading: Boolean // New parameter
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { navController.navigate(Screen.MedicationGraph.createRoute(medicationId, color.name)) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Week Progress",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.rounded_arrow_forward_ios_24),
                    contentDescription = "View full graph",
                    modifier = Modifier.size(24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val itemsForChart =
                    remember(chartEntries, currentWeekDays) { // Added currentWeekDays to key
                        if (chartEntries.isEmpty()) {
                            // Use currentWeekDays from ViewModel if available and chartEntries is empty,
                            // ensuring placeholders match the week being loaded.
                            val referenceMonday =
                                if (currentWeekDays.isNotEmpty()) currentWeekDays.first() else LocalDate.now()
                                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            val today = LocalDate.now()
                            val dayFormatter =
                                DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                            List(7) { i ->
                                val day = referenceMonday.plusDays(i.toLong())
                                ChartyGraphEntry(
                                    xValue = day.format(dayFormatter),
                                    yValue = 0f,
                                    isHighlighted = day.isEqual(today)
                                )
                            }
                        } else {
                            chartEntries.map {
                                ChartyGraphEntry(
                                    xValue = it.xValue,
                                    yValue = it.yValue,
                                    isHighlighted = it.isHighlighted
                                )
                            }
                        }
                    }

                if (isGraphLoading && chartEntries.isEmpty()) { // Show loader if loading and no data (even placeholders from chartEntries)
                    CircularProgressIndicator()
                } else {
                    val animatedItems = itemsForChart.map { chartEntry ->
                        val animatedYValue by animateFloatAsState(
                            targetValue = chartEntry.yValue,
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            ),
                            label = "barValueAnimation-${chartEntry.xValue}"
                        )
                        BarChartItem(
                            label = chartEntry.xValue,
                            value = animatedYValue,
                            isHighlighted = chartEntry.isHighlighted
                        )
                    }
                    SimpleBarChart(
                        data = animatedItems,
                        modifier = Modifier.fillMaxSize(),
                        highlightedBarColor = MaterialTheme.colorScheme.primary,
                        normalBarColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        valueTextColor = MaterialTheme.colorScheme.onSurface,
                        chartContentDescription = "Weekly doses for ${medicationState?.name ?: "this medication"}", // Corrected string template
                        explicitYAxisTopValue = maxYValue, // Pass to SimpleBarChart
                        onBarClick = { dayLabel ->
                            val dayFormatter =
                                DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                            val clickedDate = currentWeekDays.find { day ->
                                try {
                                    day.format(dayFormatter) == dayLabel
                                } catch (e: Exception) {
                                    Log.e(
                                        "WeekProgressContent",
                                        "Error formatting day from currentWeekDays: $day",
                                        e
                                    )
                                    false
                                }
                            }
                            if (clickedDate != null) {
                                val selectedDateStr =
                                    clickedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                navController.navigate(
                                    Screen.MedicationHistory.createRoute(
                                        medicationId = medicationId,
                                        colorName = color.name,
                                        selectedDate = selectedDateStr
                                    )
                                )
                            } else {
                                // Fallback or log if date not found, though this shouldn't happen if labels match
                                Log.w(
                                    "WeekProgressContent",
                                    "Clicked bar label '$dayLabel' not found in currentWeekDays."
                                )
                                // Optionally navigate to the general MedicationGraph screen as a fallback
                                navController.navigate(
                                    Screen.MedicationGraph.createRoute(
                                        medicationId,
                                        color.name
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    }

    @Composable
    private fun MedicationInformationContent(
        navController: NavController,
        medicationState: Medication?,
        medicationId: Int,
        colorName: String
    ) {
        val medicationInfoAvailable = !medicationState?.nregistro.isNullOrBlank()
        if (medicationInfoAvailable) {
            Text(
                text = "Information",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            Button(
                onClick = {
                    navController.navigate(
                        Screen.MedicationInfo.createRoute(
                            medicationId,
                            colorName
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(id = R.string.view_full_information))
            }
        }
    }



@Preview(showBackground = true, name = "Medication Details Screen - Compact")
@Composable
fun MedicationDetailsScreenCompactPreview() {
    AppTheme {
        MedicationDetailsScreen(
            medicationId = 1,
            onNavigateBack = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            isHostedInPane = false,
            graphViewModel = null,
            onNavigateToAllSchedules = { _, _ -> },
            onNavigateToMedicationHistory = { _, _ -> },
            onNavigateToMedicationGraph = { _, _ -> },
            onNavigateToMedicationInfo = { _, _ -> },
            widthSizeClass = WindowWidthSizeClass.Compact,
            navController = rememberNavController()
        )
    }
}

@Preview(showBackground = true, widthDp = 700, name = "Medication Details Screen - Medium")
@Composable
fun MedicationDetailsScreenMediumPreview() {
    AppTheme {
        MedicationDetailsScreen(
            medicationId = 1,
            onNavigateBack = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            isHostedInPane = false,
            graphViewModel = null,
            onNavigateToAllSchedules = { _, _ -> },
            onNavigateToMedicationHistory = { _, _ -> },
            onNavigateToMedicationGraph = { _, _ -> },
            onNavigateToMedicationInfo = { _, _ -> },
            widthSizeClass = WindowWidthSizeClass.Medium,
            navController = rememberNavController()
        )
    }
}

@Preview(showBackground = true, widthDp = 1000, name = "Medication Details Screen - Expanded")
@Composable
fun MedicationDetailsScreenExpandedPreview() {
    AppTheme {
        MedicationDetailsScreen(
            medicationId = 1,
            onNavigateBack = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            isHostedInPane = false,
            graphViewModel = null,
            onNavigateToAllSchedules = { _, _ -> },
            onNavigateToMedicationHistory = { _, _ -> },
            onNavigateToMedicationGraph = { _, _ -> },
            onNavigateToMedicationInfo = { _, _ -> },
            widthSizeClass = WindowWidthSizeClass.Expanded,
            navController = rememberNavController()
        )
    }
}

@Preview(showBackground = true, name = "Schedule Item - Not Taken")
@Composable
fun ScheduleItemNotTakenPreview() {
    AppTheme {
        ScheduleItem(
            time = "10:00 AM",
            label = "Aspirin",
            isTaken = false,
            onTakenChange = {},
            enabled = true
        )
    }
}

@Preview(showBackground = true, name = "Schedule Item - Taken")
@Composable
fun ScheduleItemTakenPreview() {
    AppTheme {
        ScheduleItem(
            time = "02:00 PM",
            label = "Ibuprofen",
            isTaken = true,
            onTakenChange = {},
            enabled = true
        )
    }
}

@Preview(showBackground = true, name = "Schedule Item - Disabled")
@Composable
fun ScheduleItemDisabledPreview() {
    AppTheme {
        ScheduleItem(
            time = "08:00 PM",
            label = "Vitamin C",
            isTaken = false,
            onTakenChange = {},
            enabled = false
        )
    }
}