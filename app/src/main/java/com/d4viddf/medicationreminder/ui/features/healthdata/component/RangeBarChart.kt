package com.d4viddf.medicationreminder.ui.features.healthdata.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

data class RangeChartPoint(val x: Float, val min: Float, val max: Float)

@Composable
fun RangeBarChart(
    data: List<RangeChartPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    yAxisRange: ClosedFloatingPointRange<Float>? = null
) {
    val (minX, maxX, minY, maxY) = getChartBounds(data, yAxisRange)

    Canvas(modifier = modifier.fillMaxSize()) {
        val xStep = size.width / (data.size + 1)
        data.forEachIndexed { index, dataPoint ->
            val xPos = xStep * (index + 1)
            val yMinPos = size.height - ((dataPoint.min - minY) / (maxY - minY)) * size.height
            val yMaxPos = size.height - ((dataPoint.max - minY) / (maxY - minY)) * size.height

            drawRect(
                color = barColor,
                topLeft = Offset(x = xPos - (xStep / 4), y = yMaxPos),
                size = Size(width = xStep / 2, height = yMinPos - yMaxPos)
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
