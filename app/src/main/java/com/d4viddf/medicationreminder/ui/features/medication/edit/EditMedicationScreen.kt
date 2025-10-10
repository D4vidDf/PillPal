package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.*
import com.d4viddf.medicationreminder.ui.features.medication.add.components.ColorSelector
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import com.d4viddf.medicationreminder.utils.getMedicationTypeStringResource
import java.time.LocalDate

@Composable
fun EditMedicationScreen(
    navController: NavController,
    viewModel: EditMedicationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

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

    EditMedicationScreenContent(
        state = state,
        navController = navController,
        onBackClicked = { viewModel.onBackClicked(onNavigateBack) },
        onArchiveClicked = viewModel::onArchiveClicked,
        onDeleteClicked = viewModel::onDeleteClicked,
        onEndTreatmentClicked = viewModel::onEndTreatmentClicked,
        onDismissArchiveDialog = viewModel::onDismissArchiveDialog,
        onConfirmArchive = viewModel::confirmArchive,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog,
        onConfirmDelete = {
            viewModel.confirmDelete {
                navController.popBackStack()
                navController.popBackStack()
            }
        },
        onDismissEndTreatmentDialog = viewModel::onDismissEndTreatmentDialog,
        onConfirmEndTreatment = viewModel::confirmEndTreatment,
        onDismissSaveDialog = viewModel::onDismissSaveDialog,
        onConfirmSaveChanges = {
            viewModel.confirmSaveChanges()
            onNavigateBack()
        },
        onDiscardChanges = {
            viewModel.onDismissSaveDialog()
            onNavigateBack()
        },
        onColorPickerClicked = viewModel::onColorPickerClicked,
        onColorSelected = viewModel::onColorSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicationScreenContent(
    state: EditMedicationState,
    navController: NavController,
    onBackClicked: () -> Unit,
    onArchiveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onEndTreatmentClicked: () -> Unit,
    onDismissArchiveDialog: () -> Unit,
    onConfirmArchive: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissEndTreatmentDialog: () -> Unit,
    onConfirmEndTreatment: (LocalDate) -> Unit,
    onDismissSaveDialog: () -> Unit,
    onConfirmSaveChanges: () -> Unit,
    onDiscardChanges: () -> Unit,
    onColorPickerClicked: () -> Unit,
    onColorSelected: (MedicationColor) -> Unit
) {
    BackHandler { onBackClicked() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.edit_medication_title)) },
                navigationIcon = {
                    FilledTonalIconButton (onClick = navController::popBackStack, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )

                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())) {
            GeneralInfoSection(state, navController, onColorPickerClicked, onColorSelected)
            ScheduleSection(state)
            DoseSection(state)
            MedicationDetailsSection(state, navController)

            Spacer(modifier = Modifier.height(24.dp))

            ActionButton(text = stringResource(R.string.edit_med_archive_button), onClick = onArchiveClicked)
            Spacer(modifier = Modifier.height(8.dp))
            ActionButton(text = stringResource(R.string.edit_med_end_treatment_button), onClick = onEndTreatmentClicked)
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onDeleteClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(ButtonDefaults.MediumContainerHeight)
                    .padding(horizontal = 16.dp),
                shapes= ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text(stringResource(R.string.edit_med_delete_button))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.showArchiveDialog) {
            ConfirmationDialog(
                onDismissRequest = onDismissArchiveDialog,
                onConfirmation = onConfirmArchive,
                dialogTitle = "Archive Treatment",
                dialogText = "Do you want to archive this treatment? Archiving treatment stops reminders but keeps the data in the app."
            )
        }

        if (state.showDeleteDialog) {
            ConfirmationDialog(
                onDismissRequest = onDismissDeleteDialog,
                onConfirmation = onConfirmDelete,
                dialogTitle = "Delete Treatment",
                dialogText = "Are you sure you want to delete this treatment from the app? This action will permanently erase the data."
            )
        }

        if (state.showEndTreatmentDialog) {
            EndTreatmentDatePickerDialog(
                onDismiss = onDismissEndTreatmentDialog,
                onConfirm = onConfirmEndTreatment
            )
        }

        if (state.showSaveDialog) {
            SaveChangesDialog(
                onDismissRequest = onDismissSaveDialog,
                onSaveChanges = onConfirmSaveChanges,
                onDiscardChanges = onDiscardChanges
            )
        }

        // No more modal bottom sheet
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(ButtonDefaults.MediumContainerHeight)
            .padding(horizontal = 16.dp),
        shapes= ButtonDefaults.shapes()
    ) {
        Text(text)
    }
}

