package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StockReminderScroller(
    title: String,
    subtitle: String,
    selectedDays: Int,
    range: IntRange,
    onDaysChanged: (Int) -> Unit,
    onNumberClick: () -> Unit
) {
    val listState = rememberLazyListState()

    // This derived state is the source of truth for which item is physically in the center.
    // It uses a simple and robust calculation.
    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                -1
            } else {
                val viewportCenter = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                visibleItems.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: -1
            }
        }
    }

    // This effect triggers ONLY when scrolling has finished.
    // It then syncs the external state (`selectedDays`) with the item that landed in the center.
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && centeredIndex != -1) {
            val centeredDay = range.first + centeredIndex
            if (centeredDay != selectedDays) {
                onDaysChanged(centeredDay)
            }
        }
    }

    // This effect triggers when the external state (`selectedDays`) changes.
    // This happens on initial load (async) and when the dialog is used.
    // It scrolls the list to the correct position. This will not cause a loop because
    // the effect above ignores changes while a scroll is in progress and does nothing
    // if the final centered item already matches the state.
    LaunchedEffect(selectedDays) {
        val targetIndex = (selectedDays - range.first).coerceIn(0, range.count() - 1)
        listState.animateScrollToItem(targetIndex)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNumberClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    // Padding must be half the height of the container to allow first and last items to be centered.
                    contentPadding = PaddingValues(vertical = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
                ) {
                    items(range.count()) { index ->
                        val day = range.first + index
                        // The "selected" item is the one currently in the center.
                        val isSelected = index == centeredIndex

                        Text(
                            text = day.toString(),
                            fontSize = if (isSelected) 48.sp else 32.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
