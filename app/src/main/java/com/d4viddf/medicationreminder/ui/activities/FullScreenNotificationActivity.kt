package com.d4viddf.medicationreminder.ui.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.content.Intent // Added import
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Corrected import
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

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
            AppTheme { // Corrected theme name
                val defaultBackgroundColor = MaterialTheme.colorScheme.background
                val backgroundColor = parseColor(medicationColorHex, defaultBackgroundColor)
                // Determine if the background is dark or light for text contrast
                val isDarkBackground = backgroundColor.luminance() < 0.5f
                val contentColor = if (isDarkBackground) Color.White else Color.Black

                FullScreenNotificationScreen(
                    medicationName = medicationName,
                    medicationDosage = medicationDosage,
                    medicationTypeName = medicationTypeName,
                    backgroundColor = backgroundColor,
                    contentColor = contentColor,
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
fun MedicationTypeImage(typeName: String?, modifier: Modifier = Modifier) {
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
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50)) // Circular shape
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) // Contrasting background for the shape
            .padding(24.dp), // Padding inside the circle, around the image
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = stringResource(id = contentDescResId),
            modifier = Modifier.size(100.dp) // Adjust size of the actual image if needed
        )
    }
}


@Composable
fun FullScreenNotificationScreen(
    medicationName: String,
    medicationDosage: String,
    medicationTypeName: String?,
    backgroundColor: Color,
    contentColor: Color, // For text and icons to contrast with backgroundColor
    onMarkAsTaken: () -> Unit,
    onDismiss: () -> Unit
) {
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

            MedicationTypeImage(
                typeName = medicationTypeName,
                modifier = Modifier.size(200.dp) // Size of the circular shape container
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Space between buttons
            ) {
                Button(
                    onClick = onMarkAsTaken,
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
