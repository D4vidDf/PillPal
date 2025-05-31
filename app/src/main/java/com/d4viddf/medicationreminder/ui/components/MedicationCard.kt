package com.d4viddf.medicationreminder.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.text.style.TextOverflow // No longer needed
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.ui.colors.MedicationColor

@Composable
fun MedicationCard(
    medication: Medication,
    onClick: () -> Unit // Callback for navigation
) {
    val color = try {
        MedicationColor.valueOf(medication.color)
    } catch (e: IllegalArgumentException) {
        Log.w("MedicationCard", "Invalid color string: '${medication.color}' for medication '${medication.name}'. Defaulting.", e)
        MedicationColor.LIGHT_ORANGE
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }, // Trigger navigation on click
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            // horizontalArrangement = Arrangement.SpaceBetween, // Removed for weight-based layout
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(2f) // Text part takes 2/3 of the space
            ) {
                val displayName = medication.name.split(" ").take(3).joinToString(" ")
                Text(
                    text = displayName, // Use processed name
                    style = MaterialTheme.typography.headlineSmall,
                    color= color.textColor,
                    fontWeight = FontWeight.Bold
                    // Removed maxLines = 1
                    // Removed overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${medication.dosage}",
                    style = MaterialTheme.typography.bodyLarge,
                    color= color.textColor
                )
                if (medication.reminderTime != null) {
                    Text(
                        text = "Time: ${medication.reminderTime}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Medication avatar at the end
            MedicationAvatar(color = Color.White)
        }
    }
}

@Composable
fun MedicationAvatar(color: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = color, shape = CircleShape)
    )
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationCardPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        MedicationCard(
            medication = Medication(
                id = 1,
                name = "Amoxicillin Long Name For Testing Ellipsis",
                dosage = "250mg",
                color = "LIGHT_BLUE", // Assuming MedicationColor.LIGHT_BLUE exists
                reminderTime = "10:00 AM",
                // Updated parameters as per request
                typeId = 1, // Default value
                packageSize = 0,       // Default value
                remainingDoses = 0,    // Default value
                startDate = null,      // Default value
                endDate = null         // Default value
            ),
            onClick = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationAvatarPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        MedicationAvatar(color = Color.White)
    }
}
