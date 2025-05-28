package com.d4viddf.medicationreminder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
// import androidx.compose.foundation.layout.size // No longer directly used in AnimatedShapeBackground signature
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius // Added import for CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
// import androidx.compose.ui.unit.Dp // No longer directly used in AnimatedShapeBackground signature
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay // Make sure this is imported: import kotlinx.coroutines.delay

enum class ShapeType {
    CIRCLE,
    SUNNY, // An 8-pointed star
    SQUARE,
    PENTAGON
}

// ShapeType enum (CIRCLE, SUNNY, SQUARE, PENTAGON) remains unchanged
// drawWhiteShape private function remains unchanged

@OptIn(ExperimentalAnimationApi::class) // Required for AnimatedContent
@Composable
fun AnimatedShapeBackground(
    modifier: Modifier = Modifier,
    rotationAnimationDurationMillis: Int = 10000, // Duration for one full rotation
    shapeDisplayDurationMillis: Long = 3000L      // How long each shape is displayed
) {
    val shapeTypes = remember { ShapeType.entries.toTypedArray() } // Optimized remember
    var currentShapeIndex by remember { mutableStateOf(0) }

    // Effect to cycle through shapes
    LaunchedEffect(key1 = shapeDisplayDurationMillis) { // Re-launch if duration changes
        while (true) {
            delay(shapeDisplayDurationMillis)
            currentShapeIndex = (currentShapeIndex + 1) % shapeTypes.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shapeRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationAnimationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    AnimatedContent(
        targetState = currentShapeIndex,
        transitionSpec = {
            (fadeIn(animationSpec = tween(durationMillis = 700)) +
             scaleIn(initialScale = 0.8f, animationSpec = tween(durationMillis = 700)))
            .togetherWith(
                fadeOut(animationSpec = tween(durationMillis = 700)) +
                scaleOut(targetScale = 0.8f, animationSpec = tween(durationMillis = 700))
            )
        },
        modifier = modifier, // Apply the passed modifier to AnimatedContent
        label = "shapeTransition"
    ) { index ->
        val shapeTypeToDraw = shapeTypes[index]
        // Box to center the Canvas drawing, Canvas fills this Box.
        // Rotation is applied on the Canvas itself.
        Box(
            modifier = Modifier.fillMaxSize(), // This Box fills the space given by AnimatedContent
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) { // Canvas also fills its parent Box
                rotate(degrees = rotationAngle) {
                    drawWhiteShape(shapeTypeToDraw, this.size) // Your existing drawing function
                }
            }
        }
    }
}

private fun DrawScope.drawWhiteShape(shapeType: ShapeType, canvasSize: Size) {
    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    // Make radius slightly smaller than half of the smallest dimension of the canvas
    // to ensure the shape fits well and has some padding, especially when rotating.
    val radius = (canvasSize.minDimension / 2f) * 0.9f


    when (shapeType) {
        ShapeType.CIRCLE -> {
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center
            )
        }
        ShapeType.SQUARE -> {
            val sideLength = radius * 2f / kotlin.math.sqrt(2f) // Current calculation for sideLength
            val cornerRadiusValue = sideLength * 0.1f // Example: 10% of side length for corner radius

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(center.x - sideLength / 2f, center.y - sideLength / 2f),
                size = Size(sideLength, sideLength),
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue) // Apply corner radius
            )
        }
        ShapeType.PENTAGON -> {
            val pentagonPath = Path().apply {
                val angleIncrement = (2 * PI / 5).toFloat() // 72 degrees
                val startAngleOffset = (-PI / 2).toFloat() // Start from the top point

                moveTo(
                    center.x + radius * cos(startAngleOffset),
                    center.y + radius * sin(startAngleOffset)
                )
                for (i in 1 until 5) {
                    lineTo(
                        center.x + radius * cos(angleIncrement * i + startAngleOffset),
                        center.y + radius * sin(angleIncrement * i + startAngleOffset)
                    )
                }
                close()
            }
            drawPath(pentagonPath, color = Color.White)
        }
        ShapeType.SUNNY -> { // 8-pointed star
            val starPath = Path().apply {
                val numPoints = 8
                val outerRadius = radius
                val innerRadius = radius * 0.5f // Adjust for desired star pointiness
                val angleIncrement = (2 * PI / (numPoints * 2)).toFloat()
                val startAngleOffset = (-PI / 2).toFloat() // Start from the top point

                for (i in 0 until numPoints * 2) {
                    val currentRadius = if (i % 2 == 0) outerRadius else innerRadius
                    val angle = angleIncrement * i + startAngleOffset
                    val x = center.x + currentRadius * cos(angle)
                    val y = center.y + currentRadius * sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(starPath, color = Color.White)
        }
    }
}
