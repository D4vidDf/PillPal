package com.d4viddf.medicationreminder.ui.features.onboarding.screen

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.utils.PermissionUtils
import kotlinx.coroutines.launch

enum class PermissionType { NOTIFICATION, EXACT_ALARM, FULL_SCREEN_INTENT }

data class OnboardingStepContent(
    val titleResId: Int,
    val descriptionResId: Int,
    val permissionType: PermissionType? = null
)

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavHostController,
    userPreferencesRepository: UserPreferencesRepository // Add this
) {
    val screenWidthDp = LocalWindowInfo.current.containerSize.width

    val onboardingSteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc),
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, PermissionType.EXACT_ALARM),
        OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, PermissionType.FULL_SCREEN_INTENT),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc)
    )
    val pagerState = rememberPagerState { onboardingSteps.size }
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: run {
        if (currentContext is Activity) currentContext as ComponentActivity else ComponentActivity()
    }


    if (screenWidthDp >= 600) {
        OnboardingTabletLayout(
            pagerState = pagerState,
            steps = onboardingSteps,
            navController = navController,
            activity = activity,
            userPreferencesRepository = userPreferencesRepository
        )
    } else {
        OnboardingPhoneLayout(
            pagerState = pagerState,
            steps = onboardingSteps,
            navController = navController,
            activity = activity,
            userPreferencesRepository = userPreferencesRepository
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomePageContent(onStartClick: () -> Unit) {
    val logoSize = 120.dp
    val logoBackgroundSize = 150.dp
    val welcomeTitleString = stringResource(R.string.onboarding_welcome_title)
    val appSubtitleString = stringResource(R.string.onboarding_welcome_app_subtitle)

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
                .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialShapes.Sunny.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = stringResource(R.string.onboarding_logo_content_description),
                modifier = Modifier.size(logoSize)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = welcomeTitleString,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = welcomeTitleString }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = appSubtitleString,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
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

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPhoneLayout(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    navController: NavHostController,
    activity: ComponentActivity,
    userPreferencesRepository: UserPreferencesRepository // Add this
) {
    var showWelcomePage by rememberSaveable { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isNotificationPermissionCurrentlyGranted by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, showWelcomePage) {
        if (!showWelcomePage) {
            val currentStepIndex = pagerState.currentPage
            if (currentStepIndex < steps.size) {
                val currentStep = steps[currentStepIndex]
                if (currentStep.permissionType == PermissionType.NOTIFICATION) {
                    isNotificationPermissionCurrentlyGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else { true }
                } else { isNotificationPermissionCurrentlyGranted = true } // Enable for non-notification steps
            } else { isNotificationPermissionCurrentlyGranted = true } // Enable if steps list is somehow exceeded
        } else { isNotificationPermissionCurrentlyGranted = true } // Enable if on welcome page (Next button not shown anyway)
    }

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
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                OnboardingStepPage(
                    step = steps[pageIndex],
                    activity = activity,
                    onNotificationPermissionResult = if (steps[pageIndex].permissionType == PermissionType.NOTIFICATION) {
                        { granted -> isNotificationPermissionCurrentlyGranted = granted }
                    } else {
                        null
                    }
                )
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
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.height(50.dp)
                    ) { Text(text = stringResource(R.string.previous_button_text)) }
                }
                val currentStepDetails = if (pagerState.currentPage < steps.size) steps[pagerState.currentPage] else null
                val isNotificationStep = currentStepDetails?.permissionType == PermissionType.NOTIFICATION
                val nextButtonEnabled = if (isNotificationStep) isNotificationPermissionCurrentlyGranted else true

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < steps.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                userPreferencesRepository.updateOnboardingCompleted(true) // Add this line
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.height(50.dp),
                    enabled = nextButtonEnabled
                ) { Text(text = stringResource(if (pagerState.currentPage < steps.size - 1) R.string.next_button_text else R.string.done_button_text)) }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingTabletLayout(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    navController: NavHostController,
    activity: ComponentActivity,
    userPreferencesRepository: UserPreferencesRepository // Add this
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isNotificationPermissionCurrentlyGranted by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        val currentStepIndex = pagerState.currentPage
        if (currentStepIndex < steps.size) {
            val currentStep = steps[currentStepIndex]
            if (currentStep.permissionType == PermissionType.NOTIFICATION) {
                isNotificationPermissionCurrentlyGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else { true }
            } else { isNotificationPermissionCurrentlyGranted = true } // Enable for non-notification steps
        } else { isNotificationPermissionCurrentlyGranted = true } // Enable if steps list is somehow exceeded
    }

    val welcomePaneDesc = stringResource(R.string.onboarding_pane_welcome_area_description)
    val welcomeTitle= stringResource(R.string.onboarding_welcome_title)
    val logoSize = 120.dp
    val logoBackgroundSize = 150.dp

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
                .semantics { contentDescription = welcomePaneDesc },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(logoBackgroundSize)
                    .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialShapes.Pentagon.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.onboarding_logo_content_description),
                    modifier = Modifier.size(logoSize)
                )
            }
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { contentDescription = welcomeTitle }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                OnboardingStepPage(step = steps[pageIndex], activity = activity)
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
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.height(50.dp)
                    ) { Text(text = stringResource(R.string.previous_button_text)) }
                }
                val currentStepDetails = if (pagerState.currentPage < steps.size) steps[pagerState.currentPage] else null
                val isNotificationStep = currentStepDetails?.permissionType == PermissionType.NOTIFICATION
                val nextButtonEnabled = if (isNotificationStep) isNotificationPermissionCurrentlyGranted else true

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < steps.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                userPreferencesRepository.updateOnboardingCompleted(true) // Add this line
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.height(50.dp),
                    enabled = nextButtonEnabled
                ) { Text(text = stringResource(if (pagerState.currentPage < steps.size - 1) R.string.next_button_text else R.string.done_button_text)) }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun OnboardingStepPage(
    step: OnboardingStepContent,
    activity: ComponentActivity,
    onNotificationPermissionResult: ((Boolean) -> Unit)? = null // Add this
) {
    val title = stringResource(id = step.titleResId)
    val description = stringResource(id = step.descriptionResId)
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(false) }
    var buttonTextResId by remember { mutableStateOf(R.string.onboarding_grant_permission_button) }

    // var localPermissionCheckTrigger by remember { mutableStateOf(0) } // Removed

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (step.permissionType == PermissionType.NOTIFICATION) {
                isPermissionGranted = isGranted
                buttonTextResId = if (isGranted) R.string.onboarding_permission_granted_button else R.string.onboarding_grant_permission_button
                onNotificationPermissionResult?.invoke(isGranted) // Add this line
            }
        }
    )

    LaunchedEffect(step.permissionType) { // Removed localPermissionCheckTrigger
        if (step.permissionType != null) {
            val permissionString = when (step.permissionType) {
                PermissionType.NOTIFICATION -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
                PermissionType.EXACT_ALARM -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) Manifest.permission.SCHEDULE_EXACT_ALARM else null
                PermissionType.FULL_SCREEN_INTENT -> Manifest.permission.USE_FULL_SCREEN_INTENT
            }
            if (permissionString != null) {
                isPermissionGranted = if (step.permissionType == PermissionType.EXACT_ALARM && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.canScheduleExactAlarms()
                } else { ContextCompat.checkSelfPermission(context, permissionString) == PackageManager.PERMISSION_GRANTED }
            } else if (step.permissionType == PermissionType.NOTIFICATION && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                isPermissionGranted = true
            } else if (step.permissionType == PermissionType.EXACT_ALARM && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                isPermissionGranted = true
            }
            buttonTextResId = if (isPermissionGranted) R.string.onboarding_permission_granted_button else R.string.onboarding_grant_permission_button
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                            PermissionType.NOTIFICATION -> {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                // For older SDKs, isPermissionGranted should be true via LaunchedEffect, so this path isn't taken.
                            }
                            PermissionType.EXACT_ALARM -> PermissionUtils.checkAndRequestExactAlarmPermission(activity)
                            PermissionType.FULL_SCREEN_INTENT -> PermissionUtils.checkAndRequestFullScreenIntentPermission(activity) // This typically doesn't have a system dialog.
                        }
                        // REMOVE: localPermissionCheckTrigger++
                    }
                },
                shape = MaterialTheme.shapes.medium,
                enabled = !isPermissionGranted
            ) { Text(text = stringResource(buttonTextResId)) }
        }
    }
}

