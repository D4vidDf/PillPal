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
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
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
        val chartAreaHeight = size.height - xAxisAreaHeight
        val chartAreaWidth = size.width - yAxisAreaWidth
        val chartAreaStartX = 0f

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

        val xStep = chartAreaWidth / (data.size + 1)
        data.forEachIndexed { index, dataPoint ->
            val xPos = chartAreaStartX + xStep * (index + 1)

            // Draw X-axis label
            drawContext.canvas.nativeCanvas.drawText(
                dataPoint.label,
                xPos,
                size.height - 20f,
                yAxisLabelPaint.apply { textAlign = android.graphics.Paint.Align.CENTER }
            )

            val yMinPos = chartAreaHeight - ((dataPoint.min - minY) / (maxY - minY)) * chartAreaHeight
            val yMaxPos = chartAreaHeight - ((dataPoint.max - minY) / (maxY - minY)) * chartAreaHeight

            val barWidth = xStep / 4 // Reduced bar width

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
    if (data.isEmpty()) return listOf(0f, 0f, 0f, 0f)

    val minX = 0f
    val maxX = data.size.toFloat()

    val minY = yAxisRange?.start ?: data.minOf { it.min }
    val maxY = yAxisRange?.endInclusive ?: data.maxOf { it.max }

    // Add some padding to the Y-axis to prevent points from touching the edges
    val yPadding = (maxY - minY) * 0.1f
    return listOf(minX, maxX, minY - yPadding, maxY + yPadding)
}
