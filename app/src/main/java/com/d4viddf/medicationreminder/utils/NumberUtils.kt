package com.d4viddf.medicationreminder.utils

object NumberUtils {
    fun toFraction(number: Float): String {
        if (number == 0f) return "0"

        val wholePart = number.toInt()
        val fractionalPart = number - wholePart

        if (fractionalPart == 0f) {
            return wholePart.toString()
        }

        val fractionStr = when {
            fractionalPart == 0.5f -> "½"
            fractionalPart == 0.25f -> "¼"
            fractionalPart == 0.75f -> "¾"
            (fractionalPart - 1f/3f).toDouble().let { Math.abs(it) } < 0.01 -> "⅓"
            (fractionalPart - 2f/3f).toDouble().let { Math.abs(it) } < 0.01 -> "⅔"
            else -> {
                val tolerance = 1.0E-6
                var h1 = 1.0
                var h2 = 0.0
                var k1 = 0.0
                var k2 = 1.0
                var b = fractionalPart.toDouble()
                do {
                    val a = Math.floor(b)
                    var aux = h1
                    h1 = a * h1 + h2
                    h2 = aux
                    aux = k1
                    k1 = a * k1 + k2
                    k2 = aux
                    b = 1 / (b - a)
                } while (Math.abs(fractionalPart - h1 / k1) > fractionalPart * tolerance)

                "${h1.toInt()}/${k1.toInt()}"
            }
        }

        return if (wholePart > 0) {
            "$wholePart $fractionStr"
        } else {
            fractionStr
        }
    }
}
