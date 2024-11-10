// LightTheme.kt

package com.d4viddf.medicationreminder.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


// Definición del esquema de colores claro
private val LightColors = lightColorScheme(
    primary = DarkGreen, // Verde oscuro como color principal
    onPrimary = White, // Texto blanco sobre el verde oscuro

    secondary = LightGreenAccent, // Verde claro/accent como color secundario
    onSecondary = Color(0xFF000000), // Texto negro sobre el verde claro/accent

    tertiary = OrangeAccent, // Anaranjado como un tercer color para elementos destacados
    onTertiary = Color(0xFF000000), // Texto negro sobre el anaranjado

    background = White, // Fondo blanco
    onBackground = Color(0xFF000000), // Texto negro sobre fondo blanco

    surface = White, // Superficies blancas
    onSurface = Color(0xFF000000) // Texto negro sobre superficies
)

@Composable
fun MedicationReminderLightTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
