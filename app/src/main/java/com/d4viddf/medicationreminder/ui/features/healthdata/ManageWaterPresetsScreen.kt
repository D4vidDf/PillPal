package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.healthdata.WaterPreset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ManageWaterPresetsScreen(
    navController: NavController,
    viewModel: HealthDataViewModel = hiltViewModel()
) {
    val waterPresets by viewModel.waterPresets.collectAsState()
    var showAddPresetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.manage_water_presets_title)) },
                navigationIcon = {FilledTonalIconButton (onClick = navController::popBackStack, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back)
                    )

                }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPresetDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_preset))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            items(waterPresets) { preset ->
                WaterPresetListItem(
                    preset = preset,
                    onDelete = { viewModel.deleteWaterPreset(preset.id) }
                )
                Divider()
            }
        }

        if (showAddPresetDialog) {
            var newPresetName by remember { mutableStateOf("") }
            var newPresetAmount by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddPresetDialog = false },
                title = { Text(stringResource(id = R.string.add_new_preset_title)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            label = { Text(stringResource(id = R.string.preset_name)) }
                        )
                        OutlinedTextField(
                            value = newPresetAmount,
                            onValueChange = { newPresetAmount = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(id = R.string.amount_ml)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = newPresetAmount.toDoubleOrNull()
                            if (newPresetName.isNotBlank() && amount != null) {
                                viewModel.addWaterPreset(newPresetName, amount)
                                showAddPresetDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(id = R.string.add_button))
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddPresetDialog = false }) {
                        Text(stringResource(id = R.string.dialog_cancel_button))
                    }
                }
            )
        }
    }
}

@Composable
private fun WaterPresetListItem(
    preset: WaterPreset,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = preset.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "${preset.amount} ml", style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete))
        }
    }
}
