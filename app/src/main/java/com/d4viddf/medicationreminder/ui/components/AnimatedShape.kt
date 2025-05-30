package com.d4viddf.medicationreminder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape // For MaterialShapes properties if they are ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// User's import for MaterialShapes
import androidx.compose.material3.MaterialShapes 
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

// Imports for RoundedPolygon type & Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.CornerRounding // Re-added for manual definitions
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.drawMorph // Extension function for DrawScope
import androidx.graphics.shapes.toRoundedPolygon // For converting ui.graphics.Shape

// For Canvas transform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.min

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object MaterialPolygons {
    // References to user's MaterialShapes (types are unknown here, assumed to be ui.graphics.Shape)
    private val userCircle: Shape = MaterialShapes.Circle
    private val userSquare: Shape = MaterialShapes.Square
    // Pentagon and Sunny from MaterialShapes are less likely to be complex RoundedPolygons.
    // If they were simple Shapes, toRoundedPolygon() might not yield desired geometry.
    // private val userPentagon: Shape = MaterialShapes.Pentagon
    // private val userSunny: Shape = MaterialShapes.Sunny

    // Define the list for morphing, ensuring they are all RoundedPolygon.
    // Scenario B/C hybrid approach:
    val DefaultShapesList: List<RoundedPolygon> = listOfNotNull(
        // Attempt conversion for Circle and Square, with fallbacks
        userCircle.toRoundedPolygon() ?: RoundedPolygon.Circle,
        userSquare.toRoundedPolygon() ?: RoundedPolygon.rectangle(
            width = 2f, // Assuming a 2x2 unit square
            height = 2f,
            rounding = CornerRounding(0f)
        ),
        // For Pentagon and Sunny, directly use manual RoundedPolygon definitions
        // to ensure specific geometry required for morphing.
        RoundedPolygon( // Manual Pentagon
            numVertices = 5,
            radius = 1f,
            centerX = 0f,
            centerY = 0f,
            rounding = CornerRounding(radius = 0.1f)
        ),
        RoundedPolygon( // Manual Sunny (refined version)
            numVertices = 16,
            radius = 1f,
            innerRadius = 0.4f,
            centerX = 0f,
            centerY = 0f,
            rounding = CornerRounding(radius = 0.08f)
        )
    ).also {
        if (it.size < 4) {
            println("Warning: MaterialPolygons.DefaultShapesList has fewer than 4 shapes. Some conversions might have failed or produced null.")
        }
    }
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
        return
    }

    val shapes = MaterialPolygons.DefaultShapesList
    var currentShapeIndex by remember { mutableStateOf(0) }

    val morphProgress = remember { Animatable(0f) }

    LaunchedEffect(currentShapeIndex) {
        morphProgress.snapTo(0f) 
        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = morphAnimationDurationMillis, easing = LinearEasing)
        )
    }

    LaunchedEffect(Unit) { 
        while (isActive) { 
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

    val startShape = shapes[currentShapeIndex]
    val nextShapeIndex = (currentShapeIndex + 1) % shapes.size
    val endShape = shapes[nextShapeIndex]

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
        val scaleFactor = min(canvasWidth, canvasHeight) / 2.0f

        translate(left = canvasWidth / 2f, top = canvasHeight / 2f) {
            scale(scale = scaleFactor) {
                drawMorph(
                    morph = morph,
                    progress = morphProgress.value,
                    color = shapeColor
                )
            }
        }
    }
}
