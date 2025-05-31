package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold // Added import
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset // Added import
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer // Added import
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection // Added import
import androidx.compose.ui.input.nestedscroll.NestedScrollSource // Added import
import androidx.compose.ui.input.nestedscroll.nestedScroll // Added import
import androidx.compose.ui.platform.LocalDensity // Added import
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp // Added import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.data.TodayScheduleItem // Added for type safety
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.AddPastMedicationDialog
import com.d4viddf.medicationreminder.ui.components.CustomMedicationHeader // Added import
import com.d4viddf.medicationreminder.ui.components.MedicationDetailCounters
import com.d4viddf.medicationreminder.ui.components.MedicationDetailHeader
import com.d4viddf.medicationreminder.ui.components.MedicationProgressDisplay
import com.d4viddf.medicationreminder.ui.components.ScheduleItem
import com.d4viddf.medicationreminder.ui.components.TimePickerDialog
import com.d4viddf.medicationreminder.viewmodel.MedicationReminderViewModel // Added import
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.layout.PaddingValues // For LazyColumn contentPadding
import androidx.compose.ui.draw.clip // For button styling in MinimalStickyAppBar
import androidx.compose.ui.graphics.graphicsLayer // For CustomMedicationHeader alpha/translation
import androidx.compose.ui.text.style.TextOverflow // For titles
// import com.d4viddf.medicationreminder.ui.components.CustomMedicationHeader // Already imported
// Other imports like Dp, LocalDensity, Offset, NestedScrollConnection etc. assumed to be added with Box layout step

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailsScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel(),
    scheduleViewModel: MedicationScheduleViewModel = hiltViewModel(),
    medicationTypeViewModel: MedicationTypeViewModel = hiltViewModel(),
    medicationReminderViewModel: MedicationReminderViewModel = hiltViewModel() // Added ViewModel
) {
    val localDensity = LocalDensity.current
    // These heights are placeholders; dynamic measurement or more precise calculation is better.
    val headerMaxHeightDp = 330.dp // Approximate height for CustomMedicationHeader
    val headerMinHeightDp = 56.dp  // Typical toolbar height (for the sticky part when collapsed)

    val headerMaxHeightPx = with(localDensity) { headerMaxHeightDp.toPx() }
    val headerMinHeightPx = with(localDensity) { headerMinHeightDp.toPx() }
    val headerHeightRange = headerMaxHeightPx - headerMinHeightPx

    var headerOffset by remember { mutableStateOf(0f) } // Current negative offset (scroll up)

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffset + delta
                headerOffset = newOffset.coerceIn(-headerHeightRange, 0f)
                // Consume the scroll delta that was used by the header
                return Offset(0f, headerOffset - (newOffset - delta)) // Return consumed delta
            }
        }
    }

    var medicationState by remember { mutableStateOf<Medication?>(null) }
    var scheduleState by remember { mutableStateOf<MedicationSchedule?>(null) }
    var medicationTypeState by remember { mutableStateOf<MedicationType?>(null) } // Estado para el tipo

    val progressDetails by viewModel.medicationProgressDetails.collectAsState()
    val todayScheduleItems by medicationReminderViewModel.todayScheduleItems.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Calculate scrollProgress based on headerOffset
    val scrollProgress = (-headerOffset / headerHeightRange).coerceIn(0f, 1f)

    LaunchedEffect(key1 = medicationId) {
        val med = viewModel.getMedicationById(medicationId)
        medicationState = med
        if (med != null) {
            scheduleState = scheduleViewModel.getActiveScheduleForMedication(med.id)
            viewModel.observeMedicationAndRemindersForDailyProgress(med.id)
            medicationReminderViewModel.loadTodaySchedule(medicationId) // Load today's schedule

            med.typeId?.let { typeId ->
                medicationTypeViewModel.medicationTypes.collect { types ->
                    medicationTypeState = types.find { it.id == typeId }
                }
            }
        } else {
            // Limpiar los detalles del progreso si no hay medicación
            // La función calculateAndSetDailyProgressDetails ya maneja el caso de medication == null
            // pero llamar a la de observación con un ID inválido no tendría sentido.
            // Es mejor que el ViewModel ponga progressDetails a null si med es null.
            // O, si quieres explícitamente limpiar, podrías tener una función viewModel.clearProgressDetails()
            // Por ahora, el calculateAndSetDailyProgressDetails pondrá null si med es null.
            // La lógica actual en el viewModel ya lo hace.
        }
    }

    val color = remember(medicationState?.color) {
        try {
            MedicationColor.valueOf(medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name)
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_ORANGE
        }
    }

    if (medicationState == null && progressDetails == null && todayScheduleItems.isEmpty()) { // Adjusted condition to check todayScheduleItems
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = { /* EMPTY - No Scaffold TopAppBar */ },
        ) { scaffoldInnerPadding -> // System bars padding

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldInnerPadding) // Apply system bar padding to the Box
                    .nestedScroll(nestedScrollConnection)
            ) {
                // CustomMedicationHeader will be placed here. Its offset will be controlled by headerOffset.
                // Data mapping for CustomMedicationHeader:
                val medName = medicationState?.name ?: stringResource(id = R.string.loading)
                val medType = medicationTypeState?.name ?: ""
                val medDosage = medicationState?.dosage ?: ""
                val medTypeAndDosage = if (medType.isNotEmpty() && medDosage.isNotEmpty()) "$medType - $medDosage" else medType + medDosage

                val currentProgress = progressDetails?.progress ?: 0f
                val progressText = "${progressDetails?.taken ?: 0} of ${progressDetails?.total ?: 0} taken"

                // Simplified counter data - replace with actual logic if available
                // These were previously in MedicationDetailCounters which might have more complex logic
                val dosesLeft = medicationState?.remainingDoses?.toString() ?: "N/A"
                // Duration/days left calculation would need schedule.endDate or similar
                val daysLeft = "N/A" // Placeholder

                CustomMedicationHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = headerOffset
                            alpha = 1f - scrollProgress // Fade out CustomMedicationHeader
                        },
                    medicationName = medName,
                    medicationTypeAndDosage = medTypeAndDosage,
                    progressValue = currentProgress,
                    progressText = progressText,
                    counter1Label = stringResource(id = R.string.medication_detail_counter_dose_unit_plural), // Example "tomas"
                    counter1Value = dosesLeft,
                    counter2Label = stringResource(id = R.string.medication_detail_counter_duration_remaining_days), // Example "días rest."
                    counter2Value = daysLeft,
                    headerBackgroundColor = color.backgroundColor,
                    contentColor = color.onBackgroundColor,
                    onNavigateBack = onNavigateBack,
                    onEdit = { /* TODO: Handle edit action */ },
                    scrollProgress = scrollProgress // Pass scrollProgress
                )

                MinimalStickyAppBar(
                    title = medName, // Use the same medication name
                    headerBackgroundColor = color.backgroundColor,
                    contentColor = color.onBackgroundColor,
                    onNavigateBack = onNavigateBack,
                    onEdit = { /* TODO: Handle edit action */ },
                    scrollProgress = scrollProgress,
                    headerMinHeight = headerMinHeightDp,
                    modifier = Modifier.align(Alignment.TopCenter) // Ensure it stays at the top
                )

                // The scrollable content (Today's schedule)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Content starts below the MINIMAL sticky header height
                    contentPadding = PaddingValues(top = headerMinHeightDp)
                ) {
                    // Item 1: Spacer to push content below the fully expanded CustomMedicationHeader area
                    item {
                        Spacer(modifier = Modifier.height(headerMaxHeightDp - headerMinHeightDp))
                    }

                    // Item 2: "Today" title and Add button
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
                        text = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_today_title),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showDialog = true },
                        modifier = Modifier
                            .background(Color.Black, shape = RoundedCornerShape(12.dp))
                            .padding(4.dp) // Padding inside the background, before the icon
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.content_desc_add_past_dose),
                            tint = Color.White, // White icon on black background
                            modifier = Modifier.size(FloatingActionButtonDefaults.SmallIconSize)
                        )
                    }
                }
            }

            var futureRemindersStarted = false
            items(todayScheduleItems, key = { it.id }) { todayItem ->
                val isActuallyPast = todayItem.time.isBefore(java.time.LocalTime.now()) // Recalculate for safety, though ViewModel should be accurate

                if (!isActuallyPast && !futureRemindersStarted) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    futureRemindersStarted = true
                }
                ScheduleItem(
                    time = todayItem.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                    label = todayItem.medicationName,
                    isTaken = todayItem.isTaken,
                    onTakenChange = { newState ->
                        medicationReminderViewModel.updateReminderStatus(todayItem.id, newState, medicationId)
                    },
                    // Enable toggle only for past or current items. Future items are disabled.
                    enabled = isActuallyPast || todayItem.isTaken
                )
            }
        }
    }
    if (showDialog) {
        AddPastMedicationDialog(
            medicationNameDisplay = medicationState?.name ?: stringResource(id = R.string.medication_name_placeholder), // Provide a fallback
            onDismissRequest = { showDialog = false },
            onSave = { date, time -> // Matches new signature: date is LocalDate, time is LocalTime
                medicationReminderViewModel.addPastMedicationTaken(
                    medicationId = medicationId,
                    // medicationNameParam is no longer needed by the ViewModel method
                    date = date,
                    time = time
                )
                showDialog = false // Dismiss dialog after save
            }
        )
    }
}

@Composable
private fun MinimalStickyAppBar(
    modifier: Modifier = Modifier,
    title: String,
    headerBackgroundColor: Color,
    contentColor: Color,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    scrollProgress: Float, // Value from 0 (fully expanded) to 1 (fully collapsed)
    headerMinHeight: Dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(headerMinHeight)
            .background(headerBackgroundColor.copy(alpha = scrollProgress * 0.95f))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = (scrollProgress * 0.4f).coerceAtMost(0.4f)))
                .clickable(enabled = scrollProgress > 0.5f) { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(id = R.string.back),
                modifier = Modifier.size(28.dp),
                tint = Color.White.copy(alpha = scrollProgress)
            )
        }

        // Title
        Text(
            text = title,
            color = contentColor.copy(alpha = scrollProgress),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )

        // Edit Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = (scrollProgress * 0.4f).coerceAtMost(0.4f)))
                .clickable(enabled = scrollProgress > 0.5f) { onEdit() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.edit),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = scrollProgress)
            )
        }
    }
}