@Composable
private fun GeneralInfoSection(
    state: EditMedicationState,
    navController: NavController,
    onColorPickerClicked: () -> Unit,
    onColorSelected: (MedicationColor) -> Unit
) {
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
            shape = RoundedCornerShape(12.dp, 12.dp, 6.dp, 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.label_name),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = state.medication?.name?.substringBefore(" ") ?: "Loading...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                state.medication?.let {
                    navController.navigate(Screen.EditForm.createRoute(it.id, it.color))
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.edit_med_form_color),
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val medicationType = state.medicationType?.let {
                        stringResource(id = getMedicationTypeStringResource(it.id))
                    } ?: ""
                    Text(
                        text = medicationType,
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
        Spacer(Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp, 6.dp, 12.dp, 12.dp),
            onClick = onColorPickerClicked,
            colors = CardDefaults.cardColors()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_color),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val color = state.medication?.color?.let { MedicationColor.valueOf(it) }
                        if (color != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = color.colorValue,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = color.colorName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.not_set),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (state.isColorSelectorExpanded) {
                    ColorSelector(
                        selectedColor = state.medication?.color?.let { MedicationColor.valueOf(it) } ?: MedicationColor.LIGHT_ORANGE,
                        onColorSelected = {
                            onColorSelected(it)
                            onColorPickerClicked()
                        }
                    )
                }
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
            shape = RoundedCornerShape(12.dp,12.dp,6.dp,6.dp),
            onClick = {}
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Frequency Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
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
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp,6.dp,12.dp,12.dp),
            onClick = {}
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Duration Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
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

@Preview(showBackground = true)
@Composable
fun EditMedicationScreenPreview() {
    AppTheme {
        val medication = Medication(
            id = 1,
            name = "Mestinon",
            typeId = 1,
            color = "LIGHT_PINK",
            packageSize = 100,
            remainingDoses = 90,
            saveRemainingFraction = false,
            startDate = "2023-01-01",
            endDate = null,
            reminderTime = null,
            registrationDate = null,
            nregistro = null,
            lowStockThreshold = null,
            lowStockReminderDays = null,
            isArchived = false,
            isSuspended = false
        )
        val medicationType = MedicationType(id = 1, name = "Pill", imageUrl = "")
        val schedule = MedicationSchedule(medicationId = 1, scheduleType = ScheduleType.DAILY, startDate = "2023-01-01", intervalHours = null, intervalMinutes = null, daysOfWeek = null, specificTimes = listOf(java.time.LocalTime.of(8, 0)), intervalStartTime = null, intervalEndTime = null)
        val dosage = MedicationDosage(medicationId = 1, dosage = "1 pill", startDate = "2023-01-01")

        val state = EditMedicationState(
            isLoading = false,
            medication = medication,
            medicationType = medicationType,
            schedule = schedule,
            dose = dosage
        )

        EditMedicationScreenContent(
            state = state,
            navController = rememberNavController(),
            onBackClicked = {},
            onArchiveClicked = {},
            onDeleteClicked = {},
            onEndTreatmentClicked = {},
            onDismissArchiveDialog = {},
            onConfirmArchive = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onDismissEndTreatmentDialog = {},
            onConfirmEndTreatment = {},
            onDismissSaveDialog = {},
            onConfirmSaveChanges = {},
            onDiscardChanges = {},
            onColorPickerClicked = {},
            onColorSelected = {}
        )
    }
}