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
                ScreenScaffold(
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.phone_app_not_installed_android),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Button(onClick = {
                                        wearViewModel.openPlayStoreOnPhone(RemoteActivityHelper(context))
                                    }) {
                                        Text(stringResource(R.string.open_play_store))
                                    }
                                }
                            }
                            PhoneAppStatus.INSTALLED_NO_DATA, PhoneAppStatus.INSTALLED_DATA_REQUESTED -> {
                                if (isLoading && reminders.isEmpty()) {
                                    CircularProgressIndicator()
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = if (reminders.isEmpty()) stringResource(R.string.no_reminders_today_syncing) else stringResource(R.string.checking_for_updates),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Button(onClick = {
                                            wearViewModel.triggerPhoneAppCheckAndSync(
                                                RemoteActivityHelper(context),
                                                Wearable.getMessageClient(context)
                                            )
                                        }) {
                                            Text(stringResource(R.string.retry_sync))
                                        }
                                        if (reminders.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            RemindersContent(
                                                reminders = reminders,
                                                onMarkAsTaken = { reminderToTake ->
                                                    wearViewModel.markReminderAsTakenOnWatch(reminderToTake)
                                                },
                                                onMoreClick = { navController.navigate("more") }
                                            )
                                        }
                                    }
                                }
                            }
                            PhoneAppStatus.INSTALLED_WITH_DATA -> {
                                if (reminders.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.no_reminders_today),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                } else {
                                    RemindersContent(
                                        reminders = reminders,
                                        onMarkAsTaken = { reminderToTake ->
                                            wearViewModel.markReminderAsTakenOnWatch(reminderToTake)
                                        },
                                        onMoreClick = { navController.navigate("more") }
                                    )
                                }
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
