package com.d4viddf.medicationreminder.ui.screens

import android.app.Activity
import android.app.AlarmManager // Added for AlarmManager
import android.content.Context // Added for Context
import android.content.pm.PackageManager
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
// import androidx.compose.material3.ButtonDefaults // Not strictly needed for this change, but good for reference
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton // Ensure this is here if used (it is in full file)
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
        // Corrected this line to use new specific strings for the pager's first page:
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc),
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, PermissionType.EXACT_ALARM),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc)
    )
    val pagerState = rememberPagerState { onboardingSteps.size }

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPhoneLayout(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    navController: NavHostController,
    activity: ComponentActivity
) {
    val welcomeTitleText = stringResource(R.string.onboarding_welcome_title)
    // val logoContentDesc = stringResource(R.string.onboarding_logo_content_description) // Already available in Box
    val coroutineScope = rememberCoroutineScope()
    val logoSize = 100.dp // Adjusted size for image itself
    val logoBackgroundSize = 120.dp // Slightly larger for the background shape

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box( // Wrapper for logo and its background shape
            modifier = Modifier
                .size(logoBackgroundSize)
                .padding(bottom = 16.dp) // This padding is for spacing below the logo element
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.onboarding_logo_content_description),
                modifier = Modifier.size(logoSize) // Image size, smaller than background
            )
        }

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
                // Spacer to push Next button to the end if Previous is not visible (handled by Arrangement.End)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingTabletLayout(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    navController: NavHostController,
    activity: ComponentActivity
) {
    // val welcomeTitleText = stringResource(R.string.onboarding_welcome_title) // Already available in Text
    val welcomePaneDesc = stringResource(R.string.onboarding_pane_welcome_area_description)
    // val logoContentDesc = stringResource(R.string.onboarding_logo_content_description) // Already available in Box
    val coroutineScope = rememberCoroutineScope()
    val logoSize = 120.dp // Adjusted size for image itself on tablet
    val logoBackgroundSize = 150.dp // Slightly larger for the background shape on tablet

    Row(modifier = Modifier.fillMaxSize()) {
        Column( // Left Pane
            modifier = Modifier.weight(1f).fillMaxHeight().padding(32.dp)
                .semantics { contentDescription = welcomePaneDesc },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box( // Wrapper for logo and its background shape
                modifier = Modifier
                    .size(logoBackgroundSize)
                    .padding(bottom = 24.dp) // This padding is for spacing below the logo element
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.onboarding_logo_content_description),
                    modifier = Modifier.size(logoSize) // Image size, smaller than background
                )
            }
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
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
    val context = LocalContext.current // Get context for permission check

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
        // Note: FULL_SCREEN_INTENT check might need refinement for a real app.

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
@Preview(showBackground = true, name = "Phone Onboarding")
@Composable
fun OnboardingScreenPhonePreview() {
    val navController = rememberNavController()
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: ComponentActivity()

    // Update dummySteps for preview
    val dummySteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc, PermissionType.NOTIFICATION), // Use new string for first page
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc)
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
    val navController = rememberNavController()
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: ComponentActivity()

    // Update dummySteps for preview
    val dummySteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc, PermissionType.EXACT_ALARM), // Use new string for first page
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc)
    )
    val pagerState = rememberPagerState { dummySteps.size }
    MaterialTheme {
        OnboardingTabletLayout(pagerState, dummySteps, navController, activity)
    }
}
