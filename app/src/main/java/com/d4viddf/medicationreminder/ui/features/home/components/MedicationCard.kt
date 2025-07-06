@file:OptIn(ExperimentalSharedTransitionApi::class) // Added file-level OptIn
package com.d4viddf.medicationreminder.ui.features.home.components

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Keep clickable for the card itself
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor

// Removed OptIn from here as it's now file-level
@Composable
fun MedicationCard(
    medication: Medication,
    onClick: () -> Unit, // Callback for navigation
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    enableTransition: Boolean,
    isFutureDose: Boolean = false, // New parameter
    onMarkAsTakenAction: () -> Unit, // New action lambda
    onSkipAction: () -> Unit // New action lambda
) {
    val itemShape = RoundedCornerShape(24.dp)
    val interactionsEnabled = !isFutureDose

    SwipeableActionsItem(
        enabled = interactionsEnabled, // Pass enabled state
        onMarkAsTaken = onMarkAsTakenAction, // Call lambda
        onSkip = onSkipAction, // Call lambda
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // Apply padding around the swipeable item
            .clip(itemShape) // Clip the swipeable item itself to ensure background is also rounded
    ) {
        // Content of the card, now passed to SwipeableActionsItem
        val color = try {
            MedicationColor.valueOf(medication.color)
        } catch (e: IllegalArgumentException) {
            Log.w("MedicationCard", "Invalid color string: '${medication.color}' for medication '${medication.name}'. Defaulting.", e)
            MedicationColor.LIGHT_ORANGE
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() } // Card itself is clickable for navigation
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null && enableTransition) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                rememberSharedContentState(key = "medication-background-${medication.id}"),
                                animatedVisibilityScope
                            )
                        }
                    } else Modifier
                ),
            shape = itemShape, // Use the same shape
            colors = CardDefaults.cardColors(containerColor = color.backgroundColor)
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp) // Adjust padding for IconButton
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Pushes content apart
            ) {
                Column(
                    modifier = Modifier.weight(1f) // Text content takes available space
                ) {
                    val displayName = medication.name.split(" ").take(3).joinToString(" ")
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = color.textColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = medication.dosage ?: "", // Handle potential null dosage
                        style = MaterialTheme.typography.bodyLarge,
                        color = color.textColor
                    )
                    if (medication.reminderTime != null) {
                        Text(
                            text = "Time: ${medication.reminderTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = color.textColor
                        )
                    }
                }

                // Right section for Avatar and Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedicationAvatar(color = Color.White) // Consider theming this color
                    Spacer(modifier = Modifier.width(4.dp)) // Reduced spacer

                    var menuExpanded by remember { mutableStateOf(false) }
                    Box { // Box to anchor the DropdownMenu
                        IconButton(
                            onClick = { menuExpanded = true },
                            enabled = interactionsEnabled // Control enabled state
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options", // TODO: String resource
                                tint = if (interactionsEnabled) color.textColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Adjust tint when disabled
                            )
                        }
                        // DropdownMenu should ideally not be composed if button is disabled,
                        // or its items should also appear disabled. For simplicity,
                        // we prevent it from opening by disabling the button.
                        if (interactionsEnabled) {
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                offset = DpOffset(x = (-8).dp, y = 0.dp) // Adjust offset if needed
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Mark as Taken") }, // TODO: String resource
                                    onClick = {
                                        onMarkAsTakenAction() // Call lambda
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Skip") }, // TODO: String resource
                                    onClick = {
                                        onSkipAction() // Call lambda
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationAvatar(color: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = color, shape = CircleShape)
    )
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationCardPreview() {
    AppTheme(dynamicColor = false) {
        MedicationCard(
            medication = Medication(
                id = 1,
                name = "Amoxicillin Long Name For Testing Ellipsis",
                dosage = "250mg",
                color = "LIGHT_BLUE", // Assuming MedicationColor.LIGHT_BLUE exists
                reminderTime = "10:00 AM",
                // Updated parameters as per request
                typeId = 1, // Default value
                packageSize = 0,       // Default value
                remainingDoses = 0,    // Default value
                startDate = null,      // Default value
                endDate = null         // Default value
            ),
            onClick = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            enableTransition = true,
            isFutureDose = false,
            onMarkAsTakenAction = { Log.d("Preview", "MarkAsTaken") }, // Dummy actions for preview
            onSkipAction = { Log.d("Preview", "Skip") }
        )
    }
}

@Preview(name = "Future Dose Card", showBackground = true)
@Composable
fun MedicationCardFutureDosePreview() {
    AppTheme(dynamicColor = false) {
        MedicationCard(
            medication = Medication(
                id = 2,
                name = "Future Med Preview",
                dosage = "100mg",
                color = "LIGHT_GREEN",
                reminderTime = "08:00 PM",
                typeId = 1, packageSize = 0, remainingDoses = 0, startDate = null, endDate = null
            ),
            onClick = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            enableTransition = true,
            isFutureDose = true, // Key for this preview
            onMarkAsTakenAction = { Log.d("Preview", "MarkAsTaken (disabled)") },
            onSkipAction = { Log.d("Preview", "Skip (disabled)") }
        )
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationAvatarPreview() {
    AppTheme(dynamicColor = false) {
        MedicationAvatar(color = Color.White)
    }
}
