package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

    val selectedDay by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                selectedDays
            } else {
                val viewportCenter = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val centerItem = visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                centerItem?.index?.coerceIn(range.first, range.last) ?: selectedDays
            }
        }
    }

    LaunchedEffect(selectedDay) {
        if (selectedDay != selectedDays) {
            onDaysChanged(selectedDay)
        }
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem(selectedDays)
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
                    contentPadding = PaddingValues(vertical = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
                ) {
                    items(range.last + 1) { index ->
                        val day = index
                        Text(
                            text = day.toString(),
                            fontSize = if (selectedDay == day) 48.sp else 32.sp,
                            color = if (selectedDay == day) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
