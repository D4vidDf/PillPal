package com.d4viddf.medicationreminder.ui.features.healthdata.component

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.ui.features.healthdata.util.ChartType
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

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
    yAxisLabelFormatter: (Double) -> String = { it.toInt().toString() },
    onBarSelected: (Pair<Instant, Double>?) -> Unit
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

    val selectedBar = remember { mutableStateOf<Pair<Instant, Double>?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val (minY, maxY) = yAxisRange?.let { it.start.toFloat() to it.endInclusive.toFloat() }
                            ?: data.let { d ->
                                d.minOfOrNull { it.second }?.toFloat() to d.maxOfOrNull { it.second }
                                    ?.toFloat()
                            }
                        val yRange = if (maxY == minY) 1f else (maxY ?: 0f) - (minY ?: 0f)

                        val yAxisLabelPadding = 16.dp.toPx()
                        val yAxisMaxLabelWidth = (0..5).maxOfOrNull { i ->
                            val value = (minY ?: 0f) + (yRange * i / 5)
                            yAxisTextPaint.measureText(yAxisLabelFormatter(value.toDouble()))
                        } ?: 0f
                        val yAxisLabelAreaWidth = yAxisMaxLabelWidth + yAxisLabelPadding

                        val horizontalPadding = 8.dp.toPx()
                        val chartAreaWidth = size.width - yAxisLabelAreaWidth - horizontalPadding * 2

                        val itemWidth = chartAreaWidth / data.size
                        val selectedIndex = ((offset.x - yAxisLabelAreaWidth - horizontalPadding) / itemWidth)
                            .toInt()
                            .coerceIn(0, data.size - 1)
                        val bar = data[selectedIndex]
                        selectedBar.value = bar
                        onBarSelected(bar)
                    }
                )
            }
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

        val xAxisLabelHeight = 30.dp.toPx()
        val horizontalPadding = 8.dp.toPx()
        val chartAreaWidth = size.width - yAxisLabelAreaWidth - horizontalPadding * 2
        val chartDrawableHeight = size.height - xAxisLabelHeight

        if (chartType == ChartType.BAR) {
            if (data.isNotEmpty()) {
                val itemWidth = chartAreaWidth / data.size
                val barWidth = itemWidth * 0.6f
                data.forEachIndexed { index, pair ->
                    val x = yAxisLabelAreaWidth + horizontalPadding + (itemWidth * index) + (itemWidth / 2)
                    val y = chartDrawableHeight * (((maxY ?: 0f) - pair.second.toFloat()) / yRange)
                    if (pair.second > 0) {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x - barWidth / 2, y),
                            size = Size(barWidth, chartDrawableHeight - y),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    } else {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x - barWidth / 2, chartDrawableHeight - 2.dp.toPx()),
                            size = Size(barWidth, 2.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }
            }
        } else if (chartType == ChartType.POINT || chartType == ChartType.LINE) {
            val points = data.mapIndexed { index, pair ->
                val x = yAxisLabelAreaWidth + horizontalPadding + (chartAreaWidth * index / (data.size -1))
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

        selectedBar.value?.let { bar ->
            val index = data.indexOf(bar)
            val itemWidth = chartAreaWidth / data.size
            val x = yAxisLabelAreaWidth + horizontalPadding + (itemWidth * index) + (itemWidth / 2)
            drawLine(
                color = barColor,
                start = Offset(x, 0f),
                end = Offset(x, chartDrawableHeight),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(
                color = barColor,
                radius = 4.dp.toPx(),
                center = Offset(x, chartDrawableHeight)
            )
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

        val labels = when (timeRange) {
            TimeRange.DAY -> (0..5).map {
                val zonedDateTime = ZonedDateTime.ofInstant(startTime, ZoneId.systemDefault())
                val instant = zonedDateTime.plus((it * 4).toLong(), ChronoUnit.HOURS).toInstant()
                val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                formatter.format(instant) to (it.toFloat() / 5f)
            }
            TimeRange.WEEK -> (0..6).map {
                val zonedDateTime = ZonedDateTime.ofInstant(startTime, ZoneId.systemDefault())
                val instant = zonedDateTime.plus(it.toLong(), ChronoUnit.DAYS).toInstant()
                val formatter = DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault())
                formatter.format(instant) to (it.toFloat() / 6f)
            }
            TimeRange.MONTH -> {
                val startDay = ZonedDateTime.ofInstant(startTime, ZoneId.systemDefault()).dayOfMonth
                val endDay = ZonedDateTime.ofInstant(endTime, ZoneId.systemDefault()).dayOfMonth
                val days = mutableListOf<Int>()
                var currentDay = startDay
                while (currentDay <= endDay) {
                    days.add(currentDay)
                    if (currentDay == 1) {
                        currentDay = 5
                    } else {
                        currentDay += 5
                    }
                }
                if (abs(days.last() - endDay) > 2) {
                    days.add(endDay)
                }
                days.map {
                    val zonedDateTime = ZonedDateTime.ofInstant(startTime, ZoneId.systemDefault())
                    val instant = zonedDateTime.plus((it - startDay).toLong(), ChronoUnit.DAYS).toInstant()
                    val formatter = DateTimeFormatter.ofPattern("d").withZone(ZoneId.systemDefault())
                    formatter.format(instant) to ((it - startDay).toFloat() / (endDay - startDay))
                }
            }
            TimeRange.YEAR -> (0..11).map {
                val zonedDateTime = ZonedDateTime.ofInstant(startTime, ZoneId.systemDefault())
                val instant = zonedDateTime.plus(it.toLong(), ChronoUnit.MONTHS).toInstant()
                val formatter = DateTimeFormatter.ofPattern("MMM").withZone(ZoneId.systemDefault())
                formatter.format(instant) to (it.toFloat() / 11f)
            }
        }
        labels.forEach { (label, position) ->
            val x = yAxisLabelAreaWidth + horizontalPadding + chartAreaWidth * position
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
            val label = if (value > 0) yAxisLabelFormatter(value.toDouble()) else "0"
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