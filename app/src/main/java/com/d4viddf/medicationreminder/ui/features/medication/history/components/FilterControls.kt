package com.d4viddf.medicationreminder.ui.features.medication.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedFilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterControls(
    sortAscending: Boolean,
    onSortOrderChange: (Boolean) -> Unit,
    onDateFilterSelected: () -> Unit,
    onAllTimeSelected: () -> Unit,
    onLastWeekSelected: () -> Unit,
    onLast30DaysSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDateFilterMenu by remember { mutableStateOf(false) }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedFilterChip(
                selected = true,
                onClick = { showDateFilterMenu = true },
                label = { Text(stringResource(R.string.filter_by_date)) },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
            )
            DropdownMenu(
                expanded = showDateFilterMenu,
                onDismissRequest = { showDateFilterMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.all_time)) },
                    onClick = {
                        onAllTimeSelected()
                        showDateFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.last_week)) },
                    onClick = {
                        onLastWeekSelected()
                        showDateFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.last_30_days)) },
                    onClick = {
                        onLast30DaysSelected()
                        showDateFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.custom_range)) },
                    onClick = {
                        onDateFilterSelected()
                        showDateFilterMenu = false
                    }
                )
            }
        }
        item {
            OutlinedFilterChip(
                selected = true,
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
