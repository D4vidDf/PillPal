package com.d4viddf.medicationreminder.ui.features.healthdata.component

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
    lineColor: Color = Color.Blue,
    strokeWidth: Float = 5f,
    yAxisRange: ClosedRange<Double>? = null,
    yAxisLabelFormatter: (Double) -> String = { "" }
) {
    val density = LocalDensity.current
    val textPaint = Paint().apply {
        color = Color.Black.toArgb()
        textAlign = Paint.Align.CENTER
        textSize = with(density) { 12.sp.toPx() }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val path = Path()
        val (minY, maxY) = yAxisRange?.let { it.start to it.endInclusive } ?: data.let { d ->
            d.minOfOrNull { it.second } to d.maxOfOrNull { it.second }
        }
        val range = if (maxY == minY) 1.0 else (maxY ?: 0.0) - (minY ?: 0.0)

        val minTime = startTime.epochSecond
        val maxTime = endTime.epochSecond
        val timeRangeSeconds = maxTime - minTime

        if (chartType == ChartType.LINE) {
            data.forEachIndexed { index, pair ->
                val x = size.width * ((pair.first.epochSecond - minTime).toFloat() / timeRangeSeconds)
                val y = size.height * (((maxY ?: 0.0) - pair.second) / range).toFloat()
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = strokeWidth)
            )
        } else {
            if (data.isNotEmpty()) {
                val barWidth = size.width / (data.size * 2)
                data.forEach { pair ->
                    val x = size.width * ((pair.first.epochSecond - minTime).toFloat() / timeRangeSeconds)
                    val y = size.height * (((maxY ?: 0.0) - pair.second) / range).toFloat()
                    drawRect(
                        color = lineColor,
                        topLeft = Offset(x - barWidth / 2, y),
                        size = Size(barWidth, size.height - y)
                    )
                }
            }
        }

        // Draw X-axis labels
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

        // Draw Y-axis labels
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
