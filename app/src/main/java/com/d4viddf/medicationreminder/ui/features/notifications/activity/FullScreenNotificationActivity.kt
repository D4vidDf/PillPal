package com.d4viddf.medicationreminder.ui.features.notifications.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
import com.d4viddf.medicationreminder.receivers.SnoozeBroadcastReceiver
import com.d4viddf.medicationreminder.ui.common.component.ShapesAnimation
import com.d4viddf.medicationreminder.ui.common.component.bottomsheet.BottomSheetItem
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import com.d4viddf.medicationreminder.utils.constants.IntentActionConstants
import com.d4viddf.medicationreminder.utils.constants.IntentExtraConstants
import com.d4viddf.medicationreminder.utils.constants.NotificationConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


@AndroidEntryPoint
class FullScreenNotificationActivity : ComponentActivity() {

    companion object {
        private const val TAG = "FullScreenNotification"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getIntExtra(IntentExtraConstants.EXTRA_REMINDER_ID, -1)
        val medicationName = intent.getStringExtra(IntentExtraConstants.EXTRA_MEDICATION_NAME) ?: getString(R.string.medication_name_placeholder)
        val medicationDosage = intent.getStringExtra(IntentExtraConstants.EXTRA_MEDICATION_DOSAGE) ?: getString(R.string.not_set)
        val medicationColorNameString = intent.getStringExtra(NotificationConstants.EXTRA_MED_COLOR_HEX)
        val medicationTypeName = intent.getStringExtra(NotificationConstants.EXTRA_MED_TYPE_NAME)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContent {
            AppTheme {
                val medicationColorScheme: MedicationColor = remember(medicationColorNameString) {
                    try {
                        MedicationColor.valueOf(medicationColorNameString ?: MedicationColor.BLUE.name)
                    } catch (e: IllegalArgumentException) {
                        MedicationColor.BLUE
                    }
                }

                val finalBackgroundColor: Color = medicationColorScheme.backgroundColor
                val finalContentColor: Color = medicationColorScheme.textColor
                val animatedShapesColor = finalContentColor.copy(alpha = 0.1f)

                FullScreenNotificationScreen(
                    reminderId = reminderId,
                    medicationName = medicationName,
                    medicationDosage = medicationDosage,
                    medicationTypeName = medicationTypeName,
                    backgroundColor = finalBackgroundColor,
                    contentColor = finalContentColor,
                    animatedShapesColor = animatedShapesColor,
                    onMarkAsTaken = {
                        val markTakenIntent = Intent(applicationContext, ReminderBroadcastReceiver::class.java).apply {
                            action = IntentActionConstants.ACTION_MARK_AS_TAKEN
                            putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminderId)
                        }
                        applicationContext.sendBroadcast(markTakenIntent)
                        if (reminderId != -1) {
                            NotificationManagerCompat.from(this@FullScreenNotificationActivity).cancel(reminderId)
                        }
                        Toast.makeText(this@FullScreenNotificationActivity, "${getString(R.string.notification_action_mark_as_taken)}: $medicationName", Toast.LENGTH_SHORT).show()
                        finishAndRemoveTask()
                    },
                    onDismiss = {
                        if (reminderId != -1) {
                            NotificationManagerCompat.from(this@FullScreenNotificationActivity).cancel(reminderId)
                        }
                        Toast.makeText(this@FullScreenNotificationActivity, "${getString(R.string.fullscreen_notification_action_dismiss)}: $medicationName", Toast.LENGTH_SHORT).show()
                        finishAndRemoveTask()
                    },
                    onSnooze = {
                        val snoozeIntent = Intent(applicationContext, SnoozeBroadcastReceiver::class.java).apply {
                            action = IntentActionConstants.ACTION_SNOOZE_REMINDER
                            putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminderId)
                        }
                        applicationContext.sendBroadcast(snoozeIntent)
                        if (reminderId != -1) {
                            NotificationManagerCompat.from(this@FullScreenNotificationActivity).cancel(reminderId)
                        }
                        Toast.makeText(this@FullScreenNotificationActivity, getString(R.string.notification_action_snooze) + " ($medicationName)", Toast.LENGTH_SHORT).show()
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MedicationTypeImage(
    typeName: String?,
    showTick: Boolean,
    modifier: Modifier = Modifier,
    imageTint: Color,
    animatedShapesColor: Color
) {
    val imageResId: Int
    val contentDescResId: Int

    when (typeName?.lowercase(Locale.getDefault())) {
        "pill" -> { imageResId = R.drawable.ic_med_pill; contentDescResId = R.string.medication_type_pill_image_description }
        "capsule" -> { imageResId = R.drawable.ic_med_capsule; contentDescResId = R.string.medication_type_capsule_image_description }
        "syrup", "liquid" -> { imageResId = R.drawable.ic_med_syrup; contentDescResId = R.string.medication_type_syrup_image_description }
        "injection", "syringe" -> { imageResId = R.drawable.ic_med_injection; contentDescResId = R.string.medication_type_injection_image_description }
        "drops" -> { imageResId = R.drawable.ic_med_drops; contentDescResId = R.string.medication_type_drops_image_description }
        "inhaler" -> { imageResId = R.drawable.ic_med_inhaler; contentDescResId = R.string.medication_type_inhaler_image_description }
        else -> { imageResId = R.drawable.ic_stat_medication; contentDescResId = R.string.medication_type_other_image_description }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        ShapesAnimation(
            modifier = Modifier.size(300.dp),
            shapeModifier = Modifier.size(300.dp),
            shapes = listOf(
                MaterialShapes.Cookie6Sided,
                MaterialShapes.Sunny,
                MaterialShapes.Cookie9Sided,
                MaterialShapes.Cookie12Sided,
                MaterialShapes.Pill,
            ),
            backgroundColor = animatedShapesColor,
            contentPadding = PaddingValues(12.dp),
            animationDuration = 10000,
            pauseDuration = 3000,
        ) {
            if (showTick) {
                Image(
                    painter = painterResource(id = R.drawable.ic_check),
                    modifier = Modifier.fillMaxSize(0.3f),
                    contentDescription = stringResource(id = R.string.content_description_tick_mark),
                    colorFilter = ColorFilter.tint(imageTint)
                )
            } else {
                Image(
                    painter = painterResource(id = imageResId),
                    modifier = Modifier.fillMaxSize(0.3f),
                    contentDescription = stringResource(id = contentDescResId),
                    colorFilter = ColorFilter.tint(imageTint)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FullScreenNotificationScreen(
    reminderId: Int,
    medicationName: String,
    medicationDosage: String,
    medicationTypeName: String?,
    backgroundColor: Color,
    contentColor: Color,
    animatedShapesColor: Color,
    onMarkAsTaken: () -> Unit,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showTick by remember { mutableStateOf(false) }

    // State for the bottom sheet
    var showActionsBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val stateExpandedText = stringResource(id = R.string.split_button_state_expanded)
    val stateCollapsedText = stringResource(id = R.string.split_button_state_collapsed)
    val moreOptionsText = stringResource(id = R.string.notification_actions_more_options)

    val internalOnMarkAsTaken: () -> Unit = {
        scope.launch {
            showTick = true
            delay(1200L)
            onMarkAsTaken()
        }
    }

    val textAlpha by animateFloatAsState(
        targetValue = if (showTick) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "TextAlphaAnimation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
    ) {
        // Show the bottom sheet when triggered
        if (showActionsBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showActionsBottomSheet = false },
                sheetState = sheetState
            ) {
                NotificationActionsBottomSheet(
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showActionsBottomSheet = false
                                onDismiss()
                            }
                        }
                    },
                    onSnooze = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showActionsBottomSheet = false
                                onSnooze()
                            }
                        }
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Spacer(modifier = Modifier.weight(0.2f))

            MedicationTypeImage(
                typeName = medicationTypeName,
                showTick = showTick,
                modifier = Modifier.size(300.dp),
                imageTint = contentColor,
                animatedShapesColor = animatedShapesColor
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer { alpha = textAlpha }
                    .padding(vertical = 16.dp)
            ) {
                if (!showTick) {
                    Text(
                        text = medicationName.split(" ").first(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            textAlign = TextAlign.Center,
                            lineHeight = MaterialTheme.typography.displaySmall.lineHeight * 0.9
                        ),
                        maxLines = 3,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = medicationDosage,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = contentColor.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Pushes content down

            if (!showTick) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val buttonContainerHeight = SplitButtonDefaults.LargeContainerHeight

                    SplitButtonLayout(
                        leadingButton = {
                            SplitButtonDefaults.LeadingButton(
                                onClick = internalOnMarkAsTaken,
                                modifier = Modifier.heightIn(min = buttonContainerHeight),
                                shapes = SplitButtonDefaults.leadingButtonShapesFor(buttonContainerHeight),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = contentColor.copy(alpha = 0.20f),
                                    contentColor = contentColor
                                ),
                                contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(buttonContainerHeight),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.fullscreen_notification_action_taken),
                                    style = ButtonDefaults.textStyleFor(buttonContainerHeight).copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                )
                            }
                        },
                        trailingButton = {
                            // This button now opens the bottom sheet
                            SplitButtonDefaults.TrailingButton(
                                checked = showActionsBottomSheet,
                                onCheckedChange = { showActionsBottomSheet = it },
                                modifier = Modifier
                                    .heightIn(min = buttonContainerHeight)
                                    .semantics {
                                        stateDescription = if (showActionsBottomSheet) stateExpandedText else stateCollapsedText
                                        contentDescription = moreOptionsText
                                    },
                                shapes = SplitButtonDefaults.trailingButtonShapesFor(buttonContainerHeight),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = contentColor.copy(alpha = 0.20f),
                                    contentColor = contentColor
                                )
                            ) {
                                val rotation: Float by animateFloatAsState(
                                    targetValue = if (showActionsBottomSheet) 180f else 0f,
                                    label = "TrailingIconRotation",
                                    animationSpec = tween(durationMillis = 300)
                                )
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    modifier = Modifier.graphicsLayer { this.rotationZ = rotation },
                                    contentDescription = null
                                )
                            }
                        },
                        spacing = SplitButtonDefaults.Spacing
                    )
                }
            } else {
                Spacer(modifier = Modifier.defaultMinSize(minHeight = SplitButtonDefaults.LargeContainerHeight))
            }
            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}

@Composable
private fun NotificationActionsBottomSheet(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val actions = getNotificationActions(onDismiss = onDismiss, onSnooze = onSnooze)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(actions, key = { it.id }) { item ->
            BottomSheetCard(item = item)
        }
    }
}

@Composable
private fun getNotificationActions(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
): List<BottomSheetItem> {
    return listOf(
        BottomSheetItem(
            id = "snooze",
            icon = painterResource(id = R.drawable.ic_snooze),
            text = stringResource(id = R.string.notification_action_snooze),
            action = onSnooze
        ),
        BottomSheetItem(
            id = "dismiss",
            icon = painterResource(id = R.drawable.rounded_close_24),
            text = stringResource(id = R.string.fullscreen_notification_action_dismiss),
            action = onDismiss
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomSheetCard(item: BottomSheetItem) {
    Button(
        onClick = item.action,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shapes = ButtonDefaults.shapes()

    ) {

            Icon(
                painter = item.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

    }
}

fun Color.luminance(): Float {
    val red = this.red
    val green = this.green
    val blue = this.blue
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

@Preview(name = "Light Mode - Pill", showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun FullScreenNotificationScreenPillPreview() {
    AppTheme(themePreference = "Light") {
        FullScreenNotificationScreen(
            reminderId = 1,
            medicationName = "Ibuprofen Extra Strength Long Name",
            medicationDosage = "1 Tablet (200 mg)",
            medicationTypeName = "Pill",
            backgroundColor = MedicationColor.LIGHT_BLUE.backgroundColor,
            contentColor = MedicationColor.LIGHT_BLUE.textColor,
            animatedShapesColor = MedicationColor.LIGHT_BLUE.textColor.copy(alpha = 0.1f),
            onMarkAsTaken = {},
            onDismiss = {},
            onSnooze = {}
        )
    }
}

@Preview(name = "Dark Mode - Syrup", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun FullScreenNotificationScreenSyrupDarkPreview() {
    AppTheme(themePreference = "Dark") {
        FullScreenNotificationScreen(
            reminderId = 2,
            medicationName = "Children's Cough Syrup Special",
            medicationDosage = "10 ml every 6 hours as needed for cough",
            medicationTypeName = "Syrup",
            backgroundColor = MedicationColor.PURPLE.backgroundColor,
            contentColor = MedicationColor.PURPLE.textColor,
            animatedShapesColor = MedicationColor.PURPLE.textColor.copy(alpha = 0.1f),
            onMarkAsTaken = {},
            onDismiss = {},
            onSnooze = {}
        )
    }
}

@Preview(name = "Light Mode - Show Tick", showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun FullScreenNotificationScreenTickPreview() {
    AppTheme(themePreference = "Light") {
        // In a real scenario, the state would be managed to show the tick after an action
        FullScreenNotificationScreen(
            reminderId = 1,
            medicationName = "Amoxicillin",
            medicationDosage = "250 mg",
            medicationTypeName = "Capsule",
            backgroundColor = MedicationColor.LIGHT_GREEN.backgroundColor,
            contentColor = MedicationColor.LIGHT_GREEN.textColor,
            animatedShapesColor = MedicationColor.LIGHT_GREEN.textColor.copy(alpha = 0.1f),
            onMarkAsTaken = {},
            onDismiss = {},
            onSnooze = {}
        )
    }
}