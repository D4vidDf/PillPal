package com.d4viddf.medicationreminder.ui.features.common.charts

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

enum class YAxisPosition {
    Left, Right
}

@Composable
fun HealthDataChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Blue,
    axisLabelColor: Color = MaterialTheme.colorScheme.onBackground,
    tooltipColor: Color = MaterialTheme.colorScheme.onSurface,
    tooltipBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    yAxisPosition: YAxisPosition = YAxisPosition.Right,
    showGoalLine: Boolean = false,
    goalLineValue: Float = 0f,
    goalLineColor: Color = Color.Red,
    showTooltip: Boolean = true,
    onBarSelected: (ChartDataPoint?) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxDataValue = data.maxOfOrNull { it.value } ?: 0f
    val yAxisMax = if (showGoalLine) max(maxDataValue, goalLineValue) * 1.2f else maxDataValue

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp) // Increased height for labels
            .pointerInput(data) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val yAxisAreaWidth = 80f
                        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
                        val chartAreaWidth = size.width - yAxisAreaWidth
                        val barWidth = chartAreaWidth / data.size
                        val index = ((offset.x - chartAreaStartX) / barWidth).toInt().coerceIn(0, data.lastIndex)
                        selectedIndex = index
                        onBarSelected(data.getOrNull(index))
                    },
                    onDrag = { change, _ ->
                        val yAxisAreaWidth = 80f
                        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
                        val chartAreaWidth = size.width - yAxisAreaWidth
                        val barWidth = chartAreaWidth / data.size
                        val index = ((change.position.x - chartAreaStartX) / barWidth).toInt().coerceIn(0, data.lastIndex)
                        selectedIndex = index
                        onBarSelected(data.getOrNull(index))
                    },
                    onDragEnd = {
                        selectedIndex = null
                        onBarSelected(null)
                    },
                    onDragCancel = {
                        selectedIndex = null
                        onBarSelected(null)
                    }
                )
            }
    ) {
        val yAxisAreaWidth = 80f
        val xAxisAreaHeight = 60f
        val chartAreaWidth = size.width - yAxisAreaWidth
        val chartAreaHeight = size.height - xAxisAreaHeight
        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
        val today = LocalDate.now()

        // Draw Y-axis labels
        val numYAxisLabels = 4
        val yAxisLabelPaint = android.graphics.Paint().apply {
            color = axisLabelColor.toArgb()
            textAlign = if (yAxisPosition == YAxisPosition.Left) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
            textSize = 12.sp.toPx()
        }
        if (yAxisMax > 0) {
            (0..numYAxisLabels).forEach { i ->
                val value = yAxisMax * i / numYAxisLabels
                val y = chartAreaHeight - (value / yAxisMax) * chartAreaHeight
                val xPos = if(yAxisPosition == YAxisPosition.Left) yAxisAreaWidth - 10f else size.width - yAxisAreaWidth + 10f
                drawContext.canvas.nativeCanvas.drawText(
                    "${value.roundToInt()}",
                    xPos,
                    y,
                    yAxisLabelPaint
                )
            }
        }

        val barWidth = chartAreaWidth / data.size
        val spaceBetweenBars = barWidth * 0.2f

        data.forEachIndexed { index, dataPoint ->
            val minBarHeight = 2f
            val barHeight = if (yAxisMax > 0) (dataPoint.value / yAxisMax) * chartAreaHeight else 0f
            val finalBarHeight = if (dataPoint.value == 0f && dataPoint.date.isBefore(today.plusDays(1))) minBarHeight else barHeight
            val barX = chartAreaStartX + index * barWidth

            // Draw the bar with rounded corners
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = barX + spaceBetweenBars / 2, y = chartAreaHeight - finalBarHeight),
                size = Size(width = barWidth - spaceBetweenBars, height = finalBarHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Draw the X-axis label
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    dataPoint.label,
                    barX + barWidth / 2,
                    size.height - xAxisAreaHeight + 40f, // Position label below the chart
                    android.graphics.Paint().apply {
                        color = axisLabelColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 12.sp.toPx()
                    }
                )
            }
        }

        if (showGoalLine && yAxisMax > 0) {
            val goalY = chartAreaHeight - (goalLineValue / yAxisMax) * chartAreaHeight
            drawLine(
                color = goalLineColor,
                start = Offset(x = chartAreaStartX, y = goalY),
                end = Offset(x = chartAreaStartX + chartAreaWidth, y = goalY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Dotted line and tooltip on hover
        selectedIndex?.let { index ->
            val dataPoint = data[index]
            val barX = chartAreaStartX + index * barWidth
            val barCenter = barX + spaceBetweenBars / 2 + (barWidth - spaceBetweenBars) / 2

            drawLine(
                color = axisLabelColor,
                start = Offset(barCenter, 0f),
                end = Offset(barCenter, chartAreaHeight),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            if(showTooltip) {
                val barHeight = if (yAxisMax > 0) (dataPoint.value / yAxisMax) * chartAreaHeight else 0f
                val finalBarHeight = if (dataPoint.value == 0f && dataPoint.date.isBefore(today.plusDays(1))) 2f else barHeight

                // Highlight the selected bar
                drawRoundRect(
                    color = barColor.copy(alpha = 0.5f),
                    topLeft = Offset(x = barX + spaceBetweenBars / 2, y = chartAreaHeight - finalBarHeight),
                    size = Size(width = barWidth - spaceBetweenBars, height = finalBarHeight),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Draw the tooltip text above the bar
                val tooltipText = "${dataPoint.value.roundToInt()} ml on ${dataPoint.fullLabel}"
                val textPaint = android.graphics.Paint().apply {
                    color = tooltipColor.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 14.sp.toPx()
                }

                val textBounds = Rect()
                textPaint.getTextBounds(tooltipText, 0, tooltipText.length, textBounds)

                val tooltipX = barCenter
                val tooltipY = chartAreaHeight - finalBarHeight - 20f
                val tooltipPadding = 8.dp.toPx()
                val tooltipWidth = textBounds.width() + tooltipPadding * 2
                val tooltipHeight = textBounds.height() + tooltipPadding * 2

                drawRoundRect(
                    color = tooltipBackgroundColor,
                    topLeft = Offset(tooltipX - tooltipWidth / 2, tooltipY - tooltipHeight + textBounds.bottom),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        tooltipText,
                        tooltipX,
                        tooltipY,
                        textPaint
                    )
                }
            }
        }
    }
}
