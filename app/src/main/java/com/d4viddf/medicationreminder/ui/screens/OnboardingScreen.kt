package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R

@Composable
fun OnboardingScreen() {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    if (screenWidthDp >= 600) {
        OnboardingTabletLayout()
    } else {
        OnboardingPhoneLayout()
    }
}

@Composable
fun OnboardingPhoneLayout() {
    // Hoist stringResource calls for content descriptions
    val welcomeTitleText = stringResource(R.string.onboarding_welcome_title)
    val welcomeSubtitleText = stringResource(R.string.onboarding_welcome_subtitle)
    val stepsPlaceholderText = stringResource(R.string.onboarding_steps_placeholder)
    val permissionsPlaceholderText = stringResource(R.string.onboarding_permissions_placeholder)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = welcomeTitleText,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { contentDescription = welcomeTitleText }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = welcomeSubtitleText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = welcomeSubtitleText }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stepsPlaceholderText,
            modifier = Modifier.semantics { contentDescription = stepsPlaceholderText }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = permissionsPlaceholderText,
            modifier = Modifier.semantics { contentDescription = permissionsPlaceholderText }
        )
    }
}

@Composable
fun OnboardingTabletLayout() {
    // Hoist stringResource calls
    val welcomeTitleText = stringResource(R.string.onboarding_welcome_title)
    val welcomeSubtitleText = stringResource(R.string.onboarding_welcome_subtitle)
    val stepsPlaceholderText = stringResource(R.string.onboarding_steps_placeholder)
    val permissionsPlaceholderText = stringResource(R.string.onboarding_permissions_placeholder)
    val welcomePaneDesc = stringResource(R.string.onboarding_pane_welcome_area_description)
    val stepsPaneDesc = stringResource(R.string.onboarding_pane_steps_permissions_area_description)

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
                .semantics { contentDescription = welcomePaneDesc },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = welcomeTitleText,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = welcomeTitleText }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
                .semantics { contentDescription = stepsPaneDesc },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = welcomeSubtitleText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = welcomeSubtitleText }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stepsPlaceholderText,
                modifier = Modifier.semantics { contentDescription = stepsPlaceholderText }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = permissionsPlaceholderText,
                modifier = Modifier.semantics { contentDescription = permissionsPlaceholderText }
            )
        }
    }
}

@Preview(showBackground = true, name = "Phone Preview", device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingScreenPhonePreview() {
    OnboardingScreen()
}

@Preview(showBackground = true, name = "Tablet Preview", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun OnboardingScreenTabletPreview() {
    OnboardingScreen()
}
