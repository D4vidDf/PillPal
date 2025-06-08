package com.d4viddf.medicationreminder.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
// import androidx.compose.material3.IconButton // No longer used directly here
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
// import androidx.compose.material3.Scaffold // Scaffold removed
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar // Needed for Preview's Scaffold
import androidx.compose.material3.Scaffold // Needed for Preview
import androidx.compose.material3.IconButton // Needed for Preview's TopAppBar
import androidx.compose.material.icons.Icons // Needed for Preview's TopAppBar
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Needed for Preview's TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.viewmodel.SettingsViewModel
import com.d4viddf.medicationreminder.ui.theme.AppTheme

// Assuming LanguageOption data class is accessible or redefined if needed.
// For simplicity, if it's in the old SettingsScreen.kt, we can redefine it or assume it's moved to a common place.
// For this subtask, we'll assume it's available via `import com.d4viddf.medicationreminder.ui.screens.LanguageOption`
// If not, the subtask should include its definition.
// Let's include it here for self-containment of this new file.
data class LanguageOption(val displayName: String, val tag: String)


// import androidx.compose.foundation.layout.Box // No longer needed for empty topBar
import androidx.compose.foundation.layout.fillMaxSize // Ensure fillMaxSize is imported

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit, // Still passed by ResponsiveSettingsScaffold, though not used for TopAppBar here
    viewModel: SettingsViewModel = hiltViewModel()
    // showTopAppBar: Boolean, // Parameter removed
) {
    val context = LocalContext.current
    val currentLanguageTag by viewModel.currentLanguageTag.collectAsState()
    val currentThemeKey by viewModel.currentTheme.collectAsState()

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

    // Scaffold removed, root is now Column
    Column(
        modifier = Modifier
            .fillMaxSize() // Takes full size given by parent
            .padding(16.dp) // General content padding
    ) {
        // Language Selection Group
        Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_language_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
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
                            label = { Text(stringResource(id = R.string.settings_language_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguageDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLanguageDropdown,
                            onDismissRequest = { expandedLanguageDropdown = false }
                        ) {
                            languages.forEach { languageOption ->
                                DropdownMenuItem(
                                    text = { Text(languageOption.displayName, style = MaterialTheme.typography.bodyLarge) },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Selection Group
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_theme_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
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
        }
    }
}

@Preview(showBackground = true, name = "General Settings Screen Preview")
@Composable
fun GeneralSettingsScreenPreview() {
    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("General Settings") }, // Preview-specific Title
                    navigationIcon = {
                        IconButton(onClick = {}) { // Dummy action for preview
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back" // Preview description
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
             androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                // Using hiltViewModel in Preview can be problematic if dependencies aren't set up for previews.
                // It's better to pass null or a mock ViewModel if possible, or ensure Hilt preview support is configured.
                // For this task, we'll keep hiltViewModel() as per the example, assuming it resolves or will be handled.
                GeneralSettingsScreen(onNavigateBack = {}, viewModel = hiltViewModel())
            }
        }
    }
}
