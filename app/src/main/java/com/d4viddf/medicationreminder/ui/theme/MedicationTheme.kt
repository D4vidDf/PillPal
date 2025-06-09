package com.d4viddf.medicationreminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme // For fallback
import androidx.compose.material3.darkColorScheme // For fallback
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // For fallback
import androidx.compose.ui.platform.LocalContext
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
// Assuming AppTypography is in the same package, otherwise, this import needs to be adjusted.
import com.d4viddf.medicationreminder.ui.theme.AppTypography

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
            primary = seedColor,
            onPrimary = medicationColor.textColor
            // Other MedicationColor properties can be mapped here if suitable
            // e.g., secondary = medicationColor.progressBarColor,
            // surface = medicationColor.cardColor (ensure cardColor is appropriate for surface)
        )
    } else {
        fallbackLightColorScheme.copy(
            primary = seedColor,
            onPrimary = medicationColor.textColor
        )
    }
    // The Build.VERSION.SDK_INT check is removed as the logic is now unified.
    // If it were to be kept, both branches of the SDK check would contain the above .copy() logic.

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Make sure AppTypography is imported
        content = content
    )
}
