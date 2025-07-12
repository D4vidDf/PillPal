package com.d4viddf.medicationreminder.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme // M3 import
import androidx.wear.compose.material3.ColorScheme   // M3 import
import androidx.wear.compose.material3.Typography   // M3 import
// Shapes can be defined if needed, but default M3 Wear shapes are often sufficient
// import androidx.wear.compose.material3.Shapes

// Define M3 Color Scheme for Compose Material 3
// Simplified to address potential constructor issues with surface/surfaceVariant
private val WearAppColorScheme = ColorScheme(
    primary = Color(0xFF4CAF50), // Green
    onPrimary = Color.Black,
    // primaryContainer = Color(0xFFC8E6C9), // Defaulted
    // onPrimaryContainer = Color.Black,    // Defaulted
    secondary = Color(0xFF03A9F4), // Light Blue
    onSecondary = Color.Black,
    // secondaryContainer = Color(0xFFB3E5FC), // Defaulted
    // onSecondaryContainer = Color.Black,     // Defaulted
    // tertiary = Color(0xFFFFC107), // Amber - Defaulted
    // onTertiary = Color.Black,        // Defaulted
    // tertiaryContainer = Color(0xFFFFECB3),// Defaulted
    // onTertiaryContainer = Color.Black,   // Defaulted
    error = Color(0xFFF44336), // Red
    onError = Color.White,
    // errorContainer = Color(0xFFFFCDD2), // Defaulted
    // onErrorContainer = Color.Black,    // Defaulted
    background = Color(0xFF1C1B1F), // Standard M3 Dark Background
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F), // Explicitly define surface, often same as background
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F), // Explicitly define surfaceVariant
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
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