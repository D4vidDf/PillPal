package com.d4viddf.medicationreminder.ui.activities

import android.R.attr.action
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.colors.MedicationColor // Added import
import com.d4viddf.medicationreminder.ui.colors.findMedicationColorByHex // Added import
// findMedicationColorByName import removed
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.components.AnimatedShapeBackground
// ShapeType is used by FullScreenNotificationScreen to determine currentShapeType, so it's still needed.
import com.d4viddf.medicationreminder.ui.components.ShapeType
import com.d4viddf.medicationreminder.ui.components.SplitButton // Added import for SplitButton
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.math.abs // For abs function
import androidx.compose.ui.graphics.ColorFilter // For ColorFilter.tint
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch // Added import
import kotlinx.coroutines.delay // Added import
import androidx.compose.material.icons.Icons // Added import for Material Icons
import androidx.compose.material.icons.filled.Check // Added import for Check icon
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi // Added import
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass // Added import
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass // Added import

// Helper function to parse color, placed outside the class or in a utility file
fun parseColor(hex: String?, defaultColor: Color): Color {
    return try {
        if (hex != null && hex.startsWith("#") && (hex.length == 7 || hex.length == 9)) {
            Color(android.graphics.Color.parseColor(hex))
        } else {
            defaultColor
        }
    } catch (e: IllegalArgumentException) {
        Log.w("FullScreenNotification", "Invalid color hex: $hex", e)
        defaultColor
    }
}

