package com.d4viddf.medicationreminder.wear.data

data class ProgressDetails(
    val taken: Int,
    val remaining: Int,
    val total: Int,
    val progressFraction: Float,
    val displayText: String,
    val lastTaken: String?
)
