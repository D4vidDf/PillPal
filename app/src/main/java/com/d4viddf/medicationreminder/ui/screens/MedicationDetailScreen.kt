@file:OptIn(ExperimentalSharedTransitionApi::class) // Moved OptIn to file-level
package com.d4viddf.medicationreminder.ui.screens

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
import androidx.compose.foundation.layout.width // Explicit import for Modifier.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext // Changed from .rounded to .filled for NavigateNext
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
// import androidx.compose.material.icons.automirrored.rounded.NavigateNext // Keep only one NavigateNext import
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info // Added for Medication Info Card
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.d4viddf.medicationreminder.viewmodel.MedicationGraphViewModel // Added
import com.d4viddf.medicationreminder.viewmodel.MedicationReminderViewModel
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
    onNavigateToAllSchedules: (Int) -> Unit,
    onNavigateToMedicationHistory: (Int) -> Unit,
    onNavigateToMedicationGraph: (Int) -> Unit,
    onNavigateToMedicationInfo: (Int) -> Unit
) {
    var medicationState by remember { mutableStateOf<Medication?>(null) }
    var scheduleState by remember { mutableStateOf<MedicationSchedule?>(null) }
    var medicationTypeState by remember { mutableStateOf<MedicationType?>(null) }

    val progressDetails by viewModel.medicationProgressDetails.collectAsState()
    val todayScheduleItems by medicationReminderViewModel.todayScheduleItems.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Graph Data
    val weeklyGraphData by graphViewModel?.graphData?.collectAsState() ?: remember { mutableStateOf(emptyMap()) }

    LaunchedEffect(medicationId, graphViewModel) { // Added graphViewModel to keys
        // Load base medication data
        viewModel.getMedicationById(medicationId)?.let { med ->
            medicationState = med
            scheduleState = scheduleViewModel.getActiveScheduleForMedication(med.id) // Potentially suspend
            viewModel.observeMedicationAndRemindersForDailyProgress(med.id) // Suspend
            medicationReminderViewModel.loadTodaySchedule(medicationId) // Suspend

            med.typeId?.let { typeId ->
                // This collect might be an issue if it never completes or if medId changes frequently.
                // Consider .firstOrNull() if you only need initial load or a more specific trigger.
                medicationTypeViewModel.medicationTypes.collect { types -> // Suspend
                    medicationTypeState = types.find { it.id == typeId }
                }
            }
        }

        // Load graph data
        if (graphViewModel != null) {
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

    if (medicationState == null && progressDetails == null && todayScheduleItems.isEmpty()) { // Updated condition
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        if (!isHostedInPane) {
                            Box (modifier = Modifier.padding(start = 10.dp)){
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
                                        modifier = Modifier.size(28.dp), tint = Color.White
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Original Box structure for the Edit button
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp) // Add some padding to separate from the edge of screen if needed
                                .background(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { /* TODO: Handle edit action */ } // Ensure clickable is present
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ), // This is the internal padding for the text
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.edit),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = color.backgroundColor, // Reverted
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
            // contentWindowInsets parameter is removed
        ) { innerPadding -> // innerPadding is from Scaffold
            // Vars like systemStatusBarsPaddingTop, systemNavigationBarsPaddingBottom, topAppBarHeight are removed if only used for the custom contentPadding.
            val padding =  if (isHostedInPane) PaddingValues(top = innerPadding.calculateTopPadding()) else innerPadding
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), // Reverted to use innerPadding from Scaffold
                // contentPadding argument is removed
            ) {
                item {
                    // Removed: val sharedTransitionScope = LocalSharedTransitionScope.current
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = color.backgroundColor, // This background might be redundant if TopAppBar uses it
                                shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                            )
                            .then(
                                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) { // Use with(scope)
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
                            ) // Removed top padding, TopAppBar handles it
                    ) {
                        // Row for Back and Edit is removed from here
                        // Spacer after the Row is also removed

                        // NEW: Add conditional rendering based on essential data
                        if (medicationState != null && progressDetails != null) {
                            MedicationDetailHeader(
                                medicationId = medicationId, // Pass medicationId
                                medicationName = medicationState?.name, // medicationState is confirmed non-null by the if
                                medicationDosage = medicationState?.dosage,
                                medicationImageUrl = medicationTypeState?.imageUrl, // Pasar la URL de la imagen del tipo
                                colorScheme = color,
                                modifier = Modifier.padding(top = 16.dp) // Add padding to push content below TopAppBar
                                // El modifier por defecto del componente ya tiene fillMaxWidth
                            )

                             Spacer(modifier = Modifier.height(16.dp)) // This spacer might need adjustment or removal. Keeping for now.

                            MedicationProgressDisplay(
                                progressDetails = progressDetails, // progressDetails is confirmed non-null by the if
                                colorScheme = color,
                                indicatorSizeDp = 220.dp, // Explicitly pass the size
                            )

                            Spacer(modifier = Modifier.height(16.dp)) // Espacio original antes de contadores

                            MedicationDetailCounters(
                                colorScheme = color,
                                medication = medicationState, // medicationState is confirmed non-null by the if
                                schedule = scheduleState,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        } else {
                            // Optional: Placeholder or loading indicator if the outer screen loading condition
                            // isn't sufficient and this specific block's data might still be loading.
                            // For now, keeping it empty means the Column might appear empty if this condition is false,
                            // which relies on the outer screen loading state to prevent this screen from showing prematurely.
                            // If the outer condition (medicationState == null && progressDetails == null && todayScheduleItems.isEmpty())
                            // is robust, this 'else' branch for the inner 'if' might not be strictly necessary,
                            // as the components would only be composed when data is ready.
                            // However, being explicit ensures these specific components wait for their direct data.
                        }
                    }
                }

                // Item for "Today" title and Add Past Reminder Button
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
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(id = R.string.content_desc_add_past_dose),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
                            )
                        }
                    }
                }

                // NEW CONDITIONAL MESSAGE LOGIC
                if (todayScheduleItems.isEmpty() && medicationState != null) {
                    item {
                        Text(
                            text = stringResource(id = R.string.medication_detail_no_reminders_today),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 16.dp
                                ), // Added more vertical padding
                            style = MaterialTheme.typography.bodyMedium, // Using bodyMedium for a slightly softer look
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                var futureRemindersStarted = false
                // Display only the first 5 schedule items
                items(todayScheduleItems.take(5), key = { it.id }) { todayItem ->
                    val isActuallyPast =
                        todayItem.time.isBefore(LocalTime.now()) // Recalculate for safety, though ViewModel should be accurate

                    if (!isActuallyPast && !futureRemindersStarted) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ), thickness = 3.dp, color = MaterialTheme.colorScheme.onBackground
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
                        // Enable toggle only for past or current items. Future items are disabled.
                        enabled = isActuallyPast || todayItem.isTaken
                    )
                }

                // Show "Show More" button if there are more than 5 schedules
                if (todayScheduleItems.size > 5) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { onNavigateToAllSchedules(medicationId) }, // Updated onClick
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Show More")
                            }
                        }
                    }
                }

                // Medication History Card
                item {
                    Spacer(modifier = Modifier.height(16.dp)) // Add space before the card
                    ElevatedCard(
                        onClick = { onNavigateToMedicationHistory(medicationId) }, // Updated onClick
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp), // Increased vertical padding
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null, // Decorative
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                                Text(
                                    text = stringResource(id = R.string.medication_history_title), // Placeholder for actual string
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext, // Changed to .Filled
                                contentDescription = null, // Decorative
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Graphics Card
                item {
                    Spacer(modifier = Modifier.height(16.dp)) // Add space before the card
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.current_week_dosage_title), // Placeholder
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Integrate WeeklyBarChartDisplay
                                WeeklyBarChartDisplay(graphData = weeklyGraphData, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onNavigateToMedicationGraph(medicationId) }, // Updated onClick
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(text = stringResource(id = R.string.view_more_stats)) // Placeholder
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

                // Medication Information Card
                item {
                    // Updated condition to check for nregistro availability
                    val medicationInfoAvailable = !medicationState?.nregistro.isNullOrBlank()

                    if (medicationInfoAvailable) {
                        Spacer(modifier = Modifier.height(16.dp)) // Add space before the card
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                        contentDescription = null, // Decorative
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(
                                        text = stringResource(id = R.string.medication_information_title), // Placeholder
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.medication_info_description_placeholder), // Placeholder
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Button(
                                    onClick = { onNavigateToMedicationInfo(medicationId) }, // Updated onClick
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(text = stringResource(id = R.string.view_full_information)) // Placeholder
                                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateNext, // Ensured .Filled here as well
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp)) // Existing spacer at the bottom
                }
            }
        }
        if (showDialog) { // Added AddPastMedicationDialog call
            AddPastMedicationDialog(
                medicationNameDisplay = medicationState?.name
                    ?: stringResource(id = R.string.medication_name_placeholder),
                onDismissRequest = { showDialog = false },
                onSave = { date, time ->
                    medicationReminderViewModel.addPastMedicationTaken(
                        medicationId = medicationId,
                        // medicationNameParam is no longer needed by the ViewModel method
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
            onNavigateToAllSchedules = {},
            onNavigateToMedicationHistory = {},
            onNavigateToMedicationGraph = {},
            onNavigateToMedicationInfo = {}
        )
    }
}

@Composable
private fun WeeklyBarChartDisplay(graphData: Map<String, Int>, modifier: Modifier = Modifier) {
    if (graphData.isEmpty()) {
        Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(id = R.string.loading_graph_data)) // Or "No data for this week"
        }
        return
    }

    val maxCount = graphData.values.maxOrNull() ?: 1
    // Ensure todayShortName matches exactly how keys are stored in graphData (e.g. "Mon", "Tue")
    val todayShortName = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val chartHeight = 120.dp // Smaller height for the card display
    val barMaxHeight = 100.dp // Max height for a single bar

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .padding(top = 8.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        graphData.forEach { (dayLabel, count) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp) // Narrower bars
                        .height(if (maxCount > 0) ((count.toFloat() / maxCount.toFloat()) * barMaxHeight.value).dp else 0.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (dayLabel.equals(todayShortName, ignoreCase = true))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondaryContainer
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(dayLabel, style = MaterialTheme.typography.labelSmall) // Smaller label
            }
        }
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