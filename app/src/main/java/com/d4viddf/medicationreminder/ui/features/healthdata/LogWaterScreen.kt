package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

import androidx.navigation.NavController
import com.d4viddf.medicationreminder.ui.navigation.Screen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogWaterScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: HealthDataViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val waterPresets by viewModel.waterPresets.collectAsState()
    var waterCount by remember { mutableStateOf(0) }
    var bottleCount by remember { mutableStateOf(0) }
    var bigBottleCount by remember { mutableStateOf(0) }
    val presetCounts = remember { mutableStateMapOf<Int, Int>() }
    var customAmount by remember { mutableStateOf("") }
    var showAddPresetDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val totalAmount = (waterCount * 250) +
            (bottleCount * 500) +
            (bigBottleCount * 750) +
            waterPresets.sumOf { preset ->
                (presetCounts[preset.id] ?: 0) * preset.amount
            } + (customAmount.toDoubleOrNull() ?: 0.0)

    val isButtonEnabled = totalAmount > 0

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= Instant.now().toEpochMilli()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(id = R.string.dialog_ok_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.dialog_cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.log_water_intake_title)) },
                navigationIcon = {FilledTonalIconButton (onClick = navController::popBackStack, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back)
                    )

                }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.manage_presets)) },
                            onClick = {
                                navController.navigate(Screen.ManageWaterPresets.route)
                                showMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DateInputButton(
                selectedDate = selectedDate,
                onClick = { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(32.dp)) // Increased space
            Text(
                text = stringResource(id = R.string.log_water_screen_instruction),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp)) // Increased space

            // Preset Options
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn() {
                    item {
                        WaterPresetRow(
                            title = stringResource(id = R.string.water_preset_water),
                            subtitle = "250 ml",
                            count = waterCount,
                            onIncrement = { waterCount++ },
                            onDecrement = { if (waterCount > 0) waterCount-- }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    item {
                        WaterPresetRow(
                            title = stringResource(id = R.string.water_preset_bottle),
                            subtitle = "500 ml",
                            count = bottleCount,
                            onIncrement = { bottleCount++ },
                            onDecrement = { if (bottleCount > 0) bottleCount-- }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    item {
                        WaterPresetRow(
                            title = stringResource(id = R.string.water_preset_big_bottle),
                            subtitle = "750 ml",
                            count = bigBottleCount,
                            onIncrement = { bigBottleCount++ },
                            onDecrement = { if (bigBottleCount > 0) bigBottleCount-- }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    items(waterPresets) { preset ->
                        WaterPresetRow(
                            title = preset.name,
                            subtitle = "${preset.amount} ml",
                            count = presetCounts.getOrPut(preset.id) { 0 },
                            onIncrement = { presetCounts[preset.id] = (presetCounts[preset.id] ?: 0) + 1 },
                            onDecrement = {
                                if ((presetCounts[preset.id] ?: 0) > 0) {
                                    presetCounts[preset.id] = (presetCounts[preset.id] ?: 0) - 1
                                }
                            },
                            onDelete = { viewModel.deleteWaterPreset(preset.id) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showAddPresetDialog = true }) {
                    Text(stringResource(id = R.string.add_preset))
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

            // Custom Amount Input
            OutlinedTextField(
                value = customAmount,
                onValueChange = { customAmount = it.filter { char -> char.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.personalized_amount)) },
                suffix = { Text("mL") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val logTime = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    if (waterCount > 0) {
                        viewModel.logWater(waterCount * 250.0, logTime, "Water")
                    }
                    if (bottleCount > 0) {
                        viewModel.logWater(bottleCount * 500.0, logTime, "Bottle")
                    }
                    if (bigBottleCount > 0) {
                        viewModel.logWater(bigBottleCount * 750.0, logTime, "Big Bottle")
                    }
                    waterPresets.forEach { preset ->
                        val count = presetCounts[preset.id] ?: 0
                        if (count > 0) {
                            viewModel.logWater(count * preset.amount, logTime, preset.name)
                        }
                    }
                    customAmount.toDoubleOrNull()?.let {
                        if (it > 0) {
                            viewModel.logWater(it, logTime, "Custom Qty")
                        }
                    }
                    navController.previousBackStackEntry?.savedStateHandle?.set("water_logged", true)
                    onNavigateBack()
                },
                enabled = isButtonEnabled,
                modifier = Modifier.fillMaxWidth().height(52.dp), // Made button bigger
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = stringResource(id = R.string.save_total_ml, totalAmount.toInt()),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun DateInputButton(
    selectedDate: LocalDate,
    onClick: () -> Unit
) {
    val buttonText = if (selectedDate.isEqual(LocalDate.now())) {
        stringResource(id = R.string.today)
    } else {
        selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.extraSmall
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = stringResource(id = R.string.select_date),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = buttonText, style = MaterialTheme.typography.bodyLarge)
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WaterPresetRow(
    title: String,
    subtitle: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )

            ButtonGroup {
                // UPDATED: Buttons are now bigger
                val buttonSize = 48.dp
                ToggleButton(
                    checked = false,
                    onCheckedChange = { onDecrement() },
                    enabled = count > 0,
                    modifier = Modifier.size(buttonSize),
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                ) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(id = R.string.decrement))
                }
                ToggleButton(
                    checked = false,
                    onCheckedChange = { onIncrement() },
                    modifier = Modifier.size(buttonSize),
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.increment))
                }
            }
        }
    }
}