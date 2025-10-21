package com.d4viddf.medicationreminder.ui.features.medication.edit.duration

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MedicationDurationScreen(
    viewModel: MedicationDurationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    MedicationDurationScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onStartDateClick = viewModel::onShowStartDatePicker,
        onEndDateClick = viewModel::onShowEndDatePicker
    )

    if (uiState.showStartDatePicker) {
        DatePickerModal(
            onDismiss = viewModel::onDismissStartDatePicker,
            onDateSelected = viewModel::onStartDateSelected,
            initialDate = uiState.startDate,
            maxDate = uiState.endDate
        )
    }

    if (uiState.showEndDatePicker) {
        DatePickerModal(
            onDismiss = viewModel::onDismissEndDatePicker,
            onDateSelected = viewModel::onEndDateSelected,
            onClearDate = { viewModel.onEndDateSelected(null) },
            initialDate = uiState.endDate,
            minDate = uiState.startDate
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDurationScreenContent(
    uiState: MedicationDurationState,
    onNavigateBack: () -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.duration_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp,12.dp,6.dp,6.dp)
            ) {
                DurationRow(
                    title = stringResource(R.string.start_date_label),
                    value = uiState.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
                    onClick = onStartDateClick
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp,6.dp,12.dp,12.dp)
            ) {
                DurationRow(
                    title = stringResource(R.string.end_date_label),
                    value = uiState.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: stringResource(R.string.ongoing),
                    onClick = onEndDateClick
                )
            }
        }
    }
}

@Composable
private fun DurationRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MedicationDurationScreenPreview() {
    AppTheme {
        MedicationDurationScreenContent(
            uiState = MedicationDurationState(startDate = LocalDate.now(), endDate = null),
            onNavigateBack = {},
            onStartDateClick = {},
            onEndDateClick = {}
        )
    }
}