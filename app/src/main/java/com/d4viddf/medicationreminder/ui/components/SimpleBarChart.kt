package com.d4viddf.medicationreminder.ui.components

import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

data class BarChartItem(
    val label: String,
    val value: Float,
    val isHighlighted: Boolean = false
)

@Composable
fun SimpleBarChart(
    data: List<BarChartItem>,
    modifier: Modifier = Modifier,
    highlightedBarColor: Color,
    normalBarColor: Color,
    labelTextColor: Color,
    barWidthDp: Dp = 24.dp, // Default bar width
    spaceAroundBarsDp: Dp = 8.dp, // Default space between and around bars
    barCornerRadiusDp: Dp = 4.dp,
    valueTextColor: Color = labelTextColor,
    valueTextSizeSp: Float = 10f,
    chartContentDescription: String,
    explicitYAxisTopValue: Float? = null // New parameter
) {
    // Diagnostic Logging
    Log.d("SimpleBarChartData", "Input data: ${data.map { it.value }}, explicitYAxisTopValue: $explicitYAxisTopValue")
    val actualMaxValueForLog = data.maxOfOrNull { it.value }?.coerceAtLeast(0f) ?: 0f
    Log.d("SimpleBarChartData", "Calculated actualMaxValueForLog (initial): $actualMaxValueForLog")

    if (data.isEmpty()) {
        return
    }

    val density = LocalDensity.current
    val textPaint = remember(labelTextColor, density) {
        Paint().apply {
            color = labelTextColor.toArgb() // Use toArgb() for correct color conversion
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 12.sp.toPx() }
            isAntiAlias = true
            // Add typeface or other properties if needed
        }
    }

    val valueTextPaint = remember(valueTextColor, density, valueTextSizeSp) {
        Paint().apply {
            color = valueTextColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = with(density) { valueTextSizeSp.sp.toPx() }
            isAntiAlias = true
        }
    }

    val barCornerRadiusPx = with(density) { barCornerRadiusDp.toPx() }

    // Y-axis Paint related properties for keying and measurement
    val yAxisPaintTextSize = remember(density) { with(density) { 10.sp.toPx() } } // For yAxisTextPaint
    val yAxisPaintAlign = Paint.Align.RIGHT // Constant for yAxisTextPaint

    val yAxisTextPaint = remember(labelTextColor, density, yAxisPaintTextSize, yAxisPaintAlign) { // labelTextColor can change
        Paint().apply {
            color = labelTextColor.toArgb()
            textAlign = yAxisPaintAlign
            textSize = yAxisPaintTextSize
            isAntiAlias = true
        }
    }
    val yAxisLinePaint = remember(labelTextColor) { // Paint for the Y-axis line and grid lines
        androidx.compose.ui.graphics.Paint().apply {
            color = labelTextColor // Use a slightly dimmer or specific color for grid lines
            strokeWidth = 1f // Hairline
        }
    }

    // Y-Axis Scale Calculation (Moved to top level)
    val actualMaxValue = data.maxOfOrNull { it.value }?.coerceAtLeast(0f) ?: 0f
    val yAxisTopValue: Float
    val yTickCount: Int

    if (explicitYAxisTopValue != null && explicitYAxisTopValue > 0f) {
        yAxisTopValue = explicitYAxisTopValue
        // Determine yTickCount based on this explicit value.
        // Using ceil for consistency with the else branch, ensuring at least 1 tick if top > 0.
        yTickCount = ceil(yAxisTopValue).toInt().coerceAtLeast(1)
        Log.d("SimpleBarChartData", "Using explicitYAxisTopValue: $yAxisTopValue")
    } else {
        // Fallback to current behavior if explicitYAxisTopValue is null or not positive
        val actualMaxValueFromData = data.maxOfOrNull { it.value }?.coerceAtLeast(0f) ?: 0f
        if (actualMaxValueFromData == 0f) {
            yAxisTopValue = 5f // Default scale for an all-zero chart if no explicit override
            yTickCount = 5 // Corresponds to 0, 1, 2, 3, 4, 5 (or rather, 0, 2.5, 5 for drawing)
            Log.d("SimpleBarChartData", "Data is all zero, using default yAxisTopValue: $yAxisTopValue")
        } else {
            yAxisTopValue = ceil(actualMaxValueFromData).toFloat()
            yTickCount = yAxisTopValue.toInt().coerceAtLeast(1)
            Log.d("SimpleBarChartData", "Derived yAxisTopValue from data: $yAxisTopValue")
        }
    }
    // Log final decision
    Log.d("SimpleBarChartData", "Final yAxisTopValue: $yAxisTopValue, yTickCount: $yTickCount (Explicit used: ${explicitYAxisTopValue != null && explicitYAxisTopValue > 0f})")

    // Dynamic Y-Axis Label Area Width (Moved to top level)
    val yAxisLabelPadding = remember(density) { with(density) { 4.dp.toPx() } }
    val yAxisMaxLabelWidth = remember(yAxisTopValue, yTickCount, yAxisPaintTextSize, yAxisPaintAlign) { // Restored dynamic calculation
        val tempPaint = Paint().apply {
            textAlign = yAxisPaintAlign
            textSize = yAxisPaintTextSize
            isAntiAlias = true
            // Typeface could be set here if it were variable and passed as a key
        }
        if (yTickCount >= 0) {
            (0..yTickCount).maxOfOrNull { i ->
                val tickValue = if (yTickCount > 0) yAxisTopValue * (i.toFloat() / yTickCount) else 0f
                tempPaint.measureText(tickValue.toInt().toString())
            } ?: tempPaint.measureText("0") // Fallback for "0" if maxOfOrNull is null
        } else {
             tempPaint.measureText("0") // Fallback if yTickCount is somehow negative
        }
    }
    val yAxisLabelAreaWidth = yAxisMaxLabelWidth + yAxisLabelPadding * 2f // Ensure float arithmetic
    Log.d("SimpleBarChartData", "yAxisLabelAreaWidth: $yAxisLabelAreaWidth, yAxisMaxLabelWidth: $yAxisMaxLabelWidth")

    Canvas(
        modifier = modifier.semantics { contentDescription = chartContentDescription } // Added semantics
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalItems = data.size

        if (totalItems == 0) return@Canvas

        // Calculations dependent on canvas size remain here
        val chartAreaWidth = canvasWidth - yAxisLabelAreaWidth

        // Dynamic bar width and spacing calculation
        // Let bar width be ~70% and spacing ~30% of the available space per item
        val itemAvailableWidth = if (chartAreaWidth > 0 && totalItems > 0) chartAreaWidth / totalItems else 0f // Avoid division by zero
        val dynamicBarWidthPx = itemAvailableWidth * 0.7f
        val dynamicSpaceAroundBarsPx = itemAvailableWidth * 0.3f

        val xAxisLabelHeight = textPaint.textSize * 1.5f
        val valueTextHeight = valueTextPaint.textSize + with(density) { 4.dp.toPx() }
        val topPaddingForValueText = valueTextHeight

        val chartDrawableHeight = canvasHeight - xAxisLabelHeight - topPaddingForValueText

        // Draw Y-Axis Line and Labels
        val yAxisLineStart = Offset(yAxisLabelAreaWidth, topPaddingForValueText)
        val yAxisLineEnd = Offset(yAxisLabelAreaWidth, topPaddingForValueText + chartDrawableHeight)
        drawContext.canvas.drawLine(yAxisLineStart, yAxisLineEnd, yAxisLinePaint)

        // Define the Y-axis tick values to draw: 0, middle, and top.
        val tickValues = mutableListOf<Float>()
        tickValues.add(0f) // Always show 0

        if (yAxisTopValue > 0f) {
            val middleValue = yAxisTopValue / 2f
            // Add middle value if it's distinct enough from 0 and top, and not too close to 0.
            // This avoids clutter if the scale is very small (e.g. max value is 1 or 2)
            if (middleValue.toInt() > 0 && middleValue.toInt() < yAxisTopValue.toInt()) {
                tickValues.add(middleValue)
            }
            tickValues.add(yAxisTopValue) // Always show the top value
        } else { // Case for all zero data, yAxisTopValue is 5f
            tickValues.add(yAxisTopValue / 2f) // Show 2.5 (which becomes 2)
            tickValues.add(yAxisTopValue)    // Show 5
        }

        val distinctTickValues = tickValues.distinctBy { it.toInt() } // Ensure integer values are distinct before drawing

        distinctTickValues.forEach { tickValue ->
            val tickY = topPaddingForValueText + chartDrawableHeight * (1f - (if (yAxisTopValue > 0f) (tickValue / yAxisTopValue) else 0f))
            val tickLabel = tickValue.toInt().toString()

            // Optional: Draw horizontal grid line (can be uncommented if desired)
            // drawLine(
            //    color = labelTextColor.copy(alpha = 0.3f),
            //    start = Offset(yAxisLabelAreaWidth, tickY),
            //    end = Offset(canvasWidth, tickY)
            // )

            drawContext.canvas.nativeCanvas.drawText(
                tickLabel,
                yAxisLabelAreaWidth - yAxisLabelPadding,
                tickY + yAxisTextPaint.textSize / 3f,
                yAxisTextPaint
            )
        }

        // Adjust Bar and Label Drawing Coordinates
        data.forEachIndexed { index, item ->
            val barHeight = if (yAxisTopValue > 0f) (item.value.coerceAtMost(yAxisTopValue) / yAxisTopValue) * chartDrawableHeight else 0f // Added coerceAtMost
            val barColor = if (item.isHighlighted) highlightedBarColor else normalBarColor

            val currentBarLeft = yAxisLabelAreaWidth + (dynamicSpaceAroundBarsPx / 2) + (index * itemAvailableWidth)
            val barTop = topPaddingForValueText + chartDrawableHeight - barHeight

            // Draw bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = currentBarLeft, y = barTop),
                size = Size(width = dynamicBarWidthPx, height = barHeight),
                cornerRadius = CornerRadius(barCornerRadiusPx, barCornerRadiusPx)
            )

            // Draw value text
            val valueText = item.value.toInt().toString()
            val valueTextX = currentBarLeft + dynamicBarWidthPx / 2
            val valueTextY = if (barHeight > valueTextPaint.textSize * 0.8f) { // If bar is tall enough
                barTop - valueTextPaint.descent() - with(density) { 4.dp.toPx() }
            } else { // If bar is too short or zero
                topPaddingForValueText + chartDrawableHeight - valueTextPaint.descent() - with(density) {2.dp.toPx() }
            }
            drawContext.canvas.nativeCanvas.drawText(valueText, valueTextX, valueTextY, valueTextPaint)

            // Draw X-axis label
            drawContext.canvas.nativeCanvas.drawText(
                item.label,
                currentBarLeft + dynamicBarWidthPx / 2, // Center of the bar
                canvasHeight - textPaint.textSize / 2, // Below the bar
                textPaint
            )
        }
    }
}
