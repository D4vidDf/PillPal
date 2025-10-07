package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.medication.add.components.ColorSelector
import com.d4viddf.medicationreminder.ui.features.medication.add.components.MedicationTypeSelector
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor

@Composable
fun EditFormAndColorScreen(
    viewModel: EditFormAndColorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    EditFormAndColorScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSave = viewModel::onSave,
        onTypeSelected = viewModel::onTypeSelected,
        onColorSelected = viewModel::onColorSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFormAndColorScreenContent(
    uiState: EditFormAndColorState,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onTypeSelected: (Int) -> Unit,
    onColorSelected: (MedicationColor) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_form_color_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    Button(onClick = {
                        onSave()
                        onNavigateBack()
                    }) {
                        Text(text = stringResource(id = R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                // The original code had a check for medicationTypeId and medicationColor being non-null.
                // This check is now handled by the data class, which makes the code cleaner.
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MedicationTypeSelector(
                        selectedTypeId = uiState.medicationTypeId,
                        onTypeSelected = onTypeSelected,
                        selectedColor = uiState.medicationColor
                    )
                    ColorSelector(
                        selectedColor = uiState.medicationColor,
                        onColorSelected = onColorSelected
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun EditFormAndColorScreenPreview() {
    AppTheme {
        EditFormAndColorScreenContent(
            uiState = EditFormAndColorState(
                isLoading = false,
                medicationTypeId = 1,
                medicationColor = MedicationColor.LIGHT_PINK
            ),
            onNavigateBack = {},
            onSave = {},
            onTypeSelected = {},
            onColorSelected = {}
        )
    }
}