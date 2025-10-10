package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationDosage
import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.MedicationType
import com.d4viddf.medicationreminder.data.model.ScheduleType
import com.d4viddf.medicationreminder.data.model.getFormattedSchedule
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
    var showColorSheet by remember { mutableStateOf(false) }

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
        onColorPickerClicked = { showColorSheet = true },
        onDismissColorPicker = { showColorSheet = false },
        onColorSelected = viewModel::onColorSelected,
        showColorSheet = showColorSheet
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    onDismissColorPicker: () -> Unit,
    onColorSelected: (MedicationColor) -> Unit,
    showColorSheet: Boolean
) {
    BackHandler { onBackClicked() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.edit_medication_title)) },
                navigationIcon = {
                    FilledTonalIconButton (onClick = onBackClicked, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )

                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            GeneralInfoSection(
                state = state,
                navController = navController,
                onColorPickerClicked = onColorPickerClicked,
                onDismissColorPicker = onDismissColorPicker,
                onColorSelected = onColorSelected,
                showColorSheet = showColorSheet
            )
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(stringResource(R.string.edit_med_delete_button))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.showArchiveDialog) {
            ConfirmationDialog(
                onDismissRequest = onDismissArchiveDialog,
                onConfirmation = onConfirmArchive,
                dialogTitle = stringResource(R.string.archive_treatment_dialog_title),
                dialogText = stringResource(R.string.archive_treatment_dialog_text)
            )
        }

        if (state.showDeleteDialog) {
            ConfirmationDialog(
                onDismissRequest = onDismissDeleteDialog,
                onConfirmation = onConfirmDelete,
                dialogTitle = stringResource(R.string.delete_treatment_dialog_title),
                dialogText = stringResource(R.string.delete_treatment_dialog_text)
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
            .padding(horizontal = 16.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun GeneralInfoSection(
    state: EditMedicationState,
    navController: NavController,
    onColorPickerClicked: () -> Unit,
    onDismissColorPicker: () -> Unit,
    onColorSelected: (MedicationColor) -> Unit,
    showColorSheet: Boolean
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.label_name),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = state.medication?.name?.split(" ")[0] ?: "Loading...",
                    style = MaterialTheme.typography.bodyLarge
                )
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
            onClick = onColorPickerClicked
        ) {
            val selectedColor = state.medication?.color?.let {
                try {
                    MedicationColor.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

            ColorSelector(
                selectedColor = selectedColor,
                onColorSelected = onColorSelected,
                showBottomSheet = showColorSheet,
                onDismiss = onDismissColorPicker
            )
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
            shape = RoundedCornerShape(12.dp, 12.dp, 6.dp, 6.dp),
            onClick = {}
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp, 6.dp, 12.dp, 12.dp),
            onClick = {}
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
            onDismissColorPicker = {},
            onColorSelected = {},
            showColorSheet = false
        )
    }
}