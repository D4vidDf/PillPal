@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.graphics.shapes.RoundedPolygon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.onboarding.viewmodel.OnboardingUiState
import com.d4viddf.medicationreminder.ui.features.onboarding.viewmodel.OnboardingViewModel
import com.d4viddf.medicationreminder.ui.navigation.Screen
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionForCurrentStep(pagerState.currentPage)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.checkPermissionForCurrentStep(pagerState.currentPage)
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    if (isTablet) {
        OnboardingTabletLayout(
            pagerState = pagerState,
            uiState = uiState,
            activity = activity ?: ComponentActivity(),
            onFinish = {
                viewModel.finishOnboarding()
                navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
            }
        )
    } else {
        OnboardingPhoneLayout(
            pagerState = pagerState,
            uiState = uiState,
            activity = activity ?: ComponentActivity(),
            onPermissionRequested = { viewModel.checkPermissionForCurrentStep(pagerState.currentPage) },
            onFinish = {
                viewModel.finishOnboarding()
                navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
            }
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
    onPermissionRequested: () -> Unit,
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
            onPermissionRequested = onPermissionRequested,
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
    onPermissionRequested: () -> Unit,
    onFinish: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isPermissionGranted by remember { derivedStateOf { uiState.isCurrentStepPermissionGranted } }

    Scaffold(
        bottomBar = {
            OnboardingNavigation(
                pagerState = pagerState,
                steps = uiState.steps,
                isNextEnabled = isPermissionGranted,
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
                userScrollEnabled = isPermissionGranted,
            ) { pageIndex ->
                key(uiState.steps[pageIndex].titleResId) {
                    OnboardingStepPage(
                        step = uiState.steps[pageIndex],
                        activity = activity,
                        onPermissionResult = onPermissionRequested,
                        isPermissionGranted = isPermissionGranted
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
        AnimatedLogo(
            modifier = Modifier.size(250.dp),
            shapes = listOf(
                MaterialShapes.Cookie6Sided,
                MaterialShapes.Sunny,
                MaterialShapes.Cookie9Sided,
                MaterialShapes.Flower
            )
        )
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

@Composable
fun AnimatedLogo(
    modifier: Modifier = Modifier,
    shapes: List<RoundedPolygon>
) {
    require(shapes.size >= 2) { "Must provide at least two shapes to morph between." }

    val infiniteTransition = rememberInfiniteTransition(label = "logo_animation")
    val animationDuration = 4000
    val pauseDuration = 200
    val totalDuration = animationDuration + pauseDuration

    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size - 1).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalDuration * (shapes.size - 1)
                for (i in 0 until shapes.size - 1) {
                    i.toFloat() at (totalDuration * i) using FastOutSlowInEasing
                    (i + 1).toFloat() at (totalDuration * (i + 1) - pauseDuration) using FastOutSlowInEasing
                }
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph_progress"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalDuration
                0f at 0 using FastOutSlowInEasing
                360f at animationDuration using FastOutSlowInEasing
                360f at totalDuration
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val morphShape = remember(morphProgress) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val fromIndex = floor(morphProgress).toInt().coerceIn(0, shapes.size - 2)
                val toIndex = (fromIndex + 1).coerceAtMost(shapes.size - 1)
                val progress = morphProgress - fromIndex

                val morph = Morph(shapes[fromIndex], shapes[toIndex])
                val path = Path()
                morph.toPath(progress, path)

                val matrix = Matrix()
                matrix.scale(size.width / 2f, size.height / 2f)
                matrix.translate(0.5f, 0.5f) // Corrected translation
                path.transform(matrix)
                return Outline.Generic(path)
            }
        }
    }

    Box(
        modifier = modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .graphicsLayer { rotationZ = rotation }
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = morphShape
                )
        )
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = stringResource(R.string.onboarding_welcome_title),
            modifier = Modifier.size(180.dp)
        )
    }
}


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingTabletLayout(
    pagerState: PagerState,
    uiState: OnboardingUiState,
    activity: ComponentActivity,
    onFinish: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isPermissionGranted by remember { derivedStateOf { uiState.isCurrentStepPermissionGranted } }

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
            AnimatedLogo(
                modifier = Modifier.size(250.dp),
                shapes = listOf(
                    MaterialShapes.Cookie6Sided,
                    MaterialShapes.Sunny,
                    MaterialShapes.Cookie9Sided,
                    MaterialShapes.Flower
                )
            )
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
                    isNextEnabled = isPermissionGranted,
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
                    userScrollEnabled = isPermissionGranted
                ) { pageIndex ->
                    key(uiState.steps[pageIndex].titleResId) {
                        OnboardingStepPage(
                            step = uiState.steps[pageIndex],
                            activity = activity,
                            onPermissionResult = {},
                            isPermissionGranted = isPermissionGranted
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
    onPermissionResult: () -> Unit,
    isPermissionGranted: Boolean
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionResult()
    }

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
                shape = ButtonDefaults.shape,
                enabled = !isPermissionGranted
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

// Helper list for previews. Replace R.mipmap.ic_launcher_foreground with your actual icons.
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
    isPermissionGranted: Boolean
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { previewSteps.size }
    val uiState = OnboardingUiState(
        steps = previewSteps,
        isCurrentStepPermissionGranted = isPermissionGranted
    )

    OnboardingPhoneContent(
        pagerState = pagerState,
        uiState = uiState,
        activity = ComponentActivity(),
        onPermissionRequested = {},
        onFinish = {}
    )
}

@Preview(showBackground = true, name = "Welcome Screen", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun WelcomePageContentPreview() {
    MaterialTheme {
        WelcomePageContent(onStartClick = {})
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 1: Welcome", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep1Preview() {
    MaterialTheme {
        OnboardingPhonePreview(initialPage = 0, isPermissionGranted = true)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 2: Notifications", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep2NotificationsPreview() {
    MaterialTheme {
        OnboardingPhonePreview(initialPage = 1, isPermissionGranted = false)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 2: Notifications (Granted)", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep2NotificationsGrantedPreview() {
    MaterialTheme {
        OnboardingPhonePreview(initialPage = 1, isPermissionGranted = true)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 3: Exact Alarms", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep3AlarmsPreview() {
    MaterialTheme {
        OnboardingPhonePreview(initialPage = 2, isPermissionGranted = false)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 4: Full-Screen Intent", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep4FullscreenPreview() {
    MaterialTheme {
        OnboardingPhonePreview(initialPage = 3, isPermissionGranted = false)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 5: Finish", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun OnboardingStep5FinishPreview() {
    MaterialTheme {
        OnboardingPhonePreview(initialPage = 4, isPermissionGranted = true)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Tablet Layout", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun OnboardingTabletLayoutPreview() {
    MaterialTheme {
        OnboardingTabletLayout(
            pagerState = rememberPagerState { previewSteps.size },
            uiState = OnboardingUiState(
                steps = previewSteps,
                isCurrentStepPermissionGranted = true
            ),
            activity = ComponentActivity(),
            onFinish = {}
        )
    }
}