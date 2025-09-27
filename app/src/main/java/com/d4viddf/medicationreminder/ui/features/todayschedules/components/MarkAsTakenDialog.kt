package com.d4viddf.medicationreminder.ui.features.todayschedules.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarkAsTakenDialog(
    onDismiss: () -> Unit,
    onConfirmAsTakenNow: () -> Unit,
    onConfirmAsTakenScheduled: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.mark_as_taken_dialog_title)) },
        text = { Text(text = stringResource(R.string.mark_as_taken_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirmAsTakenNow,
                shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.mark_as_taken_dialog_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onConfirmAsTakenScheduled,
                shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.mark_as_taken_dialog_scheduled_time))
            }
        }
    )
}