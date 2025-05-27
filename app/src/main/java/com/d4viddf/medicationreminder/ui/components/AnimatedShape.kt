package com.d4viddf.medicationreminder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ShapeType {
    CIRCLE,
    SUNNY, // An 8-pointed star
    SQUARE,
    PENTAGON
}

@Composable
fun AnimatedShapeBackground(
    shapeType: ShapeType,
    modifier: Modifier = Modifier, // This modifier is for the Box container
    animationDurationMillis: Int = 10000 // Duration for one full rotation
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shapeAnimation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // The Box is the container that will be sized by the caller.
    // The Canvas will fill this Box.
    Box(
        modifier = modifier, // Apply caller's modifier to the Box
        contentAlignment = Alignment.Center // Center the Canvas if it's smaller than the Box
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { // Canvas fills the Box
            rotate(degrees = rotationAngle) {
                // Draw the shape centered within the Canvas's bounds
                drawWhiteShape(shapeType, this.size)
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
            val sideLength = radius * 2f / kotlin.math.sqrt(2f) // So the corners touch the circle of 'radius'
            drawRect(
                color = Color.White,
                topLeft = Offset(center.x - sideLength / 2f, center.y - sideLength / 2f),
                size = Size(sideLength, sideLength)
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
