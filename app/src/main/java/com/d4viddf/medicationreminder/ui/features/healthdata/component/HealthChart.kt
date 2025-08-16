package com.d4viddf.medicationreminder.ui.features.healthdata.component

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import kotlin.math.ceil

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

    val yAxisTextPaint = Paint().apply {
        color = MaterialTheme.colorScheme.onSurface.toArgb()
        textAlign = Paint.Align.RIGHT
        textSize = with(density) { 12.sp.toPx() }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
    ) {
        val (minY, maxY) = yAxisRange?.let { it.start.toFloat() to it.endInclusive.toFloat() } ?: data.let { d ->
            d.minOfOrNull { it.second }?.toFloat() to d.maxOfOrNull { it.second }?.toFloat()
        }
        val yRange = if (maxY == minY) 1f else (maxY ?: 0f) - (minY ?: 0f)

        val yAxisLabelPadding = 16.dp.toPx()
        val yAxisMaxLabelWidth = (0..5).maxOfOrNull { i ->
            val value = (minY ?: 0f) + (yRange * i / 5)
            yAxisTextPaint.measureText(yAxisLabelFormatter(value.toDouble()))
        } ?: 0f
        val yAxisLabelAreaWidth = yAxisMaxLabelWidth + yAxisLabelPadding

        val minTime = startTime.epochSecond
        val maxTime = endTime.epochSecond
        val timeRangeSeconds = maxTime - minTime

        val xAxisLabelHeight = 30.dp.toPx()
        val horizontalPadding = 16.dp.toPx()
        val chartAreaWidth = size.width - yAxisLabelAreaWidth - horizontalPadding
        val chartDrawableHeight = size.height - xAxisLabelHeight

        if (chartType == ChartType.BAR) {
            if (data.isNotEmpty()) {
                val itemAvailableWidth = when (timeRange) {
                    TimeRange.DAY -> chartAreaWidth / 24 // 24 hours
                    TimeRange.WEEK -> chartAreaWidth / 7 // 7 days
                    TimeRange.MONTH -> chartAreaWidth / 30 // 30 days
                    TimeRange.THREE_MONTHS -> chartAreaWidth / 3
                    TimeRange.YEAR -> chartAreaWidth / 12 // 12 months
                }
                val barWidth = itemAvailableWidth * 0.3f
                data.forEach { pair ->
                    val x = yAxisLabelAreaWidth + horizontalPadding + chartAreaWidth * ((pair.first.epochSecond - minTime).toFloat() / timeRangeSeconds)
                    val y = chartDrawableHeight * (((maxY ?: 0f) - pair.second.toFloat()) / yRange)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x - barWidth / 2, y),
                        size = Size(barWidth, chartDrawableHeight - y),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
        } else if (chartType == ChartType.POINT || chartType == ChartType.LINE) {
            val points = data.map { pair ->
                val x = yAxisLabelAreaWidth + horizontalPadding + chartAreaWidth * ((pair.first.epochSecond - minTime).toFloat() / timeRangeSeconds)
                val y = chartDrawableHeight * (((maxY ?: 0f) - pair.second.toFloat()) / yRange)
                Offset(x, y)
            }

            if (chartType == ChartType.LINE) {
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = barColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            points.forEach { point ->
                drawCircle(
                    color = barColor,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }

        goalLineValue?.let { goal ->
            if (yRange > 0) {
                val goalY = chartDrawableHeight * (((maxY ?: 0f) - goal) / yRange)
                drawLine(
                    color = goalLineColor,
                    start = Offset(yAxisLabelAreaWidth, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        val labelCount = when (timeRange) {
            TimeRange.DAY -> 6
            TimeRange.WEEK -> 7
            TimeRange.MONTH -> 6
            TimeRange.THREE_MONTHS -> 3
            TimeRange.YEAR -> 12
        }
        for (i in 0..labelCount) {
            val instant = startTime.plusSeconds((timeRangeSeconds * i / labelCount))
            val formatter = when (timeRange) {
                TimeRange.DAY -> DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                TimeRange.WEEK -> DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault())
                TimeRange.MONTH -> DateTimeFormatter.ofPattern("d").withZone(ZoneId.systemDefault())
                TimeRange.THREE_MONTHS -> DateTimeFormatter.ofPattern("MMM").withZone(ZoneId.systemDefault())
                TimeRange.YEAR -> DateTimeFormatter.ofPattern("MMM").withZone(ZoneId.systemDefault())
            }
            val label = formatter.format(instant)
            val x = yAxisLabelAreaWidth + horizontalPadding + chartAreaWidth * i / labelCount
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                size.height,
                textPaint
            )
        }

        val yLabelCount = 5
        for (i in 0..yLabelCount) {
            val value = (minY ?: 0f) + (yRange * i / yLabelCount)
            val label = yAxisLabelFormatter(value.toDouble())
            val y = chartDrawableHeight * (1 - (i.toFloat() / yLabelCount))
            drawContext.canvas.nativeCanvas.drawText(
                label,
                yAxisLabelAreaWidth - 10.dp.toPx(),
                y,
                yAxisTextPaint
            )
        }
    }
}
