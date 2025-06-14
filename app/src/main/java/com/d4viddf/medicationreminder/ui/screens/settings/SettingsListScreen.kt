package com.d4viddf.medicationreminder.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme

// Data class to represent a settings category item
data class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val contentDescription: String
)

@Composable
fun SettingsListScreen(
    onNavigateToGeneral: () -> Unit,
    onNavigateToSound: () -> Unit,
    onNavigateToDeveloper: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current // Used for string resources

    val categories = listOf(
        SettingsCategory(
            title = stringResource(id = R.string.settings_category_general), // Assume R.string.settings_category_general = "General"
            icon = Icons.Filled.Info, // Replace with a more appropriate icon if available
            onClick = onNavigateToGeneral,
            contentDescription = stringResource(id = R.string.settings_category_general_cd) // Assume cd exists
        ),
        SettingsCategory(
            title = stringResource(id = R.string.settings_category_sound), // Assume R.string.settings_category_sound = "Sound"
            icon = Icons.Filled.VolumeUp,
            onClick = onNavigateToSound,
            contentDescription = stringResource(id = R.string.settings_category_sound_cd) // Assume cd exists
        ),
        SettingsCategory(
            title = stringResource(id = R.string.settings_category_developer), // Assume R.string.settings_category_developer = "Developer Options"
            icon = Icons.Filled.Build,
            onClick = onNavigateToDeveloper,
            contentDescription = stringResource(id = R.string.settings_category_developer_cd) // Assume cd exists
        )
    )

    LazyColumn(modifier = modifier) { // Removed .padding(top = 8.dp)
        items(categories) { category ->
            SettingsCategoryItem(
                title = category.title,
                icon = category.icon,
                contentDescription = category.contentDescription,
                onClick = category.onClick
            )
            // HorizontalDivider removed
        }
    }
}

@Composable
fun SettingsCategoryItem(
    title: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick, // Make the whole card clickable
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp) // Padding around each card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp), // Padding inside the card
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp) // Space between icon and text
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription, // For accessibility
                modifier = Modifier.size(24.dp) // Consistent icon size
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f) // Ensure text takes available space
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null, // Decorative
                tint = MaterialTheme.colorScheme.onSurfaceVariant // Subtle color
            )
        }
    }
}

// Define necessary string resources in strings.xml (conceptual, not part of this subtask action)
// <string name="settings_category_general">General</string>
// <string name="settings_category_general_cd">Navigate to general settings</string>
// <string name="settings_category_sound">Sound</string>
// <string name="settings_category_sound_cd">Navigate to sound settings</string>
// <string name="settings_category_developer">Developer Options</string>
// <string name="settings_category_developer_cd">Navigate to developer options</string>

@Preview(showBackground = true)
@Composable
fun SettingsListScreenPreview() {
    AppTheme {
        Surface {
            SettingsListScreen(
                onNavigateToGeneral = {},
                onNavigateToSound = {},
                onNavigateToDeveloper = {}
            )
        }
    }
}
