package com.d4viddf.medicationreminder.ui.features.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import coil.compose.AsyncImage // Removed
// import coil.request.ImageRequest // Removed
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem


@Composable
fun NextDoseCard(item: NextDoseUiItem, modifier: Modifier = Modifier) { // Added modifier parameter
    val medicationThemeColor = try {
        MedicationColor.valueOf(item.medicationColorName)
    } catch (e: IllegalArgumentException) {
        MedicationColor.LIGHT_ORANGE // Use a valid default color
    }

    Card(
        shape = RoundedCornerShape(16.dp), // Slightly smaller rounding
        colors = CardDefaults.cardColors(containerColor = medicationThemeColor.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Add some elevation
        modifier = modifier // Apply the passed modifier
            .height(200.dp) // Keep fixed height or make it adaptive too if needed
            // .width(150.dp) // Width is now controlled by the incoming modifier or defaults if not overridden
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Increased padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Center content vertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.medication_filled), // Static placeholder
                contentDescription = item.medicationName,
                modifier = Modifier
                    .size(64.dp) // Adjusted size
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop, // Crop might look better for a fixed size
                colorFilter = ColorFilter.tint(medicationThemeColor.textColor)
            )

            Spacer(modifier = Modifier.height(12.dp)) // Increased spacing

            Text(
                text = item.medicationName.split(" ").firstOrNull() ?: item.medicationName, // Display only the first word
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp), // Adjusted style
                fontWeight = FontWeight.Bold,
                color = medicationThemeColor.textColor,
                textAlign = TextAlign.Center,
                maxLines = 1, // Ensure it stays on one line
                overflow = TextOverflow.Clip // Clip if it somehow still overflows (e.g. very long first word)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.medicationDosage,
                style = MaterialTheme.typography.bodySmall, // Adjusted style
                color = medicationThemeColor.textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp)) // Spacing before time

            Text(
                text = item.formattedReminderTime,
                style = MaterialTheme.typography.labelMedium, //ชัดเจนขึ้น / More distinct style for time
                fontWeight = FontWeight.Medium,
                color = medicationThemeColor.textColor.copy(alpha = 0.8f), // Slightly muted color for time
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NextDoseCardPreview() {
    MaterialTheme {
        NextDoseCard(
            item = NextDoseUiItem(
                reminderId = 1,
                medicationId = 101,
                medicationName = "Metformin Long Name Example",
                medicationDosage = "500 mg Tablet",
                medicationColorName = "LIGHT_BLUE",
                medicationImageUrl = null, // Example: no image URL for this preview
                rawReminderTime = "2023-01-01T09:00:00",
                formattedReminderTime = "09:00"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NextDoseCardDarkPreview() {
    MaterialTheme {
        NextDoseCard(
            item = NextDoseUiItem(
                reminderId = 2,
                medicationId = 102,
                medicationName = "Lisinopril",
                medicationDosage = "10 mg",
                medicationColorName = "LIGHT_RED",
                medicationImageUrl = "https://example.com/lisinopril.png", // Example with an image URL
                rawReminderTime = "2023-01-01T15:30:00",
                formattedReminderTime = "15:30"
            )
        )
    }
}
