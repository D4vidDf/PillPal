package com.d4viddf.medicationreminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme // For fallback
import androidx.compose.material3.darkColorScheme // For fallback
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // For fallback

// Assuming AppTypography is in the same package, otherwise, this import needs to be adjusted.

// Basic fallback schemes if dynamic coloring is not available.
private val fallbackLightColorScheme = lightColorScheme(
    primary = Color(0xFF006970),
    secondary = Color(0xFF4A6364),
    tertiary = Color(0xFF525E7D),
    secondaryContainer = Color(0xFFC2F7FA), // Light Cyan
    onSecondaryContainer = Color(0xFF002022), // Very Dark Cyan
    // Explicitly define surface, onSurface, surfaceVariant, and onSurfaceVariant
    // for better control over dialog and card backgrounds/text in light mode.
    surface = Color(0xFFFAFDFD), // Very light cyan/almost white
    onSurface = Color(0xFF191C1C), // Very dark grey/cyan
    surfaceVariant = Color(0xFFDAE5E5), // Light greyish cyan (for card backgrounds etc.)
    onSurfaceVariant = Color(0xFF3F4949) // Darker grey/cyan (for text on surfaceVariant)
)

private val fallbackDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FD8E0),
    secondary = Color(0xFFB0CCCD),
    tertiary = Color(0xFFBAC6EA)
)

@Composable
fun MedicationSpecificTheme(
    medicationColor: MedicationColor,
    content: @Composable () -> Unit
) {
    val seedColor = medicationColor.backgroundColor // The seed color from MedicationColor
    // val context = LocalContext.current // Context no longer needed for dynamic theming
    val appIsDarkTheme = LocalAppUseDarkTheme.current // Consume here

    val colorScheme = if (appIsDarkTheme) { // Use appIsDarkTheme
        fallbackDarkColorScheme.copy(
            primary = seedColor, // which is medicationColor.backgroundColor
            onPrimary = medicationColor.textColor,
            primaryContainer = medicationColor.progressBackColor,
            onPrimaryContainer = medicationColor.progressBarColor,
            secondary = medicationColor.progressBackColor, // Using same as primaryContainer for now
            onSecondary = medicationColor.progressBarColor, // Using same as onPrimaryContainer for now
            secondaryContainer = medicationColor.cardColor,
            onSecondaryContainer = medicationColor.onBackgroundColor
            // Other colors like tertiary, background, surface, error will come from fallbackDarkColorScheme
        )
    } else {
        fallbackLightColorScheme.copy(
            primary = seedColor, // which is medicationColor.backgroundColor
            onPrimary = medicationColor.textColor,
            primaryContainer = medicationColor.progressBackColor,
            onPrimaryContainer = medicationColor.progressBarColor,
            secondary = medicationColor.progressBackColor,
            onSecondary = medicationColor.progressBarColor,
            secondaryContainer = medicationColor.cardColor,
            onSecondaryContainer = medicationColor.onBackgroundColor
            // Other colors will come from fallbackLightColorScheme
        )
    }
    // The Build.VERSION.SDK_INT check is removed as the logic is now unified.
    // If it were to be kept, both branches of the SDK check would contain the above .copy() logic.

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MaterialTheme.shapes,
        typography = AppTypography, // Make sure AppTypography is imported
        content = content
    )
}
