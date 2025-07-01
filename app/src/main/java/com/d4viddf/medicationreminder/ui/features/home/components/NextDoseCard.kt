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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem


@Composable
fun NextDoseCard(item: NextDoseUiItem) {
    val medicationThemeColor = try {
        MedicationColor.valueOf(item.medicationColorName)
    } catch (e: IllegalArgumentException) {
        MedicationColor.LIGHT_ORANGE // Use a valid default color
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = medicationThemeColor.backgroundColor),
        modifier = Modifier
            .height(180.dp)
            .fillMaxWidth() // Changed from .width(160.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.medicationImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.medicationImageUrl)
                        .crossfade(true)
                        .error(R.drawable.medication_filled) // Replace with your actual default/error drawable
                        .placeholder(R.drawable.medication_filled) // Replace with your actual placeholder
                        .build(),
                    contentDescription = item.medicationName, // Accessibility
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                    alpha = 0.15f,
                    colorFilter = ColorFilter.tint(medicationThemeColor.textColor)
                )
            } else {
                // Optional: Show a default local icon if imageUrl is null/blank
                 Image(
                    painter = painterResource(id = R.drawable.medication_filled), // Generic fallback
                    contentDescription = item.medicationName,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                    alpha = 0.15f,
                    colorFilter = ColorFilter.tint(medicationThemeColor.textColor)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = item.formattedReminderTime,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = medicationThemeColor.textColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = item.medicationName.split(" ").firstOrNull() ?: item.medicationName, // Display only the first word
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = medicationThemeColor.textColor,
                    // Removed maxLines and overflow to show only the first word as is
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.medicationDosage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = medicationThemeColor.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )


            }
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
