package com.d4viddf.medicationreminder.ui.screens

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background // Added
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // Added
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
import androidx.compose.foundation.shape.CircleShape // Added
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // Added
import androidx.compose.runtime.setValue // Added
import androidx.compose.runtime.mutableStateOf // Added
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable // Added
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.utils.PermissionUtils
import kotlinx.coroutines.launch

enum class PermissionType { NOTIFICATION, EXACT_ALARM, FULL_SCREEN_INTENT }

data class OnboardingStepContent(
    val titleResId: Int,
    val descriptionResId: Int,
    val permissionType: PermissionType? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavHostController) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val onboardingSteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc), // First pager page
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, PermissionType.EXACT_ALARM),
        // Add the new step for Full-Screen Intent permission here:
        OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, PermissionType.FULL_SCREEN_INTENT),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc) // This becomes the 5th step
    )
    val pagerState = rememberPagerState { onboardingSteps.size } // Size will update automatically

    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: run {
        if (currentContext is Activity) currentContext as ComponentActivity else ComponentActivity()
    }

    if (screenWidthDp >= 600) {
        OnboardingTabletLayout(pagerState = pagerState, steps = onboardingSteps, navController = navController, activity = activity)
    } else {
        OnboardingPhoneLayout(pagerState = pagerState, steps = onboardingSteps, navController = navController, activity = activity)
    }
}

@Composable
fun WelcomePageContent(onStartClick: () -> Unit) {
    val logoSize = 120.dp
    val logoBackgroundSize = 150.dp

    // Hoist the string resource call
    val welcomeTitleString = stringResource(R.string.onboarding_welcome_title)
    val appSubtitleString = stringResource(R.string.onboarding_welcome_app_subtitle) // Assuming this might also need a CD later

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(logoBackgroundSize)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.onboarding_logo_content_description),
                modifier = Modifier.size(logoSize)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = welcomeTitleString, // Use hoisted variable
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            // Corrected semantics:
            modifier = Modifier.semantics { contentDescription = welcomeTitleString } // Use hoisted variable
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = appSubtitleString, // Use hoisted variable
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
            // If this Text also needs a content description, it should follow the same pattern:
            // val appSubtitleDesc = stringResource(R.string.onboarding_welcome_app_subtitle)
            // modifier = Modifier.padding(horizontal = 16.dp).semantics { contentDescription = appSubtitleDesc }
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onStartClick,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            Text(text = stringResource(R.string.start_onboarding_button_text))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPhoneLayout(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    navController: NavHostController,
    activity: ComponentActivity
) {
    var showWelcomePage by rememberSaveable { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    if (showWelcomePage) {
        WelcomePageContent(onStartClick = { showWelcomePage = false })
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Top padding before pager

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { pageIndex ->
                val step = steps[pageIndex]
                OnboardingStepPage(step = step, activity = activity)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = if (pagerState.currentPage > 0) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(text = stringResource(R.string.previous_button_text))
                    }
                } else {
                    // Spacer for alignment if not using Arrangement.End for the single button case
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < steps.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(text = stringResource(if (pagerState.currentPage < steps.size - 1) R.string.next_button_text else R.string.done_button_text))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingTabletLayout(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    navController: NavHostController,
    activity: ComponentActivity
) {
    // Ensure these are defined at the start of the composable
    val welcomeTitleText = stringResource(R.string.onboarding_welcome_title)
    val welcomePaneDesc = stringResource(R.string.onboarding_pane_welcome_area_description) // For the left pane's Column

    val coroutineScope = rememberCoroutineScope() // Already present
    val logoSize = 120.dp
    val logoBackgroundSize = 150.dp

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(32.dp)
                .semantics { contentDescription = welcomePaneDesc },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(logoBackgroundSize)
                    .padding(bottom = 24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.onboarding_logo_content_description),
                    modifier = Modifier.size(logoSize)
                )
            }
            Text(
                text = welcomeTitleText, // Use hoisted variable
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                // Corrected semantics for the Text:
                modifier = Modifier.semantics { contentDescription = welcomeTitleText } // Use hoisted variable
            )
        }
        Column( // Right Pane
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = if (pagerState.currentPage > 0) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(text = stringResource(R.string.previous_button_text))
                    }
                } else {
                    // Spacer for alignment
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < steps.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(text = stringResource(if (pagerState.currentPage < steps.size - 1) R.string.next_button_text else R.string.done_button_text))
                }
            }
        }
    }
}

