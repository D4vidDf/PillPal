package com.d4viddf.medicationreminder.ui.calendar // Or a more appropriate package like com.d4viddf.medicationreminder.ui.components.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
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
             secondsOffset.snapTo(secondsOffset.value + secondsDelta)
        }
        // onDateRangeChanged(startDateTime, endDateTime) // Call after scroll changes
        delta // Consume the delta
    }

    // Basic fling behavior, can be enhanced with anchoring later if needed.
    val scrollFlingBehavior = object : FlingBehavior {
        val decay = exponentialDecay<Float>() // This is a Float decay, animateDecay on Animatable<Long, AnimationVector1D> will need its own or a compatible one.
                                         // However, Animatable.animateDecay will typically use its own decay characteristics or one provided.
                                         // The 'decay' variable here might not be directly used by secondsOffset.animateDecay if it implies a specific type.
                                         // Let's assume Animatable<Long, AnimationVector1D> can handle decay with a Float-based velocity.
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            // Convert scroll velocity (pixels/sec) to value units/sec for the animation
            // Positive initialVelocity means user swiped left, content moved left, time should go "forward" (offset increases)
            // animateDecay increases the value if velocity is positive.
            // delta in scrollableState: positive means drag towards positive (right), content moves left.
            // So if initialVelocity from fling is positive (content was moving left, user swiped left), secondsOffset should increase.
            // secondsOffset.animateDecay expects velocity in units per second.
            // A positive pixel velocity (swipe left) should lead to a positive secondsOffset change.
            // My .toSeconds() converts pixel delta to time delta:
            //   - positive pixel delta (drag right, content moves right) -> positive time delta
            //   - negative pixel delta (drag left, content moves left) -> negative time delta
            // For fling:
            //   - initialVelocity > 0 means content was moving left (user swiped from right to left) -> time should go forward -> secondsOffset should increase.
            //   - initialVelocity < 0 means content was moving right (user swiped from left to right) -> time should go backward -> secondsOffset should decrease.
            // So, velocity for animateDecay should be initialVelocity.toSeconds().toFloat(), but need to check sign.
            // If initialVelocity > 0 (swipe R-to-L, content moves left, time increases): initialVelocity.toSeconds() would be negative if toSeconds() uses (pixel * secondsPerPixel) and secondsPerPixel is positive.
            // Let's assume secondsPerPixel is positive.
            // If initialVelocity is positive (pixels/s, list moving left), then this is like dragging finger from right to left.
            // This means we are moving towards later times. `secondsOffset` should increase.
            // `animateDecay` takes a velocity where positive means increasing the value.
            // So, if `initialVelocity` (pixels/s) is positive, `initialVelocity.toSeconds().toFloat()` should be positive.
            // My `Float.toSeconds()`: `(this * secondsPerPixel).roundToLong()`. `secondsPerPixel` is `viewSpanSeconds.toFloat() / composableWidthPx.toFloat()`, which is positive.
            // So, `initialVelocity.toSeconds().toFloat()` has the correct sign for `animateDecay`.

            val velocityInSecondsPerSecond = initialVelocity.toSeconds().toFloat()

            coroutineScope.launch { // Launching the animation as animateDecay itself might not be what performFling expects to directly suspend on for its whole duration
                secondsOffset.animateDecay(velocityInSecondsPerSecond, decay) // Pass the decay spec here
                // onDateRangeChanged(startDateTime, endDateTime) // If re-introduced
            }
            return initialVelocity // Indicate all velocity was consumed by launching an animation
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
}
