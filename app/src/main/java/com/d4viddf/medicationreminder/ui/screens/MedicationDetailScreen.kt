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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.d4viddf.medicationreminder.ui.components.ScheduleItem
import com.d4viddf.medicationreminder.ui.components.TimePickerDialog
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailsScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel(),
    scheduleViewModel: MedicationScheduleViewModel = hiltViewModel(),
    medicationTypeViewModel: MedicationTypeViewModel = hiltViewModel() // Añadir ViewModel de tipo
) {
    var medicationState by remember { mutableStateOf<Medication?>(null) }
    var scheduleState by remember { mutableStateOf<MedicationSchedule?>(null) }
    var medicationTypeState by remember { mutableStateOf<MedicationType?>(null) } // Estado para el tipo

    val progressDetails by viewModel.medicationProgressDetails.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = medicationId) {
        val med = viewModel.getMedicationById(medicationId)
        medicationState = med
        if (med != null) {
            // scheduleState se necesita para MedicationDetailCounters
            scheduleState = scheduleViewModel.getActiveScheduleForMedication(med.id)

            // Iniciar la observación para el progreso DIARIO
            viewModel.observeMedicationAndRemindersForDailyProgress(med.id)

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

    if (medicationState == null && progressDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn {
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
                                .safeDrawingPadding()
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = stringResource(id = com.d4viddf.medicationreminder.R.string.back),
                                modifier = Modifier.size(28.dp), tint = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .safeDrawingPadding()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable { /* TODO: Handle edit action */ }
                                .padding(0.dp), // Reset padding for content description if it affects layout
                            contentAlignment = Alignment.Center,
                            // Adding content description to the Box acting as a button
                            // For a Box with Text, this might be redundant if TalkBack reads the Text,
                            // but if it were an Icon, this would be crucial.
                            // Semantics can be used for better control: Modifier.semantics { contentDescription = "Edit medication" }
                        ) {
                            Text(stringResource(id = com.d4viddf.medicationreminder.R.string.edit), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

            // Item for Today's Schedule title and Add button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .size(FloatingActionButtonDefaults.SmallIconSize) // Small FAB size
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(id = com.d4viddf.medicationreminder.R.string.content_desc_add_past_dose),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Placeholder for actual medication list logic
            // This will be replaced with dynamic data from the ViewModel
            val todayReminders = remember { // Replace with actual ViewModel data
                listOf(
                    Triple("9:00 AM", "Medication A", true), // time, name, isPast
                    Triple("1:00 PM", "Medication B", true),
                    Triple("6:00 PM", "Medication C", false) // isPast = false for future
                )
            }
            var futureRemindersStarted = false
            items(todayReminders) { (time, label, isPast) ->
                if (!isPast && !futureRemindersStarted) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    futureRemindersStarted = true
                }
                ScheduleItem(
                    time = time,
                    label = label,
                    isTaken = isPast, // Placeholder: for now, past items are "taken"
                    onTakenChange = { /* TODO */ },
                    enabled = isPast // Future items are not enabled for marking as taken
                )
            }
        }
    }
    if (showDialog) {
        AddPastMedicationDialog(
            onDismiss = { showDialog = false },
            onSave = { _, _, _ -> // TODO: Will call ViewModel
                showDialog = false
            }
        )
    }
}