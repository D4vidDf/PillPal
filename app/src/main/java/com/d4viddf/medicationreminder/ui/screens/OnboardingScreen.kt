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
import androidx.compose.ui.res.stringResource // Required for stringResource
import com.d4viddf.medicationreminder.R // Required for R class

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
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { contentDescription = stringResource(R.string.onboarding_welcome_title) }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = stringResource(R.string.onboarding_welcome_subtitle) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_steps_placeholder),
            modifier = Modifier.semantics { contentDescription = stringResource(R.string.onboarding_steps_placeholder) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_permissions_placeholder),
            modifier = Modifier.semantics { contentDescription = stringResource(R.string.onboarding_permissions_placeholder) }
        )
    }
}

@Composable
fun OnboardingTabletLayout() {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
                .semantics { contentDescription = stringResource(R.string.onboarding_pane_welcome_area_description) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = stringResource(R.string.onboarding_welcome_title) }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
                .semantics { contentDescription = stringResource(R.string.onboarding_pane_steps_permissions_area_description) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = stringResource(R.string.onboarding_welcome_subtitle) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_steps_placeholder),
                modifier = Modifier.semantics { contentDescription = stringResource(R.string.onboarding_steps_placeholder) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_permissions_placeholder),
                modifier = Modifier.semantics { contentDescription = stringResource(R.string.onboarding_permissions_placeholder) }
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
