package com.d4viddf.medicationreminder.ui.features.healthdata.util

import androidx.annotation.StringRes

sealed class DateRangeText {
    data class StringResource(@StringRes val resId: Int) : DateRangeText()
    data class FormattedString(val text: String) : DateRangeText()
}
