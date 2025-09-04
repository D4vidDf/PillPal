package com.d4viddf.medicationreminder.ui.features.healthdata.component

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange

@Composable
fun DateRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(selectedTabIndex = selectedRange.ordinal, modifier = modifier) {
        TimeRange.values().forEach { range ->
            Tab(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                text = { Text(text = stringResource(id = range.toStringResource())) }
            )
        }
    }
}

@Composable
private fun TimeRange.toStringResource(): Int {
    return when (this) {
        TimeRange.DAY -> R.string.time_range_day
        TimeRange.WEEK -> R.string.time_range_week
        TimeRange.MONTH -> R.string.time_range_month
        TimeRange.YEAR -> R.string.time_range_year
    }
}
