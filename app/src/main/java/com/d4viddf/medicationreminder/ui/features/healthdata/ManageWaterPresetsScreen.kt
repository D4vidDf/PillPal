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
                title = { Text("Manage Water Presets") },
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
                Icon(Icons.Default.Add, contentDescription = "Add Preset")
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
                title = { Text("Add New Preset") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            label = { Text("Preset Name") }
                        )
                        OutlinedTextField(
                            value = newPresetAmount,
                            onValueChange = { newPresetAmount = it.filter { char -> char.isDigit() } },
                            label = { Text("Amount (ml)") },
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
                        Text("Add")
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddPresetDialog = false }) {
                        Text("Cancel")
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
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}
