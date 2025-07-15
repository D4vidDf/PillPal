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
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button // M3
import androidx.wear.compose.material3.ButtonDefaults // M3
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme // M3
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text // M3
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun OnboardingScreen(onDismiss: () -> Unit, hasAlarmPermission: Boolean, onRequestPermission: () -> Unit) {
    // Using ScalingLazyColumn for onboarding as it's often a single screen of content.
    // TransformingLazyColumn could also be used if preferred.

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        contentPadding = rememberResponsiveColumnPadding(
            first = ColumnItemType.ListHeader,
            last = ColumnItemType.IconButton
        ),
        edgeButton = {
            EdgeButton(
                onClick = onDismiss,
                buttonSize = EdgeButtonSize.Medium,
                colors = ButtonDefaults.buttonColors( // M3 ButtonDefaults
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(stringResource(R.string.got_it), textAlign = TextAlign.Center)
            }
        },
        timeText = { TimeText() }
    ) {
            contentPadding ->
        TransformingLazyColumn (
            state = listState,
            contentPadding = contentPadding,
        ){
            item(key = "onboarding_title") {
                ListHeader(
                    modifier =
                        Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )  {
                    Text(
                        text = stringResource(R.string.onboarding_message_title)
                    )
                }
            }
            item(key = "onboarding_body") {
                Text(
                    text = stringResource(R.string.onboarding_message_body),
                    style = MaterialTheme.typography.bodyMedium, // M3 Typography
                    color = MaterialTheme.colorScheme.onBackground, // M3 ColorScheme
                    textAlign = TextAlign.Center
                )
            }
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
