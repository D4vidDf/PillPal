package com.d4viddf.medicationreminder.ui.theme

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.d4viddf.medicationreminder.R

// Define an enum for the color options
enum class MedicationColor(
    @StringRes val colorNameResId: Int, // Add a field for the color name
    val colorValue: Color,
    val backgroundColor: Color,
    val textColor: Color,
    val cardColor: Color,
    val onBackgroundColor: Color,
    val progressBackColor: Color,
    val progressBarColor: Color,
) {
    ORANGE(
        colorNameResId = R.string.color_orange,
        colorValue = Color(0xFFd96126),
        backgroundColor = Color(0xFFd96126),
        textColor = Color(0xFFfffdf3),
        cardColor = Color(0xFF2c1407),
        onBackgroundColor = Color(0xFFfffefe),
        progressBackColor = Color(0xFFde7a46),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    PINK(
        colorNameResId = R.string.color_pink,
        colorValue = Color(0xFFd6418c),
        backgroundColor = Color(0xFFd6418c),
        textColor = Color(0xFFfffdff),
        cardColor = Color(0xFF2c0d1d),
        onBackgroundColor = Color(0xFFfffcff),
        progressBackColor = Color(0xFFdd5e9d),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    GREEN(
        colorNameResId = R.string.color_green,
        colorValue = Color(0xFF009965),
        backgroundColor = Color(0xFF009965),
        textColor = Color(0xFFe2fffa),
        cardColor = Color(0xFF001f13),
        onBackgroundColor = Color(0xFFfdfffd),
        progressBackColor = Color(0xFF27a87d),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    BLUE(
        colorNameResId = R.string.color_blue,
        colorValue = Color(0xFF269dd8),
        backgroundColor = Color(0xFF269dd8),
        textColor = Color(0xFFfeffff),
        cardColor = Color(0xFF071f2c),
        onBackgroundColor = Color(0xFFfbfeff),
        progressBackColor = Color(0xFF47abdf),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    PURPLE(
        colorNameResId = R.string.color_purple,
        colorValue = Color(0xFF6b5eee),
        backgroundColor = Color(0xFF6b5eee),
        textColor = Color(0xFFfffeff),
        cardColor = Color(0xFF15122e),
        onBackgroundColor = Color(0xFFffffff),
        progressBackColor = Color(0xFF7f76f1),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    YELLOW(
        colorNameResId = R.string.color_yellow,
        colorValue = Color(0xFFf0b300),
        backgroundColor = Color(0xFFf0b300),
        textColor = Color(0xFF0D0D0D),
        cardColor = Color(0xFF312400),
        onBackgroundColor = Color(0xFFfffff9),
        progressBackColor = Color(0xFFf3bf26),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    LIGHT_YELLOW(
        colorNameResId = R.string.color_light_yellow,
        colorValue = Color(0xFFfcdd82),
        backgroundColor = Color(0xFFfcdd82),
        textColor = Color(0xFF372c11),
        cardColor = Color(0xFF322d19),
        onBackgroundColor = Color(0xFFfffefb),
        progressBackColor = Color(0xFFe8cd7a),
        progressBarColor = Color(0xff2e2918)
    ),
    LIGHT_ORANGE(
        colorNameResId = R.string.color_light_orange,
        colorValue = Color(0xFFfcc793),
        backgroundColor = Color(0xFFfcc793),
        textColor = Color(0xFF33281f),
        cardColor = Color(0xFF2e251c),
        onBackgroundColor = Color(0xFFfffdf9),
        progressBackColor = Color(0xFFe8b888),
        progressBarColor = Color(0xff2e251c)
    ),
    LIGHT_PINK(
        colorNameResId = R.string.color_light_pink,
        colorValue = Color(0xFFf7c8ee),
        backgroundColor = Color(0xFFf7c8ee),
        textColor = Color(0xFF2f2631),
        cardColor = Color(0xFF312730),
        onBackgroundColor = Color(0xFFfffeff),
        progressBackColor = Color(0xFFe2b8db),
        progressBarColor = Color(0xff2d262c)
    ),
    LIGHT_PURPLE(
        colorNameResId = R.string.color_light_purple,
        colorValue = Color(0xFFd3cffb),
        backgroundColor = Color(0xFFd3cffb),
        textColor = Color(0xFF302e40),
        cardColor = Color(0xFF2a2932),
        onBackgroundColor = Color(0xFFfefdff),
        progressBackColor = Color(0xFFc2bfe8),
        progressBarColor = Color(0xff26262d)
    ),
    LIGHT_GREEN(
        colorNameResId = R.string.color_light_green,
        colorValue = Color(0xFFbae0b7),
        backgroundColor = Color(0xFFbae0b7),
        textColor = Color(0xFF2a2f27),
        cardColor = Color(0xFF262d25),
        onBackgroundColor = Color(0xFFfcfefe),
        progressBackColor = Color(0xFFadceab),
        progressBarColor = Color(0xff232822)
    ),
    LIGHT_BLUE(
        colorNameResId = R.string.color_light_blue,
        colorValue = Color(0xFFc5dfec),
        backgroundColor = Color(0xFFc5dfec),
        textColor = Color(0xFF262f36),
        cardColor = Color(0xFF292d2e),
        onBackgroundColor = Color(0xFFffffff),
        progressBackColor = Color(0xFFb5cdd7),
        progressBarColor = Color(0xff252a2b)
    )
}

// Create an array of the available colors
val medicationColors = MedicationColor.entries.toTypedArray()

// Helper function to find MedicationColor by hex string
fun findMedicationColorByHex(hexColorString: String?): MedicationColor? {
    if (hexColorString.isNullOrBlank()) return null
    try {
        val inputColorInt = android.graphics.Color.parseColor(hexColorString)
        // Compare only RGB portions to ignore Alpha, as it might not be consistently stored/used.
        val inputRGB = inputColorInt and 0xFFFFFF

        return MedicationColor.entries.find {
            var enumColorRGB = it.backgroundColor.toArgb() and 0xFFFFFF
            enumColorRGB == inputRGB
        }
    } catch (e: IllegalArgumentException) {
        Log.w("MedicationColorUtil", "Invalid hex string for comparison: $hexColorString", e)
        return null
    }
}