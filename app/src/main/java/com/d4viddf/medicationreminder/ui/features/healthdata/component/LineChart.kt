package com.d4viddf.medicationreminder.ui.features.healthdata.component

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LineChart(
    data: List<Pair<Instant, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Blue,
    strokeWidth: Float = 5f
) {
    if (data.size < 2) {
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
        val maxValue = data.maxOfOrNull { it.second } ?: 0.0
        val minValue = data.minOfOrNull { it.second } ?: 0.0
        val range = if (maxValue == minValue) 1.0 else maxValue - minValue

        val minTime = data.minOf { it.first }.epochSecond
        val maxTime = data.maxOf { it.first }.epochSecond
        val timeRange = maxTime - minTime

        data.forEachIndexed { index, pair ->
            val x = size.width * ((pair.first.epochSecond - minTime).toFloat() / timeRange)
            val y = size.height * ((maxValue - pair.second) / range).toFloat()
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

        // Draw X-axis labels
        val formatter = DateTimeFormatter.ofPattern("d/M").withZone(ZoneId.systemDefault())
        val labelCount = 5
        for (i in 0..labelCount) {
            val t = minTime + (timeRange * i / labelCount)
            val instant = Instant.ofEpochSecond(t)
            val label = formatter.format(instant)
            val x = size.width * i / labelCount
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                size.height,
                textPaint
            )
        }
    }
}
