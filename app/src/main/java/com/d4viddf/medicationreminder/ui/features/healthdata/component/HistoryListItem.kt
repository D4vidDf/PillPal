package com.d4viddf.medicationreminder.ui.features.healthdata.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.ui.theme.Dimensions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryListItem(
    index: Int,
    size: Int,
    date: LocalDate,
    value: String,
    sourceApp: String?,
    onClick: () -> Unit
) {
    val shape = when {
        size == 1 -> RoundedCornerShape(Dimensions.PaddingMedium)
        index == 0 -> RoundedCornerShape(topStart = Dimensions.PaddingMedium, topEnd = Dimensions.PaddingMedium)
        index == size - 1 -> RoundedCornerShape(bottomStart = Dimensions.PaddingMedium, bottomEnd = Dimensions.PaddingMedium)
        else -> RoundedCornerShape(0.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clickable(onClick = onClick),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.PaddingLarge)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
                )
                if (sourceApp != null && sourceApp != "com.d4viddf.medicationreminder") {
                    Text(
                        text = "from $sourceApp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(text = value)
        }
    }
}
