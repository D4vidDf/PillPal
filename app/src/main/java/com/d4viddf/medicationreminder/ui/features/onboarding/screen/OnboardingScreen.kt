@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.d4viddf.medicationreminder.ui.features.onboarding.screen

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.utils.PermissionUtils
import kotlinx.coroutines.launch
import kotlin.math.floor

// --- Data Classes and Enums ---
enum class PermissionType { NOTIFICATION, EXACT_ALARM, FULL_SCREEN_INTENT }

data class OnboardingStepContent(
    val titleResId: Int,
    val descriptionResId: Int,
    val imageResId: Int,
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
    userPreferencesRepository: UserPreferencesRepository
) {
    val onboardingSteps = listOf(
        OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc, R.mipmap.ic_launcher_foreground),
        OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, R.mipmap.ic_launcher_foreground, PermissionType.NOTIFICATION),
        OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, R.mipmap.ic_launcher_foreground, PermissionType.EXACT_ALARM),
        OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, R.mipmap.ic_launcher_foreground, PermissionType.FULL_SCREEN_INTENT),
        OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc, R.mipmap.ic_launcher_foreground)
    )
    val pagerState = rememberPagerState { onboardingSteps.size }
    val activity = LocalContext.current.findActivity() as? ComponentActivity

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    if (isTablet) {
        OnboardingTabletLayout(
            pagerState = pagerState,
            steps = onboardingSteps,
            navController = navController,
            activity = activity ?: ComponentActivity(),
            userPreferencesRepository = userPreferencesRepository
        )
    } else {
        OnboardingPhoneLayout(
            pagerState = pagerState,
            steps = onboardingSteps,
            navController = navController,
            activity = activity ?: ComponentActivity(),
            userPreferencesRepository = userPreferencesRepository
        )
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
    userPreferencesRepository: UserPreferencesRepository
) {
    val coroutineScope = rememberCoroutineScope()
    var showWelcomeScreen by rememberSaveable { mutableStateOf(true) }

    if (showWelcomeScreen) {
        WelcomePageContent(onStartClick = { showWelcomeScreen = false })
    } else {
        Scaffold(
            bottomBar = {
                OnboardingNavigation(
                    pagerState = pagerState,
                    steps = steps,
                    onNext = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < steps.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                userPreferencesRepository.updateOnboardingCompleted(true)
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
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
                    .padding(padding)
            ) {
                // Fixed height Box to keep the image position consistent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp), // Adjust height as needed
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = pagerState.currentPage, label = "onboarding_image_crossfade") { page ->
                        Image(
                            painter = painterResource(id = steps[page].imageResId),
                            contentDescription = null,
                            modifier = Modifier.size(180.dp)
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    OnboardingStepPage(
                        step = steps[pageIndex],
                        activity = activity
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

        // Animated Logo Box - now larger on phones
        AnimatedLogo(
            modifier = Modifier.size(250.dp),
            shapes = listOf(
                MaterialShapes.Cookie6Sided,
                MaterialShapes.Cookie9Sided,
                MaterialShapes.Cookie12Sided,
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
            shapes= ButtonDefaults.shapes()
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
    val totalDuration = animationDuration

    // Animate through the list of shapes
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size - 1).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalDuration * (shapes.size - 1)
                for (i in 0 until shapes.size - 1) {
                    i.toFloat() at (totalDuration * i) with FastOutSlowInEasing
                    (i + 1).toFloat() at (totalDuration * (i + 1)) with FastOutSlowInEasing
                }
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph_progress"
    )

    // Animate rotation based on the same total duration to keep them in sync
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalDuration
                0f at 0 with FastOutSlowInEasing
                360f at animationDuration with FastOutSlowInEasing
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
                matrix.translate(0.5f, 0.5f)
                path.transform(matrix)
                return Outline.Generic(path)
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // The background box rotates and morphs
        Box(
            modifier = Modifier.size(600.dp)
                .graphicsLayer { rotationZ = rotation }
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = morphShape
                )
        )

        // The image itself does not rotate
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = stringResource(R.string.onboarding_welcome_title),
            modifier = Modifier.size(180.dp) // Make it smaller than the background
        )
    }
}


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingTabletLayout(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    navController: NavHostController,
    activity: ComponentActivity,
    userPreferencesRepository: UserPreferencesRepository
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
            // Use the same animated logo on the tablet layout
            AnimatedLogo(
                modifier = Modifier.size(250.dp),
                shapes = listOf(MaterialShapes.Cookie6Sided,
                    MaterialShapes.Cookie9Sided,
                    MaterialShapes.Cookie12Sided,
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
                    steps = steps,
                    onNext = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < steps.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                userPreferencesRepository.updateOnboardingCompleted(true)
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) { pageIndex ->
                OnboardingStepPage(step = steps[pageIndex], activity = activity)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun OnboardingStepPage(
    step: OnboardingStepContent,
    activity: ComponentActivity
) {
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPermissionGranted = granted
    }

    fun checkPermission() {
        isPermissionGranted = when (step.permissionType) {
            PermissionType.NOTIFICATION ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true
            PermissionType.EXACT_ALARM ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.canScheduleExactAlarms()
                } else true
            PermissionType.FULL_SCREEN_INTENT -> true
            null -> true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, step.permissionType) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(step.permissionType) {
        checkPermission()
    }

    // The main Column now only contains the text and the button.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp), // Only horizontal padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Align content to the top
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
                enabled = !isPermissionGranted,
                modifier = Modifier.fillMaxWidth(0.8f),
                shapes= ButtonDefaults.shapes()
            ) {
                Text(
                    text = stringResource(
                        if (isPermissionGranted) R.string.onboarding_permission_granted_button
                        else R.string.onboarding_grant_permission_button
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingNavigation(
    pagerState: PagerState,
    steps: List<OnboardingStepContent>,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val context = LocalContext.current
    var isNextEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val currentStep = steps.getOrNull(pagerState.currentPage)
            isNextEnabled = when (currentStep?.permissionType) {
                PermissionType.NOTIFICATION ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else true
                PermissionType.EXACT_ALARM ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
                    } else true
                else -> true
            }
        }
    }

    // Use a Box to perfectly center the indicators
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        if (pagerState.currentPage > 0) {
            TextButton(
                onClick = onPrevious,
                modifier = Modifier.align(Alignment.CenterStart)
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
            shapes= ButtonDefaults.shapes()
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

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Welcome Screen")
@Composable
fun WelcomePageContentPreview() {
    MaterialTheme {
        WelcomePageContent(onStartClick = {})
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 1: Welcome")
@Composable
fun OnboardingStep1Preview() {
    MaterialTheme {
        OnboardingStepPage(
            step = OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc, R.mipmap.ic_launcher_foreground),
            activity = ComponentActivity()
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 2: Notifications")
@Composable
fun OnboardingStep2NotificationsPreview() {
    MaterialTheme {
        OnboardingStepPage(
            step = OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, R.mipmap.ic_launcher_foreground, PermissionType.NOTIFICATION),
            activity = ComponentActivity()
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 3: Exact Alarms")
@Composable
fun OnboardingStep3AlarmsPreview() {
    MaterialTheme {
        OnboardingStepPage(
            step = OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, R.mipmap.ic_launcher_foreground, PermissionType.EXACT_ALARM),
            activity = ComponentActivity()
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 4: Full-Screen Intent")
@Composable
fun OnboardingStep4FullscreenPreview() {
    MaterialTheme {
        OnboardingStepPage(
            step = OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, R.mipmap.ic_launcher_foreground, PermissionType.FULL_SCREEN_INTENT),
            activity = ComponentActivity()
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true, name = "Step 5: Finish")
@Composable
fun OnboardingStep5FinishPreview() {
    MaterialTheme {
        OnboardingStepPage(
            step = OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc, R.mipmap.ic_launcher_foreground),
            activity = ComponentActivity()
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Tablet Layout", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun OnboardingTabletLayoutPreview() {
    val context = LocalContext.current
    MaterialTheme {
        OnboardingTabletLayout(
            pagerState = rememberPagerState { 5 },
            steps = listOf(
                OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc, R.mipmap.ic_launcher_foreground),
                OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, R.mipmap.ic_launcher_foreground, PermissionType.NOTIFICATION)
            ),
            navController = rememberNavController(),
            activity = ComponentActivity(),
            userPreferencesRepository = UserPreferencesRepository(context)
        )
    }
}