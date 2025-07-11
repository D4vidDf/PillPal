package com.d4viddf.medicationreminder.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.protolayout.material.LayoutElementBuilders.LayoutColor
import androidx.wear.protolayout.material3.ColorScheme

// Define M3 Color Scheme
private val LightColors = ColorScheme(
    primary = LayoutColor(Color(0xFF4CAF50).value.toInt()), // Green
    onPrimary = LayoutColor(Color.Black.value.toInt()),
    primaryContainer = LayoutColor(Color(0xFFC8E6C9).value.toInt()),
    onPrimaryContainer = LayoutColor(Color.Black.value.toInt()),
    secondary = LayoutColor(Color(0xFF03A9F4).value.toInt()), // Light Blue
    onSecondary = LayoutColor(Color.Black.value.toInt()),
    secondaryContainer = LayoutColor(Color(0xFFB3E5FC).value.toInt()),
    onSecondaryContainer = LayoutColor(Color.Black.value.toInt()),
    tertiary = LayoutColor(Color(0xFFFFC107).value.toInt()), // Amber
    onTertiary = LayoutColor(Color.Black.value.toInt()),
    tertiaryContainer = LayoutColor(Color(0xFFFFECB3).value.toInt()),
    onTertiaryContainer = LayoutColor(Color.Black.value.toInt()),
    error = LayoutColor(Color(0xFFF44336).value.toInt()), // Red
    onError = LayoutColor(Color.White.value.toInt()),
    errorContainer = LayoutColor(Color(0xFFFFCDD2).value.toInt()),
    onErrorContainer = LayoutColor(Color.Black.value.toInt()),
    background = LayoutColor(Color(0xFF121212).value.toInt()), // Dark background for Wear OS
    onBackground = LayoutColor(Color.White.value.toInt()),
    onSurface = LayoutColor(Color.White.value.toInt()),
    onSurfaceVariant = LayoutColor(Color(0xFFB0B0B0).value.toInt()), // Lighter grey for less emphasis
    outline = LayoutColor(Color(0xFF505050).value.toInt())
)

// Define M3 Typography (using defaults or customize as needed)
// For Wear OS, typography is often simpler. Using default M3 Wear typography is a good start.
// val AppTypography = Typography() // Default M3 Wear Typography will be used if not specified.

@Composable
fun MedicationReminderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = MaterialTheme.colors, // Using our defined M3 colors
        // typography = AppTypography, // Optionally provide custom typography
        content = content
    )
}