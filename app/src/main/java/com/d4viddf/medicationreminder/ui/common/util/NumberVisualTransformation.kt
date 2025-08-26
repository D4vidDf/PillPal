package com.d4viddf.medicationreminder.ui.common.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

class NumberVisualTransformation : VisualTransformation {
    private val formatter = DecimalFormat("#,###")

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = try {
            formatter.format(originalText.toLong())
        } catch (e: NumberFormatException) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val originalStr = originalText.take(offset)
                val formattedStr = try {
                    formatter.format(originalStr.toLong())
                } catch (e: NumberFormatException) {
                    originalStr
                }
                return formattedStr.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                return formattedText.take(offset).count { it.isDigit() }
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
