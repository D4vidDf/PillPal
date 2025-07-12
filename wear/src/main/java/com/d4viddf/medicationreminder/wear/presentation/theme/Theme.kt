package com.d4viddf.medicationreminder.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.Typography

// Define M3 Color Scheme for Compose Material 3 - Minimal Valid Set
private val WearAppColorScheme = ColorScheme(
    primary = Color(0xFF4CAF50), // Green
    onPrimary = Color.Black,
    secondary = Color(0xFF03A9F4), // Light Blue
    onSecondary = Color.Black,
    error = Color(0xFFF44336), // Red
    onError = Color.White,
    background = Color(0xFF1C1B1F), // Standard M3 Dark Background
    onBackground = Color(0xFFE6E1E5),
    // Explicitly define surface, onSurface, surfaceVariant, onSurfaceVariant if they were the specific issue
    // If the error was "No parameter with name 'surface' found", then these should NOT be here.
    // The previous attempt that failed omitted these, which is what I'm providing again as the most likely fix
    // if the constructor is more constrained than typical M3.
    // However, if the issue was an *import* of ColorScheme, then a full definition might work once imports are fixed.
    // Given the error, let's assume the Wear M3 ColorScheme constructor is more minimal or defaults these heavily.
    outline = Color(0xFF938F99)
    // If the above still fails, try with ONLY primary, onPrimary, background, onBackground, error, onError
)

val AppTypography = Typography() // Using default M3 Wear Typography

@Composable
fun MedicationReminderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearAppColorScheme,
        typography = AppTypography,
        content = content
    )
}