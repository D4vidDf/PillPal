package com.d4viddf.medicationreminder.ui.features.common.charts

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import com.d4viddf.medicationreminder.ui.theme.Dimensions
import java.time.LocalDate
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
    yAxisMax: Float,
    yAxisLabelFormatter: (Float) -> String = { it.roundToInt().toString() },
    onBarSelected: (ChartDataPoint?) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val allDataIsZero = data.all { it.value == 0f }

    val goalLineAlpha by animateFloatAsState(
        targetValue = if (data.isNotEmpty() && !allDataIsZero && showGoalLine && yAxisMax > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 600)
    )

    var chartAreaHeight by remember { mutableStateOf(0f) }
    val animatables = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    val minBarHeight = 20f

    LaunchedEffect(data, yAxisMax, chartAreaHeight) {
        if (chartAreaHeight == 0f) return@LaunchedEffect

        val newKeys = data.map { it.fullLabel }.toSet()
        val keysToRemove = animatables.keys.filter { it !in newKeys }
        keysToRemove.forEach { animatables.remove(it) }

        data.forEach { dataPoint ->
            val key = dataPoint.fullLabel
            val targetHeight = if (dataPoint.value == 0f && dataPoint.date.isBefore(LocalDate.now().plusDays(1))) {
                minBarHeight
            } else {
                if (yAxisMax > 0) (dataPoint.value / yAxisMax) * chartAreaHeight else 0f
            }

            val animatable = animatables[key]
            if (animatable == null) {
                animatables[key] = Animatable(0f)
            }
            launch {
                animatables[key]?.animateTo(targetHeight, animationSpec = tween(600))
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
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
        val localChartAreaHeight = size.height - xAxisAreaHeight
        if (chartAreaHeight != localChartAreaHeight) {
            chartAreaHeight = localChartAreaHeight
        }
        val chartAreaWidth = size.width - yAxisAreaWidth
        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
        val today = LocalDate.now()

        // Draw Y-axis labels
        val numYAxisLabels = 4
        val yAxisLabelPaint = android.graphics.Paint().apply {
            color = axisLabelColor.toArgb()
            textAlign = if (yAxisPosition == YAxisPosition.Left) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
            textSize = 12.sp.toPx()
        }
        if (data.isNotEmpty() && !allDataIsZero && yAxisMax > 0) {
            val labelValues = mutableListOf<Float>()
            if (showGoalLine) {
                labelValues.add(goalLineValue)
            }
            labelValues.add(yAxisMax)

            labelValues.distinct().sorted().forEach { value ->
                val y = localChartAreaHeight - (value / yAxisMax) * localChartAreaHeight
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

        data.forEachIndexed { index, dataPoint ->
            val finalBarHeight = animatables[dataPoint.fullLabel]?.value ?: 0f
            val barX = chartAreaStartX + spacing + index * (fixedBarWidth + spacing)
            val barCenter = barX + fixedBarWidth / 2

            // Draw the bar with rounded corners
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = barX, y = localChartAreaHeight - finalBarHeight),
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
            val goalY = localChartAreaHeight - (goalLineValue / yAxisMax) * localChartAreaHeight
            drawLine(
                color = goalLineColor.copy(alpha = goalLineAlpha),
                start = Offset(x = chartAreaStartX, y = goalY),
                end = Offset(x = chartAreaStartX + chartAreaWidth, y = goalY),
                strokeWidth = (Dimensions.PaddingSmall / 4).toPx()
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
                end = Offset(barCenter, localChartAreaHeight),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            if(showTooltip) {
                val finalBarHeight = animatables[dataPoint.fullLabel]?.value ?: 0f

                // Highlight the selected bar
                drawRoundRect(
                    color = barColor.copy(alpha = 0.5f),
                    topLeft = Offset(x = barX, y = localChartAreaHeight - finalBarHeight),
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
                val tooltipY = localChartAreaHeight - finalBarHeight - 20f
                val tooltipPadding = Dimensions.PaddingMedium.toPx()
                val tooltipWidth = textBounds.width() + tooltipPadding * 2
                val tooltipHeight = textBounds.height() + tooltipPadding * 2

                drawRoundRect(
                    color = tooltipBackgroundColor,
                    topLeft = Offset(tooltipX - tooltipWidth / 2, tooltipY - tooltipHeight + textBounds.bottom),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(Dimensions.PaddingSmall.toPx(), Dimensions.PaddingSmall.toPx())
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
