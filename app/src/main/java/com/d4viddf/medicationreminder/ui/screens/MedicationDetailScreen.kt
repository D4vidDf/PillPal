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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.data.TodayScheduleItem // Added
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.AddPastMedicationDialog // Added
import com.d4viddf.medicationreminder.ui.components.MedicationDetailCounters
import com.d4viddf.medicationreminder.ui.components.MedicationDetailHeader
import com.d4viddf.medicationreminder.ui.components.MedicationProgressDisplay
// ScheduleItem will be local, so no import from components
import com.d4viddf.medicationreminder.viewmodel.MedicationReminderViewModel // Added
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter // Added
import java.time.format.FormatStyle // Added


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MedicationDetailsScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel(),
    scheduleViewModel: MedicationScheduleViewModel = hiltViewModel(),
    medicationTypeViewModel: MedicationTypeViewModel = hiltViewModel(),
    medicationReminderViewModel: MedicationReminderViewModel = hiltViewModel() // Added
) {
    var medicationState by remember { mutableStateOf<Medication?>(null) }
    var scheduleState by remember { mutableStateOf<MedicationSchedule?>(null) }
    var medicationTypeState by remember { mutableStateOf<MedicationType?>(null) }

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
        // Scaffold was part of previous attempts, but original file is just LazyColumn
        LazyColumn(modifier = Modifier.fillMaxSize()) { // Add .padding(scaffoldInnerPadding) if a Scaffold is reintroduced
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = color.backgroundColor,
                            shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                        )
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 16.dp)
                ) {
                    // Row para Back y Edit (sin cambios)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable { /* TODO: Handle edit action */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(id = R.string.edit), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Usar el nuevo componente MedicationDetailHeader
                    MedicationDetailHeader(
                        medicationName = medicationState?.name,
                        medicationDosage = medicationState?.dosage,
                        medicationImageUrl = medicationTypeState?.imageUrl, // Pasar la URL de la imagen del tipo
                        colorScheme = color
                        // El modifier por defecto del componente ya tiene fillMaxWidth
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Ajustado el espacio después del header

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
                            .background(Color.Black, shape = RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.content_desc_add_past_dose),
                            tint = Color.White,
                            modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
                        )
                    }
                }
            }

            var futureRemindersStarted = false
            items(todayScheduleItems, key = { it.id }) { todayItem ->
                val isActuallyPast = todayItem.time.isBefore(java.time.LocalTime.now()) // Recalculate for safety, though ViewModel should be accurate

                if (!isActuallyPast && !futureRemindersStarted) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), thickness = 3.dp)
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

    if (showDialog) { // Added AddPastMedicationDialog call
        AddPastMedicationDialog(
            medicationNameDisplay = medicationState?.name ?: stringResource(id = R.string.medication_name_placeholder),
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

@Preview(showBackground = true, name = "Medication Details Screen")
@Composable
fun MedicationDetailsScreenPreview() {
    AppTheme {
        // ViewModel parameters are omitted to use defaults,
        // which might result in a preview with no dynamic data.
        MedicationDetailsScreen(
            medicationId = 1,
            onNavigateBack = {}
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

// ScheduleItem Composable - Adapted
@Composable
private fun ScheduleItem(
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
            Text(text = time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Switch(
            checked = isTaken,
            onCheckedChange = onTakenChange, // Use the callback
            enabled = enabled // Control if switch can be interacted with
        )
    }
}