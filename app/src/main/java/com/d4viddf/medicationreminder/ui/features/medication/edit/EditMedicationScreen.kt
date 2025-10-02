package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.navigation.Screen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicationScreen(
    navController: NavController,
    viewModel: EditMedicationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadMedicationData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        viewModel.onBackClicked(onNavigateBack)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.edit_medication_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClicked(onNavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
import com.d4viddf.medicationreminder.data.model.Medication
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

        Column(modifier = Modifier.padding(paddingValues)) {
            GeneralInfoSection(state, navController)
            ScheduleSection(state)
            DoseSection(state)
            MedicationDetailsSection(state, navController)

            Spacer(modifier = Modifier.weight(1f))

            ActionButton(text = stringResource(R.string.edit_med_archive_button), onClick = { viewModel.onArchiveClicked() })
            ActionButton(text = stringResource(R.string.edit_med_delete_button), onClick = { viewModel.onDeleteClicked() })
            ActionButton(text = stringResource(R.string.edit_med_end_treatment_button), onClick = { viewModel.onEndTreatmentClicked() })
            val suspendText = if (state.medication?.isSuspended == true) stringResource(R.string.edit_med_restore_treatment_button) else stringResource(R.string.edit_med_suspend_treatment_button)
            ActionButton(text = suspendText, onClick = { viewModel.toggleSuspend() })

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.showArchiveDialog) {
            ConfirmationDialog(
                onDismissRequest = { viewModel.onDismissArchiveDialog() },
                onConfirmation = { viewModel.confirmArchive() },
                dialogTitle = "Archive Treatment",
                dialogText = "Do you want to archive this treatment? Archiving treatment stops reminders but keeps the data in the app."
            )
        }

        if (state.showDeleteDialog) {
            ConfirmationDialog(
                onDismissRequest = { viewModel.onDismissDeleteDialog() },
                onConfirmation = {
                    viewModel.confirmDelete {
                        navController.popBackStack() // Pop EditMedicationScreen
                        navController.popBackStack() // Pop MedicationDetailScreen
                    }
                },
                dialogTitle = "Delete Treatment",
                dialogText = "Are you sure you want to delete this treatment from the app? This action will permanently erase the data."
            )
        }

        if (state.showEndTreatmentDialog) {
            EndTreatmentDatePickerDialog(
                onDismiss = { viewModel.onDismissEndTreatmentDialog() },
                onConfirm = { selectedDate ->
                    viewModel.confirmEndTreatment(selectedDate)
                }
            )
        }

        if (state.showSaveDialog) {
            SaveChangesDialog(
                onDismissRequest = { viewModel.onDismissSaveDialog() },
                onSaveChanges = {
                    viewModel.confirmSaveChanges()
                    onNavigateBack()
                },
                onDiscardChanges = {
                    viewModel.onDismissSaveDialog() // Hide dialog
                    onNavigateBack() // Then navigate back
                }
            )
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun GeneralInfoSection(state: EditMedicationState, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.edit_med_general_info_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = state.medication?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            state.medication?.let {
                                navController.navigate(Screen.EditForm.createRoute(it.id))
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.edit_med_form_color),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.medicationType?.name ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ColorSelector(
                    selectedColor = state.medication?.color?.let { MedicationColor.valueOf(it) } ?: MedicationColor.LIGHT_ORANGE,
                    onColorSelected = { color -> viewModel.onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun ScheduleSection(state: EditMedicationState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.edit_med_schedule_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Frequency Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: Navigate to frequency selection */ },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.edit_med_frequency), style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.schedule?.getFormattedSchedule() ?: stringResource(id = R.string.not_set),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Duration Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: Navigate to duration selection */ },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.edit_med_duration), style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val durationText = if (state.medication?.startDate != null) {
                            "${state.medication.startDate} - ${state.medication.endDate ?: "Ongoing"}"
                        } else {
                            "Not set"
                        }
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoseSection(state: EditMedicationState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.edit_med_dose_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: Navigate to dose editing */ }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.edit_med_amount), style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.dose?.dosage ?: stringResource(id = R.string.not_set),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicationDetailsSection(state: EditMedicationState, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.edit_med_details_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        state.medication?.let {
                            navController.navigate(Screen.EditStock.createRoute(it.id))
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.edit_med_stock), style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val stockText = state.medication?.remainingDoses?.toString() ?: stringResource(id = R.string.not_set)
                    Text(
                        text = stockText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}