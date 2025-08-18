package com.d4viddf.medicationreminder.ui.features.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
            .height(250.dp) // Increased height for labels
            .pointerInput(data) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val yAxisAreaWidth = 80f
                        val chartAreaWidth = size.width - yAxisAreaWidth
                        val barWidth = chartAreaWidth / data.size
                        val index = ((offset.x - yAxisAreaWidth) / barWidth).toInt().coerceIn(0, data.lastIndex)
                        selectedIndex = index
                    },
                    onDrag = { change, _ ->
                        val yAxisAreaWidth = 80f
                        val chartAreaWidth = size.width - yAxisAreaWidth
                        val barWidth = chartAreaWidth / data.size
                        val index = ((change.position.x - yAxisAreaWidth) / barWidth).toInt().coerceIn(0, data.lastIndex)
                        selectedIndex = index
                    },
                    onDragEnd = { selectedIndex = null },
                    onDragCancel = { selectedIndex = null }
                )
            }
    ) {
        val yAxisAreaWidth = 80f
        val xAxisAreaHeight = 60f
        val chartAreaWidth = size.width - yAxisAreaWidth
        val chartAreaHeight = size.height - xAxisAreaHeight

        // Draw Y-axis labels
        val numYAxisLabels = 4
        val yAxisLabelPaint = android.graphics.Paint().apply {
            color = Color.Black.toArgb()
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = 12.sp.toPx()
        }
        if (maxValue > 0) {
            (0..numYAxisLabels).forEach { i ->
                val value = maxValue * i / numYAxisLabels
                val y = chartAreaHeight - (value / maxValue) * chartAreaHeight
                drawContext.canvas.nativeCanvas.drawText(
                    "${value.roundToInt()}",
                    yAxisAreaWidth - 10f,
                    y,
                    yAxisLabelPaint
                )
            }
        }

        val barWidth = chartAreaWidth / data.size
        val spaceBetweenBars = barWidth * 0.2f

        data.forEachIndexed { index, dataPoint ->
            val barHeight = if (maxValue > 0) (dataPoint.value / maxValue) * chartAreaHeight else 0f
            val barX = yAxisAreaWidth + index * barWidth

            // Draw the bar with rounded corners
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = barX + spaceBetweenBars / 2, y = chartAreaHeight - barHeight),
                size = Size(width = barWidth - spaceBetweenBars, height = barHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Draw the X-axis label
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    dataPoint.label,
                    barX + barWidth / 2,
                    size.height - xAxisAreaHeight + 40f, // Position label below the chart
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
            val barHeight = if (maxValue > 0) (dataPoint.value / maxValue) * chartAreaHeight else 0f
            val barX = yAxisAreaWidth + index * barWidth

            // Highlight the selected bar
            drawRoundRect(
                color = barColor.copy(alpha = 0.5f),
                topLeft = Offset(x = barX + spaceBetweenBars / 2, y = chartAreaHeight - barHeight),
                size = Size(width = barWidth - spaceBetweenBars, height = barHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Draw the tooltip text above the bar
            val tooltipText = "${dataPoint.value.roundToInt()} ml on ${dataPoint.fullLabel}"
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    tooltipText,
                    barX + barWidth / 2,
                    chartAreaHeight - barHeight - 20f,
                    android.graphics.Paint().apply {
                        color = Color.DarkGray.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 14.sp.toPx()
                    }
                )
            }
        }
    }
}
