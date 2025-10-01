package com.d4viddf.medicationreminder.ui.common.util

import java.text.NumberFormat
import java.util.Locale

fun formatNumber(number: Number): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).format(number)
}
