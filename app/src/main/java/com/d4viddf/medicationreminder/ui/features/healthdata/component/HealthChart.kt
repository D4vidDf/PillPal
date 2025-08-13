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
import java.time.Instant

@Composable
fun HealthChart(
    data: List<Pair<Instant, Double>>,
    modifier: Modifier = Modifier,
    chartType: ChartType,
    lineColor: Color = Color.Blue,
    strokeWidth: Float = 5f,
    yAxisRange: ClosedRange<Double>? = null,
    xAxisLabelFormatter: (Instant) -> String = { "" },
    yAxisLabelFormatter: (Double) -> String = { "" }
) {
    if (data.isEmpty()) {
        return
    }

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

        val minTime = data.minOf { it.first }.epochSecond
        val maxTime = data.maxOf { it.first }.epochSecond
        val timeRange = maxTime - minTime

        if (chartType == ChartType.LINE) {
            data.forEachIndexed { index, pair ->
                val x = size.width * ((pair.first.epochSecond - minTime).toFloat() / timeRange)
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
            val barWidth = size.width / (data.size * 2)
            data.forEach { pair ->
                val x = size.width * ((pair.first.epochSecond - minTime).toFloat() / timeRange)
                val y = size.height * (((maxY ?: 0.0) - pair.second) / range).toFloat()
                drawRect(
                    color = lineColor,
                    topLeft = Offset(x - barWidth / 2, y),
                    size = Size(barWidth, size.height - y)
                )
            }
        }

        // Draw X-axis labels
        val labelCount = 5
        for (i in 0..labelCount) {
            val t = minTime + (timeRange * i / labelCount)
            val instant = Instant.ofEpochSecond(t)
            val label = xAxisLabelFormatter(instant)
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
