package com.d4viddf.medicationreminder.ui.features.medication.add.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationSearchResult
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.features.home.components.MedicationSearchResultCard
import com.d4viddf.medicationreminder.ui.features.medication.add.MedicationSearchViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationNameInput(
    medicationName: String,
    onMedicationNameChange: (String) -> Unit,
    onMedicationSelected: (MedicationSearchResult?) -> Unit,
    modifier: Modifier = Modifier, // This is for the whole component
    searchResultsListModifier: Modifier = Modifier, // New parameter for the LazyColumn
    viewModel: MedicationSearchViewModel? = hiltViewModel() // Changed to MedicationSearchViewModel and made nullable
) {

    val coroutineScope = rememberCoroutineScope()
    // Handle nullable viewModel for preview
    val searchResults by viewModel?.medicationSearchResults?.collectAsState() ?: remember {
        mutableStateOf(
            emptyList<MedicationSearchResult>()
        )
    }
    val isLoading by viewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) } // Collect isLoading

    var isInputValid by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    var explicitlySelectedItem by remember { mutableStateOf<MedicationSearchResult?>(null) }

    val displayedResults = if (explicitlySelectedItem != null) {
        listOf(explicitlySelectedItem!!) // Create a list containing only the chosen item
    } else {
        searchResults // Otherwise, show results from the current search query
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(id = R.string.medication_name_input_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // OutlinedTextField for medication name
        OutlinedTextField(
            value = medicationName,
            onValueChange = { newQuery ->
                onMedicationNameChange(newQuery)
                isInputValid = newQuery.isNotEmpty()
                explicitlySelectedItem = null // Clear the "chosen" item state

                // Only search if the input is at least 3 characters long
                if (newQuery.length >= 3) {
                    // No need for Dispatchers.IO here as ViewModel handles it
                    viewModel?.searchMedication(newQuery)
                } else {
                    viewModel?.clearSearchResults()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            ),
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.medication_name_placeholder),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.search_icon_content_description)
                )
            },
            trailingIcon = {
                if (medicationName.isNotEmpty()) {
                    IconButton(onClick = {
                        onMedicationNameChange("")
                        explicitlySelectedItem = null
                        viewModel?.clearSearchResults() // Use null-safe call
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.medication_name_clear_search_acc)
                        )
                    }
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
            isError = !isInputValid
        )


        // Scrollable list of search results
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth() // This can stay if the LazyColumn should always be full width
                .fillMaxHeight()
                .then(searchResultsListModifier) // Apply the passed modifier
        ) {
            if (medicationName.isNotEmpty() && displayedResults.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMedicationSelected(
                                    MedicationSearchResult(
                                        name = medicationName,
                                        atcCode = null,
                                        description = null,
                                        safetyNotes = null,
                                        documentUrls = emptyList(),
                                        administrationRoutes = emptyList(),
                                        dosage = null,
                                        nregistro = null,
                                        labtitular = null,
                                        comercializado = false,
                                        requiereReceta = false,
                                        generico = false,
                                        imageUrl = null
                                    )
                                )
                                focusManager.clearFocus()
                            }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Text(
                            text = stringResource(
                                id = R.string.add_medication_as_name,
                                medicationName
                            )
                        )
                    }
                    HorizontalDivider()
                }
            }
            items(displayedResults, key = { it.nregistro ?: it.name }) { result -> // Use displayedResults
                val isSelected =
                    explicitlySelectedItem?.nregistro == result.nregistro && explicitlySelectedItem != null
                MedicationSearchResultCard(
                    medicationResult = result,
                    onClick = {
                        onMedicationSelected(result) // Propagates to parent, updates medicationName text field
                        explicitlySelectedItem = result // Store the chosen item object
                        focusManager.clearFocus()
                    },
                    isSelected = isSelected
                )
            }
        }

        if (!isInputValid) {
            Text(
                text = stringResource(id = R.string.medication_name_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationNameInputPreview() {
    AppTheme(dynamicColor = false) {
        MedicationNameInput(
            medicationName = "Ibuprofen",
            onMedicationNameChange = {},
            onMedicationSelected = {},
            searchResultsListModifier = Modifier.heightIn(max = 200.dp), // Provide a default for preview
            viewModel = null // Pass null for preview
        )
    }
}