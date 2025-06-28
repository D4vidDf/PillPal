package com.d4viddf.medicationreminder.ui.features.calendar // Or a more appropriate package like com.d4viddf.medicationreminder.ui.components.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToLong

// Helper to convert Long to AnimationVector1D for Animatable
private val LongToVector: TwoWayConverter<Long, AnimationVector1D> =
    TwoWayConverter({ AnimationVector1D(it.toFloat()) }, { it.value.toLong() })

@Composable
fun rememberScheduleCalendarState(
    referenceDateTime: LocalDateTime = LocalDate.now().atStartOfDay(), // Default to start of today
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    // Add onDateRangeChanged callback if needed for ViewModel to fetch data
    // onDateRangeChanged: (LocalDateTime, LocalDateTime) -> Unit = { _, _ -> }
): ScheduleCalendarState {
    return remember(referenceDateTime, coroutineScope) {
        ScheduleCalendarState(
            referenceDateTime = referenceDateTime,
            coroutineScope = coroutineScope
            // onDateRangeChanged = onDateRangeChanged
        )
    }
}

class ScheduleCalendarState(
    private val referenceDateTime: LocalDateTime, // A fixed point in time, e.g., start of an epoch or a far past date
    private val coroutineScope: CoroutineScope
    // private val onDateRangeChanged: (LocalDateTime, LocalDateTime) -> Unit
) {
    // Represents the current horizontal scroll position in seconds from the referenceDateTime.
    // Positive values scroll into the future, negative into the past.
    private val secondsOffset = Animatable(0L, LongToVector)

    // The visible duration on screen, fixed to 7 days for now.
    private val viewSpanSeconds = Duration.ofDays(7).seconds

    // Pixel width of the calendar composable, provided by BoxWithConstraints
    private var composableWidthPx by mutableStateOf(1)

    /**
     * Updates the state with the component's width and potentially a new view span.
     * For now, viewSpan is fixed.
     */
    fun updateView(newWidth: Int) {
        composableWidthPx = newWidth.coerceAtLeast(1)
        // Potentially trigger onDateRangeChanged here if it were a parameter
        // onDateRangeChanged(startDateTime, endDateTime)
    }

    val startDateTime: LocalDateTime by derivedStateOf {
        referenceDateTime.plusSeconds(secondsOffset.value)
    }

    val endDateTime: LocalDateTime by derivedStateOf {
        startDateTime.plusSeconds(viewSpanSeconds)
    }

    val dateAtCenter: LocalDate by derivedStateOf {
        // Calculate the timestamp at the center of the current view
        val centerViewSecondsOffset = (viewSpanSeconds / 2.0).roundToLong()
        // Add this to the current startDateTime's seconds value
        val centerSeconds = secondsOffset.value + centerViewSecondsOffset
        // Convert to LocalDateTime and then to LocalDate
        referenceDateTime.plusSeconds(centerSeconds).toLocalDate()
    }

    // How many seconds are represented by a single pixel.
    private val secondsPerPixel: Float by derivedStateOf {
        viewSpanSeconds.toFloat() / composableWidthPx.toFloat()
    }

    // How many pixels represent a single second.
    private val pixelsPerSecond: Float by derivedStateOf {
        composableWidthPx.toFloat() / viewSpanSeconds.toFloat()
    }

    // Converts a pixel displacement to a duration in seconds.
    private fun Float.toSeconds(): Long {
        return (this * secondsPerPixel).roundToLong()
    }

    // Converts a duration in seconds to a pixel displacement.
    @Suppress("unused") // Might be useful later
    private fun Long.toPx(): Float {
        return this * pixelsPerSecond
    }

    val scrollableState = ScrollableState { delta ->
        // delta is the pixel amount scrolled by user
        // Convert pixel delta to seconds delta
        val secondsDelta = delta.toSeconds()
        coroutineScope.launch {
            // Subtract because scrolling right (positive delta) means time moves earlier from offset's perspective
            // or rather, if delta is positive (finger drags left, content moves left, time goes forward)
            // secondsOffset should increase.
            // If delta is negative (finger drags right, content moves right, time goes backward)
            // secondsOffset should decrease.
            // The ScrollableState provides delta: positive means drag towards positive (right), content moves left.
            // So positive delta means we want to see later times, so secondsOffset increases.
             secondsOffset.snapTo(secondsOffset.value - secondsDelta)
        }
        // onDateRangeChanged(startDateTime, endDateTime) // Call after scroll changes
        delta // Consume the delta
    }

    // Basic fling behavior, can be enhanced with anchoring later if needed.
    val scrollFlingBehavior = object : FlingBehavior {
        // Attempt: Moderate friction.
        val decaySpec = exponentialDecay<Long>(frictionMultiplier = 16.0f) // Slightly more friction than default.

        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            // Convert pixel velocity to "seconds displacement per second" for the animation.
            val velocityInSecondsPerSecond = initialVelocity.toSeconds() // This is already a Long after .toSeconds()

            // Only animate if there's significant velocity.
            if (abs(initialVelocity) > 1f) { // Check against pixel velocity
                coroutineScope.launch {
                    secondsOffset.animateDecay(
                        initialVelocity = velocityInSecondsPerSecond, // Pass the converted velocity
                        animationSpec = decaySpec
                    )
                }
            }
            // Return the initial velocity to indicate it was processed (or part of it).
            // Or return 0f if we want to signify all velocity is handled by the decay.
            // Standard behavior is often to return what's left, but for a custom decay,
            // consuming it all (return 0f) or returning initialVelocity can both make sense.
            // Let's try returning 0f to say we've handled it.
            return 0f
        }
    }

    /**
     * Calculates the horizontal offset fraction (0.0 to 1.0) for a given LocalDateTime
     * within the current visible window [startDateTime, endDateTime].
     * If the dateTime is outside this window, it will be coerced to 0.0 or 1.0.
     */
    fun offsetFraction(dateTime: LocalDateTime): Float {
        val windowDuration = ChronoUnit.SECONDS.between(startDateTime, endDateTime)
        if (windowDuration == 0L) return 0f // Avoid division by zero

        val timeOffsetFromStart = ChronoUnit.SECONDS.between(startDateTime, dateTime)
        return (timeOffsetFromStart.toFloat() / windowDuration.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Calculates the pixel width and starting X offset for an event.
     * @param start The start LocalDateTime of the event.
     * @param end The end LocalDateTime of the event. For ongoing events, this might be
     *            set to `state.endDateTime` or slightly beyond to ensure it fills the view.
     * @param totalWidthPx The total pixel width of the container where events are drawn.
     * @return Pair<Int, Int> where first is width in pixels, second is offsetX in pixels.
     */
    fun widthAndOffsetForEvent(
        start: LocalDateTime,
        end: LocalDateTime,
        totalWidthPx: Int
    ): Pair<Int, Int> {
        // Calculate the fraction of the timeline that the start and end points represent
        val startFraction = offsetFraction(start)
        val endFraction = offsetFraction(end)

        val offsetX = (startFraction * totalWidthPx).toInt()
        val width = ((endFraction - startFraction) * totalWidthPx).toInt().coerceAtLeast(0)

        return Pair(width, offsetX)
    }

    suspend fun scrollToDate(date: LocalDate, targetOffsetFraction: Float = 0.5f, initialSnap: Boolean = false) { // Added initialSnap
        if (composableWidthPx <= 0) return

        val targetPixelOffsetInView = composableWidthPx * targetOffsetFraction
        val secondsFromViewStartToTargetPixel = (targetPixelOffsetInView * (viewSpanSeconds.toFloat() / composableWidthPx.toFloat())).roundToLong()
        val targetDateStartSecondsFromReference = ChronoUnit.SECONDS.between(referenceDateTime, date.atTime(12, 0))
        val newSecondsOffset = targetDateStartSecondsFromReference - secondsFromViewStartToTargetPixel

        secondsOffset.stop() // Add this line to stop ongoing animations

        if (initialSnap) {
            secondsOffset.snapTo(newSecondsOffset)
        } else {
            secondsOffset.animateTo(
                targetValue = newSecondsOffset,
                animationSpec = tween(durationMillis = 300)
            )
        }
        // After animation, if you have onDateRangeChanged, you might call it:
        // onDateRangeChanged(startDateTime, endDateTime)
    }
}
