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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPastMedicationDialog(
    onDismiss: () -> Unit,
    onSave: (dateMillis: Long?, timeHour: Int, timeMinute: Int) -> Unit // TODO: Add medication selection parameter
) {
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val timeState = rememberTimePickerState(initialHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY), initialMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE), is24Hour = true)
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
                    Text(text = dateState.selectedDateMillis?.let { java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(it)) } ?: stringResource(id = R.string.button_select_date))
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
                    onSave(dateState.selectedDateMillis, timeState.hour, timeState.minute)
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
                TextButton(onClick = { showDatePicker = false }) { Text(text = stringResource(id = R.string.dialog_ok_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(text = stringResource(id = R.string.dialog_cancel_button)) }
            },
            datePicker = {
                DatePicker(state = dateState, dateValidator = { it <= System.currentTimeMillis() })
            }
        )
    }

    if (showTimePicker) {
        TimePickerDialog( // This now refers to the component in the same package (if this file is also in ui.components)
                         // or needs an import if TimePickerDialog.kt is in a different sub-package of components.
                         // Assuming it's in com.d4viddf.medicationreminder.ui.components.TimePickerDialog
            title = stringResource(id = R.string.dialog_select_time_title),
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(text = stringResource(id = R.string.dialog_ok_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(text = stringResource(id = R.string.dialog_cancel_button)) }
            }
        ) {
            TimePicker(state = timeState, modifier = Modifier.padding(16.dp))
        }
    }
}
