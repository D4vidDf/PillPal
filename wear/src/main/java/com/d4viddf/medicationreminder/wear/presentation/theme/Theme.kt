package com.d4viddf.medicationreminder.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme // M3 import
import androidx.wear.compose.material3.ColorScheme   // M3 import
import androidx.wear.compose.material3.Typography   // M3 import
// Shapes can be defined if needed, but default M3 Wear shapes are often sufficient
// import androidx.wear.compose.material3.Shapes

// Define M3 Color Scheme for Compose Material 3
private val WearAppColorScheme = ColorScheme(
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
    background = Color(0xFF1C1B1F), // Standard M3 Dark Background
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F), // Usually same as background for Wear M3
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F), // M3 surface variant
    onSurfaceVariant = Color(0xFFCAC4D0), // M3 onSurfaceVariant
    outline = Color(0xFF938F99) // M3 outline
    // surfaceContainerHighest, surfaceBright, etc. can be defined if needed
)

// Define M3 Typography for Compose Material 3
// Using default M3 Wear typography is often a good start and recommended.
// If specific customizations are needed, they can be defined here.
// For example:
/*
val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    // ... other text styles
)
*/
// If not providing a custom Typography, MaterialTheme will use its defaults.
val AppTypography = Typography() // Using default M3 Wear Typography

@Composable
fun MedicationReminderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearAppColorScheme,
        typography = AppTypography, // Provide the M3 Typography
        // shapes = AppShapes, // Optionally provide custom shapes
        content = content
    )
}