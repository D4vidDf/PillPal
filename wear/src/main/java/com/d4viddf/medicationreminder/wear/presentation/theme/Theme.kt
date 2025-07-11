package com.d4viddf.medicationreminder.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.protolayout.material3.ColorScheme

// Define M3 Color Scheme
private val LightColors = ColorScheme(
    primary = Color(0xFF4CAF50), // Green
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF03A9F4), // Light Blue
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFB3E5FC),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFFFFC107), // Amber
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFECB3),
    onTertiaryContainer = Color.Black,
    error = Color(0xFFF44336), // Red
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color.Black,
    background = Color(0xFF121212), // Dark background for Wear OS
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0), // Lighter grey for less emphasis
    outline = Color(0xFF505050)
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