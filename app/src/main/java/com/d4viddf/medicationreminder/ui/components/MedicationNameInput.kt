package com.d4viddf.medicationreminder.ui.components

import MedicationSearchResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationInfoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationNameInput(
    label: String = "Medication Name",
    medicationName: String,
    onMedicationNameChange: (String) -> Unit,
    onMedicationSelected: (MedicationSearchResult?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicationInfoViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val searchResults by viewModel.medicationSearchResults.collectAsState()
    var isInputValid by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.padding(16.dp)) {
        // Input Label Above the TextField
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = if (isInputValid) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (!expanded) {
                    expanded = true
                }
            }
        ) {
            OutlinedTextField(
                value = medicationName,
                onValueChange = {
                    onMedicationNameChange(it)
                    isInputValid = it.isNotEmpty()

                    if (it.length >= 3) {
                        expanded = true // Ensure dropdown is open when typing a valid name
                        isLoading = true
                        coroutineScope.launch(Dispatchers.IO) {
                            viewModel.searchMedication(it)
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .menuAnchor(),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                shape = MaterialTheme.shapes.large, // Rounded corners for TextField
                isError = !isInputValid
            )

            if (expanded) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        // Dropdown should only close when an option is selected
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondary) // Set background color to light green accent
                ) {
                    // First option for custom input
                    DropdownMenuItem(
                        text = { Text("Personalized Entry: $medicationName") },
                        onClick = {
                            expanded = false
                            onMedicationSelected(null)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.padding(4.dp).background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
                    )
                    // Displaying search results
                    searchResults.forEach { result ->
                        DropdownMenuItem(
                            text = { Text(result.name) },
                            onClick = {
                                expanded = false
                                onMedicationNameChange(result.name)
                                onMedicationSelected(result)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.padding(4.dp).background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
                        )
                    }
                }
            }
        }

        if (!isInputValid) {
            Text(
                text = "Please enter a valid medication name",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Description below TextField
        Text(
            text = "Enter the name of the medication you want to search for. You can also select from the suggestions below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
