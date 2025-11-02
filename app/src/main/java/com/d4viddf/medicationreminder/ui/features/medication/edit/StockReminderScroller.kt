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

    // Scroll to the initial position when the composable is first launched
    // or when the selectedDays value changes from an external source (like the dialog).
    LaunchedEffect(selectedDays) {
        val indexToScroll = (selectedDays - range.first).coerceIn(0, range.count() - 1)
        listState.animateScrollToItem(indexToScroll)
    }

    // This derived state will calculate the centered item based on your provided logic.
    val centeredDayIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                // If nothing is visible, return an invalid index
                -1
            } else {
                // Your centering logic
                val viewportCenter = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 3
                val centerItem = visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 0.8f) - viewportCenter) }
                centerItem?.index ?: -1
            }
        }
    }

    // This effect will run only when the user stops scrolling.
    // It checks if the centered item is different from the current state and updates it.
    // This avoids the infinite loop.
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && centeredDayIndex != -1) {
            val newDay = range.first + centeredDayIndex
            if (newDay != selectedDays) {
                onDaysChanged(newDay)
            }
        }
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
                modifier = Modifier.height(172.dp),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
                ) {
                    items(range.count()) { index ->
                        val day = range.first + index
                        val isSelected = (centeredDayIndex == -1 && day == selectedDays) || centeredDayIndex == index
                        Text(
                            text = day.toString(),
                            fontSize = if (isSelected) 46.sp else 30.sp,
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
