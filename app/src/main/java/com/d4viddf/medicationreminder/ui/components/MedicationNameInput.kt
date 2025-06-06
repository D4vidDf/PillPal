package com.d4viddf.medicationreminder.ui.components

import MedicationSearchResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationInfoViewModel
import com.d4viddf.medicationreminder.ui.components.MedicationSearchResultCard // New import
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass // New import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationNameInput(
    medicationName: String,
    onMedicationNameChange: (String) -> Unit,
    onMedicationSelected: (MedicationSearchResult?) -> Unit,
    modifier: Modifier = Modifier, // This is for the whole component
    searchResultsListModifier: Modifier = Modifier, // New parameter for the LazyColumn
    viewModel: MedicationInfoViewModel = hiltViewModel()
) {
    // val windowSizeClass = LocalWindowSizeClass.current // REMOVE THIS
    // val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium // REMOVE THIS
    val coroutineScope = rememberCoroutineScope()
    val searchResults by viewModel.medicationSearchResults.collectAsState()
    var isInputValid by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    var selectedMedicationNRegistro by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_name_input_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // OutlinedTextField for medication name
        OutlinedTextField(
            value = medicationName,
            onValueChange = {
                onMedicationNameChange(it)
                isInputValid = it.isNotEmpty()
                selectedMedicationNRegistro = null // Reset selection on new query

                // Only search if the input is at least 3 characters long
                if (it.length >= 3) {
                    coroutineScope.launch(Dispatchers.IO) {
                        viewModel.searchMedication(it)
                    }
                } else {
                    // Clear search results if the input is less than 3 characters
                    viewModel.clearSearchResults()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textStyle = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_name_placeholder),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
            },
            trailingIcon = {
                IconButton(onClick = { onMedicationNameChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_name_clear_search_acc))
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
            isError =!isInputValid
        )

        // Scrollable list of search results
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth() // This can stay if the LazyColumn should always be full width
                .fillMaxHeight()
                .then(searchResultsListModifier) // Apply the passed modifier
        ) {
            items(searchResults, key = { it.nregistro ?: it.name }) { result -> // Added a key for better performance
                val isSelected = selectedMedicationNRegistro == result.nregistro
                MedicationSearchResultCard(
                    medicationResult = result,
                    onClick = {
                        onMedicationSelected(result)
                        selectedMedicationNRegistro = result.nregistro // Set the selected item here
                        // viewModel.clearSearchResults() // DO NOT clear results, allow selection to be visible
                        focusManager.clearFocus()
                    },
                    isSelected = isSelected // Pass the calculated boolean
                )
            }
        }

        if (!isInputValid) {
            Text(
                text = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_name_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationNameInputPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        MedicationNameInput(
            medicationName = "Ibuprofen",
            onMedicationNameChange = {},
            onMedicationSelected = {},
            searchResultsListModifier = Modifier.heightIn(max = 200.dp) // Provide a default for preview
        )
    }
}