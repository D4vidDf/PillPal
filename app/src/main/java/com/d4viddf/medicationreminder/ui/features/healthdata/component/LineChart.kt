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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp

@Composable
fun LineChart(
    data: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    showLines: Boolean = true,
    showPoints: Boolean = false,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    pointColor: Color = MaterialTheme.colorScheme.secondary,
    yAxisRange: ClosedFloatingPointRange<Float>? = null
) {
    val (minX, maxX, minY, maxY) = getChartBounds(data, yAxisRange)
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    Canvas(modifier = modifier.fillMaxSize()) {
        if (data.size > 1) {
            val path = Path()
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.5f), Color.Transparent),
                startY = 0f,
                endY = size.height
            )

            data.forEachIndexed { index, dataPoint ->
                val currentX = ((dataPoint.x - minX) / (maxX - minX)) * size.width
                val currentY = size.height - ((dataPoint.y - minY) / (maxY - minY)) * size.height

                // Draw X-axis label
                drawContext.canvas.nativeCanvas.drawText(
                    dataPoint.label,
                    currentX,
                    size.height - 20f,
                    android.graphics.Paint().apply {
                        color = onBackgroundColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 12.sp.toPx()
                    }
                )

                if (index == 0) {
                    path.moveTo(currentX, currentY)
                } else {
                    path.lineTo(currentX, currentY)
                }
            }

            if (showLines) {
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                // Fill area under the line
                path.lineTo(size.width, size.height)
                path.lineTo(0f, size.height)
                path.close()
                drawPath(path, brush = gradientBrush)
            }
        }

        if (showPoints) {
            data.forEach { dataPoint ->
                val x = ((dataPoint.x - minX) / (maxX - minX)) * size.width
                val y = size.height - ((dataPoint.y - minY) / (maxY - minY)) * size.height
                drawCircle(
                    color = pointColor,
                    radius = 10f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun getChartBounds(
    data: List<LineChartPoint>,
    yAxisRange: ClosedFloatingPointRange<Float>? = null
): List<Float> {
    if (data.isEmpty()) return listOf(0f, 0f, 0f, 0f)

    val minX = data.minOf { it.x }
    val maxX = data.maxOf { it.x }

    val minY = yAxisRange?.start ?: data.minOf { it.y }
    val maxY = yAxisRange?.endInclusive ?: data.maxOf { it.y }

    // Add some padding to the Y-axis to prevent points from touching the edges
    val yPadding = (maxY - minY) * 0.1f
    return listOf(minX, maxX, minY - yPadding, maxY + yPadding)
}
