package com.d4viddf.medicationreminder.wear.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.d4viddf.medicationreminder.wear.R

@Composable
fun MoreScreen(
    onSyncClick: () -> Unit,
    onOpenAppClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Button(
                onClick = onSyncClick,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(text = stringResource(R.string.sync_with_phone))
            }
        }
        item {
            Button(
                onClick = onOpenAppClick,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(text = stringResource(R.string.open_app_on_phone))
            }
        }
        item {
            Button(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.settings))
            }
        }
    }
}