@AndroidEntryPoint
class FullScreenNotificationActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_MED_DOSAGE = "extra_med_dosage"
        const val EXTRA_MED_COLOR_HEX = "extra_med_color_hex"
        const val EXTRA_MED_TYPE_NAME = "extra_med_type_name"
        private const val TAG = "FullScreenNotification"
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class) // Added annotation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
        val medicationName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medication"
        val medicationDosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: "N/A"
        val medicationColorHex = intent.getStringExtra(EXTRA_MED_COLOR_HEX)
        val medicationTypeName = intent.getStringExtra(EXTRA_MED_TYPE_NAME)

        Log.d(TAG, "Activity created for Reminder ID: $reminderId, Name: $medicationName, Dosage: $medicationDosage, Color: $medicationColorHex, Type: $medicationTypeName")

        val windowSizeClass = calculateWindowSizeClass(this) // Added calculation
        val widthSizeClass = windowSizeClass.widthSizeClass // Added calculation

        setContent {
            AppTheme {
                // medicationColorHex holds the enum name string from the intent
                val medicationColorNameString = medicationColorHex // Renaming for clarity in this block as per instructions

                val medicationColorScheme: com.d4viddf.medicationreminder.ui.colors.MedicationColor = try {
                    com.d4viddf.medicationreminder.ui.colors.MedicationColor.valueOf(
                        medicationColorNameString ?: com.d4viddf.medicationreminder.ui.colors.MedicationColor.BLUE.name // Provide default name if string is null
                    )
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Invalid MedicationColor name: '$medicationColorNameString'. Defaulting to BLUE.")
                    com.d4viddf.medicationreminder.ui.colors.MedicationColor.BLUE 
                }

                val finalBackgroundColor: Color = medicationColorScheme.backgroundColor
                val finalContentColor: Color = medicationColorScheme.textColor 
                Log.d(TAG, "Using MedicationColor enum ${medicationColorScheme.name} for colors (descriptive name: ${medicationColorScheme.colorName})")

                FullScreenNotificationScreen(
                    widthSizeClass = widthSizeClass, // New parameter
                    reminderId = reminderId, 
                    medicationName = medicationName,
                    medicationDosage = medicationDosage,
                    medicationTypeName = medicationTypeName,
                    backgroundColor = finalBackgroundColor,
                    contentColor = finalContentColor,
                    onMarkAsTaken = {
                        Log.i(TAG, "Mark as Taken clicked for Reminder ID: $reminderId")
                        val markTakenIntent = Intent(applicationContext, com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver::class.java).apply {
                            action = com.d4viddf.medicationreminder.notifications.NotificationHelper.ACTION_MARK_AS_TAKEN
                            putExtra(com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
                        }
                        applicationContext.sendBroadcast(markTakenIntent)
                        androidx.core.app.NotificationManagerCompat.from(this@FullScreenNotificationActivity).cancel(reminderId)
                        Toast.makeText(this@FullScreenNotificationActivity, getString(R.string.notification_action_mark_as_taken) + " for " + medicationName, Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onDismiss = {
                        Log.i(TAG, "Dismiss clicked for Reminder ID: $reminderId")
                        if (reminderId != -1) { // Ensure reminderId is valid before cancelling
                           androidx.core.app.NotificationManagerCompat.from(this@FullScreenNotificationActivity).cancel(reminderId)
                        }
                        Toast.makeText(this@FullScreenNotificationActivity, getString(R.string.fullscreen_notification_action_dismiss) + " for " + medicationName, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun MedicationTypeImage(
    typeName: String?,
    // shapeType: com.d4viddf.medicationreminder.ui.components.ShapeType, // REMOVED
    // iconColor: Color, // Removed
    showTick: Boolean,
    modifier: Modifier = Modifier
) {
    val imageResId: Int
    val contentDescResId: Int

    when (typeName?.lowercase(Locale.getDefault())) {
        "pill" -> {
            imageResId = R.drawable.ic_med_pill
            contentDescResId = R.string.medication_type_pill_image_description
        }
        "capsule" -> {
            imageResId = R.drawable.ic_med_capsule
            contentDescResId = R.string.medication_type_capsule_image_description
        }
        "syrup" -> {
            imageResId = R.drawable.ic_med_syrup
            contentDescResId = R.string.medication_type_syrup_image_description
        }
        "injection" -> {
            imageResId = R.drawable.ic_med_injection
            contentDescResId = R.string.medication_type_injection_image_description
        }
        "drops" -> {
            imageResId = R.drawable.ic_med_drops
            contentDescResId = R.string.medication_type_drops_image_description
        }
        "inhaler" -> {
            imageResId = R.drawable.ic_med_inhaler
            contentDescResId = R.string.medication_type_inhaler_image_description
        }
        else -> {
            imageResId = R.drawable.ic_stat_medication // Fallback generic icon
            contentDescResId = R.string.medication_type_other_image_description
        }
    }

    Box(
        modifier = modifier, // This modifier sizes the container for the animated shape and icon
        contentAlignment = Alignment.Center
    ) {
        com.d4viddf.medicationreminder.ui.components.AnimatedShapeBackground(
            // shapeType = shapeType, // shapeType is no longer a parameter here
            modifier = Modifier.fillMaxSize() // Animated shape fills the Box
        )
        if (showTick) {
            Image(
                imageVector = Icons.Filled.Check, // Changed to imageVector
                contentDescription = stringResource(id = R.string.content_description_tick_mark),
                modifier = Modifier.size(120.dp), // Size can remain 120.dp or be adjusted if needed
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
        } else {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = stringResource(id = contentDescResId),
                modifier = Modifier.size(100.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary) // Changed tint
            )
        }
    }
}


@Composable
fun FullScreenNotificationScreen(
    widthSizeClass: WindowWidthSizeClass,
    reminderId: Int, // Kept for potential future use, though not directly used in new layouts
    medicationName: String,
    medicationDosage: String,
    medicationTypeName: String?,
    backgroundColor: Color,
    contentColor: Color,
    onMarkAsTaken: () -> Unit, // This is the one from Activity
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showTick by remember { mutableStateOf(false) }

    val internalOnMarkAsTaken: () -> Unit = {
        scope.launch {
            showTick = true
            delay(1000L)
            onMarkAsTaken() // Call the original onMarkAsTaken from Activity
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        when (widthSizeClass) {
            WindowWidthSizeClass.Compact -> {
                CompactLayout(
                    medicationName = medicationName,
                    medicationDosage = medicationDosage,
                    medicationTypeName = medicationTypeName,
                    contentColor = contentColor,
                    showTick = showTick,
                    onMarkAsTakenClick = internalOnMarkAsTaken,
                    onDismissClick = onDismiss
                )
            }
            else -> { // Medium, Expanded, and any other fallbacks
                LargeScreenLayout(
                    medicationName = medicationName,
                    medicationDosage = medicationDosage,
                    medicationTypeName = medicationTypeName,
                    contentColor = contentColor,
                    showTick = showTick,
                    onMarkAsTakenClick = internalOnMarkAsTaken,
                    onDismissClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun CompactLayout(
    medicationName: String,
    medicationDosage: String,
    medicationTypeName: String?,
    contentColor: Color,
    showTick: Boolean,
    onMarkAsTakenClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp) // Applied spacing
        ) {
            if (!showTick) {
                Text(
                    text = medicationName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = medicationDosage,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp) // Removed bottom padding to use spacedBy
                )
            } else {
                // Potentially a Spacer or an empty Composable to maintain some balance if needed when tick is shown
                // For now, relying on Arrangement.spacedBy and the central MedicationTypeImage
            }

            MedicationTypeImage(
                typeName = medicationTypeName,
                showTick = showTick,
                modifier = Modifier.size(200.dp)
            )

            if (!showTick) {
                SplitButton(
                    primaryActionText = stringResource(id = R.string.fullscreen_notification_action_taken),
                    onPrimaryActionClick = onMarkAsTakenClick,
                    secondaryActions = listOf(
                        stringResource(id = R.string.fullscreen_notification_action_dismiss) to onDismissClick
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true, // The whole SplitButton is conditional on !showTick
                    primaryButtonModifier = Modifier.weight(1f) // Allows primary text to expand
                )
            } else {
                 // Spacer to balance the layout when buttons are hidden, if needed.
                 // For now, the MedicationTypeImage will be centered by the parent Box.
            }
        }
    }
}

@Composable
private fun LargeScreenLayout(
    medicationName: String,
    medicationDosage: String,
    medicationTypeName: String?,
    contentColor: Color,
    showTick: Boolean,
    onMarkAsTakenClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Left Pane
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showTick) {
                Text(
                    text = medicationName,
                    style = MaterialTheme.typography.displaySmall, // Larger style
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp)) // Added Spacer
                Text(
                    text = medicationDosage,
                    style = MaterialTheme.typography.headlineSmall, // Slightly larger style
                    color = contentColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(32.dp)) // Added Spacer
                SplitButton(
                    primaryActionText = stringResource(id = R.string.fullscreen_notification_action_taken),
                    onPrimaryActionClick = onMarkAsTakenClick,
                    secondaryActions = listOf(
                        stringResource(id = R.string.fullscreen_notification_action_dismiss) to onDismissClick
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f), // Adjust width as needed, e.g., 80% of the column
                    enabled = true, // The whole SplitButton is conditional on !showTick
                    primaryButtonModifier = Modifier.weight(1f) // Allows primary text to expand
                )
            } else {
                // Spacer to maintain balance if text/buttons are hidden
                // The MedicationTypeImage on the right will be the main focus.
                // We can add a spacer of equivalent approximate height of the hidden content
                // For simplicity, this is omitted but can be added if visual balance is off.
            }
        }

        // Right Pane
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(), // Fill height for vertical centering of content
            contentAlignment = Alignment.Center
        ) {
            MedicationTypeImage(
                typeName = medicationTypeName,
                showTick = showTick,
                modifier = Modifier.size(300.dp) // Larger size for MedicationTypeImage
            )
        }
    }
}

// Extension function to calculate luminance, useful for determining text color
fun Color.luminance(): Float {
    val red = this.red
    val green = this.green
    val blue = this.blue
    // Formula for luminance (Y)
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

@Preview(name = "Light Mode - Pill")
@Composable
fun FullScreenNotificationScreenPillPreview() {
    AppTheme {
        FullScreenNotificationScreen(
            widthSizeClass = WindowWidthSizeClass.Compact, // Added for preview
            reminderId = 1,
            medicationName = "Ibuprofen",
            medicationDosage = "200 mg",
            medicationTypeName = "Pill",
            backgroundColor = Color(0xFFE0E0E0), // Light Gray
            contentColor = Color.Black,
            onMarkAsTaken = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Dark Mode - Syrup", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FullScreenNotificationScreenSyrupDarkPreview() {
    AppTheme {
        FullScreenNotificationScreen(
            widthSizeClass = WindowWidthSizeClass.Compact, // Added for preview
            reminderId = 2, // Changed from 1 to 2 to match original
            medicationName = "Paracetamol Syrup",
            medicationDosage = "10 ml",
            medicationTypeName = "Syrup",
            backgroundColor = Color(0xFF757575), // Darker Gray
            contentColor = Color.White,
            onMarkAsTaken = {},
            onDismiss = {}
        )
    }
}