@Preview(showBackground = true, name = "Phone Onboarding Welcome")
@Composable
fun OnboardingScreenPhoneWelcomePreview() {
    MaterialTheme { WelcomePageContent(onStartClick = {}) }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Phone Onboarding Pager")
@Composable
fun OnboardingScreenPhonePagerPreview() {
    val navController = rememberNavController()
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: ComponentActivity()
    // Dummy UserPreferencesRepository for preview
    val dummyUserPreferencesRepository = UserPreferencesRepository(currentContext)
    val dummySteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc),
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, PermissionType.EXACT_ALARM),
        OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, PermissionType.FULL_SCREEN_INTENT),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc)
    )
    val pagerState = rememberPagerState { dummySteps.size }
    MaterialTheme {
        // Previewing OnboardingPhoneLayout directly as OnboardingScreen now requires the repository
        // For the OnboardingStepPage call inside this preview's OnboardingPhoneLayout,
        // we need to simulate its structure or simplify.
        // The actual OnboardingPhoneLayout now passes a conditional lambda.
        // For simplicity in preview, we'll assume the HorizontalPager inside OnboardingPhoneLayout
        // will call OnboardingStepPage with onNotificationPermissionResult = null.
        // This means we don't need to change OnboardingStepPage's call *within the preview's Pager* explicitly here,
        // as the preview directly calls OnboardingPhoneLayout.
        // The critical part is that OnboardingPhoneLayout itself correctly passes the lambda.
        // If we were previewing OnboardingStepPage directly, we'd pass null.
        OnboardingPhoneLayout(
            pagerState = pagerState,
            steps = dummySteps,
            navController = navController,
            activity = activity,
            userPreferencesRepository = dummyUserPreferencesRepository // This is for OnboardingPhoneLayout
            // The HorizontalPager inside OnboardingPhoneLayout will call OnboardingStepPage.
            // We rely on the actual implementation of OnboardingPhoneLayout to correctly pass
            // the lambda or null to OnboardingStepPage.
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Tablet Onboarding", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun OnboardingScreenTabletPreview() {
    val navController = rememberNavController()
    val currentContext = LocalContext.current
    val activity = currentContext as? ComponentActivity ?: ComponentActivity()
    val dummyUserPreferencesRepository = UserPreferencesRepository(currentContext)
    val dummySteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc),
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, PermissionType.EXACT_ALARM),
        OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, PermissionType.FULL_SCREEN_INTENT),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc)
    )
    val pagerState = rememberPagerState { dummySteps.size }
    MaterialTheme {
        OnboardingTabletLayout(
            pagerState = pagerState, // Corrected: pagerState was passed first
            steps = dummySteps,
            navController = navController,
            activity = activity,
            userPreferencesRepository = dummyUserPreferencesRepository // This is for OnboardingTabletLayout
        )
    }
}