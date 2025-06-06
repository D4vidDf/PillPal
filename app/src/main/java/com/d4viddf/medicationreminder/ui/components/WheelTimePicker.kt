package com.d4viddf.medicationreminder.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import java.time.LocalTime
import kotlin.math.absoluteValue

@Composable
fun WheelTimePicker(
    modifier: Modifier = Modifier,
    startTime: LocalTime = LocalTime.now(),
    minTime: LocalTime = LocalTime.MIN,
    maxTime: LocalTime = LocalTime.MAX,
    size: DpSize = DpSize(128.dp, 180.dp),
    rowCount: Int = 3,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    textColor: Color = Color.White,
    onSnappedTime: (LocalTime) -> Unit = {}
) {
    var snappedTime by remember { mutableStateOf(startTime) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Hour Wheel Picker
            WheelPicker(
                modifier = Modifier.size(width = size.width / 5*2, height = size.height),
                count = 24,
                rowCount = rowCount,
                startIndex = startTime.hour,
                size = DpSize(size.width / 5 *2, size.height),
                onScrollFinished = { snappedIndex ->
                    val newHour = snappedIndex
                    val newTime = snappedTime.withHour(newHour)
                    if (!newTime.isBefore(minTime) && !newTime.isAfter(maxTime)) {
                        snappedTime = newTime
                    }
                    onSnappedTime(snappedTime)
                    newHour
                },
                content = { index ->
                    val isSelected = index == snappedTime.hour
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(size.height / rowCount)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.Gray.copy(alpha = 0.3f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString().padStart(2, '0'),
                            style = textStyle,
                            color = textColor,
                            maxLines = 1,
                            fontSize = if (isSelected) 28.sp else 24.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            )

            // Separator
            Text(
                text = ":",
                fontSize = 36.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Minute Wheel Picker
            WheelPicker(
                modifier = Modifier.size(width = size.width / 5*2, height = size.height),
                count = 60,
                rowCount = rowCount,
                startIndex = startTime.minute,
                size = DpSize(size.width / 5*2, size.height),
                onScrollFinished = { snappedIndex ->
                    val newMinute = snappedIndex
                    val newTime = snappedTime.withMinute(newMinute)
                    if (!newTime.isBefore(minTime) && !newTime.isAfter(maxTime)) {
                        snappedTime = newTime
                    }
                    onSnappedTime(snappedTime)
                    newMinute
                },
                content = { index ->
                    val isSelected = index == snappedTime.minute
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(size.height / rowCount)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.Gray.copy(alpha = 0.3f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString().padStart(2, '0'),
                            style = textStyle,
                            color = textColor,
                            maxLines = 1,
                            fontSize = if (isSelected) 28.sp else 24.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun WheelPicker(
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    count: Int,
    rowCount: Int,
    size: DpSize = DpSize(128.dp, 180.dp),
    onScrollFinished: (snappedIndex: Int) -> Int,
    content: @Composable LazyItemScope.(index: Int) -> Unit,
) {
    val lazyListState = rememberLazyListState(startIndex)
    val snapperFlingBehavior = rememberSnapperFlingBehavior(lazyListState = lazyListState)
    val isScrollInProgress = lazyListState.isScrollInProgress

    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress) {
            val currentItemIndex = lazyListState.firstVisibleItemIndex
            onScrollFinished(currentItemIndex)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier
                .height(size.height)
                .width(size.width),
            state = lazyListState,
            flingBehavior = snapperFlingBehavior,
            contentPadding = PaddingValues(vertical = size.height / rowCount * ((rowCount - 1) / 2))
        ) {
            items(count) { index ->
                val alpha = calculateAnimatedAlpha(
                    lazyListState = lazyListState,
                    index = index,
                    rowCount = rowCount
                )

                Box(
                    modifier = Modifier
                        .height(size.height / rowCount)
                        .width(size.width)
                        .graphicsLayer { this.alpha = alpha },
                    contentAlignment = Alignment.Center
                ) {
                    content(index)
                }
            }
        }
    }
}

@Composable
private fun calculateAnimatedAlpha(
    lazyListState: LazyListState,
    index: Int,
    rowCount: Int
): Float {
    val distanceToIndexSnap = (lazyListState.firstVisibleItemIndex - index).absoluteValue
    val layoutInfo = lazyListState.layoutInfo
    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
    val singleViewportHeight = viewportHeight / rowCount

    return if (distanceToIndexSnap in 0..singleViewportHeight.toInt()) {
        1.2f - (distanceToIndexSnap / singleViewportHeight)
    } else {
        0.2f
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "WheelTimePicker Preview")
@Composable
fun WheelTimePickerPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) { // Use Surface for background
            WheelTimePicker(
                startTime = LocalTime.of(10, 30),
                onSnappedTime = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "WheelPicker Preview")
@Composable
fun WheelPickerPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) { // Use Surface for background
            WheelPicker(
                count = 10,
                rowCount = 3,
                startIndex = 5,
                onScrollFinished = { it },
                content = { index ->
                    Text(
                        text = "Item $index",
                        color = Color.Black, // Ensure text is visible on default Surface
                        modifier = Modifier.padding(8.dp)
                    )
                }
            )
        }
    }
}
