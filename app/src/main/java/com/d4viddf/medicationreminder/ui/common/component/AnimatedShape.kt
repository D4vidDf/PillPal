package com.d4viddf.medicationreminder.ui.common.component // Or your actual package

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.material3.MaterialTheme // For default color from theme
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ShapeType {
    CIRCLE,
    SUNNY, // An 8-pointed star
    SQUARE,
    PENTAGON
}

@OptIn(ExperimentalAnimationApi::class) // Required for AnimatedContent
@Composable
fun AnimatedShape( // Renamed to match your newer naming
    modifier: Modifier = Modifier,
    rotationAnimationDurationMillis: Int = 10000,
    shapeDisplayDurationMillis: Long = 3000L,
    // Add shapeColor parameter
    shapeColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val shapeTypes = remember { ShapeType.entries.toTypedArray() }
    var currentShapeIndex by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = shapeDisplayDurationMillis) {
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                rotate(degrees = rotationAngle) {
                    // Pass the shapeColor to your drawing function
                    drawCustomShape(
                        shapeType = shapeTypeToDraw,
                        canvasSize = this.size,
                        color = shapeColor // Use the parameter here
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun AnimatedShapePreview() {
    AppTheme(dynamicColor = false) {
        AnimatedShape()
    }
}

// Renamed drawWhiteShape to drawCustomShape and added color parameter
private fun DrawScope.drawCustomShape(shapeType: ShapeType, canvasSize: Size, color: Color) {
    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val radius = (canvasSize.minDimension / 2f) * 0.9f // Reduced slightly for padding

    when (shapeType) {
        ShapeType.CIRCLE -> {
            drawCircle(
                color = color, // Use passed color
                radius = radius,
                center = center
            )
        }
        ShapeType.SQUARE -> {
            val sideLength = radius * 2f / sqrt(2f)
            val cornerRadiusValue = sideLength * 0.1f

            drawRoundRect(
                color = color, // Use passed color
                topLeft = Offset(center.x - sideLength / 2f, center.y - sideLength / 2f),
                size = Size(sideLength, sideLength),
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue)
            )
        }
        ShapeType.PENTAGON -> {
            val pentagonPath = Path().apply {
                val angleIncrement = (2 * PI / 5).toFloat()
                val startAngleOffset = (-PI / 2).toFloat()

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
            drawPath(pentagonPath, color = color) // Use passed color
        }
        ShapeType.SUNNY -> { // 8-pointed star
            val starPath = Path().apply {
                val numPoints = 8
                val outerRadius = radius
                val innerRadius = radius * 0.5f
                val angleIncrement = (2 * PI / (numPoints * 2)).toFloat()
                val startAngleOffset = (-PI / 2).toFloat()

                for (i in 0 until numPoints * 2) {
                    val currentRadius = if (i % 2 == 0) outerRadius else innerRadius
                    val angle = angleIncrement * i + startAngleOffset
                    val x = center.x + currentRadius * cos(angle)
                    val y = center.y + currentRadius * sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(starPath, color = color) // Use passed color
        }
    }
}