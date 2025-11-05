package com.d4viddf.medicationreminder.ui.features.medication.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterControls(
    sortAscending: Boolean,
    onSortOrderChange: (Boolean) -> Unit,
    onDateFilterSelected: () -> Unit,
    isDateFilterActive: Boolean,
    onClearDateFilter: () -> Unit,
    dateFilterLabel: String?,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                selected = isDateFilterActive,
                onClick = onDateFilterSelected,
                label = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                    )
                    {
                        Text(dateFilterLabel ?: stringResource(R.string.filter_by_date))

                    }
                },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingIcon = {
                    if (isDateFilterActive) {
                        IconButton(onClick = onClearDateFilter) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_filter))
                        }
                    }
                }
            )
        }
        item {
            FilterChip(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                selected = false,
                onClick = { onSortOrderChange(!sortAscending) },
                label = {
                    Text(
                        text = if (sortAscending) stringResource(R.string.sort_by_oldest)
                        else stringResource(R.string.sort_by_newest)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = stringResource(R.string.sort_order)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun FilterControlsPreview() {
    var sortAscending by remember { mutableStateOf(true) }
    var isDateFilterActive by remember { mutableStateOf(false) }
    var dateFilterLabel by remember { mutableStateOf<String?>(null) }

    FilterControls(
        sortAscending = sortAscending,
        onSortOrderChange = { sortAscending = it },
        onDateFilterSelected = {
            isDateFilterActive = true
            dateFilterLabel = "Jan 1, 2023"
        },
        isDateFilterActive = isDateFilterActive,
        onClearDateFilter = {
            isDateFilterActive = false
            dateFilterLabel = null
        },
        dateFilterLabel = dateFilterLabel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun FilterControlsDateActivePreview() {
    FilterControls(
        sortAscending = false,
        onSortOrderChange = {},
        onDateFilterSelected = {},
        isDateFilterActive = true,
        onClearDateFilter = {},
        dateFilterLabel = "Dec 25, 2023"
    )
}

