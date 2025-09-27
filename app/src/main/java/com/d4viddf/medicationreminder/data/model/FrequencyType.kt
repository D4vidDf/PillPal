package com.d4viddf.medicationreminder.data.model

import androidx.annotation.StringRes
import com.d4viddf.medicationreminder.R

enum class FrequencyType(@StringRes val stringResId: Int) {
    ONCE_A_DAY(R.string.frequency_once_a_day),
    MULTIPLE_TIMES_A_DAY(R.string.frequency_multiple_times_a_day),
    INTERVAL(R.string.frequency_interval);

    companion object {
        // Optional: A function to get FrequencyType from a string if needed for migration or parsing
        // For this refactor, we'll primarily be using the enum directly.
    }
}