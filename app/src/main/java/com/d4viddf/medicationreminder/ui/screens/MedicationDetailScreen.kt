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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.MedicationDetailCounters
import com.d4viddf.medicationreminder.ui.components.MedicationProgressDisplay
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailsScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel(),
    scheduleViewModel: MedicationScheduleViewModel = hiltViewModel()
) {
    var medicationState by remember { mutableStateOf<Medication?>(null) }
    var scheduleState by remember { mutableStateOf<MedicationSchedule?>(null) } // Necesario para MedicationDetailCounters

    val progressDetails by viewModel.medicationProgressDetails.collectAsState()

    LaunchedEffect(key1 = medicationId) {
        val med = viewModel.getMedicationById(medicationId)
        medicationState = med
        if (med != null) { // Solo continuar si la medicación se cargó
            scheduleState = scheduleViewModel.getActiveScheduleForMedication(med.id)
            viewModel.calculateAndSetProgressDetails(med) // Calcular progreso
        } else {
            viewModel.calculateAndSetProgressDetails(null) // Limpiar progreso si no hay medicación
        }
    }

    val color = remember(medicationState?.color) {
        try {
            MedicationColor.valueOf(medicationState?.color ?: MedicationColor.LIGHT_ORANGE.name)
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_ORANGE
        }
    }

    if (medicationState == null && progressDetails == null) { // Ajustar condición de carga
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
                    // ... (Header: Back, Edit, Nombre, Dosis) ...
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
                                contentDescription = "Back",
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
                            Text("Edit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = medicationState?.name ?: "Cargando nombre...",
                                fontSize = 36.sp, fontWeight = FontWeight.Bold,
                                color = color.textColor, lineHeight = 40.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = medicationState?.dosage?.takeIf { it.isNotBlank() } ?: "Sin dosificación",
                                fontSize = 20.sp, color = color.textColor
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        // Image(...)
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    MedicationProgressDisplay(
                        progressDetails = progressDetails,
                        colorScheme = color
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MedicationDetailCounters(
                        colorScheme = color,
                        medication = medicationState,
                        schedule = scheduleState,
                        modifier = Modifier.padding(horizontal = 12.dp) // Para que no toque los bordes si es largo

                    )
                }
            }

            // ... (item para la lista de "Today's Schedule") ...
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Today", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    ScheduleItem(time = "9:00", label = "After waking up", enabled = true)
                    ScheduleItem(time = "15:00", label = "With lunch", enabled = false)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ScheduleItem Composable (sin cambios)
@Composable
fun ScheduleItem(time: String, label: String, enabled: Boolean) {
    // ... (implementación de ScheduleItem)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Switch(checked = enabled, onCheckedChange = { /* Handle toggle */ })
    }
}