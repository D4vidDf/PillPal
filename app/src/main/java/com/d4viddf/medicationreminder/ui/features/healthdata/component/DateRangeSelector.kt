package com.d4viddf.medicationreminder.ui.features.healthdata.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.ui.features.healthdata.util.DateRangeText
import com.d4viddf.medicationreminder.ui.theme.AppTheme

@Composable
fun DateRangeSelector(
    dateRange: DateRangeText?,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    isNextEnabled: Boolean,
    onDateRangeClick: () -> Unit,
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp).background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (widthSizeClass == WindowWidthSizeClass.Compact) {
            // Layout for small screens: [<] [Date] [>]
            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous"
                )
            }
            val text = when (dateRange) {
                is DateRangeText.StringResource -> stringResource(id = dateRange.resId)
                is DateRangeText.FormattedString -> dateRange.text
                null -> ""
            }
            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onDateRangeClick),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onNextClick, enabled = isNextEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next"
                )
            }
        } else {
            // Layout for larger screens: [<][>] [Date]
            Row {
                IconButton(onClick = onPreviousClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous"
                    )
                }
                IconButton(onClick = onNextClick, enabled = isNextEnabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next"
                    )
                }
            }
            val text = when (dateRange) {
                is DateRangeText.StringResource -> stringResource(id = dateRange.resId)
                is DateRangeText.FormattedString -> dateRange.text
                null -> ""
            }
            Text(
                text = text,
                modifier = Modifier
                    .clickable(onClick = onDateRangeClick)
                    .padding(start = 16.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DateRangeSelectorPreviewCompact() {
    AppTheme {
        DateRangeSelector(
            dateRange = DateRangeText.FormattedString("Today"),
            onPreviousClick = {},
            onNextClick = {},
            isNextEnabled = false,
            onDateRangeClick = {},
            widthSizeClass = WindowWidthSizeClass.Compact
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DateRangeSelectorPreviewMedium() {
    AppTheme {
        DateRangeSelector(
            dateRange = DateRangeText.FormattedString("This Week"),
            onPreviousClick = {},
            onNextClick = {},
            isNextEnabled = true,
            onDateRangeClick = {},
            widthSizeClass = WindowWidthSizeClass.Medium
        )
    }
}
