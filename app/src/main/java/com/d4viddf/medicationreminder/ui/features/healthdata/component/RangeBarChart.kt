package com.d4viddf.medicationreminder.ui.features.healthdata.component

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
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
import kotlin.math.roundToInt

data class RangeChartPoint(val x: Float, val min: Float, val max: Float, val label: String)

@Composable
fun RangeBarChart(
    data: List<RangeChartPoint>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    goal: Float? = null,
    yAxisRange: ClosedFloatingPointRange<Float>? = null,
    yAxisLabelFormatter: (Float) -> String = { it.roundToInt().toString() },
    onBarSelected: (RangeChartPoint?) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val (minX, maxX, minY, maxY) = getChartBounds(data, yAxisRange)
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val tooltipColor = MaterialTheme.colorScheme.onSurface
    val tooltipBackgroundColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val xStep = size.width / (data.size + 1)
                        val index = (offset.x / xStep).toInt() - 1
                        if (index in data.indices) {
                            selectedIndex = index
                            onBarSelected(data[index])
                        }
                    },
                    onDrag = { change, _ ->
                        val xStep = size.width / (data.size + 1)
                        val index = (change.position.x / xStep).toInt() - 1
                        if (index in data.indices) {
                            selectedIndex = index
                            onBarSelected(data[index])
                        }
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
        val yAxisAreaWidth = 120f
        val xAxisAreaHeight = 60f
        val chartPadding = 16.dp.toPx()
        val chartAreaHeight = size.height - xAxisAreaHeight
        val chartAreaWidth = size.width - yAxisAreaWidth - (2 * chartPadding)
        val chartAreaStartX = chartPadding

        // Draw Y-axis labels
        val yAxisLabelPaint = android.graphics.Paint().apply {
            color = onBackgroundColor.toArgb()
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = 12.sp.toPx()
        }
        if (data.isNotEmpty() && maxY > minY) {
            val numLabels = 3
            for (i in 0..numLabels) {
                val value = minY + (maxY - minY) * i / numLabels
                val y = chartAreaHeight - ((value - minY) / (maxY - minY)) * chartAreaHeight
                drawContext.canvas.nativeCanvas.drawText(
                    yAxisLabelFormatter(value),
                    size.width - 20f,
                    y,
                    yAxisLabelPaint
                )
            }
        }

        // Draw goal line
        goal?.let {
            if (it > 0 && it in minY..maxY) {
                val y = chartAreaHeight - ((it - minY) / (maxY - minY)) * chartAreaHeight
                drawLine(
                    color = onBackgroundColor,
                    start = Offset(chartAreaStartX, y),
                    end = Offset(chartAreaStartX + chartAreaWidth, y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
        }

        val xStep = chartAreaWidth / (labels.size + 1)
        labels.forEachIndexed { index, label ->
            val xPos = chartAreaStartX + xStep * (index + 1)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                xPos,
                size.height - 20f,
                yAxisLabelPaint.apply { textAlign = android.graphics.Paint.Align.CENTER }
            )
            // Draw guide line
            drawLine(
                color = onBackgroundColor,
                start = Offset(xPos, size.height - xAxisAreaHeight),
                end = Offset(xPos, size.height - xAxisAreaHeight - 10f),
                strokeWidth = 2f
            )
        }

        data.forEachIndexed { index, dataPoint ->
            val xPos = chartAreaStartX + xStep * (index + 1)
            val yMinPos = chartAreaHeight - ((dataPoint.min - minY) / (maxY - minY)) * chartAreaHeight
            val yMaxPos = chartAreaHeight - ((dataPoint.max - minY) / (maxY - minY)) * chartAreaHeight

            val barWidth = xStep / 4 // Reduced bar width

            if (dataPoint.min > 0 || dataPoint.max > 0) {
                if (dataPoint.min == dataPoint.max) {
                    // Draw a dot
                    drawCircle(
                        color = barColor,
                        radius = barWidth / 2,
                        center = Offset(x = xPos, y = yMinPos)
                    )
                } else {
                    // Draw a rounded bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x = xPos - (barWidth / 2), y = yMaxPos),
                        size = Size(width = barWidth, height = yMinPos - yMaxPos),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )
                }
            }
        }

        selectedIndex?.let { index ->
            val dataPoint = data[index]
            val xPos = chartAreaStartX + xStep * (index + 1)

            val tooltipText = "Min: ${dataPoint.min.roundToInt()}, Max: ${dataPoint.max.roundToInt()}"
            val textPaint = android.graphics.Paint().apply {
                color = tooltipColor.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 14.sp.toPx()
            }

            val textBounds = Rect()
            textPaint.getTextBounds(tooltipText, 0, tooltipText.length, textBounds)

            val yMaxPos = chartAreaHeight - ((dataPoint.max - minY) / (maxY - minY)) * chartAreaHeight
            val tooltipX = xPos
            val tooltipY = yMaxPos - 20f
            val tooltipPadding = 16.dp.toPx()
            val tooltipWidth = textBounds.width() + tooltipPadding * 2
            val tooltipHeight = textBounds.height() + tooltipPadding * 2

            drawRoundRect(
                color = tooltipBackgroundColor,
                topLeft = Offset(tooltipX - tooltipWidth / 2, tooltipY - tooltipHeight + textBounds.bottom),
                size = Size(tooltipWidth, tooltipHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            drawContext.canvas.nativeCanvas.drawText(
                tooltipText,
                tooltipX,
                tooltipY,
                textPaint
            )
        }
    }
}

private fun getChartBounds(
    data: List<RangeChartPoint>,
    yAxisRange: ClosedFloatingPointRange<Float>? = null
): List<Float> {
    if (data.isEmpty()) return listOf(0f, 0f, 30f, 45f) // Default range for temperature

    val minX = 0f
    val maxX = data.size.toFloat()

    var minY = yAxisRange?.start ?: data.minOfOrNull { it.min } ?: 30f
    var maxY = yAxisRange?.endInclusive ?: data.maxOfOrNull { it.max } ?: 45f

    if (minY == maxY) {
        minY -= 5f
        maxY += 5f
    }

    // Add some padding to the Y-axis to prevent points from touching the edges
    val yPadding = (maxY - minY) * 0.1f
    return listOf(minX, maxX, minY - yPadding, maxY + yPadding)
}
