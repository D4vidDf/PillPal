package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.viewmodel.SettingsViewModel

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
                .padding(16.dp)
        ) {
            // Language Selection
            Text(
                text = stringResource(id = R.string.settings_language_label),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = expandedLanguageDropdown,
                onExpandedChange = { expandedLanguageDropdown = !expandedLanguageDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedLanguageDisplay, // Show the display name
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguageDropdown) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedLanguageDropdown,
                    onDismissRequest = { expandedLanguageDropdown = false }
                ) {
                    languages.forEach { languageOption ->
                        DropdownMenuItem(
                            text = { Text(languageOption.displayName) },
                            onClick = {
                                viewModel.updateLanguageTag(languageOption.tag)
                                expandedLanguageDropdown = false
                                // Activity recreation will be handled by MainActivity observing the flow
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Theme Selection
            Text(
                text = stringResource(id = R.string.settings_theme_label),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(Modifier.selectableGroup()) {
                themeOptions.forEach { (themeDisplayName, themeKey) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (currentThemeKey == themeKey), // Compare with the key from ViewModel
                                onClick = {
                                    viewModel.updateTheme(themeKey)
                                    // No need to update selectedThemeName locally as it's derived from currentThemeKey
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RadioButton(
                            selected = (currentThemeKey == themeKey),
                            onClick = null
                        )
                        Text(
                            text = themeDisplayName, // This is already localized
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
