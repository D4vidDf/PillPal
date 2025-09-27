package com.d4viddf.medicationreminder.ui.features.common.charts

import java.time.LocalDate

data class ChartDataPoint(
    val value: Float,       // The value for the bar height (e.g., 2000f for ml of water)
    val label: String,      // The label for the X-axis (e.g., "L", "M", "X")
    val fullLabel: String,  // The full label for the tooltip (e.g., "Lunes, 11 Ago")
    val date: LocalDate
)
