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
    secondary = Color(0xFF03A9F4), // Light Blue
    onSecondary = Color.Black,
    error = Color(0xFFF44336), // Red
    onError = Color.White,
    background = Color(0xFF1C1B1F), // Standard M3 Dark Background
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F), // Surface often matches background in dark themes for Wear
    onSurface = Color(0xFFE6E1E5), // Text/icons on surface
    surfaceVariant = Color(0xFF2C2B2F), // A slightly lighter variant for card backgrounds etc.
    onSurfaceVariant = Color(0xFFCAC4D0), // Text/icons on surfaceVariant
    outline = Color(0xFF938F99)
    // Let other container colors, tertiary etc. be defaulted by the MaterialTheme
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