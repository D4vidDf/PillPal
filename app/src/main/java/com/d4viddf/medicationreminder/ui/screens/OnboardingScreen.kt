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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Medication Reminder!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Let's get you set up.",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "[Placeholder for Onboarding Steps]")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "[Placeholder for Permission Settings]")
    }
}

@Composable
fun OnboardingTabletLayout() {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Pane: Welcome Message
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Medication Reminder!",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        // Right Pane: Onboarding Steps and Permissions
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Let's get you set up.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "[Placeholder for Onboarding Steps]")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "[Placeholder for Permission Settings]")
        }
    }
}

@Preview(showBackground = true, name = "Phone Preview", device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingScreenPhonePreview() {
    OnboardingScreen() // This will now correctly preview the phone layout
}

@Preview(showBackground = true, name = "Tablet Preview", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun OnboardingScreenTabletPreview() {
    OnboardingScreen() // This will now correctly preview the tablet layout
}
