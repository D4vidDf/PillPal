package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
import com.d4viddf.medicationreminder.R // Assuming R class is in this package
import androidx.compose.material3.SelectableDates
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPastMedicationDialog(
    onDismiss: () -> Unit,
    onSave: (dateMillis: Long?, timeHour: Int, timeMinute: Int) -> Unit // TODO: Add medication selection parameter
) {
    var dialogSelectedDate by remember { mutableStateOf(LocalDate.now()) } // State for the date
    val currentYear = dialogSelectedDate.year

    val datePickerState = rememberDatePickerState( // Renamed from dateState to datePickerState for clarity
        initialSelectedDateMillis = dialogSelectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        yearRange = IntRange(currentYear - 100, currentYear),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Allow selection of dates from the past up to and including today.
                val endOfTodayMillis = today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                return utcTimeMillis <= endOfTodayMillis
            }

            override fun isSelectableYear(year: Int): Boolean {
                // Allow selection of years within the defined yearRange.
                return year <= currentYear && year >= currentYear - 100
            }
        }
    )
    val timePickerState = rememberTimePickerState( // Renamed from timeState for clarity
        initialHour = LocalTime.now().hour,
        initialMinute = LocalTime.now().minute,
        is24Hour = true
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var medicationName by remember { mutableStateOf("") } // Placeholder for medication selection

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.dialog_add_past_dose_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = medicationName,
                    onValueChange = { medicationName = it },
                    label = { Text(stringResource(id = R.string.label_medication_name_in_dialog)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showDatePicker = true }) {
                    Text(text = dialogSelectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showTimePicker = true }) {
                    Text(text = "${timeState.hour}:${String.format("%02d",timeState.minute)}")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Convert dialogSelectedDate back to millis for onSave, or change onSave signature
                    val selectedMillis = dialogSelectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onSave(selectedMillis, timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text(stringResource(id = R.string.button_save_dose))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
                            dialogSelectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
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
                        // timePickerState directly holds the hour and minute,
                        // no separate state update needed here if only used for onSave.
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
