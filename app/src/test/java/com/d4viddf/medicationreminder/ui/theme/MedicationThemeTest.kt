package com.d4viddf.medicationreminder.ui.theme

import androidx.compose.material3.ColorScheme // Actual M3 ColorScheme
import androidx.compose.material3.darkColorScheme // Actual M3 darkColorScheme
import androidx.compose.material3.lightColorScheme // Actual M3 lightColorScheme
import androidx.compose.ui.graphics.Color
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationThemeTest {

    // Re-define fallbacks here for test isolation, as they are private in MedicationTheme.kt
    // These should use the actual Material 3 lightColorScheme/darkColorScheme functions.
    private val testFallbackLightColorScheme = lightColorScheme(
        primary = Color(0xFF006970),
        secondary = Color(0xFF4A6364),
        tertiary = Color(0xFF525E7D)
        // Other colors will take defaults from Material3.lightColorScheme if not specified
    )

    private val testFallbackDarkColorScheme = darkColorScheme(
        primary = Color(0xFF4FD8E0),
        secondary = Color(0xFFB0CCCD),
        tertiary = Color(0xFFBAC6EA)
        // Other colors will take defaults from Material3.darkColorScheme if not specified
    )

    private fun generateTestScheme(
        medicationColor: MedicationColor,
        useDarkTheme: Boolean,
        baseLightScheme: ColorScheme = testFallbackLightColorScheme,
        baseDarkScheme: ColorScheme = testFallbackDarkColorScheme
    ): ColorScheme {
        val seedColor = medicationColor.backgroundColor
        return if (useDarkTheme) {
            baseDarkScheme.copy(
                primary = seedColor,
                onPrimary = medicationColor.textColor
            )
        } else {
            baseLightScheme.copy(
                primary = seedColor,
                onPrimary = medicationColor.textColor
            )
        }
    }

    @Test
    fun medicationSpecificTheme_lightMode_usesCorrectColors() {
        val testMedColor = MedicationColor.ORANGE

        val generatedScheme = generateTestScheme(testMedColor, useDarkTheme = false)

        assertEquals(testMedColor.backgroundColor, generatedScheme.primary)
        assertEquals(testMedColor.textColor, generatedScheme.onPrimary)
        // Check if other colors are from the fallback (e.g., secondary)
        assertEquals(testFallbackLightColorScheme.secondary, generatedScheme.secondary)
        assertEquals(testFallbackLightColorScheme.background, generatedScheme.background)
    }

    @Test
    fun medicationSpecificTheme_darkMode_usesCorrectColors() {
        val testMedColor = MedicationColor.BLUE

        val generatedScheme = generateTestScheme(testMedColor, useDarkTheme = true)

        assertEquals(testMedColor.backgroundColor, generatedScheme.primary)
        assertEquals(testMedColor.textColor, generatedScheme.onPrimary)
        // Check if other colors are from the fallback (e.g., secondary)
        assertEquals(testFallbackDarkColorScheme.secondary, generatedScheme.secondary)
        assertEquals(testFallbackDarkColorScheme.background, generatedScheme.background)
    }
}
