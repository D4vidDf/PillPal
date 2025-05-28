package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R // Assuming R file will be generated/found

@Composable
fun SplitButton(
    primaryActionText: String,
    onPrimaryActionClick: () -> Unit,
    secondaryActions: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier, // Modifier for the Row container
    enabled: Boolean = true,
    primaryButtonModifier: Modifier = Modifier, // Modifier specifically for the primary button (e.g., for weight)
    secondaryButtonModifier: Modifier = Modifier // Modifier specifically for the secondary button
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Primary Action Button
        Button(
            onClick = onPrimaryActionClick,
            enabled = enabled,
            modifier = primaryButtonModifier,
            // Shape to make the left side rounded, right side flat
            shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50, topEndPercent = 0, bottomEndPercent = 0)
        ) {
            Text(primaryActionText)
        }

        // Divider line between the buttons (very thin)
        if (enabled) { // Show divider only if enabled, could be part of the OutlinedButton's border effectively too
             Divider(
                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), // Standard divider color
                 modifier = Modifier
                     .fillMaxHeight() // Fill the height of the Row
                     .width(1.dp)
             )
        }


        // Dropdown Toggle Button - using OutlinedButton style for a connected look
        // Adjust styling if it doesn't mesh well with Filled Button
        OutlinedButton(
            onClick = { showDropdownMenu = !showDropdownMenu },
            enabled = enabled,
            modifier = secondaryButtonModifier
                .widthIn(min = 56.dp) // Ensure enough space for the icon
                .wrapContentWidth(), 
            contentPadding = PaddingValues(horizontal = 12.dp), // Minimal padding
            // Shape to make the right side rounded, left side flat
            shape = RoundedCornerShape(topStartPercent = 0, bottomStartPercent = 0, topEndPercent = 50, bottomEndPercent = 50),
            // Remove border if Divider is used, or style border to match
            border = if (enabled) ButtonDefaults.outlinedButtonBorder else null 
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(id = R.string.split_button_more_options) 
            )
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
        ) {
            secondaryActions.forEach { (text, action) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        action()
                        showDropdownMenu = false
                    },
                    enabled = enabled // Dropdown items are also disabled if the whole button is
                )
            }
        }
    }
}
