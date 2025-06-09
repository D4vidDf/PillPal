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
    tertiary = Color(0xFF525E7D)
)

private val fallbackDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FD8E0),
    secondary = Color(0xFFB0CCCD),
    tertiary = Color(0xFFBAC6EA)
)

@Composable
fun MedicationSpecificTheme(
    medicationColor: MedicationColor,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val seedColor = medicationColor.backgroundColor // The seed color from MedicationColor
    val context = LocalContext.current

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (useDarkTheme) {
            dynamicDarkColorScheme(context, seedColor)
        } else {
            dynamicLightColorScheme(context, seedColor)
        }
    } else {
        if (useDarkTheme) {
            fallbackDarkColorScheme.copy(
                primary = seedColor,
                onPrimary = medicationColor.textColor,
                primaryContainer = seedColor.copy(alpha = 0.8f) // Corrected: ensure alpha is float
            )
        } else {
            fallbackLightColorScheme.copy(
                primary = seedColor,
                onPrimary = medicationColor.textColor,
                primaryContainer = seedColor.copy(alpha = 0.3f) // Corrected: ensure alpha is float
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Make sure AppTypography is imported
        content = content
    )
}
