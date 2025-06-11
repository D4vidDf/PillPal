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

    val barWidthPx = with(density) { barWidthDp.toPx() }
    val barCornerRadiusPx = with(density) { barCornerRadiusDp.toPx() }
    val spaceAroundBarsPx = with(density) { spaceAroundBarsDp.toPx() }
    // Total space for one bar + its surrounding space on one side (e.g., right side)
    val itemWidthPx = barWidthPx + spaceAroundBarsPx
    // Minimum width is sum of all item widths + initial space on the left.
    val calculatedMinWidthPx = (data.size * itemWidthPx) + spaceAroundBarsPx // This is the content width

    Canvas(
        modifier = modifier.then(
            Modifier.width(with(density) { calculatedMinWidthPx.toDp() })
        )
    ) {
        val canvasHeight = size.height
        // val canvasWidth = size.width // This would now be calculatedMinWidthPx, effectively

        // val yAxisLabelAreaWidth = 40.dp.toPx() // Example: space for Y-axis labels
        val yAxisLabelAreaWidth = 0f // No Y-axis value labels for now, so no space reserved

        val maxValue = data.maxOfOrNull { it.value } ?: 0f
        // Removed: if (maxValue == 0f && data.any { it.value != 0f }) { return@Canvas }


        data.forEachIndexed { index, item ->
            // barHeight calculation should use the drawable area height
            // Adjust drawableHeight to account for value text as well if it's drawn above the bar
            val spaceForXAxisLabels = textPaint.textSize * 1.5f // Approximate height for x-axis labels
            val spaceForValueText = valueTextPaint.textSize + with(density) { 4.dp.toPx() } // Approximate height for value text + gap
            val drawableHeight = canvasHeight - spaceForXAxisLabels - spaceForValueText // Adjusted drawable height

            val barHeight = if (maxValue > 0f) (item.value / maxValue) * drawableHeight else 0f
            val barColor = if (item.isHighlighted) highlightedBarColor else normalBarColor

            // Recalculate barLeft and itemWidthPx if yAxisLabelAreaWidth is > 0
            // For now, assuming it's 0f, so original calculations for barLeft are fine relative to yAxisLabelAreaWidth.
            val currentBarLeft = yAxisLabelAreaWidth + spaceAroundBarsPx + (index * itemWidthPx)
            // barTop is now calculated from the top of the value text area
            val barTop = canvasHeight - barHeight - spaceForXAxisLabels

            // Draw bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = currentBarLeft, y = barTop),
                size = Size(width = barWidthPx, height = barHeight),
                cornerRadius = CornerRadius(barCornerRadiusPx, barCornerRadiusPx)
            )

            // Draw value text
            val valueText = item.value.toInt().toString() // Or String.format for decimals
            // Position value text above the bar
            val valueTextYPosition = barTop - valueTextPaint.descent() - with(density) { 4.dp.toPx() }

            // Adjust Y position for value text if bar height is too small to avoid overlap with X-axis labels
            // This ensures value text is drawn within the allocated spaceForValueText or just above X-axis if bar is zero
            val textYPos = if (barHeight > (valueTextPaint.textSize / 2) ) { // If bar is tall enough to have text clearly above it
                               valueTextYPosition
                           } else { // If bar is too short or zero, position text at the top of where the bar would start (within its value text area)
                               canvasHeight - spaceForXAxisLabels - valueTextPaint.descent() - with(density) { 2.dp.toPx()}
                           }

            drawContext.canvas.nativeCanvas.drawText(
                valueText,
                currentBarLeft + barWidthPx / 2, // Center of the bar
                textYPos, // Use adjusted Y position
                valueTextPaint
            )

            // Draw X-axis label
            drawContext.canvas.nativeCanvas.drawText(
                item.label,
                currentBarLeft + barWidthPx / 2, // Center of the bar
                canvasHeight - textPaint.textSize / 2, // Below the bar, within its allocated space
                textPaint
            )
        }
    }
}