@Composable
fun OnboardingStepPage(step: OnboardingStepContent, activity: ComponentActivity) {
    val title = stringResource(id = step.titleResId)
    val description = stringResource(id = step.descriptionResId)
    val context = LocalContext.current

    var isPermissionGranted = false
    var buttonTextResId = R.string.onboarding_grant_permission_button

    if (step.permissionType != null) {
        val permissionString = when (step.permissionType) {
            PermissionType.NOTIFICATION -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.POST_NOTIFICATIONS else null
            PermissionType.EXACT_ALARM -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) android.Manifest.permission.SCHEDULE_EXACT_ALARM else null
            PermissionType.FULL_SCREEN_INTENT -> android.Manifest.permission.USE_FULL_SCREEN_INTENT
        }

        if (permissionString != null) {
            if (step.permissionType == PermissionType.EXACT_ALARM && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                isPermissionGranted = alarmManager.canScheduleExactAlarms()
            } else {
                 isPermissionGranted = ContextCompat.checkSelfPermission(context, permissionString) == PackageManager.PERMISSION_GRANTED
            }
        } else if (step.permissionType == PermissionType.NOTIFICATION && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted = true
        } else if (step.permissionType == PermissionType.EXACT_ALARM && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            isPermissionGranted = true
        }

        if (isPermissionGranted) {
            buttonTextResId = R.string.onboarding_permission_granted_button
        }
    }

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
            Button(
                onClick = {
                    if (!isPermissionGranted) {
                        when (step.permissionType) {
                            PermissionType.NOTIFICATION -> PermissionUtils.requestPostNotificationPermission(activity)
                            PermissionType.EXACT_ALARM -> PermissionUtils.checkAndRequestExactAlarmPermission(activity)
                            PermissionType.FULL_SCREEN_INTENT -> PermissionUtils.checkAndRequestFullScreenIntentPermission(activity)
                        }
                    }
                },
                shape = MaterialTheme.shapes.medium,
                enabled = !isPermissionGranted
            ) {
                Text(text = stringResource(buttonTextResId))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Phone Onboarding Welcome") // Updated name
@Composable
fun OnboardingScreenPhoneWelcomePreview() { // Updated name for clarity
    MaterialTheme {
        WelcomePageContent(onStartClick = {})
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Phone Onboarding Pager") // New Preview
@Composable
fun OnboardingScreenPhonePagerPreview() {
    val navController = rememberNavController()
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: ComponentActivity()
    val dummySteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc),
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, PermissionType.EXACT_ALARM),
        OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, PermissionType.FULL_SCREEN_INTENT),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc)
    )
    val pagerState = rememberPagerState { dummySteps.size }
    MaterialTheme {
        // Simulate being past the welcome page for this preview
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { pageIndex ->
                OnboardingStepPage(step = dummySteps[pageIndex], activity = activity)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                 Button(onClick = {}, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.height(50.dp)) {
                    Text(text = stringResource(R.string.next_button_text))
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Tablet Onboarding", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun OnboardingScreenTabletPreview() { // This preview inherently shows the pager structure
    val navController = rememberNavController()
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: ComponentActivity()

    val dummySteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc),
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, PermissionType.EXACT_ALARM),
        OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, PermissionType.FULL_SCREEN_INTENT),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc)
    )
    val pagerState = rememberPagerState { dummySteps.size }
    MaterialTheme {
        OnboardingTabletLayout(pagerState, dummySteps, navController, activity)
    }
}
