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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.d4viddf.medicationreminder.common.IntentActionConstants
import com.d4viddf.medicationreminder.common.IntentExtraConstants
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
import com.d4viddf.medicationreminder.ui.common.components.AnimatedShape
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


@AndroidEntryPoint
class FullScreenNotificationActivity : ComponentActivity() {

    companion object {
        // All EXTRAs moved to IntentExtraConstants or NotificationConstants
        private const val TAG = "FullScreenNotification"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getIntExtra(com.d4viddf.medicationreminder.common.IntentExtraConstants.EXTRA_REMINDER_ID, -1)
        val medicationName = intent.getStringExtra(com.d4viddf.medicationreminder.common.IntentExtraConstants.EXTRA_MEDICATION_NAME) ?: getString(R.string.medication_name_placeholder)
        val medicationDosage = intent.getStringExtra(com.d4viddf.medicationreminder.common.IntentExtraConstants.EXTRA_MEDICATION_DOSAGE) ?: getString(R.string.not_set)
        val medicationColorNameString = intent.getStringExtra(com.d4viddf.medicationreminder.common.NotificationConstants.EXTRA_MED_COLOR_HEX)
        val medicationTypeName = intent.getStringExtra(com.d4viddf.medicationreminder.common.NotificationConstants.EXTRA_MED_TYPE_NAME)

        Log.d(TAG, "Activity created for Reminder ID: $reminderId, Name: $medicationName, Dosage: $medicationDosage, ColorNameString: $medicationColorNameString, Type: $medicationTypeName")

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
                        Log.w(TAG, "Invalid MedicationColor name: '$medicationColorNameString'. Defaulting to BLUE.")
                        MedicationColor.BLUE
                    }
                }

                val finalBackgroundColor: Color = medicationColorScheme.backgroundColor
                val finalContentColor: Color = medicationColorScheme.textColor
                val animatedShapesColor = finalContentColor.copy(alpha = 0.1f) // Or your desired alpha

                Log.d(TAG, "Using MedicationColor: ${medicationColorScheme.name} (Enum Name), ${medicationColorScheme.colorName} (Descriptive Name)")
                Log.d(TAG, "Background color: $finalBackgroundColor, Content color: $finalContentColor, Animated Shapes color: $animatedShapesColor")


                FullScreenNotificationScreen(
                    reminderId = reminderId,
                    medicationName = medicationName,
                    medicationDosage = medicationDosage,
                    medicationTypeName = medicationTypeName,
                    backgroundColor = finalBackgroundColor,
                    contentColor = finalContentColor,
                    animatedShapesColor = animatedShapesColor,
                    onMarkAsTaken = {
                        Log.i(TAG, "Mark as Taken clicked for Reminder ID: $reminderId")
                        val markTakenIntent = Intent(applicationContext, ReminderBroadcastReceiver::class.java).apply { // FQDN for ReminderBroadcastReceiver
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
                        Log.i(TAG, "Dismiss clicked for Reminder ID: $reminderId")
                        if (reminderId != -1) {
                            NotificationManagerCompat.from(this@FullScreenNotificationActivity).cancel(reminderId)
                        }
                        Toast.makeText(this@FullScreenNotificationActivity, "${getString(R.string.fullscreen_notification_action_dismiss)}: $medicationName", Toast.LENGTH_SHORT).show()
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }
}

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
        // --- Use your OLD/Working AnimatedShape ---
        AnimatedShape( // Call your refactored composable
            modifier = Modifier.fillMaxSize(),
            shapeColor = animatedShapesColor // Pass the desired color
        )
        // --- End Animated Background ---

        // Overlay the medication type image or tick mark
        if (showTick) {
            Image(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(id = R.string.content_description_tick_mark),
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(imageTint)
            )
        } else {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = stringResource(id = contentDescResId),
                modifier = Modifier.size(100.dp),
                colorFilter = ColorFilter.tint(imageTint)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showTick by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

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
                modifier = Modifier.size(200.dp),
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
                        text = medicationName,
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

            Spacer(modifier = Modifier.weight(1f)) // Pushes buttons down

            if (!showTick) {
                // Box to control the alignment of the SplitButtonLayout
                Box(
                    modifier = Modifier
                        .fillMaxWidth() // This Box takes full width
                        .padding(bottom = 32.dp), // Padding for the button group area
                    contentAlignment = Alignment.Center // Center its child (SplitButtonLayout)
                ) {
                    val buttonContainerHeight = SplitButtonDefaults.LargeContainerHeight

                    SplitButtonLayout( // This will now wrap its content width by default if not constrained
                        // or use .wrapContentWidth() if you want it to be exactly as wide as its content
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
                            SplitButtonDefaults.TrailingButton(
                                checked = isDropdownExpanded,
                                onCheckedChange = { isDropdownExpanded = it },
                                modifier = Modifier
                                    .heightIn(min = buttonContainerHeight)
                                    .semantics {
                                        stateDescription = if (isDropdownExpanded) stateExpandedText else stateCollapsedText
                                        contentDescription = moreOptionsText
                                    },
                                shapes = SplitButtonDefaults.trailingButtonShapesFor(buttonContainerHeight),
                                // Use filled button colors for trailing to match leading, or keep outlined
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = contentColor.copy(alpha = 0.20f), // Consistent with leading
                                    contentColor = contentColor
                                )
                                // If you prefer outlined for trailing:
                                // colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
                            ) {
                                val rotation: Float by animateFloatAsState(
                                    targetValue = if (isDropdownExpanded) 180f else 0f,
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

                    // Material 3 DropdownMenu will inherently have M3 styling.
                    // It aligns itself relative to its anchor (which is implicitly the parent Box here,
                    // often aligning to the trailing button if there's enough space or if anchored).
                    // You can use an `ExposedDropdownMenuBox` for more precise anchor control if needed.
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        // Modifier to align relative to the TrailingButton if desired.
                        // This requires getting a reference to the TrailingButton,
                        // which is complex with SplitButtonLayout.
                        // Often, default alignment or slight offset is acceptable.
                        // For now, let's keep default behavior.
                        // modifier = Modifier.align(Alignment.BottomEnd) // Example, might need custom offset
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.fullscreen_notification_action_dismiss)) },
                            onClick = {
                                onDismiss()
                                isDropdownExpanded = false
                            }
                        )
                        // Add more DropdownMenuItems (e.g., Snooze options) here if needed
                    }
                }
            } else {
                Spacer(modifier = Modifier.defaultMinSize(minHeight = SplitButtonDefaults.LargeContainerHeight + 32.dp))
            }
            Spacer(modifier = Modifier.weight(0.1f))
        }
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
            onDismiss = {}
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
            onDismiss = {}
        )
    }
}

@Preview(name = "Light Mode - Show Tick", showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun FullScreenNotificationScreenTickPreview() {
    AppTheme(themePreference = "Light") {
        var showTickPreview by remember { mutableStateOf(true) }
        FullScreenNotificationScreen(
            reminderId = 1,
            medicationName = "Amoxicillin",
            medicationDosage = "250 mg",
            medicationTypeName = "Capsule",
            backgroundColor = MedicationColor.LIGHT_GREEN.backgroundColor,
            contentColor = MedicationColor.LIGHT_GREEN.textColor,
            animatedShapesColor = MedicationColor.LIGHT_GREEN.textColor.copy(alpha = 0.1f),
            onMarkAsTaken = { showTickPreview = true },
            onDismiss = {}
        )
    }
}