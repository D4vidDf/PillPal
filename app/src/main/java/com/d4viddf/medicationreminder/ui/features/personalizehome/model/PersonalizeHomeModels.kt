package com.d4viddf.medicationreminder.ui.features.personalizehome.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

// Represents a single configurable item within a section (e.g., "Today Progress" card)
data class HomeItem(
    val id: String,
    @StringRes val nameRes: Int,
    var isVisible: Boolean = true,
    var displayUnit: String? = null // For "next dose" card: "minutes" or "seconds"
)

// Represents a section on the home screen (e.g., "Progress", "Health")
data class HomeSection(
    val id: String,
    @StringRes val nameRes: Int,
    var isVisible: Boolean = true,
    val items: List<HomeItem>
)