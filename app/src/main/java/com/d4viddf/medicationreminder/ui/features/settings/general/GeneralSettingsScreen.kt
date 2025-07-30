package com.d4viddf.medicationreminder.ui.features.settings.general

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.ThemeKeys
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.features.settings.SettingsViewModel

// data class LanguageOption(val displayName: String, val tag: String) // Remove this line

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // val currentLanguageTag by viewModel.currentLanguageTag.collectAsState() // Remove this line
    val currentThemeKey by viewModel.currentTheme.collectAsState()

    // val languages = remember { // Remove this entire block
    //     listOf(
    //         LanguageOption(context.getString(R.string.language_english), "en-US"),
    //         LanguageOption(context.getString(R.string.language_spanish), "es-ES"),
    //         LanguageOption(context.getString(R.string.language_galician), "gl-ES"),
    //         LanguageOption(context.getString(R.string.language_euskera), "eu-ES"),
    //         LanguageOption(context.getString(R.string.language_catalan), "ca-ES")
    //     )
    // }
    // var expandedLanguageDropdown by remember { mutableStateOf(false) } // Remove this line
    // val selectedLanguageDisplay = remember(currentLanguageTag, languages) { // Remove this line (and its multi-line block if applicable)
    //     languages.find { it.tag == currentLanguageTag }?.displayName ?: languages.firstOrNull()?.displayName ?: ""
    // }

    val themeOptions = remember {
        listOf(
            context.getString(R.string.settings_theme_light_option) to ThemeKeys.LIGHT,
            context.getString(R.string.settings_theme_dark_option) to ThemeKeys.DARK,
            context.getString(R.string.settings_theme_system_option) to ThemeKeys.SYSTEM
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Language Selection Group
        // Surface( // Remove this entire Surface block
        //         modifier = Modifier.fillMaxWidth(),
        //         shape = MaterialTheme.shapes.medium,
        //         border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        //     ) {
        //         Column(modifier = Modifier.padding(16.dp)) {
        //             Text(
        //                 text = stringResource(id = R.string.settings_language_label),
        //                 style = MaterialTheme.typography.titleMedium,
        //                 modifier = Modifier.padding(bottom = 12.dp)
        //             )
        //             ExposedDropdownMenuBox(
        //                 expanded = expandedLanguageDropdown,
        //                 onExpandedChange = { expandedLanguageDropdown = !expandedLanguageDropdown },
        //                 modifier = Modifier.fillMaxWidth()
        //             ) {
        //                 OutlinedTextField(
        //                     value = selectedLanguageDisplay,
        //                     onValueChange = {},
        //                     readOnly = true,
        //                     label = { Text(stringResource(id = R.string.settings_language_label)) },
        //                     trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguageDropdown) },
        //                     modifier = Modifier.menuAnchor().fillMaxWidth(),
        //                     textStyle = MaterialTheme.typography.bodyLarge
        //                 )
        //                 ExposedDropdownMenu(
        //                     expanded = expandedLanguageDropdown,
        //                     onDismissRequest = { expandedLanguageDropdown = false }
        //                 ) {
        //                     languages.forEach { languageOption ->
        //                         DropdownMenuItem(
        //                             text = { Text(languageOption.displayName, style = MaterialTheme.typography.bodyLarge) },
        //                             onClick = {
        //                                 viewModel.updateLanguageTag(languageOption.tag)
        //                                 expandedLanguageDropdown = false
        //                             }
        //                         )
        //                     }
        //                 }
        //             }
        //         }
        //     }

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
    // This extra brace was the original culprit for a similar error if it existed.
    // Ensure GeneralSettingsScreen function is properly closed before Preview.
}

@OptIn(ExperimentalMaterial3Api::class) // Added OptIn for Preview
@Preview(showBackground = true, name = "General Settings Screen Preview")
@Composable
fun GeneralSettingsScreenPreview() {
    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("General Settings") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_arrow_back_ios_24),
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
               GeneralSettingsScreen(onNavigateBack = {}, viewModel = hiltViewModel())
           }
        }
    }
}
