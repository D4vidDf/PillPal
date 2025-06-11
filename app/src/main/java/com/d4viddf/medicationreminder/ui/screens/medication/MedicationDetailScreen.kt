@file:OptIn(ExperimentalSharedTransitionApi::class) // Moved OptIn to file-level
package com.d4viddf.medicationreminder.ui.screens.medication

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext // Changed from .rounded to .filled for NavigateNext
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft // Ensure this import is present
// import androidx.compose.material.icons.automirrored.rounded.NavigateNext // Keep only one NavigateNext import
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info // Added for Medication Info Card
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// Removed Canvas import
import androidx.compose.ui.draw.clip
// Removed Rect import
import androidx.compose.ui.graphics.Color
// Removed Path import
import androidx.compose.ui.platform.LocalDensity // New import
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import android.util.Log // Added for logging
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.AddPastMedicationDialog
import com.d4viddf.medicationreminder.ui.components.MedicationDetailCounters
import com.d4viddf.medicationreminder.ui.components.MedicationDetailHeader
import com.d4viddf.medicationreminder.ui.components.MedicationProgressDisplay
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme // Added import
import com.d4viddf.medicationreminder.viewmodel.ChartyGraphEntry // Added import
import com.d4viddf.medicationreminder.viewmodel.MedicationGraphViewModel // Added
import com.d4viddf.medicationreminder.viewmodel.MedicationReminderViewModel
// Custom SimpleBarChart imports
import com.d4viddf.medicationreminder.ui.components.SimpleBarChart
import com.d4viddf.medicationreminder.ui.components.BarChartItem
// Removed Charty Imports
// import com.himanshoe.charty.charts.BarChart
// import com.himanshoe.charty.common.Point
// import com.himanshoe.charty.common.config.ChartColor
// import com.himanshoe.charty.common.config.SolidChartColor
// import com.himanshoe.charty.common.extensions.asSolidChartColor
// import com.himanshoe.charty.bar.config.BarChartConfig
// import com.himanshoe.charty.common.config.LabelConfig
// import com.himanshoe.charty.bar.config.BarChartColorConfig
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ScheduleItem Composable - Adapted
@Composable
fun ScheduleItem(
    time: String,
    label: String,
    isTaken: Boolean, // Added
    onTakenChange: (Boolean) -> Unit, // Added
    enabled: Boolean // For Switch's enabled state (isPast or already taken)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Standardized padding for items
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
            onCheckedChange = onTakenChange, // Use the callback
            enabled = enabled // Control if switch can be interacted with
        )
    }
}

