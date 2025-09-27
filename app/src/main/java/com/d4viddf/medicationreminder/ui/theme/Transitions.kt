// In app/src/main/java/com/d4viddf/medicationreminder/ui/common/theme/Transitions.kt
package com.d4viddf.medicationreminder.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.NavBackStackEntry

private const val ANIMATION_DURATION_MILLIS = 300

// This remains the same: a standard slide for opening new screens.
val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(ANIMATION_DURATION_MILLIS)
    )
}

// This also remains the same for forward navigation.
val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(ANIMATION_DURATION_MILLIS)
    )
}

// This is updated to your exact request: The returning screen does not animate.
val popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    EnterTransition.None
}

// This is updated to your exact request: The exiting screen scales down to 90%.
// This creates the "floating" effect for the predictive back gesture.
val popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    scaleOut(
        targetScale = 0.9f,
        transformOrigin = TransformOrigin.Center,
    )
}