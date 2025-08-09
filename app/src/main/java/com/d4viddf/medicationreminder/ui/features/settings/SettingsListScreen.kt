package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme

// Data class to represent a settings category item
data class SettingsCategory(
    val title: String,
    val icon: Any, // Changed to Any
    val onClick: () -> Unit,
    val contentDescription: String
)

@Composable
fun SettingsListScreen(
    onNavigateToGeneral: () -> Unit,
    onNavigateToSound: () -> Unit,
    onNavigateToDeveloper: () -> Unit,
    onNavigateToConnectedDevices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current // Used for string resources

    val categories = listOf(
        SettingsCategory(
            title = stringResource(id = R.string.settings_category_general), // Assume R.string.settings_category_general = "General"
            icon = painterResource(id = R.drawable.rounded_info_i_24),
            onClick = onNavigateToGeneral,
            contentDescription = stringResource(id = R.string.settings_category_general_cd) // Assume cd exists
        ),
        SettingsCategory(
            title = stringResource(id = R.string.settings_category_sound), // Assume R.string.settings_category_sound = "Sound"
            icon = painterResource(id = R.drawable.rounded_volume_up_24),
            onClick = onNavigateToSound,
            contentDescription = stringResource(id = R.string.settings_category_sound_cd) // Assume cd exists
        ),
        SettingsCategory(
            title = stringResource(id = R.string.connected_devices_title),
            icon = painterResource(id = R.drawable.ic_rounded_devices_wearables_24),
            onClick = onNavigateToConnectedDevices,
            contentDescription = stringResource(id = R.string.settings_category_connected_devices_cd)
        ),
        SettingsCategory(
            title = stringResource(id = R.string.settings_category_developer), // Assume R.string.settings_category_developer = "Developer Options"
            icon = painterResource(id = R.drawable.rounded_build_24),
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
    icon: Any, // Changed to Any
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
            when (icon) {
                is ImageVector -> Icon(
                    imageVector = icon,
                    contentDescription = contentDescription, // For accessibility
                    modifier = Modifier.size(24.dp) // Consistent icon size
                )
                is Painter -> Icon(
                    painter = icon,
                    contentDescription = contentDescription, // For accessibility
                    modifier = Modifier.size(24.dp) // Consistent icon size
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f) // Ensure text takes available space
            )
            Icon(
                painter = painterResource(id = R.drawable.rounded_arrow_forward_ios_24),
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
