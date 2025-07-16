package com.d4viddf.medicationreminder.wear.presentation.components

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material3.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearRepository
import com.d4viddf.medicationreminder.wear.presentation.*
import com.google.android.gms.wearable.Wearable
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme

@Composable
fun WearApp(wearViewModel: WearViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var isFirstLaunch by remember { mutableStateOf(sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)) }
    var hasAlarmPermission by remember { mutableStateOf(checkAlarmPermission(context)) }

    val reminders by wearViewModel.reminders.collectAsStateWithLifecycle()
    val isLoading by wearViewModel.isLoading.collectAsStateWithLifecycle()
    val phoneAppStatus by wearViewModel.phoneAppStatus.collectAsStateWithLifecycle()
    val selectedReminder by wearViewModel.selectedReminder.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasAlarmPermission = isGranted
        if (!isGranted) {
            Log.w("WearApp", "Schedule exact alarm permission denied by user.")
        }
    }

    MedicationReminderTheme {
        val navController = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "reminders"
        ) {
            composable("reminders") {
                val listState = rememberTransformingLazyColumnState()
                ScreenScaffold(
                    scrollState = listState,
                ) {
                    LaunchedEffect(Unit) {
                        wearViewModel.triggerPhoneAppCheckAndSync(
                            RemoteActivityHelper(context),
                            Wearable.getMessageClient(context)
                        )
                    }

                    if (isFirstLaunch) {
                        OnboardingScreen(
                            onDismiss = {
                                sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                                isFirstLaunch = false
                                if (!hasAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                                }
                            },
                            hasAlarmPermission = hasAlarmPermission,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                                }
                            }
                        )
                    } else {
                        when (phoneAppStatus) {
                            PhoneAppStatus.UNKNOWN, PhoneAppStatus.CHECKING -> {
                                CircularProgressIndicator()
                            }
                            PhoneAppStatus.NOT_INSTALLED_ANDROID -> {
                                if (reminders.isNotEmpty()) {
                                    RemindersContent(
                                        reminders = reminders,
                                        onMarkAsTaken = { reminderToTake ->
                                            wearViewModel.markReminderAsTakenOnWatch(reminderToTake)
                                        },
                                        onMoreClick = { navController.navigate("more") },
                                        onReminderClick = { reminder ->
                                            wearViewModel.selectReminder(reminder)
                                            navController.navigate("medicationDetail/${reminder.medicationId}")
                                        },
                                        phoneAppStatus = phoneAppStatus,
                                        onRetry = {
                                            wearViewModel.triggerPhoneAppCheckAndSync(
                                                RemoteActivityHelper(context),
                                                Wearable.getMessageClient(context)
                                            )
                                        }
                                    )
                                } else {
                                    PhoneAppNotInstalledScreen(
                                        onOpenPlayStore = {
                                            wearViewModel.openPlayStoreOnPhone(
                                                RemoteActivityHelper(context)
                                            )
                                        },
                                        onRetry = {
                                            wearViewModel.triggerPhoneAppCheckAndSync(
                                                RemoteActivityHelper(context),
                                                Wearable.getMessageClient(context)
                                            )
                                        }
                                    )
                                }
                            }
                            PhoneAppStatus.INSTALLED_NO_DATA, PhoneAppStatus.INSTALLED_DATA_REQUESTED -> {
                                if (isLoading && reminders.isEmpty()) {
                                    CircularProgressIndicator()
                                } else {
                                    RemindersContent(
                                        reminders = reminders,
                                        onMarkAsTaken = { reminderToTake ->
                                            wearViewModel.markReminderAsTakenOnWatch(reminderToTake)
                                        },
                                        onMoreClick = { navController.navigate("more") },
                                        onReminderClick = { reminder ->
                                            wearViewModel.selectReminder(reminder)
                                            navController.navigate("medicationDetail/${reminder.medicationId}")
                                        },
                                        phoneAppStatus = phoneAppStatus,
                                        onRetry = {
                                            wearViewModel.triggerPhoneAppCheckAndSync(
                                                RemoteActivityHelper(context),
                                                Wearable.getMessageClient(context)
                                            )
                                        }
                                    )
                                }
                            }
                            PhoneAppStatus.INSTALLED_WITH_DATA -> {
                                RemindersContent(
                                    reminders = reminders,
                                    onMarkAsTaken = { reminderToTake ->
                                        wearViewModel.markReminderAsTakenOnWatch(reminderToTake)
                                    },
                                    onMoreClick = { navController.navigate("more") },
                                    onReminderClick = { reminder ->
                                        wearViewModel.selectReminder(reminder)
                                        navController.navigate("medicationDetail/${reminder.medicationId}")
                                    },
                                    phoneAppStatus = phoneAppStatus,
                                    onRetry = {
                                        wearViewModel.triggerPhoneAppCheckAndSync(
                                            RemoteActivityHelper(context),
                                            Wearable.getMessageClient(context)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            composable("more") {
                MoreScreen(
                    onSyncClick = {
                        wearViewModel.triggerPhoneAppCheckAndSync(
                            RemoteActivityHelper(context),
                            Wearable.getMessageClient(context)
                        )
                        navController.popBackStack()
                    },
                    onOpenAppClick = {
                        wearViewModel.openPlayStoreOnPhone(RemoteActivityHelper(context))
                    },
                    onSettingsClick = { /* TODO */ }
                )
            }
            composable("medicationDetail/{medicationId}") { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getString("medicationId")?.toIntOrNull()
                MedicationDetailScreen(
                    medicationId = medicationId,
                    viewModel = wearViewModel,
                    onOpenOnPhone = {
                        wearViewModel.openMedicationDetailsOnPhone(
                            RemoteActivityHelper(context),
                            selectedReminder
                        )
                    }
                )
            }
        }
    }
}


@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultWearAppPreview() {
    MedicationReminderTheme {
        val context = LocalContext.current.applicationContext as Application
        val previewViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application = context, wearRepository = WearRepository(
            context
        )
        ))
        WearApp(wearViewModel = previewViewModel)
    }
}
