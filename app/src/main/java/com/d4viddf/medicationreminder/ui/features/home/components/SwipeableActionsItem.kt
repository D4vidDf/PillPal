package com.d4viddf.medicationreminder.ui.features.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.SkipNext // Using SkipNext for now
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissState
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableActionsItem(
    modifier: Modifier = Modifier,
    onMarkAsTaken: () -> Unit,
    onSkip: () -> Unit,
    enabled: Boolean = true, // New parameter
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val dismissState = rememberDismissState(
        confirmValueChange = { dismissValue ->
            if (!enabled) return@rememberDismissState false // Do not allow swipe if not enabled

            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> { // Swiped from right to left (reveal "Skip")
                    onSkip()
                    coroutineScope.launch {
                        dismissState.reset()
                    }
                    true // Consume the dismiss event
                }
                SwipeToDismissBoxValue.StartToEnd -> { // Swiped from left to right (reveal "Mark as Taken")
                    onMarkAsTaken()
                    coroutineScope.launch {
                        dismissState.reset()
                    }
                    true // Consume the dismiss event
                }
                SwipeToDismissBoxValue.Settled -> false // Do not consume if settled (no action)
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.25f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = enabled, // Controlled by the new 'enabled' parameter
        enableDismissFromEndToStart = enabled, // Controlled by the new 'enabled' parameter
        backgroundContent = {
            if (enabled) { // Only show background if enabled
                SwipeBackground(dismissState = dismissState)
            } else {
                // Optionally, provide a different background or an empty Box when disabled
                Box(modifier = Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
            }
        }
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(dismissState: DismissState) {
    val direction = dismissState.dismissDirection ?: return

    val backgroundColor by animateColorAsState(
        targetValue = when (dismissState.targetValue) {
            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer // Greenish for "Taken"
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer // Orangish for "Skip"
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        }, label = "backgroundColorAnimation"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
        label = "iconScaleAnimation"
    )

    Row(
        modifier = Modifier
            .fillMaxHeight() // Important to fill the height of the item
            .background(backgroundColor)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (direction == DismissDirection.StartToEnd) Arrangement.Start else Arrangement.End
    ) {
        if (direction == DismissDirection.StartToEnd) { // "Mark as Taken"
            SwipeAction(
                icon = Icons.Filled.Done,
                text = "Mark as Taken",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.scale(iconScale)
            )
        } else { // "Skip"
            SwipeAction(
                icon = Icons.Filled.SkipNext,
                text = "Skip",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.scale(iconScale)
            )
        }
    }
}

@Composable
private fun SwipeAction(
    icon: ImageVector,
    text: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            color = tint,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
