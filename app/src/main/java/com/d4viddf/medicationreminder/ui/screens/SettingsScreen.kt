package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.foundation.clickable
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

data class LanguageOption(val displayName: String, val tag: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentLanguageTag by viewModel.currentLanguageTag.collectAsState()
    val currentThemeKey by viewModel.currentTheme.collectAsState()
    val currentVolume by viewModel.currentVolume.collectAsState()
    val maxVolume by viewModel.maxVolume.collectAsState()
    val currentNotificationSoundUri by viewModel.currentNotificationSoundUri.collectAsState()
    // The notificationSoundName is now updated whenever currentNotificationSoundUri changes,
    // because getNotificationSoundName is called within the Composable that observes currentNotificationSoundUri.
    // val notificationSoundName = viewModel.getNotificationSoundName(currentNotificationSoundUri) // This line can be removed or kept, it's fine either way.

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.updateNotificationSoundUri(uri?.toString())
        }
    }

    val languages = remember {
        listOf(
            LanguageOption(context.getString(R.string.language_english), "en"),
            LanguageOption(context.getString(R.string.language_spanish), "es"),
            LanguageOption(context.getString(R.string.language_galician), "gl"),
            LanguageOption(context.getString(R.string.language_euskera), "eu"),
            LanguageOption(context.getString(R.string.language_catalan), "ca")
        )
    }
    var expandedLanguageDropdown by remember { mutableStateOf(false) }
    val selectedLanguageDisplay = remember(currentLanguageTag, languages) {
        languages.find { it.tag == currentLanguageTag }?.displayName ?: languages.firstOrNull()?.displayName ?: ""
    }

    val themeOptions = remember {
        listOf(
            context.getString(R.string.settings_theme_light_option) to com.d4viddf.medicationreminder.data.ThemeKeys.LIGHT,
            context.getString(R.string.settings_theme_dark_option) to com.d4viddf.medicationreminder.data.ThemeKeys.DARK,
            context.getString(R.string.settings_theme_system_option) to com.d4viddf.medicationreminder.data.ThemeKeys.SYSTEM
        )
    }
    // The selectedThemeDisplayName is now derived from the ViewModel's currentThemeKey
    val selectedThemeDisplayName = remember(currentThemeKey, themeOptions) {
        themeOptions.find { it.second == currentThemeKey }?.first ?: themeOptions.first { it.second == com.d4viddf.medicationreminder.data.ThemeKeys.SYSTEM }.first
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.settings_screen_title)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp) // Outer padding for the whole screen content
        ) {
            // Language Selection Group
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) { // Inner padding for content
                    Text(
                        text = stringResource(id = R.string.settings_language_label),
                        style = MaterialTheme.typography.titleMedium, // Expressive: titleMedium or titleSmall
                        modifier = Modifier.padding(bottom = 12.dp) // Adjusted padding
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedLanguageDropdown,
                        onExpandedChange = { expandedLanguageDropdown = !expandedLanguageDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedLanguageDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(id = R.string.settings_language_label)) }, // Added label for context
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguageDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge // Ensure text style
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLanguageDropdown,
                            onDismissRequest = { expandedLanguageDropdown = false }
                        ) {
                            languages.forEach { languageOption ->
                                DropdownMenuItem(
                                    text = { Text(languageOption.displayName, style = MaterialTheme.typography.bodyLarge) }, // Applied style
                                    onClick = {
                                        viewModel.updateLanguageTag(languageOption.tag)
                                        expandedLanguageDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // Spacer between sections

            // Theme Selection Group
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) { // Inner padding for content
                    Text(
                        text = stringResource(id = R.string.settings_theme_label),
                        style = MaterialTheme.typography.titleMedium, // Expressive: titleMedium or titleSmall
                        modifier = Modifier.padding(bottom = 12.dp) // Adjusted padding
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        themeOptions.forEachIndexed { index, (themeDisplayName, themeKey) ->
                            SegmentedButton(
                                selected = (currentThemeKey == themeKey),
                                onClick = { viewModel.updateTheme(themeKey) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                                label = { Text(themeDisplayName, style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // Spacer between sections

            // Volume Control Group
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_volume_label), // Ensure this string resource exists
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (currentVolume > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = stringResource(id = if (currentVolume > 0) R.string.volume_up_content_description else R.string.volume_off_content_description) // Ensure these string resources exist
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = currentVolume.toFloat(),
                            onValueChange = { newVolume ->
                                viewModel.setVolume(newVolume.roundToInt())
                            },
                            valueRange = 0f..(if (maxVolume > 0) maxVolume.toFloat() else 1f), // Avoid division by zero or empty range if maxVolume is 0
                            steps = if (maxVolume > 1) maxVolume - 1 else 0, // Ensure steps are at least 0
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // Spacer between sections

            // Notification Sound Setting Group
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true) // Allowing selection of "None" or "Silent"
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            )
                            // Pass current URI if one is set, to show it as selected in the picker
                            currentNotificationSoundUri?.let {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                            }
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp), // Inner padding for content
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_notification_sound_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f) // Takes available space, pushing the sound name to the end
                    )
                    Text(
                        // viewModel.getNotificationSoundName will be recomposed if currentNotificationSoundUri changes
                        text = viewModel.getNotificationSoundName(currentNotificationSoundUri),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
