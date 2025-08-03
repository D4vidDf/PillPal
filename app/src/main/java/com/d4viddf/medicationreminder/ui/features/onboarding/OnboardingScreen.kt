@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package com.d4viddf.medicationreminder.ui.features.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.component.ShapesAnimation
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.utils.PermissionUtils
import kotlinx.coroutines.launch
import kotlin.math.floor

// --- Data Classes and Enums ---
enum class PermissionType { NOTIFICATION, EXACT_ALARM, FULL_SCREEN_INTENT }

data class OnboardingStepContent(
    val titleResId: Int,
    val descriptionResId: Int,
    @DrawableRes val iconResId: Int,
    val permissionType: PermissionType? = null
)

// --- Helper Function ---
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavHostController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState { uiState.steps.size }
    val activity = LocalContext.current.findActivity() as? ComponentActivity

    // When the screen resumes (e.g., returning from settings), re-check permissions.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateNextButtonState(pagerState.currentPage)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // When the page changes, update the enabled state of the 'Next' button.
    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateNextButtonState(pagerState.currentPage)
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    val onPermissionResult: (PermissionType, Boolean) -> Unit = { type, isGranted ->
        viewModel.updatePermissionStatus(type, isGranted)
        viewModel.updateNextButtonState(pagerState.currentPage)
    }

    val onFinish = {
        viewModel.finishOnboarding()
        navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
    }

    if (isTablet) {
        OnboardingTabletLayout(
            pagerState = pagerState,
            uiState = uiState,
            activity = activity ?: ComponentActivity(),
            onPermissionResult = onPermissionResult,
            onFinish = onFinish
        )
    } else {
        OnboardingPhoneLayout(
            pagerState = pagerState,
            uiState = uiState,
            activity = activity ?: ComponentActivity(),
            onPermissionResult = onPermissionResult,
            onFinish = onFinish
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPhoneLayout(
    pagerState: PagerState,
    uiState: OnboardingUiState,
    activity: ComponentActivity,
    onPermissionResult: (PermissionType, Boolean) -> Unit,
    onFinish: () -> Unit
) {
    var showWelcomeScreen by rememberSaveable { mutableStateOf(true) }

    if (showWelcomeScreen) {
        WelcomePageContent(onStartClick = { showWelcomeScreen = false })
    } else {
        OnboardingPhoneContent(
            pagerState = pagerState,
            uiState = uiState,
            activity = activity,
            onPermissionResult = onPermissionResult,
            onFinish = onFinish
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingPhoneContent(
    pagerState: PagerState,
    uiState: OnboardingUiState,
    activity: ComponentActivity,
    onPermissionResult: (PermissionType, Boolean) -> Unit,
    onFinish: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            OnboardingNavigation(
                pagerState = pagerState,
                steps = uiState.steps,
                isNextEnabled = uiState.isNextEnabled,
                onNext = {
                    if (pagerState.currentPage < uiState.steps.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                onPrevious = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            MorphingIcon(pagerState = pagerState, steps = uiState.steps)
            Spacer(modifier = Modifier.height(48.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = uiState.isNextEnabled,
            ) { pageIndex ->
                val step = uiState.steps[pageIndex]
                key(step.titleResId) {
                    OnboardingStepPage(
                        step = step,
                        activity = activity,
                        onPermissionResult = { isGranted ->
                            step.permissionType?.let { onPermissionResult(it, isGranted) }
                        },
                        isPermissionGranted = uiState.permissionStatus[step.permissionType] ?: false
                    )
                }
            }
        }
    }
}


@Composable
fun WelcomePageContent(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        ShapesAnimation(
            modifier = Modifier.size(300.dp),
            shapeModifier = Modifier.size(300.dp),
            contentPadding = PaddingValues(12.dp),
            shapes = listOf(
                MaterialShapes.Cookie6Sided,
                MaterialShapes.Sunny,
                MaterialShapes.Cookie9Sided,
                MaterialShapes.Cookie12Sided
            ),
            pauseDuration = 0
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = stringResource(R.string.onboarding_welcome_title),
                modifier = Modifier.size(180.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.onboarding_welcome_app_subtitle),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shapes = ButtonDefaults.shapes()
        ) {
            Text(text = stringResource(R.string.start_onboarding_button_text))
        }
    }
}


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingTabletLayout(
    pagerState: PagerState,
    uiState: OnboardingUiState,
    activity: ComponentActivity,
    onPermissionResult: (PermissionType, Boolean) -> Unit,
    onFinish: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ShapesAnimation(
                modifier = Modifier.size(300.dp),
                shapeModifier = Modifier.size(300.dp),
                contentPadding = PaddingValues(12.dp),
                shapes = listOf(
                    MaterialShapes.Cookie6Sided,
                    MaterialShapes.Sunny,
                    MaterialShapes.Cookie9Sided,
                    MaterialShapes.Cookie12Sided
                ),
                pauseDuration = 0
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.onboarding_welcome_title),
                    modifier = Modifier.size(180.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                OnboardingNavigation(
                    pagerState = pagerState,
                    steps = uiState.steps,
                    isNextEnabled = uiState.isNextEnabled,
                    onNext = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < uiState.steps.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onFinish()
                            }
                        }
                    },
                    onPrevious = {
                        coroutineScope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                MorphingIcon(pagerState = pagerState, steps = uiState.steps)
                Spacer(modifier = Modifier.height(48.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    userScrollEnabled = uiState.isNextEnabled
                ) { pageIndex ->
                    val step = uiState.steps[pageIndex]
                    key(step.titleResId) {
                        OnboardingStepPage(
                            step = step,
                            activity = activity,
                            onPermissionResult = { isGranted ->
                                step.permissionType?.let { onPermissionResult(it, isGranted) }
                            },
                            isPermissionGranted = uiState.permissionStatus[step.permissionType] ?: false
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun OnboardingStepPage(
    step: OnboardingStepContent,
    activity: ComponentActivity,
    onPermissionResult: (Boolean) -> Unit,
    isPermissionGranted: Boolean
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(id = step.titleResId),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = step.descriptionResId),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (step.permissionType != null) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    when (step.permissionType) {
                        PermissionType.NOTIFICATION ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }

                        PermissionType.EXACT_ALARM ->
                            PermissionUtils.checkAndRequestExactAlarmPermission(activity)

                        PermissionType.FULL_SCREEN_INTENT ->
                            PermissionUtils.checkAndRequestFullScreenIntentPermission(activity)
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                enabled = !isPermissionGranted,
                shapes = ButtonDefaults.shapes(),
                ) {
                val buttonTextRes = if (isPermissionGranted) {
                    R.string.onboarding_permission_granted_button
                } else {
                    R.string.onboarding_grant_permission_button
                }
                Text(text = stringResource(id = buttonTextRes))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingNavigation(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    isNextEnabled: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val isPreviousButtonVisible by remember {
        derivedStateOf { pagerState.currentPage > 0 }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        if (isPreviousButtonVisible) {
            TextButton(
                onClick = onPrevious,
                modifier = Modifier.align(Alignment.CenterStart),
                shapes = ButtonDefaults.shapes()
            ) {
                Text(text = stringResource(R.string.previous_button_text))
            }
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            steps.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }

        Button(
            onClick = onNext,
            enabled = isNextEnabled,
            modifier = Modifier.align(Alignment.CenterEnd),
            shapes = ButtonDefaults.shapes()
        ) {
            Text(
                text = stringResource(
                    if (pagerState.currentPage < steps.size - 1) R.string.next_button_text
                    else R.string.done_button_text
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MorphingIcon(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>
) {
    val morphProgress = pagerState.currentPage + pagerState.currentPageOffsetFraction

    val morphShape = remember(morphProgress) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val fromIndex = floor(morphProgress).toInt().coerceIn(0, steps.size - 2)
                val toIndex = (fromIndex + 1).coerceAtMost(steps.size - 1)
                val progress = morphProgress - fromIndex

                val shapes = listOf(
                    MaterialShapes.Pill,
                    MaterialShapes.Slanted,
                    MaterialShapes.Pentagon,
                    MaterialShapes.Sunny,
                    MaterialShapes.Cookie7Sided
                )

                val morph = Morph(shapes[fromIndex], shapes[toIndex])
                val path = Path()
                morph.toPath(progress, path)

                val matrix = Matrix()
                matrix.scale(size.width / 2f, size.height / 2f)
                matrix.translate(0.5f, 0.5f)
                path.transform(matrix)
                return Outline.Generic(path)
            }
        }
    }


    Box(
        modifier = Modifier
            .size(240.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = morphShape
            ),
        contentAlignment = Alignment.Center
    ) {
        val iconAlpha by animateFloatAsState(
            targetValue = 1f - (pagerState.currentPageOffsetFraction * 2).coerceIn(0f, 1f),
            label = "icon_alpha_fade_out"
        )
        val nextIconAlpha by animateFloatAsState(
            targetValue = (pagerState.currentPageOffsetFraction * 2 - 1).coerceIn(0f, 1f),
            label = "icon_alpha_fade_in"
        )

        Icon(
            painter = painterResource(id = steps[pagerState.currentPage].iconResId),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer { alpha = iconAlpha },
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        if (pagerState.currentPage < steps.size - 1) {
            Icon(
                painter = painterResource(id = steps[pagerState.currentPage + 1].iconResId),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { alpha = nextIconAlpha },
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}


// --- PREVIEWS ---

private val previewSteps = listOf(
    OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc, R.mipmap.ic_launcher_foreground),
    OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, R.mipmap.ic_launcher_foreground, PermissionType.NOTIFICATION),
    OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, R.mipmap.ic_launcher_foreground, PermissionType.EXACT_ALARM),
    OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, R.mipmap.ic_launcher_foreground, PermissionType.FULL_SCREEN_INTENT),
    OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc, R.mipmap.ic_launcher_foreground)
)

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingPhonePreview(
    initialPage: Int,
    isNextEnabled: Boolean,
    permissionStatus: Map<PermissionType, Boolean>
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { previewSteps.size }
    val uiState = OnboardingUiState(
        steps = previewSteps,
        isNextEnabled = isNextEnabled,
        permissionStatus = permissionStatus
    )

    AppTheme{
       OnboardingPhoneContent(
           pagerState = pagerState,
           uiState = uiState,
           activity = LocalContext.current.findActivity() as ComponentActivity,
           onPermissionResult = { _,_ -> },
           onFinish = {}
       )
   }
}

@Preview(showBackground = true, name = "Welcome Screen", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun WelcomePageContentPreview() {
    AppTheme {
        WelcomePageContent(onStartClick = {})
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 1: Welcome", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep1Preview() {
    OnboardingPhonePreview(initialPage = 0, isNextEnabled = true, permissionStatus = emptyMap())
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 2: Notifications (Not Granted)", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep2NotificationsPreview() {
    OnboardingPhonePreview(initialPage = 1, isNextEnabled = false, permissionStatus = mapOf(PermissionType.NOTIFICATION to false))
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 2: Notifications (Granted)", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep2NotificationsGrantedPreview() {
    OnboardingPhonePreview(initialPage = 1, isNextEnabled = true, permissionStatus = mapOf(PermissionType.NOTIFICATION to true))
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 5: Finish", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep5FinishPreview() {
    OnboardingPhonePreview(initialPage = 4, isNextEnabled = true, permissionStatus = mapOf())
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Tablet Layout", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun OnboardingTabletLayoutPreview() {
    AppTheme {
        OnboardingTabletLayout(
            pagerState = rememberPagerState { previewSteps.size },
            uiState = OnboardingUiState(
                steps = previewSteps,
                isNextEnabled = true
            ),
            activity = LocalContext.current.findActivity() as ComponentActivity,
            onPermissionResult = {_,_ ->},
            onFinish = {}
        )
    }
}