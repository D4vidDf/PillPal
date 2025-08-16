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
        horizontalArrangement = if (widthSizeClass == WindowWidthSizeClass.Compact) Arrangement.SpaceBetween else Arrangement.Start
    ) {
        Row {
            IconButton(onClick = onPreviousClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
            }
            if (widthSizeClass != WindowWidthSizeClass.Compact) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = onNextClick) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
            }
        }
        Text(
            text = dateRange,
            modifier = Modifier
                .weight(if (widthSizeClass == WindowWidthSizeClass.Compact) 1f else 0f)
                .clickable(onClick = onDateRangeClick)
                .padding(start = if (widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
            textAlign = if (widthSizeClass == WindowWidthSizeClass.Compact) TextAlign.Center else TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
