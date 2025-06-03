@file:OptIn(ExperimentalSharedTransitionApi::class) // Moved OptIn to file-level
package com.d4viddf.medicationreminder.ui.screens

// ScheduleItem will be local, so no import from components
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
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
import com.d4viddf.medicationreminder.viewmodel.MedicationReminderViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class) // Removed ExperimentalSharedTransitionApi
@Composable
fun MedicationDetailsScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?, // Add this
    animatedVisibilityScope: AnimatedVisibilityScope?, // Make nullable
    enableSharedTransition: Boolean, // Added new parameter
    viewModel: MedicationViewModel = hiltViewModel(),
    scheduleViewModel: MedicationScheduleViewModel = hiltViewModel(),
    medicationTypeViewModel: MedicationTypeViewModel = hiltViewModel(),
    medicationReminderViewModel: MedicationReminderViewModel = hiltViewModel() // Added
) {
    var medicationState by remember { mutableStateOf<Medication?>(null) }
    var scheduleState by remember { mutableStateOf<MedicationSchedule?>(null) }
    var medicationTypeState by remember { mutableStateOf<MedicationType?>(null) }

    // var contentVisible by remember { mutableStateOf(!enableSharedTransition) } // DELETED
    // LaunchedEffect(key1 = enableSharedTransition) { // DELETED BLOCK
    //     if (enableSharedTransition) {
    //         kotlinx.coroutines.delay(300)
    //         contentVisible = true
    //     } else {
    //         contentVisible = true
    //     }
    // }

    val progressDetails by viewModel.medicationProgressDetails.collectAsState()
    val todayScheduleItems by medicationReminderViewModel.todayScheduleItems.collectAsState() // Added
    var showDialog by remember { mutableStateOf(false) } // Added for AddPastMedicationDialog

    LaunchedEffect(key1 = medicationId) {
        val med = viewModel.getMedicationById(medicationId)
        medicationState = med
        if (med != null) {
            scheduleState = scheduleViewModel.getActiveScheduleForMedication(med.id)
            viewModel.observeMedicationAndRemindersForDailyProgress(med.id)
            medicationReminderViewModel.loadTodaySchedule(medicationId) // Added data load call

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
                        containerColor = color.backgroundColor,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding) // Apply innerPadding
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
                                            animatedVisibilityScope!!
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

                        // Usar el nuevo componente MedicationDetailHeader
                        MedicationDetailHeader(
                            medicationId = medicationId, // Pass medicationId
                            medicationName = medicationState?.name,
                            medicationDosage = medicationState?.dosage,
                            medicationImageUrl = medicationTypeState?.imageUrl, // Pasar la URL de la imagen del tipo
                            colorScheme = color,
                            modifier = Modifier.padding(top = 16.dp) // Add padding to push content below TopAppBar
                            // El modifier por defecto del componente ya tiene fillMaxWidth
                        )

                        // Spacer(modifier = Modifier.height(16.dp)) // This spacer might need adjustment or removal. Keeping for now.

                        MedicationProgressDisplay(
                            progressDetails = progressDetails,
                            colorScheme = color,
                            indicatorSizeDp = 220.dp // Explicitly pass the size
                        )

                        Spacer(modifier = Modifier.height(16.dp)) // Espacio original antes de contadores

                        MedicationDetailCounters(
                            colorScheme = color,
                            medication = medicationState,
                            schedule = scheduleState,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
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
                items(todayScheduleItems, key = { it.id }) { todayItem ->
                    val isActuallyPast =
                        todayItem.time.isBefore(java.time.LocalTime.now()) // Recalculate for safety, though ViewModel should be accurate

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
                item {
                    Spacer(modifier = Modifier.height(48.dp)) // Espacio original antes de contadores
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
            enableSharedTransition = false // Added for preview
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
