package com.d4viddf.medicationreminder.wear.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.d4viddf.medicationreminder.wear.R
import androidx.compose.ui.tooling.preview.Preview // Added
import androidx.wear.tooling.preview.devices.WearDevices // Added
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme // Added

@Composable
fun OnboardingScreen(onDismiss: () -> Unit, hasAlarmPermission: Boolean, onRequestPermission: () -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = rememberScalingLazyListState()
    ) {
        item(key = "onboarding_title") {
            Text(
                text = stringResource(R.string.onboarding_message_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
            )
        }
        item(key = "onboarding_body") {
            Text(
                text = stringResource(R.string.onboarding_message_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        if (!hasAlarmPermission) {
            item(key = "onboarding_permission_button") {
                Button(
                    onClick = {
                        Log.d("OnboardingScreen", "Enable Alarms button clicked.")
                        onRequestPermission()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.enable_alarms_permission))
                }
            }
            item(key = "onboarding_spacer_after_permission") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        item(key = "onboarding_dismiss_button") {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                 colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(stringResource(R.string.got_it))
            }
        }
        item(key = "onboarding_bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun OnboardingPreview() {
    MedicationReminderTheme {
        OnboardingScreen({}, false, {})
    }
}
