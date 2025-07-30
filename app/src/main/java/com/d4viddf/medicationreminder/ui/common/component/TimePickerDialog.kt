package com.d4viddf.medicationreminder.ui.common.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme

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

@Preview(showBackground = true, name = "Time Picker Dialog Preview")
@Composable
fun TimePickerDialogPreview() {
    AppTheme {
        TimePickerDialog(
            title = "Select Time",
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {}) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {}) {
                    Text("Cancel")
                }
            },
            content = {
                Text("Time Picker Content Here. This dialog is a wrapper, actual TimePicker would be passed as content.")
            }
        )
    }
}
