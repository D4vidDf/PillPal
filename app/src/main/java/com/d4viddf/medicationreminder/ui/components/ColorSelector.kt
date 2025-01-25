package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.colors.medicationColors

@Composable
fun ColorSelector(
    selectedColor: MedicationColor,
    onColorSelected: (MedicationColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = medicationColors // Use the MedicationColors enum

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Select Color",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            colors.forEach { color ->
                val borderColor = if (color == selectedColor) MaterialTheme.colorScheme.primary else Color.Transparent
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(4.dp, borderColor, CircleShape)
                        .background(color.backgroundColor, shape = CircleShape)
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}