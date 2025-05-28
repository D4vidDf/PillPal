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
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.math.abs // For abs function
import androidx.compose.ui.graphics.ColorFilter // For ColorFilter.tint
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch // Added import
import kotlinx.coroutines.delay // Added import
import androidx.compose.material.icons.Icons // Added import for Material Icons
import androidx.compose.material.icons.filled.Check // Added import for Check icon

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
        val medicationName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medication"
        val medicationDosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: "N/A"
        val medicationColorHex = intent.getStringExtra(EXTRA_MED_COLOR_HEX)
        val medicationTypeName = intent.getStringExtra(EXTRA_MED_TYPE_NAME)

        Log.d(TAG, "Activity created for Reminder ID: $reminderId, Name: $medicationName, Dosage: $medicationDosage, Color: $medicationColorHex, Type: $medicationTypeName")

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
                    reminderId = reminderId, // Add this
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
    reminderId: Int, // Add this
    medicationName: String,
    medicationDosage: String,
    medicationTypeName: String?,
    backgroundColor: Color,
    contentColor: Color, // For text and icons to contrast with backgroundColor
    onMarkAsTaken: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope() // Added scope
    var showTick by remember { mutableStateOf(false) }
    // val shapeTypes = com.d4viddf.medicationreminder.ui.components.ShapeType.entries.toTypedArray() // REMOVED
    // val currentShapeType = shapeTypes[kotlin.math.abs(reminderId.hashCode()) % shapeTypes.size] // REMOVED

    val internalOnMarkAsTaken: () -> Unit = { // Explicitly typed
        scope.launch { // Launch coroutine
            showTick = true
            delay(1000L) // Delay for 1 second
            onMarkAsTaken() // Call original lambda
        }
    }

    Surface( // Use Surface as the root to easily set background color
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            if (!showTick) {
                Spacer(modifier = Modifier.weight(0.2f)) // Less weight at top

                Text(
                    text = medicationName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp) // Allow space if name is long
                )

                Text(
                    text = medicationDosage,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor.copy(alpha = 0.8f), // Slightly less prominent than name
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                )
            } else {
                 Spacer(modifier = Modifier.weight(0.2f)) // Maintain some spacing at the top
            }

            MedicationTypeImage(
                typeName = medicationTypeName,
                // shapeType = currentShapeType, // REMOVED
                // iconColor = contentColor, // Removed argument
                showTick = showTick,
                modifier = Modifier.size(200.dp)
            )

            if (!showTick) {
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp) // Space between buttons
                ) {
                    Button(
                        onClick = internalOnMarkAsTaken, // Use the new internal lambda
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium // Standard button shape
                    ) {
                        Text(stringResource(id = R.string.fullscreen_notification_action_taken), style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium, // Standard button shape
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer, // Contrasting color
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(stringResource(id = R.string.fullscreen_notification_action_dismiss), style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f)) // Ensure tick animation remains centered
            }
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
            reminderId = 2,
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
