package com.d4viddf.medicationreminder.ui.colors

import androidx.compose.ui.graphics.Color

// Define an enum for the color options
enum class MedicationColor(
    val backgroundColor: Color,
    val textColor: Color,
    val cardColor: Color,
    val onBackgroundColor: Color,
    val progressBackColor: Color,
    val progressBarColor: Color,
) {
    ORANGE(
        backgroundColor = Color(0xFFd96126),
        textColor = Color(0xFFfffdf3),
        cardColor = Color(0xFF2c1407),
        onBackgroundColor = Color(0xFFfffefe),
        progressBackColor = Color(0xFFde7a46),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    PINK(
        backgroundColor = Color(0xFFd6418c),
        textColor = Color(0xFFfffdff),
        cardColor = Color(0xFF2c0d1d),
        onBackgroundColor = Color(0xFFfffcff),
        progressBackColor = Color(0xFFdd5e9d),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    GREEN(
        backgroundColor = Color(0xFF009965),
        textColor = Color(0xFFe2fffa),
        cardColor = Color(0xFF001f13),
        onBackgroundColor = Color(0xFFfdfffd),
        progressBackColor = Color(0xFF27a87d),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    BLUE(
        backgroundColor = Color(0xFF269dd8),
        textColor = Color(0xFFfeffff),
        cardColor = Color(0xFF071f2c),
        onBackgroundColor = Color(0xFFfbfeff),
        progressBackColor = Color(0xFF47abdf),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    PURPLE(
        backgroundColor = Color(0xFF6b5eee),
        textColor = Color(0xFFfffeff),
        cardColor = Color(0xFF15122e),
        onBackgroundColor = Color(0xFFffffff),
        progressBackColor = Color(0xFF7f76f1),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    YELLOW(
        backgroundColor = Color(0xFFf0b300),
        textColor = Color(0xFFfffef4),
        cardColor = Color(0xFF312400),
        onBackgroundColor = Color(0xFFfffff9),
        progressBackColor = Color(0xFFf3bf26),
        progressBarColor = Color(0xFFFFFFFF)
    ),
    LIGHT_YELLOW(
        backgroundColor = Color(0xFFfcdd82),
        textColor = Color(0xFF372c11),
        cardColor = Color(0xFF322d19),
        onBackgroundColor = Color(0xFFfffefb),
        progressBackColor = Color(0xFFe8cd7a),
        progressBarColor = Color(0xff2e2918)
    ),
    LIGHT_ORANGE(
        backgroundColor = Color(0xFFfcc793),
        textColor = Color(0xFF33281f),
        cardColor = Color(0xFFF2e251c),
        onBackgroundColor = Color(0xFFfffdf9),
        progressBackColor = Color(0xFFe8b888),
        progressBarColor = Color(0xff2e251c)
    ),
    LIGHT_PINK(
        backgroundColor = Color(0xFFf7c8ee),
        textColor = Color(0xFF2f2631),
        cardColor = Color(0xFF312730),
        onBackgroundColor = Color(0xFFfffeff),
        progressBackColor = Color(0xFFe2b8db),
        progressBarColor = Color(0xff2d262c)
    ),
    LIGHT_PURPLE(
        backgroundColor = Color(0xFFd3cffb),
        textColor = Color(0xFF302e40),
        cardColor = Color(0xFF2a2932),
        onBackgroundColor = Color(0xFFfefdff),
        progressBackColor = Color(0xFFc2bfe8),
        progressBarColor = Color(0xff26262d)
    ),
    LIGHT_GREEN(
        backgroundColor = Color(0xFFbae0b7),
        textColor = Color(0xFF2a2f27),
        cardColor = Color(0xFF262d25),
        onBackgroundColor = Color(0xFFfcfefe),
        progressBackColor = Color(0xFFadceab),
        progressBarColor = Color(0xff232822)
    ),
    LIGHT_BLUE(
        backgroundColor = Color(0xFFc5dfec),
        textColor = Color(0xFF262f36),
        cardColor = Color(0xFF292d2e),
        onBackgroundColor = Color(0xFFffffff),
        progressBackColor = Color(0xFFb5cdd7),
        progressBarColor = Color(0xff252a2b)
    )
}

// Create an array of the available colors
val medicationColors = MedicationColor.values()
