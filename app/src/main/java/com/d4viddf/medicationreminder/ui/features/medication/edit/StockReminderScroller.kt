package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StockReminderScroller(
    selectedDays: Int,
    range: IntRange,
    onDaysChanged: (Int) -> Unit,
    onNumberClick: () -> Unit,
    labelFormatter: (Int) -> String = { it.toString() }
) {
    val listState = rememberLazyListState()

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                -1
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: -1
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && centeredIndex != -1) {
            val centeredDay = range.first + centeredIndex
            if (centeredDay != selectedDays) {
                onDaysChanged(centeredDay)
            }
        }
    }

    LaunchedEffect(selectedDays) {
        val targetIndex = (selectedDays - range.first).coerceIn(0, range.count() - 1)
        if (targetIndex != centeredIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNumberClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.height(176.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            ) {
                items(range.count()) { index ->
                    val day = range.first + index
                    val isSelected = index == centeredIndex
                    Text(
                        text = labelFormatter(day),
                        fontSize = if (isSelected) 44.sp else 32.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun StockReminderScrollerPreview() {
    var selectedDays by remember { mutableStateOf(5) }
    StockReminderScroller(
        selectedDays = selectedDays,
        range = 1..20,
        onDaysChanged = {
            selectedDays = it
        },
        onNumberClick = {}
    )
}
