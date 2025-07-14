package com.d4viddf.medicationreminder.wear.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn // Keep or change to TransformingLazyColumn if edge effects are desired
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState // Or rememberTransformingLazyListState
import com.d4viddf.medicationreminder.wear.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.Button // M3
import androidx.wear.compose.material3.ButtonDefaults // M3
import androidx.wear.compose.material3.MaterialTheme // M3
import androidx.wear.compose.material3.Text // M3
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme

@Composable
fun OnboardingScreen(onDismiss: () -> Unit, hasAlarmPermission: Boolean, onRequestPermission: () -> Unit) {
    // Using ScalingLazyColumn for onboarding as it's often a single screen of content.
    // TransformingLazyColumn could also be used if preferred.
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = rememberScalingLazyListState() // M3 compatible
    ) {
        item(key = "onboarding_title") {
            Text(
                text = stringResource(R.string.onboarding_message_title),
                style = MaterialTheme.typography.titleMedium, // M3 Typography
                color = MaterialTheme.colorScheme.onBackground, // M3 ColorScheme
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
            )
        }
        item(key = "onboarding_body") {
            Text(
                text = stringResource(R.string.onboarding_message_body),
                style = MaterialTheme.typography.bodyMedium, // M3 Typography
                color = MaterialTheme.colorScheme.onBackground, // M3 ColorScheme
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        if (!hasAlarmPermission) {
            item(key = "onboarding_permission_button") {
                Button( // M3 Button
                    onClick = {
                        Log.d("OnboardingScreen", "Enable Alarms button clicked.")
                        onRequestPermission()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors( // M3 ButtonDefaults
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
            Button( // M3 Button
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                 colors = ButtonDefaults.buttonColors( // M3 ButtonDefaults
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
    MedicationReminderTheme { // This is now M3 Theme
        OnboardingScreen({}, false, {})
    }
}
