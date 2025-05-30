package com.d4viddf.medicationreminder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas // Added
import androidx.compose.foundation.layout.fillMaxSize
// Remove MaterialTheme import if no longer directly used by this composable for shapes
// import androidx.compose.material3.MaterialTheme 
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive // Added

// Imports for RoundedPolygon & Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.drawMorph // Extension function for DrawScope

// For Canvas transform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.min

object MaterialPolygons {
    val Circle = RoundedPolygon.Circle

    val Square = RoundedPolygon(
        numVertices = 4,
        radius = 1f,
        centerX = 0f,
        centerY = 0f,
        rounding = CornerRounding(0f) // Sharp corners
    )

    val Pentagon = RoundedPolygon(
        numVertices = 5,
        radius = 1f, // Outer radius
        centerX = 0f,
        centerY = 0f,
        rounding = CornerRounding(radius = 0.1f) // Slight rounding for aesthetics
    )

    // Refined Sunny shape
    val Sunny = RoundedPolygon(
        numVertices = 16, // 8 points (16 vertices for alternating inner/outer radii)
        radius = 1f,      // Outer radius for the tips of the sun rays
        innerRadius = 0.4f, // Inner radius for the valleys between rays (was 0.5f)
        centerX = 0f,
        centerY = 0f,
        rounding = CornerRounding(radius = 0.08f) // Rounding for tips/valleys (was 0.05f)
    )

    val DefaultShapesList: List<RoundedPolygon> = listOf(Circle, Square, Pentagon, Sunny)
}

@Composable
fun AnimatedShapeBackground(
    modifier: Modifier = Modifier,
    rotationAnimationDurationMillis: Int = 10000,
    shapeDisplayDurationMillis: Long = 3000L, // Time each shape is "dominant"
    morphAnimationDurationMillis: Int = 1000, // Duration of morph from one shape to next
    shapeColor: Color = Color.White
) {
    if (MaterialPolygons.DefaultShapesList.isEmpty()) {
        // Handle empty shapes list gracefully, though it's hardcoded here
        return
    }

    val shapes = MaterialPolygons.DefaultShapesList
    var currentShapeIndex by remember { mutableStateOf(0) }

    val morphProgress = remember { Animatable(0f) }

    // This LaunchedEffect runs when currentShapeIndex changes.
    // It resets the morphProgress and animates it to 1f.
    LaunchedEffect(currentShapeIndex) {
        morphProgress.snapTo(0f) // Reset progress for the new shape pair
        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = morphAnimationDurationMillis, easing = LinearEasing)
        )
    }

    // This LaunchedEffect runs continuously to cycle through shapes.
    // It waits for shapeDisplayDurationMillis, then updates currentShapeIndex.
    LaunchedEffect(Unit) { // Key Unit ensures it runs once and continuously
        while (isActive) { // Loop while the composable is active
            // Wait for the full duration of the current shape being visible
            // (which includes its morph-in animation time)
            delay(shapeDisplayDurationMillis)
            currentShapeIndex = (currentShapeIndex + 1) % shapes.size
        }
    }

    val infiniteRotation = rememberInfiniteTransition(label = "shapeInfiniteRotation")
    val rotationAngle by infiniteRotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationAnimationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // Determine the start and end shapes for the current morph
    val startShape = shapes[currentShapeIndex]
    val nextShapeIndex = (currentShapeIndex + 1) % shapes.size
    val endShape = shapes[nextShapeIndex]

    // Create the Morph object. This will be recomposed whenever startShape or endShape changes.
    val morph = remember(startShape, endShape) {
        Morph(startShape, endShape)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = rotationAngle }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Scale factor to fit the polygon (defined with radius 1 or diameter 2) into the canvas.
        // We use minDimension / 2f because radius is 1f. If polygons were defined to fill a 2x2 box,
        // then minDimension / 2f is correct. If they are defined with width/height 1, then minDimension.
        // Given radius = 1f, diameter is 2f. So, scale by minDimension / 2.0f.
        val scaleFactor = min(canvasWidth, canvasHeight) / 2.0f

        // Center the drawing
        translate(left = canvasWidth / 2f, top = canvasHeight / 2f) {
            // Scale the canvas so the polygon (nominal size) fits
            scale(scale = scaleFactor) {
                // Draw the morph
                drawMorph(
                    morph = morph,
                    progress = morphProgress.value,
                    color = shapeColor
                )
            }
        }
    }
}

[end of app/src/main/java/com/d4viddf/medicationreminder/ui/components/AnimatedShape.kt]
