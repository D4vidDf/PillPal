package com.d4viddf.medicationreminder.ui.features.healthdata.component



import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlin.math.roundToInt

@Composable
fun LineChart(
    data: List<LineChartPoint>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    showPoints: Boolean = false,
    showLine: Boolean = true,
    showGradient: Boolean = true,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    pointColor: Color = MaterialTheme.colorScheme.secondary,
    goal: Float? = null,
    yAxisRange: ClosedFloatingPointRange<Float>? = null,
    yAxisLabelFormatter: (Float) -> String = { it.roundToInt().toString() }
) {
    val (minX, maxX, minY, maxY) = getChartBounds(data, yAxisRange, labels)
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    Canvas(modifier = modifier.fillMaxSize()) {
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
        if (maxY > minY) {
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

        val xStep = if (labels.size > 1) chartAreaWidth / (labels.size - 1) else 0f
        labels.forEachIndexed { index, label ->
            val xPos = chartAreaStartX + xStep * index
            drawContext.canvas.nativeCanvas.drawText(
                label,
                xPos,
                size.height - 20f,
                android.graphics.Paint().apply {
                    color = onBackgroundColor.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 12.sp.toPx()
                }
            )
        }

        if (data.size > 1 && showLine) {
            val path = Path()
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.5f), Color.Transparent),
                startY = 0f,
                endY = chartAreaHeight
            )

            var lastValidPoint: LineChartPoint? = null
            data.forEach { dataPoint ->
                if (dataPoint.y != -1f) {
                    val currentX = chartAreaStartX + ((dataPoint.x - minX) / (maxX - minX)) * chartAreaWidth
                    val currentY = chartAreaHeight - ((dataPoint.y - minY) / (maxY - minY)) * chartAreaHeight
                    if (lastValidPoint == null) {
                        path.moveTo(currentX, currentY)
                    } else {
                        path.lineTo(currentX, currentY)
                    }
                    lastValidPoint = dataPoint
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            if (showGradient) {
                val lastX = chartAreaStartX + ((data.last().x - minX) / (maxX - minX)) * chartAreaWidth
                val firstX = chartAreaStartX + ((data.first().x - minX) / (maxX - minX)) * chartAreaWidth
                path.lineTo(lastX, chartAreaHeight)
                path.lineTo(firstX, chartAreaHeight)
                path.close()
                drawPath(path, brush = gradientBrush)
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

        if (showPoints) {
            data.forEach { dataPoint ->
                if (dataPoint.showPoint) {
                    val x = chartAreaStartX + ((dataPoint.x - minX) / (maxX - minX)) * chartAreaWidth
                    val y = chartAreaHeight - ((dataPoint.y - minY) / (maxY - minY)) * chartAreaHeight
                    drawCircle(
                        color = pointColor,
                        radius = 10f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

private fun getChartBounds(
    data: List<LineChartPoint>,
    yAxisRange: ClosedFloatingPointRange<Float>? = null,
    labels: List<String>
): List<Float> {
    if (labels.isEmpty()) return listOf(0f, 0f, 0f, 0f)

    val minX = 0f
    val maxX = (labels.size - 1).toFloat()

    val validData = data.filter { it.y != -1f }
    val minYValue = validData.minOfOrNull { it.y } ?: 0f
    val maxYValue = validData.maxOfOrNull { it.y } ?: 0f

    val minY = yAxisRange?.start ?: minYValue
    val maxY = yAxisRange?.endInclusive ?: maxYValue

    // Add some padding to the Y-axis to prevent points from touching the edges
    val yPadding = (maxY - minY) * 0.1f
    return listOf(minX, maxX, maxOf(0f, minY - yPadding), maxY + yPadding)
}
