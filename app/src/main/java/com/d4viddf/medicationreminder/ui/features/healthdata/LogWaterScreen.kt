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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogWaterScreen(
    onNavigateBack: () -> Unit,
    viewModel: HealthDataViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var waterCount by remember { mutableStateOf(0) }
    var bottleCount by remember { mutableStateOf(0) }
    var bigBottleCount by remember { mutableStateOf(0) }
    var customAmount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }

    val totalAmount = (waterCount * 250) +
            (bottleCount * 500) +
            (bigBottleCount * 750) +
            (customAmount.toDoubleOrNull() ?: 0.0)

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
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Water Intake") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DateInputButton(
                selectedDate = selectedDate,
                onClick = { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(32.dp)) // Increased space
            Text(
                text = "Choose at least one option or add a personalized one",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp)) // Increased space

            // Preset Options
            WaterPresetRow(
                title = "Water",
                subtitle = "250 ml",
                count = waterCount,
                onIncrement = { waterCount++ },
                onDecrement = { if (waterCount > 0) waterCount-- }
            )
            Spacer(modifier = Modifier.height(24.dp)) // Increased space
            WaterPresetRow(
                title = "Bottle",
                subtitle = "500 ml",
                count = bottleCount,
                onIncrement = { bottleCount++ },
                onDecrement = { if (bottleCount > 0) bottleCount-- }
            )
            Spacer(modifier = Modifier.height(24.dp)) // Increased space
            WaterPresetRow(
                title = "Big Bottle",
                subtitle = "750 ml",
                count = bigBottleCount,
                onIncrement = { bigBottleCount++ },
                onDecrement = { if (bigBottleCount > 0) bigBottleCount-- }
            )

            Spacer(modifier = Modifier.height(32.dp)) // Increased space

            // Custom Amount Input
            OutlinedTextField(
                value = customAmount,
                onValueChange = { customAmount = it.filter { char -> char.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Personalized Amount") },
                suffix = { Text("mL") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Type (e.g., Coffee, Tea)") },
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    val logTime = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    viewModel.logWater(totalAmount, logTime, if (type.isNotBlank()) type else null)
                    onNavigateBack()
                },
                enabled = isButtonEnabled,
                modifier = Modifier.fillMaxWidth().height(52.dp), // Made button bigger
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = "Save Total (${totalAmount.toInt()} mL)",
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
        "Today"
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
            contentDescription = "Select Date",
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
    onDecrement: () -> Unit
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
                    Icon(Icons.Default.Remove, contentDescription = "Decrement")
                }
                ToggleButton(
                    checked = false,
                    onCheckedChange = { onIncrement() },
                    modifier = Modifier.size(buttonSize),
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increment")
                }
            }
        }
    }
}