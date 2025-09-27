package com.d4viddf.medicationreminder.ui.common.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.floor

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShapesAnimation(
    modifier: Modifier = Modifier,
    shapeModifier: Modifier = Modifier,
    shapes: List<RoundedPolygon>,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    animationDuration: Int = 4000,
    pauseDuration: Int = 200,
    content: @Composable BoxScope.() -> Unit
) {
    require(shapes.size >= 2) { "Must provide at least two shapes to morph between." }

    val infiniteTransition = rememberInfiniteTransition(label = "logo_animation")
    val totalDuration = animationDuration + pauseDuration

    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size - 1).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalDuration * (shapes.size - 1)
                for (i in 0 until shapes.size - 1) {
                    i.toFloat() at (totalDuration * i) using FastOutSlowInEasing
                    (i + 1).toFloat() at (totalDuration * (i + 1) - pauseDuration) using FastOutSlowInEasing
                }
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph_progress"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalDuration
                0f at 0 using FastOutSlowInEasing
                360f at animationDuration using FastOutSlowInEasing
                360f at totalDuration
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val morphShape = remember(morphProgress) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val fromIndex = floor(morphProgress).toInt().coerceIn(0, shapes.size - 2)
                val toIndex = (fromIndex + 1).coerceAtMost(shapes.size - 1)
                val progress = morphProgress - fromIndex

                val morph = Morph(shapes[fromIndex], shapes[toIndex])
                val path = Path()
                morph.toPath(progress, path)

                val matrix = Matrix()
                matrix.scale(size.width / 2f, size.height / 2f)
                matrix.translate(0.5f, 0.5f) // Corrected translation
                path.transform(matrix)
                return Outline.Generic(path)
            }
        }
    }

    Box(
        modifier = modifier, // The main container
        contentAlignment = Alignment.Center
    ) {
        // Sibling 1: The animated shape (rotates)
        Box(
            modifier = shapeModifier
                .graphicsLayer { rotationZ = rotation }
                .background(
                    color = backgroundColor,
                    shape = morphShape
                )
        )

        // Sibling 2: The content (stationary)
        // This Box applies the padding and holds the content.
        Box(
            modifier = Modifier.padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}