// Old WeeklyBarChartDisplay composable removed

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
    graphViewModel: MedicationGraphViewModel? = hiltViewModel(), // Made nullable for preview
    isHostedInPane: Boolean,
    onNavigateToAllSchedules: (medicationId: Int, colorName: String) -> Unit,
    onNavigateToMedicationHistory: (medicationId: Int, colorName: String) -> Unit,
    onNavigateToMedicationGraph: (medicationId: Int, colorName: String) -> Unit,
    onNavigateToMedicationInfo: (medicationId: Int, colorName: String) -> Unit
) {
    Log.d("DetailScreenGraphEntry", "MedicationDetailScreen composed. medicationId: $medicationId, graphViewModel is null: ${graphViewModel == null}")

    var medicationState by remember { mutableStateOf<Medication?>(null) }
    var scheduleState by remember { mutableStateOf<MedicationSchedule?>(null) }
    var medicationTypeState by remember { mutableStateOf<MedicationType?>(null) }

    val progressDetails by viewModel.medicationProgressDetails.collectAsState()
    val todayScheduleItems by medicationReminderViewModel.todayScheduleItems.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val chartEntries: List<ChartyGraphEntry> by (graphViewModel?.chartyGraphData?.collectAsState(initial = emptyList<ChartyGraphEntry>())
        ?: remember { mutableStateOf(emptyList<ChartyGraphEntry>()) })

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

        if (medicationId > 0 && graphViewModel != null) {
            val today = LocalDate.now()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val currentWeekDays = List(7) { i -> monday.plusDays(i.toLong()) }
            graphViewModel.loadWeeklyGraphData(medicationId, currentWeekDays)
        } else if (graphViewModel != null) {
            graphViewModel.clearGraphData()
            Log.d("MedicationDetailScreen", "Invalid medicationId ($medicationId), clearing graph data for details screen.")
        }
    }

    val color = remember(medicationState?.color) {
        try {
            MedicationColor.valueOf(medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name)
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_ORANGE
        }
    }

    MedicationSpecificTheme(medicationColor = color) {
        if (medicationState == null && progressDetails == null && todayScheduleItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else { // This 'else' was missing a closing brace in the original error context if Scaffold was not part of it.
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            if (!isHostedInPane) {
                                Box (modifier = Modifier.padding(start = 10.dp)){ // Original padding
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            .clickable { onNavigateBack() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                            contentDescription = stringResource(id = R.string.back), // R.string.back was original
                                            modifier = Modifier.size(28.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 10.dp) // Original padding
                                    .background(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(20.dp) // Pill/capsule shape
                                    )
                                    .clickable { /* TODO: Handle edit action */ } // Keep original action
                                    .padding(horizontal = 16.dp, vertical = 8.dp), // Padding for text inside
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.edit), // R.string.edit was original
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp // Explicit font size as before
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = color.backgroundColor, // Restored
                            navigationIconContentColor = Color.White, // Restored
                            actionIconContentColor = Color.White, // Restored for custom action
                            titleContentColor = Color.White // Assuming title would be white if present
                        )
                    )
                }
            ) { innerPadding ->
                val padding =  if (isHostedInPane) PaddingValues(top = innerPadding.calculateTopPadding()) else innerPadding
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = color.backgroundColor,
                                    shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                                )
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
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 24.dp
                                )
                        ) {
                            if (medicationState != null && progressDetails != null) {
                                MedicationDetailHeader(
                                    medicationId = medicationId,
                                    medicationName = medicationState?.name,
                                    medicationDosage = medicationState?.dosage,
                                    medicationImageUrl = medicationTypeState?.imageUrl,
                                    colorScheme = color,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                MedicationProgressDisplay(
                                    progressDetails = progressDetails,
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
                            } else {
                                // Optional placeholder
                            }
                        }
                    }

                    item {
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
                            IconButton(
                                onClick = { showDialog = true },
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer, // NEW
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(id = R.string.content_desc_add_past_dose),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer, // NEW
                                    modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
                                )
                            }
                        }
                    }

                    if (todayScheduleItems.isEmpty() && medicationState != null) {
                        item {
                            Text(
                                text = stringResource(id = R.string.medication_detail_no_reminders_today),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 16.dp
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    var futureRemindersStarted = false
                    items(todayScheduleItems.take(5), key = { it.id }) { todayItem ->
                        val isActuallyPast =
                            todayItem.time.isBefore(LocalTime.now())

                        if (!isActuallyPast && !futureRemindersStarted) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ), thickness = 3.dp, color = MaterialTheme.colorScheme.onBackground
                            )
                            futureRemindersStarted = true
                        }
                        ScheduleItem( // This is the call to ScheduleItem
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
                            enabled = isActuallyPast || todayItem.isTaken
                        )
                    }

                    if (todayScheduleItems.size > 5) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { onNavigateToAllSchedules(medicationId, medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(text = "Show More")
                                }
                            }
                        }
                    }

                    item { // Medication History Card
                        Spacer(modifier = Modifier.height(16.dp))
                        ElevatedCard(
                            onClick = { onNavigateToMedicationHistory(medicationId, medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer // NEW
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer // NEW
                                    )
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(
                                        text = stringResource(id = R.string.medication_history_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer // NEW
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer // NEW
                                )
                            }
                        }
                    }

                    item { // Graphics Card
                        Spacer(modifier = Modifier.height(16.dp))
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer // NEW
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.current_week_dosage_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer, // NEW
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // SimpleBarChart IMPLEMENTATION:
                                    val finalBarChartItems = remember(chartEntries) {
                                        if (chartEntries.isEmpty()) {
                                            // Generate default items for the current week
                                            val today = LocalDate.now()
                                            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                            val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                                            List(7) { i ->
                                                val day = monday.plusDays(i.toLong())
                                                BarChartItem(
                                                    label = day.format(dayFormatter),
                                                    value = 0f,
                                                    isHighlighted = day.isEqual(today) // Highlight current day
                                                )
                                            }
                                        } else {
                                            chartEntries.map { entry ->
                                                BarChartItem(
                                                    label = entry.xValue, // e.g., "Mon", "Tue"
                                                    value = entry.yValue,
                                                    isHighlighted = entry.isHighlighted
                                                )
                                            }
                                        }
                                    }

                                    SimpleBarChart(
                                        data = finalBarChartItems,
                                        modifier = Modifier.fillMaxSize(), // The chart will fill the 150.dp Box
                                        highlightedBarColor = MaterialTheme.colorScheme.primary,
                                        normalBarColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        // barWidthDp and spaceAroundBarsDp will use defaults from SimpleBarChart
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onNavigateToMedicationGraph(medicationId, medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name) },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(text = stringResource(id = R.string.view_more_stats))
                                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                    Icon(
                                        imageVector = Icons.Filled.BarChart,
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                }
                            }
                        }
                    }

                    item { // Medication Information Card
                        val medicationInfoAvailable = !medicationState?.nregistro.isNullOrBlank()
                        if (medicationInfoAvailable) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer // NEW
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer // NEW
                                        )
                                        Spacer(modifier = Modifier.size(12.dp))
                                        Text(
                                            text = stringResource(id = R.string.medication_information_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer // NEW
                                        )
                                    }
                                    Text(
                                        text = stringResource(id = R.string.medication_info_description_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer, // NEW
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Button(
                                        onClick = { onNavigateToMedicationInfo(medicationId, medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name) },
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text(text = stringResource(id = R.string.view_full_information))
                                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                } // Closes LazyColumn
            } // Closes Scaffold content lambda
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
        } // Closes the main else block
    } // Closes MedicationSpecificTheme
} // Closes MedicationDetailsScreen

@Preview(showBackground = true, name = "Medication Details Screen")
@Composable
fun MedicationDetailsScreenPreview() {
    AppTheme {
        // ViewModel parameters are omitted to use defaults,
        // which might result in a preview with no dynamic data.
        MedicationDetailsScreen(
            medicationId = 1,
            onNavigateBack = {},
            sharedTransitionScope = null, // Pass null for preview
            animatedVisibilityScope = null, // Preview won't have a real scope
            isHostedInPane = false,
            graphViewModel = null, // Added for preview
            onNavigateToAllSchedules = { _, _ -> }, // Adjusted for new signature
            onNavigateToMedicationHistory = { _, _ -> }, // Adjusted for new signature
            onNavigateToMedicationGraph = { _, _ -> }, // Adjusted for new signature
            onNavigateToMedicationInfo = { _, _ -> } // Adjusted for new signature
        )
    }
}

// Previews for ScheduleItem moved up with its definition.
// Keep other previews at the end or move them as well if preferred.
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