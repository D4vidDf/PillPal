package com.d4viddf.medicationreminder.ui.features.healthdata

import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChartPoint

data class HeartRateChartData(
    val lineChartData: List<LineChartPoint> = emptyList(),
    val labels: List<String> = emptyList()
)
