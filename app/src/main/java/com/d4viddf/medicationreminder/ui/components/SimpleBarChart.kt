package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.Canvas
// import androidx.compose.foundation.layout.fillMaxWidth // Not directly used in SimpleBarChart itself
// import androidx.compose.foundation.layout.height // Not directly used in SimpleBarChart itself
import androidx.compose.foundation.layout.width // Added import for Modifier.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius // Added import
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb // Import for toArgb
import androidx.compose.ui.graphics.Paint as ComposePaint // Alias to avoid conflict if Android Paint is used
import android.graphics.Paint // For text drawing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil // Added import
import android.util.Log // Added for logging
import androidx.compose.ui.semantics.contentDescription // Added for Semantics
import androidx.compose.ui.semantics.semantics // Added for Semantics

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
    barCornerRadiusDp: Dp = 4.dp, // New parameter
    valueTextColor: Color = labelTextColor, // New parameter
    valueTextSizeSp: Float = 10f, // New parameter
    chartContentDescription: String // New parameter for accessibility
) {
    // Diagnostic Logging
    Log.d("SimpleBarChartData", "Input data: ${data.map { it.value }}")
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

    // Y-axis Paint (can be same as textPaint or customized)
    val yAxisTextPaint = remember(labelTextColor, density) {
        Paint().apply {
            color = labelTextColor.toArgb()
            textAlign = Paint.Align.RIGHT // Align to the right for Y-axis labels
            textSize = with(density) { 10.sp.toPx() } // Smaller text for Y-axis
            isAntiAlias = true
        }
    }
    val yAxisLinePaint = remember(labelTextColor) { // Paint for the Y-axis line and grid lines
        androidx.compose.ui.graphics.Paint().apply {
            color = labelTextColor // Use a slightly dimmer or specific color for grid lines
            strokeWidth = 1f // Hairline
        }
    }


    Canvas(
        modifier = modifier.semantics { contentDescription = chartContentDescription } // Added semantics
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalItems = data.size

        if (totalItems == 0) return@Canvas

        // Y-Axis Scale Calculation (Refined) - This part is already updated from previous step.
        // ... (yAxisTopValue and yTickCount calculation remains here) ...

        // Dynamic Y-Axis Label Area Width
        val yAxisLabelPadding = with(density) { 4.dp.toPx() }
        val yAxisMaxLabelWidth = remember(yAxisTopValue, yAxisTextPaint, yTickCount) {
            if (yTickCount >= 0) { // Ensure yTickCount is not negative, loop 0..0 is valid for single tick "0"
                (0..yTickCount).maxOfOrNull { i ->
                    val tickValue = if (yTickCount > 0) yAxisTopValue * (i.toFloat() / yTickCount) else 0f
                    yAxisTextPaint.measureText(tickValue.toInt().toString())
                } ?: yAxisTextPaint.measureText("0") // Fallback for "0" if maxOfOrNull is null
            } else {
                 yAxisTextPaint.measureText("0") // Fallback if yTickCount is somehow negative
            }
        }
        val yAxisLabelAreaWidth = yAxisMaxLabelWidth + yAxisLabelPadding * 2
        Log.d("SimpleBarChartData", "yAxisLabelAreaWidth: $yAxisLabelAreaWidth, yAxisMaxLabelWidth: $yAxisMaxLabelWidth")


        val chartAreaWidth = canvasWidth - yAxisLabelAreaWidth

        // Dynamic bar width and spacing calculation
        // Let bar width be ~70% and spacing ~30% of the available space per item
        val itemAvailableWidth = chartAreaWidth / totalItems
        val dynamicBarWidthPx = itemAvailableWidth * 0.7f
        val dynamicSpaceAroundBarsPx = itemAvailableWidth * 0.3f


        // Y-Axis Implementation (The older duplicated block has been removed)
        val actualMaxValue = data.maxOfOrNull { it.value }?.coerceAtLeast(0f) ?: 0f
        val yAxisTopValue: Float
        val yTickCount: Int

        if (actualMaxValue == 0f) {
            yAxisTopValue = 5f // Default scale for an all-zero chart, showing ticks up to 5.
            yTickCount = 5
        } else { // actualMaxValue is > 0 (guaranteed to be integer counts like 1.0, 2.0 etc from ViewModel)
            yAxisTopValue = ceil(actualMaxValue).toFloat()
            // If yAxisTopValue is 1.0, yTickCount will be 1 (labels 0, 1).
            // If yAxisTopValue is 3.0, yTickCount will be 3 (labels 0,1,2,3).
            // If yAxisTopValue is 6.0, yTickCount will be 6 (labels 0,1,2,3,4,5,6).
            yTickCount = yAxisTopValue.toInt().coerceAtLeast(1)
        }
        Log.d("SimpleBarChartData", "yAxisTopValue: $yAxisTopValue, yTickCount: $yTickCount")

        val xAxisLabelHeight = textPaint.textSize * 1.5f
        val valueTextHeight = valueTextPaint.textSize + with(density) { 4.dp.toPx() }
        val topPaddingForValueText = valueTextHeight

        val chartDrawableHeight = canvasHeight - xAxisLabelHeight - topPaddingForValueText

        // Draw Y-Axis Line and Labels
        val yAxisLineStart = Offset(yAxisLabelAreaWidth, topPaddingForValueText)
        val yAxisLineEnd = Offset(yAxisLabelAreaWidth, topPaddingForValueText + chartDrawableHeight)
        drawContext.canvas.drawLine(yAxisLineStart, yAxisLineEnd, yAxisLinePaint)

        for (i in 0..yTickCount) {
            val tickValue = if (yTickCount > 0) yAxisTopValue * (i.toFloat() / yTickCount) else 0f
            val tickY = topPaddingForValueText + chartDrawableHeight * (1f - (if (yAxisTopValue > 0f) (tickValue / yAxisTopValue) else 0f))

            // Optional: Draw horizontal grid line
            // drawLine(
            //    color = labelTextColor.copy(alpha = 0.3f), // Light grid lines
            //    start = Offset(yAxisLabelAreaWidth, tickY),
            //    end = Offset(canvasWidth, tickY)
            // )
            val tickLabel = tickValue.toInt().toString() // Define the label text
            drawContext.canvas.nativeCanvas.drawText(
                tickLabel, // Use defined tickLabel
                yAxisLabelAreaWidth - yAxisLabelPadding, // Position text with padding
                tickY + yAxisTextPaint.textSize / 3f, // Adjust for vertical centering
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
