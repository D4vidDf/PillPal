package com.d4viddf.medicationreminder.ui.features.healthdata.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DateRangeSelector(
    dateRange: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onDateRangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
        }
        Text(
            text = dateRange,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onDateRangeClick),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNextClick) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Next")
        }
    }
}
