package com.d4viddf.medicationreminder.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme // Added import
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // Added import
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape // Added import
import androidx.compose.ui.graphics.graphicsLayer // Added import
import kotlinx.coroutines.delay

// ShapeType enum is no longer needed as we'll use MaterialTheme.shapes

@OptIn(ExperimentalAnimationApi::class) // Required for AnimatedContent
@Composable
fun AnimatedShapeBackground(
    modifier: Modifier = Modifier,
    rotationAnimationDurationMillis: Int = 10000, // Duration for one full rotation
    shapeDisplayDurationMillis: Long = 3000L,     // How long each shape is displayed
    shapeColor: Color = Color.White // Allow customizing shape color
) {
    // Define a list of Material Shapes to cycle through
    val materialShapes = listOf(
        MaterialTheme.shapes.small,
        MaterialTheme.shapes.medium,
        MaterialTheme.shapes.large,
        MaterialTheme.shapes.extraLarge 
    )
    var currentShapeIndex by remember { mutableStateOf(0) }

    // Effect to cycle through shapes
    LaunchedEffect(key1 = shapeDisplayDurationMillis, key2 = materialShapes.size) { // Re-launch if duration or list size changes
        if (materialShapes.isEmpty()) return@LaunchedEffect // Guard against empty list
        while (true) {
            delay(shapeDisplayDurationMillis)
            currentShapeIndex = (currentShapeIndex + 1) % materialShapes.size
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
            // Using a slightly simpler transition for shapes, adjust as needed
            (fadeIn(animationSpec = tween(durationMillis = 700)))
                .togetherWith(fadeOut(animationSpec = tween(durationMillis = 700)))
        },
        modifier = modifier, // Apply the passed modifier to AnimatedContent
        label = "shapeTransition"
    ) { index ->
        if (materialShapes.isEmpty()) {
            // Handle empty shapes list, perhaps draw a default or nothing
            Box(modifier = Modifier.fillMaxSize())
            return@AnimatedContent
        }
        val currentMaterialShape = materialShapes[index]
        
        Box(
            modifier = Modifier
                .fillMaxSize() // This Box fills the space given by AnimatedContent
                .graphicsLayer { rotationZ = rotationAngle } // Apply rotation here
                .clip(currentMaterialShape) // Clip the Box to the current Material Shape
                .background(shapeColor), // Apply background color to the clipped Box
            contentAlignment = Alignment.Center 
            // No content needed inside as the shape is defined by clip and background
        ) {
            // The old Canvas is no longer needed.
            // If there was content to draw inside the shape, it would go here.
        }
    }
}

// The custom drawWhiteShape function is no longer needed.
