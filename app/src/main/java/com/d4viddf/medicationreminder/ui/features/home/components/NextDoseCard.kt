package com.d4viddf.medicationreminder.ui.features.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem


@Composable
fun NextDoseCard(item: NextDoseUiItem) {
    val medicationThemeColor = try {
        MedicationColor.valueOf(item.medicationColorName)
    } catch (e: IllegalArgumentException) {
        MedicationColor.LIGHT_GREY // Default color
    }

    Card(
        shape = RoundedCornerShape(16.dp), // Slightly more rounded
        colors = CardDefaults.cardColors(containerColor = medicationThemeColor.backgroundColor),
        modifier = Modifier
            .height(180.dp) // Increased height
            .width(160.dp)  // Increased width
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = item.medicationTypeIconRes),
                contentDescription = item.medicationName, // Accessibility
                modifier = Modifier
                    .size(100.dp) // Adjust size of the icon
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp)), // Rounded corners for the image itself
                contentScale = ContentScale.Fit,
                alpha = 0.15f, // Make it a bit like a watermark
                colorFilter = ColorFilter.tint(medicationThemeColor.textColor) // Tint with text color
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, // Center text content
                verticalArrangement = Arrangement.SpaceAround // Distribute space
            ) {
                Text(
                    text = item.medicationName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), // Larger name
                    fontWeight = FontWeight.Bold,
                    color = medicationThemeColor.textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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
                Spacer(modifier = Modifier.weight(1f)) // Pushes time to the bottom
                Text(
                    text = item.formattedReminderTime,
                    style = MaterialTheme.typography.bodyLarge, // Make time a bit more prominent
                    fontWeight = FontWeight.SemiBold,
                    color = medicationThemeColor.textColor,
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
                medicationTypeIconRes = R.drawable.ic_medication_pill_solid, // Example icon
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
                medicationTypeIconRes = R.drawable.ic_medication_capsule_solid, // Example icon
                formattedReminderTime = "15:30"
            )
        )
    }
}
