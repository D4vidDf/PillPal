// Updated OnboardingScreen.kt
package com.d4viddf.medicationreminder.ui.screens

import android.app.Activity // Keep for potential future use with context if needed directly
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.utils.PermissionUtils // Assuming PermissionUtils is correctly located
import kotlinx.coroutines.launch

enum class PermissionType { NOTIFICATION, EXACT_ALARM, FULL_SCREEN_INTENT }

data class OnboardingStepContent(
    val titleResId: Int,
    val descriptionResId: Int,
    val permissionType: PermissionType? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavHostController) { // Added navController
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val onboardingSteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_title, R.string.onboarding_step1_desc),
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, PermissionType.EXACT_ALARM),
        // Optional: Add a step for FULL_SCREEN_INTENT if deemed necessary for onboarding
        // OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, PermissionType.FULL_SCREEN_INTENT),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc)
    )
    val pagerState = rememberPagerState { onboardingSteps.size }

    // It's generally safer to obtain the activity context where it's directly needed or pass it down
    // For preview purposes and direct use, this is okay for now.
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: run {
        // Fallback or error for preview if context is not a ComponentActivity
        // This should ideally not happen in a real app scenario if the entry point is an Activity
        // For preview, we might need a more robust way or accept it might not fully work for permission dialogues
        if (currentContext is Activity) currentContext as ComponentActivity else ComponentActivity() // Dummy for preview if all else fails
    }


    if (screenWidthDp >= 600) {
        OnboardingTabletLayout(pagerState = pagerState, steps = onboardingSteps, navController = navController, activity = activity)
    } else {
        OnboardingPhoneLayout(pagerState = pagerState, steps = onboardingSteps, navController = navController, activity = activity)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPhoneLayout(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    navController: NavHostController, // Added
    activity: ComponentActivity // Added
) {
    val welcomeTitleText = stringResource(R.string.onboarding_welcome_title)
    val logoContentDesc = stringResource(R.string.onboarding_logo_content_description)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = logoContentDesc,
            modifier = Modifier.size(120.dp).padding(bottom = 16.dp)
        )
        Text(
            text = welcomeTitleText,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = welcomeTitleText }
        )
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { pageIndex ->
            val step = steps[pageIndex]
            OnboardingStepPage(step = step, activity = activity)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                if (pagerState.currentPage < steps.size - 1) {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } else {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            }
        }) {
            Text(text = stringResource(if (pagerState.currentPage < steps.size - 1) R.string.next_button_text else R.string.done_button_text))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingTabletLayout(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    navController: NavHostController, // Added
    activity: ComponentActivity // Added
) {
    val welcomeTitleText = stringResource(R.string.onboarding_welcome_title)
    val welcomePaneDesc = stringResource(R.string.onboarding_pane_welcome_area_description)
    val logoContentDesc = stringResource(R.string.onboarding_logo_content_description)
    val coroutineScope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(32.dp)
                .semantics { contentDescription = welcomePaneDesc },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = logoContentDesc,
                modifier = Modifier.size(150.dp).padding(bottom = 24.dp)
            )
            Text(
                text = welcomeTitleText,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { contentDescription = welcomeTitleText }
            )
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { pageIndex ->
                val step = steps[pageIndex]
                OnboardingStepPage(step = step, activity = activity)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                coroutineScope.launch {
                    if (pagerState.currentPage < steps.size - 1) {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                }
            }) {
                Text(text = stringResource(if (pagerState.currentPage < steps.size - 1) R.string.next_button_text else R.string.done_button_text))
            }
        }
    }
}

@Composable
fun OnboardingStepPage(step: OnboardingStepContent, activity: ComponentActivity) {
    val title = stringResource(id = step.titleResId)
    val description = stringResource(id = step.descriptionResId)
    val grantPermissionButtonText = stringResource(R.string.onboarding_grant_permission_button)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = title }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = description }
        )
        if (step.permissionType != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                when (step.permissionType) {
                    PermissionType.NOTIFICATION -> PermissionUtils.requestPostNotificationPermission(activity)
                    PermissionType.EXACT_ALARM -> PermissionUtils.checkAndRequestExactAlarmPermission(activity)
                    PermissionType.FULL_SCREEN_INTENT -> PermissionUtils.checkAndRequestFullScreenIntentPermission(activity)
                }
            }) {
                Text(text = grantPermissionButtonText)
            }
        }
    }
}

// Previews - Need to pass NavController. Can use rememberNavController for preview.
@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Phone Onboarding")
@Composable
fun OnboardingScreenPhonePreview() {
    val navController = rememberNavController() // For preview
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: ComponentActivity() // Simplified for preview

    val dummySteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_title, R.string.onboarding_step1_desc, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc)
    )
    val pagerState = rememberPagerState { dummySteps.size }
    MaterialTheme {
        OnboardingPhoneLayout(pagerState, dummySteps, navController, activity)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Tablet Onboarding", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun OnboardingScreenTabletPreview() {
    val navController = rememberNavController() // For preview
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: ComponentActivity() // Simplified for preview

    val dummySteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_title, R.string.onboarding_step1_desc, PermissionType.EXACT_ALARM),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc)
    )
    val pagerState = rememberPagerState { dummySteps.size }
    MaterialTheme {
        OnboardingTabletLayout(pagerState, dummySteps, navController, activity)
    }
}
