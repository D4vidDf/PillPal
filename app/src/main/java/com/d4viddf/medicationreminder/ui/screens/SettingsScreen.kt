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
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Added import
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
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.BuildConfig
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.viewmodel.SettingsViewModel
import androidx.compose.material3.OutlinedButton
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect // Import LaunchedEffect
// import android.content.Intent // Duplicate removed
import android.util.Log // Import Log for error logging in LaunchedEffect
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
    // Removed: val shareIntent by viewModel.shareIntentFlow.collectAsState(initial = null)
    // We will collect shareRequest as a flow of events.
    val currentVolume by viewModel.currentVolume.collectAsState()
    val maxVolume by viewModel.maxVolume.collectAsState()
    val currentNotificationSoundUri by viewModel.currentNotificationSoundUri.collectAsState()

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

    LaunchedEffect(Unit) { // Keyed on Unit to run once and keep collecting
        viewModel.shareRequest.collect { intent: android.content.Intent -> // Explicitly typed intent
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot share logs: No app available to handle this action.", Toast.LENGTH_LONG).show()
                Log.e("SettingsScreen", "Error starting share intent activity", e)
                // Optionally log to FileLogger as well if this is a critical failure to note
                // com.d4viddf.medicationreminder.utils.FileLogger.log("SettingsScreenError", "Error starting share intent activity", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
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
            NotificationSoundSettingItem(
                soundName = viewModel.getNotificationSoundName(currentNotificationSoundUri),
                onClick = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply { // Qualified Intent
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) // Changed to TYPE_ALARM
                        )
                        currentNotificationSoundUri?.let {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                        }
                    }
                    ringtonePickerLauncher.launch(intent)
                }
            )

            // Developer Options Section (Conditional)
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Developer Options", // Consider using stringResource
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.restartDailyWorker()
                        Toast.makeText(context, "Daily worker restart initiated.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restart Daily Worker")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* TODO: Call ViewModel */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share App Logs")
                }
                Spacer(modifier = Modifier.height(16.dp)) // Ensure padding at the bottom
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Screen")
@Composable
fun SettingsScreenPreview() {
    AppTheme {
        // ViewModel parameter is omitted to use its default,
        // which might result in a preview with default states.
        SettingsScreen(onNavigateBack = {})
    }
}

@Composable
private fun NotificationSoundSettingItem(
    soundName: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Apply clickable here
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.settings_notification_sound_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = soundName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Notification Sound Default")
@Composable
private fun NotificationSoundSettingItemDefaultPreview() {
    AppTheme {
        NotificationSoundSettingItem(
            soundName = "Default",
            onClick = {}
        )
    }
}

@Preview(name = "Notification Sound Custom")
@Composable
private fun NotificationSoundSettingItemCustomPreview() {
    AppTheme {
        NotificationSoundSettingItem(
            soundName = "My Cool Ringtone",
            onClick = {}
        )
    }
}
