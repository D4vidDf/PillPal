package com.d4viddf.medicationreminder.ui.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NextDoseCard(
    item: NextDoseUiItem,
    modifier: Modifier = Modifier,
    onNavigateToDetails: (medicationId: Int) -> Unit
) {
    val medicationThemeColor = try {
        MedicationColor.valueOf(item.medicationColorName)
    } catch (e: IllegalArgumentException) {
        MedicationColor.LIGHT_ORANGE // A valid default color
    }

    val shapes = remember {
        listOf(
            MaterialShapes.Cookie12Sided,
            MaterialShapes.VerySunny,
            MaterialShapes.Sunny,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Clover8Leaf
        )
    }
    val randomShape = remember { shapes.random() }

    val cardContentDescription = stringResource(
        R.string.next_dose_card_cd,
        item.medicationName,
        item.medicationDosage,
        item.formattedReminderTime
    )

    Card(
        colors = CardDefaults.cardColors(),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onNavigateToDetails(item.medicationId) }
            .semantics { contentDescription = cardContentDescription }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Pushes content to the top and bottom edges
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // This Box remains at the top
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(randomShape.toShape())
                    .background(medicationThemeColor.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.medication_filled),
                    contentDescription = item.medicationName,
                    modifier = Modifier.size(60.dp),
                    tint = medicationThemeColor.textColor
                )
            }

            // The Row with name and time is now at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.medicationName.split(" ").first(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = item.formattedReminderTime,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NextDoseCardPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 220.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            NextDoseCard(
                item = NextDoseUiItem(
                    reminderId = 1,
                    medicationId = 101,
                    medicationName = "Metformin",
                    medicationDosage = "500 mg Tablet",
                    medicationColorName = "LIGHT_BLUE",
                    medicationImageUrl = null,
                    rawReminderTime = "2023-01-01T09:00:00",
                    formattedReminderTime = "09:00",
                ),
                onNavigateToDetails = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NextDoseCardSecondaryPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 220.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            NextDoseCard(
                item = NextDoseUiItem(
                    reminderId = 2,
                    medicationId = 102,
                    medicationName = "Ibuprofen Extra",
                    medicationDosage = "200 mg",
                    medicationColorName = "LIGHT_RED",
                    medicationImageUrl = null,
                    rawReminderTime = "2023-01-01T12:00:00",
                    formattedReminderTime = "12:00",
                ),
                onNavigateToDetails = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}