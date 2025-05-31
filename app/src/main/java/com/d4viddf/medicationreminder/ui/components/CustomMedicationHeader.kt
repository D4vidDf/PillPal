package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator // Added for future use
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer // Added import for alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.R

@Composable
fun CustomMedicationHeader(
    modifier: Modifier = Modifier,
    medicationName: String,
    medicationTypeAndDosage: String,
    progressValue: Float,
    progressText: String,
    counter1Label: String,
    counter1Value: String,
    counter2Label: String,
    counter2Value: String,
    headerBackgroundColor: Color,
    contentColor: Color,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    scrollProgress: Float // New parameter
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Apply alpha to the entire header based on scrollProgress for fade-out effect
            // This is a simple way; more granular alpha can be applied to individual elements if needed.
            // For this iteration, let's apply to the main content column excluding the buttons if they are to remain
            // or be replaced by MinimalStickyAppBar buttons.
            // The prompt suggests fading out the large header elements including its buttons.
            .graphicsLayer { alpha = 1f - scrollProgress }
            .background(
                color = headerBackgroundColor,
                shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
            )
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 16.dp)
    ) {
        // Row for Back and Edit buttons - this will also fade with the main header
        Row(
            modifier = Modifier
                .fillMaxWidth(),
                // .graphicsLayer { alpha = 1f - scrollProgress }, // Alpha applied to parent Column
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(id = R.string.back), // Using R.string.back as per common practice
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onEdit() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.edit),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main content that fades (name, dosage, progress, counters)
        // Wrapping these in a Column to apply alpha collectively if buttons have separate fade logic
        // However, the prompt implies the whole CustomMedicationHeader (including its buttons) fades.
        // So alpha on the root Column of CustomMedicationHeader is appropriate.

        Text(
            text = medicationName,
            style = MaterialTheme.typography.headlineMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = medicationTypeAndDosage,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (progressValue >= 0 && progressValue <= 1) {
             LinearProgressIndicator(
                 progress = { progressValue },
                 modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                 color = contentColor,
                 trackColor = contentColor.copy(alpha = 0.3f)
             )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = progressText,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            CounterItem(label = counter1Label, value = counter1Value, textColor = contentColor)
            CounterItem(label = counter2Label, value = counter2Value, textColor = contentColor)
        }
    }
}

@Composable
private fun CounterItem(label: String, value: String, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = textColor, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f))
    }
}
