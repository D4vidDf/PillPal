package com.d4viddf.medicationreminder.ui.features.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun HealthDataChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Blue // You can customize this
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxValue = data.maxOfOrNull { it.value } ?: 0f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp) // Or any height you prefer
            .pointerInput(data) { // Relaunch gesture detection if data changes
                detectDragGestures(
                    onDragStart = { offset ->
                        // Find the bar corresponding to the initial touch offset
                        val barWidth = size.width / data.size
                        val index = (offset.x / barWidth).toInt().coerceIn(0, data.lastIndex)
                        selectedIndex = index
                    },
                    onDrag = { change, _ ->
                        // Update selected bar as the user drags their finger
                        val barWidth = size.width / data.size
                        val index = (change.position.x / barWidth).toInt().coerceIn(0, data.lastIndex)
                        selectedIndex = index
                    },
                    onDragEnd = { selectedIndex = null }, // Clear selection when finger is lifted
                    onDragCancel = { selectedIndex = null }
                )
            }
    ) {
        val barWidth = size.width / data.size
        val spaceBetweenBars = barWidth * 0.2f // 20% of bar width is space

        data.forEachIndexed { index, dataPoint ->
            val barHeight = if (maxValue > 0) (dataPoint.value / maxValue) * size.height else 0f
            val barX = index * barWidth

            // Draw the bar
            drawRect(
                color = barColor,
                topLeft = Offset(x = barX + spaceBetweenBars / 2, y = size.height - barHeight),
                size = Size(width = barWidth - spaceBetweenBars, height = barHeight)
            )

            // Draw the X-axis label
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    dataPoint.label,
                    barX + barWidth / 2,
                    size.height + 40f, // Position label below the chart
                    android.graphics.Paint().apply {
                        color = Color.Black.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 12.sp.toPx()
                    }
                )
            }
        }

        // Draw tooltip if a bar is selected
        selectedIndex?.let { index ->
            val dataPoint = data[index]
            val barHeight = if (maxValue > 0) (dataPoint.value / maxValue) * size.height else 0f
            val barX = index * barWidth

            // Highlight the selected bar
            drawRect(
                color = barColor.copy(alpha = 0.5f), // A slightly transparent highlight
                topLeft = Offset(x = barX + spaceBetweenBars / 2, y = size.height - barHeight),
                size = Size(width = barWidth - spaceBetweenBars, height = barHeight)
            )

            // Draw the tooltip text above the bar
            val tooltipText = "${dataPoint.value.roundToInt()} ml on ${dataPoint.fullLabel}"
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    tooltipText,
                    barX + barWidth / 2,
                    size.height - barHeight - 20f, // Position tooltip above the bar
                    android.graphics.Paint().apply {
                        color = Color.DarkGray.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 14.sp.toPx()
                        // You can add a background to the text for better readability
                    }
                )
            }
        }
    }
}
