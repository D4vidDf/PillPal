package com.d4viddf.medicationreminder.ui.features.common.charts

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.ui.common.util.formatNumber
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class YAxisPosition {
    Left, Right
}

@Composable
fun HealthDataChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Blue,
    axisLabelColor: Color = MaterialTheme.colorScheme.onBackground,
    tooltipColor: Color = MaterialTheme.colorScheme.onSurface,
    tooltipBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    yAxisPosition: YAxisPosition = YAxisPosition.Right,
    showGoalLine: Boolean = false,
    goalLineValue: Float = 0f,
    goalLineColor: Color = Color.Red,
    showTooltip: Boolean = true,
    yAxisLabelFormatter: (Float) -> String = { it.roundToInt().toString() },
    onBarSelected: (ChartDataPoint?) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxDataValue = data.maxOfOrNull { it.value } ?: 0f
    var yAxisMax = if (showGoalLine) max(maxDataValue, goalLineValue) else maxDataValue
    if (yAxisMax == goalLineValue) {
        yAxisMax += 1000f
    }

    val goalLineAlpha by animateFloatAsState(
        targetValue = if (data.isNotEmpty() && showGoalLine && yAxisMax > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    val animatables = remember { mutableStateMapOf<String, Animatable<Float>>() }
    LaunchedEffect(data) {
        // Remove animatables for bars that no longer exist
        (animatables.keys - data.map { it.fullLabel }.toSet()).forEach {
            animatables.remove(it)
        }

        // Add/update animatables for current bars
        data.forEach { dataPoint ->
            val key = dataPoint.fullLabel
            val targetValue = dataPoint.value
            if (!animatables.containsKey(key)) {
                animatables[key] = Animatable(0f) // New bars start from 0
            }
            launch {
                animatables[key]?.animateTo(targetValue, animationSpec = tween(300))
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp) // Increased height for labels
            .pointerInput(data) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val yAxisAreaWidth = 120f
                        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
                        val chartAreaWidth = size.width - yAxisAreaWidth
                        val barWidth = chartAreaWidth / data.size
                        val index = ((offset.x - chartAreaStartX) / barWidth).toInt().coerceIn(0, data.lastIndex)
                        selectedIndex = index
                        onBarSelected(data.getOrNull(index))
                    },
                    onDrag = { change, _ ->
                        val yAxisAreaWidth = 120f
                        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
                        val chartAreaWidth = size.width - yAxisAreaWidth
                        val barWidth = chartAreaWidth / data.size
                        val index = ((change.position.x - chartAreaStartX) / barWidth).toInt().coerceIn(0, data.lastIndex)
                        selectedIndex = index
                        onBarSelected(data.getOrNull(index))
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
        val chartAreaWidth = size.width - yAxisAreaWidth
        val chartAreaHeight = size.height - xAxisAreaHeight
        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
        val today = LocalDate.now()

        // Draw Y-axis labels
        val numYAxisLabels = 4
        val yAxisLabelPaint = android.graphics.Paint().apply {
            color = axisLabelColor.toArgb()
            textAlign = if (yAxisPosition == YAxisPosition.Left) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
            textSize = 12.sp.toPx()
        }
        if (data.isNotEmpty() && yAxisMax > 0) {
            val labelValues = mutableListOf<Float>()
            (0..numYAxisLabels).forEach { i ->
                labelValues.add(yAxisMax * i / numYAxisLabels)
            }
            if (showGoalLine) {
                labelValues.add(goalLineValue)
            }

            labelValues.distinct().sorted().forEach { value ->
                val y = chartAreaHeight - (value / yAxisMax) * chartAreaHeight
                val xPos = if(yAxisPosition == YAxisPosition.Left) yAxisAreaWidth - 20f else size.width - yAxisAreaWidth + 20f
                drawContext.canvas.nativeCanvas.drawText(
                    yAxisLabelFormatter(value),
                    xPos,
                    y,
                    yAxisLabelPaint
                )
            }
        }

        val maxBars = 31
        val fixedBarWidth = (chartAreaWidth / maxBars) * 0.8f
        val totalBarWidth = data.size * fixedBarWidth
        val totalSpacing = chartAreaWidth - totalBarWidth
        val spacing = if (data.size > 0) totalSpacing / (data.size + 1) else 0f

        val minBarHeight = 20f
        data.forEachIndexed { index, dataPoint ->
            val animatedValue = animatables[dataPoint.fullLabel]?.value ?: 0f
            val barHeight = if (yAxisMax > 0) (animatedValue / yAxisMax) * chartAreaHeight else 0f
            val finalBarHeight = if (dataPoint.value == 0f && dataPoint.date.isBefore(today.plusDays(1))) minBarHeight else barHeight
            val barX = chartAreaStartX + spacing + index * (fixedBarWidth + spacing)
            val barCenter = barX + fixedBarWidth / 2

            // Draw the bar with rounded corners
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = barX, y = chartAreaHeight - finalBarHeight),
                size = Size(width = fixedBarWidth, height = finalBarHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Draw the X-axis label
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    dataPoint.label,
                    barCenter,
                    size.height - xAxisAreaHeight + 40f, // Position label below the chart
                    android.graphics.Paint().apply {
                        color = axisLabelColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 12.sp.toPx()
                    }
                )
            }
        }

        if (goalLineAlpha > 0f) {
            val goalY = chartAreaHeight - (goalLineValue / yAxisMax) * chartAreaHeight
            drawLine(
                color = goalLineColor.copy(alpha = goalLineAlpha),
                start = Offset(x = chartAreaStartX, y = goalY),
                end = Offset(x = chartAreaStartX + chartAreaWidth, y = goalY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Dotted line and tooltip on hover
        selectedIndex?.let { index ->
            val dataPoint = data[index]

            val barX = chartAreaStartX + spacing + index * (fixedBarWidth + spacing)
            val barCenter = barX + fixedBarWidth / 2

            drawLine(
                color = axisLabelColor,
                start = Offset(barCenter, 0f),
                end = Offset(barCenter, chartAreaHeight),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            if(showTooltip) {
                val animatedValue = animatables[dataPoint.fullLabel]?.value ?: 0f
                val barHeight = if (yAxisMax > 0) (animatedValue / yAxisMax) * chartAreaHeight else 0f
                val finalBarHeight = if (dataPoint.value == 0f && dataPoint.date.isBefore(today.plusDays(1))) minBarHeight else barHeight

                // Highlight the selected bar
                drawRoundRect(
                    color = barColor.copy(alpha = 0.5f),
                    topLeft = Offset(x = barX, y = chartAreaHeight - finalBarHeight),
                    size = Size(width = fixedBarWidth, height = finalBarHeight),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Draw the tooltip text above the bar
                val tooltipText = "${formatNumber(dataPoint.value.roundToInt())} ml on ${dataPoint.fullLabel}"
                val textPaint = android.graphics.Paint().apply {
                    color = tooltipColor.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 14.sp.toPx()
                }

                val textBounds = Rect()
                textPaint.getTextBounds(tooltipText, 0, tooltipText.length, textBounds)

                val tooltipX = barCenter
                val tooltipY = chartAreaHeight - finalBarHeight - 20f
                val tooltipPadding = 8.dp.toPx()
                val tooltipWidth = textBounds.width() + tooltipPadding * 2
                val tooltipHeight = textBounds.height() + tooltipPadding * 2

                drawRoundRect(
                    color = tooltipBackgroundColor,
                    topLeft = Offset(tooltipX - tooltipWidth / 2, tooltipY - tooltipHeight + textBounds.bottom),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        tooltipText,
                        tooltipX,
                        tooltipY,
                        textPaint
                    )
                }
            }
        }
    }
}
