package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.Canvas
// import androidx.compose.foundation.layout.fillMaxWidth // Not directly used in SimpleBarChart itself
// import androidx.compose.foundation.layout.height // Not directly used in SimpleBarChart itself
import androidx.compose.foundation.layout.width // Added import for Modifier.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    spaceAroundBarsDp: Dp = 8.dp // Default space between and around bars
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
            // Add typeface or other properties if needed
        }
    }

    val barWidthPx = with(density) { barWidthDp.toPx() }
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
        if (maxValue == 0f && data.any { it.value != 0f }) { // Check if not all values are zero before returning
            // If maxValue is 0 due to all values being 0, we might still want to draw labels or a baseline.
            // For now, if all actual values are indeed 0, this will prevent division by zero and draw nothing.
             return@Canvas
        }


        data.forEachIndexed { index, item ->
            // barHeight calculation should use the drawable area height
            val drawableHeight = canvasHeight - textPaint.textSize * 2 // Space for X-axis labels
            val barHeight = if (maxValue > 0f) (item.value / maxValue) * drawableHeight else 0f
            val barColor = if (item.isHighlighted) highlightedBarColor else normalBarColor

            // Recalculate barLeft and itemWidthPx if yAxisLabelAreaWidth is > 0
            // For now, assuming it's 0f, so original calculations for barLeft are fine relative to yAxisLabelAreaWidth.
            val currentBarLeft = yAxisLabelAreaWidth + spaceAroundBarsPx + (index * itemWidthPx)
            val barTop = canvasHeight - barHeight - (textPaint.textSize * 1.5f) // Position bar above label

            // Draw bar
            drawRect(
                color = barColor,
                topLeft = Offset(x = currentBarLeft, y = barTop),
                size = Size(width = barWidthPx, height = barHeight)
            )

            // Draw label
            drawContext.canvas.nativeCanvas.drawText(
                item.label,
                currentBarLeft + barWidthPx / 2, // Center of the bar
                canvasHeight - textPaint.textSize / 2, // Below the bar
                textPaint
            )
        }
    }
}
