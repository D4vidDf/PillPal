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
    valueTextSizeSp: Float = 10f // New parameter
) {
    if (data.isEmpty()) {
        // Handle empty data case if necessary, or let the caller handle it.
        // For now, Canvas won't draw anything if data is empty with current logic.
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


    Canvas(modifier = modifier) { // Removed self-calculated width modifier
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalItems = data.size

        if (totalItems == 0) return@Canvas

        val yAxisLabelAreaWidth = with(density) { 30.dp.toPx() } // Space for Y-axis labels
        val chartAreaWidth = canvasWidth - yAxisLabelAreaWidth

        // Dynamic bar width and spacing calculation
        // Let bar width be ~70% and spacing ~30% of the available space per item
        val itemAvailableWidth = chartAreaWidth / totalItems
        val dynamicBarWidthPx = itemAvailableWidth * 0.7f
        val dynamicSpaceAroundBarsPx = itemAvailableWidth * 0.3f


        // Y-Axis Implementation
        val actualMaxValue = data.maxOfOrNull { it.value } ?: 0f
        val yAxisMaxValue = if (actualMaxValue < 3f) 3f else actualMaxValue // Ensure at least a few ticks
        val numberOfYTicks = 4 // Example: 0, max/3, 2*max/3, max

        val xAxisLabelHeight = textPaint.textSize * 1.5f // Space for X-axis labels
        val valueTextHeight = valueTextPaint.textSize + with(density) { 4.dp.toPx() } // Approx height for value text
        val topPaddingForValueText = valueTextHeight // Space above bars for value text

        val chartDrawableHeight = canvasHeight - xAxisLabelHeight - topPaddingForValueText

        // Draw Y-Axis Line and Labels
        val yAxisLineStart = Offset(yAxisLabelAreaWidth, topPaddingForValueText)
        val yAxisLineEnd = Offset(yAxisLabelAreaWidth, topPaddingForValueText + chartDrawableHeight)
        drawContext.canvas.drawLine(yAxisLineStart, yAxisLineEnd, yAxisLinePaint)

        for (i in 0..numberOfYTicks) {
            val tickValue = yAxisMaxValue * (i.toFloat() / numberOfYTicks)
            val tickY = topPaddingForValueText + chartDrawableHeight * (1f - (tickValue / yAxisMaxValue))

            // Optional: Draw horizontal grid line
            // drawLine(
            //    color = labelTextColor.copy(alpha = 0.3f), // Light grid lines
            //    start = Offset(yAxisLabelAreaWidth, tickY),
            //    end = Offset(canvasWidth, tickY)
            // )

            // Draw Y-axis label text
            drawContext.canvas.nativeCanvas.drawText(
                tickValue.toInt().toString(),
                yAxisLabelAreaWidth - with(density) { 4.dp.toPx() }, // Position left of Y-axis line
                tickY + yAxisTextPaint.textSize / 3, // Center text vertically on tick
                yAxisTextPaint
            )
        }

        // Adjust Bar and Label Drawing Coordinates
        data.forEachIndexed { index, item ->
            val barHeight = if (yAxisMaxValue > 0f) (item.value / yAxisMaxValue) * chartDrawableHeight else 0f
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
