package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPastMedicationDialog(
    medicationNameDisplay: String, // New parameter
    onDismissRequest: () -> Unit, // Renamed from onDismiss for clarity with AlertDialog
    onSave: (date: LocalDate, time: LocalTime) -> Unit // Updated onSave signature
) {
    var dialogSelectedDate by remember { mutableStateOf(LocalDate.now()) }
    var dialogSelectedTime by remember { mutableStateOf(LocalTime.now()) } // State for time
    val currentYear = dialogSelectedDate.year

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dialogSelectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        yearRange = IntRange(currentYear - 100, currentYear),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Allow selection of dates from the past up to and including today.
                val endOfTodayMillis = LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                return utcTimeMillis <= endOfTodayMillis
            }

            override fun isSelectableYear(year: Int): Boolean {
                // Allow selection of years within the defined yearRange.
                return year <= currentYear && year >= currentYear - 100
            }
        }
    )
    val timePickerState = rememberTimePickerState(
        initialHour = dialogSelectedTime.hour, // Initialize with dialogSelectedTime
        initialMinute = dialogSelectedTime.minute,
        is24Hour = true
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    // Removed: var medicationName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest, // Updated parameter name
        title = { Text("Log past dose for: $medicationNameDisplay") }, // Updated title
        text = {
            Column {
                // Removed OutlinedTextField for medicationName
                Spacer(modifier = Modifier.height(8.dp)) // Keep spacer or adjust as needed
                Button(onClick = { showDatePicker = true }) {
                    Text(text = dialogSelectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showTimePicker = true }) {
                    // Display the dialogSelectedTime, not the raw picker state initially
                    Text(text = dialogSelectedTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                }
            }
        },
        confirmButton = {
            TextButton( // Changed from Button to TextButton for consistency if desired, or keep Button
                onClick = {
                    onSave(dialogSelectedDate, dialogSelectedTime) // Use new onSave signature
                    onDismissRequest() // Dismiss after save
                }
            ) {
                Text(stringResource(id = R.string.button_save_dose))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { // Updated parameter name
                Text(stringResource(id = R.string.dialog_cancel_button))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Correctly interpret the millis from DatePicker as UTC
                            dialogSelectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.dialog_ok_button)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) { Text(stringResource(R.string.dialog_cancel_button)) }
            }
        ) { // Content lambda for DatePickerDialog
            DatePicker(state = datePickerState)
        }
    }

    // It's good practice to also update a local state for time if it needs to be displayed or used elsewhere before saving.
    // For simplicity, this example directly uses timePickerState.hour and timePickerState.minute on save.
    // If display of selected time before saving was needed, similar state management as dialogSelectedDate would be used.

    if (showTimePicker) {
        TimePickerDialog(
            title = stringResource(id = R.string.dialog_select_time_title),
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogSelectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) { Text(stringResource(id = R.string.dialog_ok_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(text = stringResource(id = R.string.dialog_cancel_button)) }
            }
        ) {
            TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun AddPastMedicationDialogPreview() {
    // Using dynamicColor = false for previews as dynamic colors are device/system specific.
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        AddPastMedicationDialog(
            medicationNameDisplay = "Amoxicillin",
            onDismissRequest = {},
            onSave = { _, _ -> }
        )
    }
}
