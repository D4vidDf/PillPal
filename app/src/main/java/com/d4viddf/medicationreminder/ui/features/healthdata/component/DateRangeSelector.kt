package com.d4viddf.medicationreminder.ui.features.healthdata.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
            }
            IconButton(onClick = onNextClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }
        if (widthSizeClass == WindowWidthSizeClass.Compact) {
            Text(
                text = dateRange,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onDateRangeClick),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = dateRange,
                modifier = Modifier
                    .clickable(onClick = onDateRangeClick)
                    .padding(start = 16.dp),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
