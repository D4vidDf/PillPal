package com.d4viddf.medicationreminder.ui.features.medication.graph.component

import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.ui.features.healthdata.util.ChartType
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    chartType: ChartType = ChartType.BAR,
    timeRange: TimeRange = TimeRange.DAY,
    highlightedBarColor: Color = MaterialTheme.colorScheme.primary,
    normalBarColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueTextColor: Color = MaterialTheme.colorScheme.onSurface,
    goalLineColor: Color = MaterialTheme.colorScheme.error,
    barWidthDp: Dp = 24.dp, // Default bar width
    spaceAroundBarsDp: Dp = 8.dp, // Default space between and around bars
    barCornerRadiusDp: Dp = 4.dp,
    valueTextSizeSp: Float = 10f,
    chartContentDescription: String,
    explicitYAxisTopValue: Float? = null,
    goalLineValue: Float? = null,
    onBarClick: ((label: String) -> Unit)? = null
) {
    // ... (rest of the code is the same until the Canvas)

    Canvas(
        // ... (modifier)
    ) {
        // ... (canvas setup)

        if (chartType == ChartType.POINT) {
            data.forEach { item ->
                val x = 0f // ... calculate x
                val y = 0f // ... calculate y
                drawCircle(
                    color = normalBarColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        } else { // BAR chart
            // ... (existing bar chart drawing logic)
        }

        // Draw Goal Line
        // ...

        // Draw X-axis labels
        val xAxisLabelFormatter = when (timeRange) {
            TimeRange.DAY -> DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
            TimeRange.WEEK -> DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault())
            TimeRange.MONTH -> DateTimeFormatter.ofPattern("d").withZone(ZoneId.systemDefault())
            TimeRange.YEAR -> DateTimeFormatter.ofPattern("MMM").withZone(ZoneId.systemDefault())
        }
        val labelCount = when (timeRange) {
            TimeRange.DAY -> 6 // Every 4 hours
            TimeRange.WEEK -> 7 // Every day
            TimeRange.MONTH -> 6 // Every 5 days
            TimeRange.YEAR -> 12 // Every month
        }
        // ... (rest of the drawing logic)
    }
}
