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
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class YAxisPosition {
    Left, Right
}

private class AnimatingBar(
    val point: ChartDataPoint,
    initialX: Float,
    initialHeight: Float,
    initialAlpha: Float = 1f
) {
    val key: String = point.fullLabel
    val xOffset = Animatable(initialX)
    val height = Animatable(initialHeight)
    val alpha = Animatable(initialAlpha)
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
    val allDataIsZero = data.all { it.value == 0f }
    val maxDataValue = data.maxOfOrNull { it.value } ?: 0f
    var yAxisMax = if (showGoalLine) max(maxDataValue, goalLineValue) else maxDataValue
    if (yAxisMax == goalLineValue) {
        yAxisMax += 1000f
    }

    val goalLineAlpha by animateFloatAsState(
        targetValue = if (data.isNotEmpty() && !allDataIsZero && showGoalLine && yAxisMax > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 600)
    )

    var chartAreaHeight by remember { mutableStateOf(0f) }
    var chartAreaWidth by remember { mutableStateOf(0f) }
    val animatingBars = remember { mutableStateListOf<AnimatingBar>() }
    val minBarHeight = 20f

    LaunchedEffect(data, yAxisMax, chartAreaWidth, chartAreaHeight) {
        if (chartAreaHeight == 0f || chartAreaWidth == 0f) return@LaunchedEffect

        val yAxisAreaWidth = 120f
        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
        val maxBars = 31
        val fixedBarWidth = (chartAreaWidth / maxBars) * 0.8f
        val totalBarWidth = data.size * fixedBarWidth
        val totalSpacing = chartAreaWidth - totalBarWidth
        val spacing = if (data.size > 0) totalSpacing / (data.size + 1) else 0f

        val dataMap = data.associateBy { it.fullLabel }
        val animatingBarsMap = animatingBars.associateBy { it.key }

        // Animate exiting bars
        (animatingBarsMap.keys - dataMap.keys).forEach { key ->
            val bar = animatingBarsMap[key]
            if (bar != null) {
                launch {
                    bar.alpha.animateTo(0f, animationSpec = tween(300))
                    bar.height.animateTo(0f, animationSpec = tween(300))
                    animatingBars.remove(bar)
                }
            }
        }

        // Animate entering and moving bars
        data.forEachIndexed { index, dataPoint ->
            val key = dataPoint.fullLabel
            val targetX = chartAreaStartX + spacing + index * (fixedBarWidth + spacing)
            val targetHeight = if (dataPoint.value == 0f && dataPoint.date.isBefore(LocalDate.now().plusDays(1))) {
                minBarHeight
            } else {
                if (yAxisMax > 0) (dataPoint.value / yAxisMax) * chartAreaHeight else 0f
            }

            val existingBar = animatingBarsMap[key]
            if (existingBar == null) {
                // Entering bar
                val newBar = AnimatingBar(
                    point = dataPoint,
                    initialX = chartAreaStartX + chartAreaWidth / 2, // Start from center
                    initialHeight = 0f,
                    initialAlpha = 0f
                )
                animatingBars.add(newBar)
                launch {
                    newBar.xOffset.animateTo(targetX, animationSpec = tween(600))
                    newBar.height.animateTo(targetHeight, animationSpec = tween(600))
                    newBar.alpha.animateTo(1f, animationSpec = tween(300))
                }
            } else {
                // Moving/updating bar
                launch { existingBar.xOffset.animateTo(targetX, animationSpec = tween(600)) }
                launch { existingBar.height.animateTo(targetHeight, animationSpec = tween(600)) }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp) // Increased height for labels
            .pointerInput(data) {
                // This gesture detection is now less accurate because it doesn't know about
                // the animated positions. For this iteration, we accept this limitation.
                // A more complex solution would involve hit-testing against `animatingBars`.
                detectDragGestures(
                    onDragStart = { offset ->
                        val yAxisAreaWidth = 120f
                        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
                        val currentChartAreaWidth = size.width - yAxisAreaWidth
                        val barWidth = currentChartAreaWidth / data.size
                        val index = ((offset.x - chartAreaStartX) / barWidth).toInt().coerceIn(0, data.lastIndex)
                        selectedIndex = index
                        onBarSelected(data.getOrNull(index))
                    },
                    onDrag = { change, _ ->
                        val yAxisAreaWidth = 120f
                        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f
                        val currentChartAreaWidth = size.width - yAxisAreaWidth
                        val barWidth = currentChartAreaWidth / data.size
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
        val localChartAreaWidth = size.width - yAxisAreaWidth
        if (chartAreaHeight != localChartAreaHeight) chartAreaHeight = localChartAreaHeight
        if (chartAreaWidth != localChartAreaWidth) chartAreaWidth = localChartAreaWidth

        val chartAreaStartX = if (yAxisPosition == YAxisPosition.Left) yAxisAreaWidth else 0f

        // Draw Y-axis labels
        val numYAxisLabels = 4
        val yAxisLabelPaint = android.graphics.Paint().apply {
            color = axisLabelColor.toArgb()
            textAlign = if (yAxisPosition == YAxisPosition.Left) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
            textSize = 12.sp.toPx()
        }
        if (data.isNotEmpty() && !allDataIsZero && yAxisMax > 0) {
            val labelValues = mutableListOf<Float>()
            (0..numYAxisLabels).forEach { i -> labelValues.add(yAxisMax * i / numYAxisLabels) }
            if (showGoalLine) labelValues.add(goalLineValue)

            labelValues.distinct().sorted().forEach { value ->
                val y = localChartAreaHeight - (value / yAxisMax) * localChartAreaHeight
                val xPos = if(yAxisPosition == YAxisPosition.Left) yAxisAreaWidth - 20f else size.width - yAxisAreaWidth + 20f
                drawContext.canvas.nativeCanvas.drawText(yAxisLabelFormatter(value), xPos, y, yAxisLabelPaint)
            }
        }

        val maxBars = 31
        val fixedBarWidth = (localChartAreaWidth / maxBars) * 0.8f

        // Draw the bars from the animating list
        animatingBars.forEach { bar ->
            val barCenter = bar.xOffset.value + fixedBarWidth / 2
            drawRoundRect(
                color = barColor.copy(alpha = bar.alpha.value),
                topLeft = Offset(x = bar.xOffset.value, y = localChartAreaHeight - bar.height.value),
                size = Size(width = fixedBarWidth, height = bar.height.value),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Draw the X-axis label
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = axisLabelColor.copy(alpha = bar.alpha.value).toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 12.sp.toPx()
                }
                drawText(bar.point.label, barCenter, size.height - xAxisAreaHeight + 40f, paint)
            }
        }

        if (goalLineAlpha > 0f) {
            val goalY = localChartAreaHeight - (goalLineValue / yAxisMax) * localChartAreaHeight
            drawLine(
                color = goalLineColor.copy(alpha = goalLineAlpha),
                start = Offset(x = chartAreaStartX, y = goalY),
                end = Offset(x = chartAreaStartX + localChartAreaWidth, y = goalY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Tooltip logic needs to find the selected bar from the animatingBars list
        selectedIndex?.let { index ->
            data.getOrNull(index)?.let { selectedDataPoint ->
                animatingBars.find { it.key == selectedDataPoint.fullLabel }?.let { bar ->
                    val barCenter = bar.xOffset.value + fixedBarWidth / 2
                    drawLine(
                        color = axisLabelColor,
                        start = Offset(barCenter, 0f),
                        end = Offset(barCenter, localChartAreaHeight),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    if(showTooltip) {
                        drawRoundRect(
                            color = barColor.copy(alpha = 0.5f),
                            topLeft = Offset(x = bar.xOffset.value, y = localChartAreaHeight - bar.height.value),
                            size = Size(width = fixedBarWidth, height = bar.height.value),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        val tooltipText = "${formatNumber(bar.point.value.roundToInt())} ml on ${bar.point.fullLabel}"
                        val textPaint = android.graphics.Paint().apply {
                            color = tooltipColor.toArgb()
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 14.sp.toPx()
                        }
                        val textBounds = Rect()
                        textPaint.getTextBounds(tooltipText, 0, tooltipText.length, textBounds)
                        val tooltipX = barCenter
                        val tooltipY = localChartAreaHeight - bar.height.value - 20f
                        val tooltipPadding = 8.dp.toPx()
                        val tooltipWidth = textBounds.width() + tooltipPadding * 2
                        val tooltipHeight = textBounds.height() + tooltipPadding * 2
                        drawRoundRect(
                            color = tooltipBackgroundColor,
                            topLeft = Offset(tooltipX - tooltipWidth / 2, tooltipY - tooltipHeight + textBounds.bottom),
                            size = Size(tooltipWidth, tooltipHeight),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )
                        drawContext.canvas.nativeCanvas.drawText(tooltipText, tooltipX, tooltipY, textPaint)
                    }
                }
            }
        }
    }
}
