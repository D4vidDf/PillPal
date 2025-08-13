package com.d4viddf.medicationreminder.ui.features.healthdata.component

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.ui.features.healthdata.util.ChartType
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HealthChart(
    data: List<Pair<Instant, Double>>,
    modifier: Modifier = Modifier,
    chartType: ChartType,
    timeRange: TimeRange,
    startTime: Instant,
    endTime: Instant,
    goalLineValue: Float? = null,
    yAxisRange: ClosedRange<Double>? = null,
    yAxisLabelFormatter: (Double) -> String = { it.toInt().toString() }
) {
    val density = LocalDensity.current
    val textPaint = Paint().apply {
        color = MaterialTheme.colorScheme.onSurface.toArgb()
        textAlign = Paint.Align.CENTER
        textSize = with(density) { 12.sp.toPx() }
    }
    val barColor = MaterialTheme.colorScheme.primary
    val goalLineColor = MaterialTheme.colorScheme.error

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val (minY, maxY) = yAxisRange?.let { it.start to it.endInclusive } ?: data.let { d ->
            d.minOfOrNull { it.second } to d.maxOfOrNull { it.second }
        }
        val range = if (maxY == minY) 1.0 else (maxY ?: 0.0) - (minY ?: 0.0)

        val minTime = startTime.epochSecond
        val maxTime = endTime.epochSecond
        val timeRangeSeconds = maxTime - minTime

        if (chartType == ChartType.BAR) {
            if (data.isNotEmpty()) {
                val barWidth = size.width / (data.size * 2)
                data.forEach { pair ->
                    val x = size.width * ((pair.first.epochSecond - minTime).toFloat() / timeRangeSeconds)
                    val y = size.height * (((maxY ?: 0.0) - pair.second) / range).toFloat()
                    drawRect(
                        color = barColor,
                        topLeft = Offset(x - barWidth / 2, y),
                        size = Size(barWidth, size.height - y)
                    )
                }
            }
        } else if (chartType == ChartType.POINT) {
            data.forEach { pair ->
                val x = size.width * ((pair.first.epochSecond - minTime).toFloat() / timeRangeSeconds)
                val y = size.height * (((maxY ?: 0.0) - pair.second) / range).toFloat()
                drawCircle(
                    color = barColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        goalLineValue?.let { goal ->
            if (range > 0) {
                val goalY = size.height * (((maxY ?: 0.0) - goal) / range).toFloat()
                drawLine(
                    color = goalLineColor,
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        val xAxisLabelFormatter = when (timeRange) {
            TimeRange.DAY -> DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
            TimeRange.WEEK -> DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault())
            TimeRange.MONTH -> DateTimeFormatter.ofPattern("d").withZone(ZoneId.systemDefault())
            TimeRange.YEAR -> DateTimeFormatter.ofPattern("MMM").withZone(ZoneId.systemDefault())
        }
        val labelCount = when (timeRange) {
            TimeRange.DAY -> 6
            TimeRange.WEEK -> 7
            TimeRange.MONTH -> 6
            TimeRange.YEAR -> 12
        }
        for (i in 0 until labelCount) {
            val t = minTime + (timeRangeSeconds * i / labelCount)
            val instant = Instant.ofEpochSecond(t)
            val label = xAxisLabelFormatter.format(instant)
            val x = size.width * i / labelCount
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                size.height,
                textPaint
            )
        }

        val yLabelCount = 5
        for (i in 0..yLabelCount) {
            val value = (minY ?: 0.0) + (range * i / yLabelCount)
            val label = yAxisLabelFormatter(value)
            val y = size.height * (1 - (i.toFloat() / yLabelCount))
            drawContext.canvas.nativeCanvas.drawText(
                label,
                0f,
                y,
                textPaint
            )
        }
    }
}
