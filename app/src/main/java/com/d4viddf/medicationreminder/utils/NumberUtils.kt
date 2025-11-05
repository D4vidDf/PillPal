package com.d4viddf.medicationreminder.utils

object NumberUtils {
    fun toFraction(number: Float): String {
        if (number == 0f) return "0"
        if (number == 0.5f) return "½"
        if (number == 0.25f) return "¼"
        if (number == 0.75f) return "¾"
        if (number == 0.33f) return "⅓"
        if (number == 0.66f) return "⅔"
        val tolerance = 1.0E-6
        var h1 = 1.0
        var h2 = 0.0
        var k1 = 0.0
        var k2 = 1.0
        var b = number.toDouble()
        do {
            val a = Math.floor(b)
            var aux = h1
            h1 = a * h1 + h2
            h2 = aux
            aux = k1
            k1 = a * k1 + k2
            k2 = aux
            b = 1 / (b - a)
        } while (Math.abs(number - h1 / k1) > number * tolerance)

        return "${h1.toInt()}/${k1.toInt()}"
    }
}
