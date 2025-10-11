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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.medication.add.components.MedicationTypeSelector
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun EditFormScreen(
    viewModel: EditFormViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowSizeClass = calculateWindowSizeClass()

    EditFormScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSave = viewModel::onSave,
        onTypeSelected = viewModel::onTypeSelected,
        widthSizeClass = windowSizeClass.widthSizeClass
    )
}

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

@Composable
fun EditFormScreenContent(
    uiState: EditFormState,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onTypeSelected: (Int) -> Unit,
    widthSizeClass: WindowWidthSizeClass
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                MedicationTypeSelector(
                    modifier = Modifier.weight(1f),
                    selectedTypeId = uiState.medicationTypeId,
                    onTypeSelected = onTypeSelected,
                    selectedColor = uiState.medicationColor,
                    widthSizeClass = widthSizeClass
                )
                Button(
                    onClick = {
                        onSave()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = stringResource(id = R.string.save))
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun EditFormScreenPreview() {
    AppTheme {
        EditFormScreenContent(
            uiState = EditFormState(
                isLoading = false,
                medicationTypeId = 1,
                medicationColor = MedicationColor.LIGHT_PINK
            ),
            onNavigateBack = {},
            onSave = {},
            onTypeSelected = {},
            widthSizeClass = WindowWidthSizeClass.Compact
        )
    }
}