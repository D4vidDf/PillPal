package com.d4viddf.medicationreminder.ui.features.medication.edit.duration

import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onClearDate: (() -> Unit)? = null,
    initialDate: LocalDate? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ?: Instant.now().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        onDateSelected(selectedDate)
                    }
                    onDismiss()
                }
            ) {
                Text("Select new date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            if (onClearDate != null) {
                TextButton(onClick = {
                    onClearDate()
                    onDismiss()
                }) {
                    Text("No end date")
                }
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            dateValidator = { timestamp ->
                val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                (minDate == null || date.isAfter(minDate) || date.isEqual(minDate)) &&
                        (maxDate == null || date.isBefore(maxDate) || date.isEqual(maxDate))
            }
        )
    }
}