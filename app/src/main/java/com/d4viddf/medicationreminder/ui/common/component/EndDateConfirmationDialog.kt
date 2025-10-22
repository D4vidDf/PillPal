package com.d4viddf.medicationreminder.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R

@Composable
fun EndDateConfirmationDialog(
    onDismissRequest: () -> Unit,
    onSelectDate: () -> Unit,
    onNoEndDate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.end_date_label)) },
        text = { Text(text = stringResource(R.string.end_date_dialog_text)) },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.dialog_cancel_button))
                }
                TextButton(onClick = onNoEndDate) {
                    Text(stringResource(R.string.no_end_date))
                }
                TextButton(onClick = onSelectDate) {
                    Text(stringResource(R.string.select_new_date))
                }
            }
        }
    )
}
