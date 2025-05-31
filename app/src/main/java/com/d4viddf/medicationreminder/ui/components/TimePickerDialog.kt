package com.d4viddf.medicationreminder.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Custom Composable to host TimePicker in an AlertDialog
@Composable
fun TimePickerDialog(
    title: String, // Now expects a string resource
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}
