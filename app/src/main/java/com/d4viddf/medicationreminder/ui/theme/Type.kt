// com/d4viddf/medicationreminder/ui/common/theme/Type.kt

package com.d4viddf.medicationreminder.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.d4viddf.medicationreminder.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Keep Lato for body text - it's great for readability.
val bodyFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Lato"),
        fontProvider = provider,
    )
)

// Use a more expressive font for headlines. Playfair Display is an excellent choice.
val displayFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Roboto Flex"),
        fontProvider = provider,
    )
)

// Default Material 3 typography values
private val baseline = Typography()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
    displayLargeEmphasized = baseline.displayLargeEmphasized.copy(fontFamily = displayFontFamily),
    displayMediumEmphasized = baseline.displayMediumEmphasized.copy(fontFamily = displayFontFamily),
    displaySmallEmphasized = baseline.displaySmallEmphasized.copy(fontFamily = displayFontFamily),
    headlineLargeEmphasized = baseline.headlineLargeEmphasized.copy(fontFamily = displayFontFamily),
    headlineMediumEmphasized = baseline.headlineMediumEmphasized.copy(fontFamily = displayFontFamily),
    headlineSmallEmphasized = baseline.headlineSmallEmphasized.copy(fontFamily = displayFontFamily),
    titleLargeEmphasized = baseline.titleLargeEmphasized.copy(fontFamily = displayFontFamily),
    titleMediumEmphasized = baseline.titleMediumEmphasized.copy(fontFamily = displayFontFamily),
    titleSmallEmphasized = baseline.titleSmallEmphasized.copy(fontFamily = displayFontFamily),
    bodyLargeEmphasized = baseline.bodyLargeEmphasized.copy(fontFamily = bodyFontFamily),
    bodyMediumEmphasized = baseline.bodyMediumEmphasized.copy(fontFamily = bodyFontFamily),
    bodySmallEmphasized = baseline.bodySmallEmphasized.copy(fontFamily = bodyFontFamily),
    labelLargeEmphasized = baseline.labelLargeEmphasized.copy(fontFamily = bodyFontFamily),
    labelMediumEmphasized = baseline.labelMediumEmphasized.copy(fontFamily = bodyFontFamily),
    labelSmallEmphasized = baseline.labelSmallEmphasized.copy(fontFamily = bodyFontFamily),
)