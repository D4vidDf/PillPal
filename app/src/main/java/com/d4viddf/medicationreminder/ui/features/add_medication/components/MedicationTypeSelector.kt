package com.d4viddf.medicationreminder.ui.features.add_medication.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel

@Composable
fun MedicationTypeSelector(
    modifier: Modifier = Modifier, // This modifier comes from AddMedicationScreen (e.g., .fillMaxWidth().height(400.dp))
    selectedTypeId: Int,
    onTypeSelected: (Int) -> Unit,
    viewModel: MedicationTypeViewModel = hiltViewModel(),
    selectedColor: MedicationColor,
) {
    val medicationTypes by viewModel.medicationTypes.collectAsState(initial = emptyList())

    if (medicationTypes.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(), // Use fillMaxSize within the bounds of the passed modifier
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        // The incoming modifier (which includes .height(400.dp)) is applied to this Column.
        Column(
            modifier = modifier.fillMaxHeight(), // This Column now has the fixed height (e.g., 400dp)
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.medication_type_selector_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp) // Add some padding
            ) {
                items(medicationTypes.size) { index -> // Changed from type to index for clarity
                    val type = medicationTypes[index]
                    // Logic for cornerShape can remain, but ensure it doesn't depend on 'type' as index if list changes
                    val cornerShape = when (index) { // Assuming medicationTypes list order is stable for this
                        0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                        2 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 16.dp, bottomEnd = 8.dp)
                        // Adjust these indices based on a 3-column grid if they refer to specific visual positions
                        medicationTypes.size - 3 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 16.dp, topEnd = 8.dp, bottomEnd = 8.dp) // Example for bottom-left-ish
                        medicationTypes.size - 1 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 16.dp) // Example for bottom-right
                        else -> RoundedCornerShape(8.dp)
                    }
                    MedicationTypeItem(
                        type = type,
                        isSelected = type.id == selectedTypeId,
                        onClick = { onTypeSelected(type.id) },
                        selectedColor = selectedColor,
                        cornerRadius = cornerShape
                    )
                }
            }
        }
    }
}

// --- Preview Code ---

private val sampleMedicationTypes = listOf(
    MedicationType(id = 1, name = "Tablet", imageUrl = "https://example.com/tablet.png"),
    MedicationType(id = 2, name = "Capsule", imageUrl = "https://example.com/capsule.png"),
    MedicationType(id = 3, name = "Liquid", imageUrl = "https://example.com/liquid.png"),
    MedicationType(id = 4, name = "Injection", imageUrl = "https://example.com/injection.png"),
    MedicationType(id = 5, name = "Cream", imageUrl = "https://example.com/cream.png"),
    MedicationType(id = 6, name = "Drops", imageUrl = "https://example.com/drops.png")
)

@Preview(showBackground = true)
@Composable
fun MedicationTypeItemPreview() {
    AppTheme { // Changed to AppTheme
        Box(modifier = Modifier.padding(8.dp).size(120.dp)) { // Added Box for better sizing in preview
            MedicationTypeItem(
                type = sampleMedicationTypes[0],
                isSelected = true,
                selectedColor = MedicationColor.LIGHT_PINK,
                cornerRadius = RoundedCornerShape(8.dp),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 400)
@Composable
fun MedicationTypeSelectorPreview() {
    AppTheme { // Changed to AppTheme
        val selectedTypeId = 1
        val selectedColor = MedicationColor.LIGHT_PINK

        // Replicating MedicationTypeSelector's layout for preview purposes
        // without using the actual ViewModel-dependent composable.
        Column(
            modifier = Modifier.fillMaxSize(), // Adjusted to fill available space for preview
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Medication Type", // Using a placeholder title
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                items(sampleMedicationTypes.size) { index ->
                    val type = sampleMedicationTypes[index]
                    val cornerShape = when (index) {
                        0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                        2 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 16.dp, bottomEnd = 8.dp)
                        sampleMedicationTypes.size - 3 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 16.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                        sampleMedicationTypes.size - 1 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(8.dp)
                    }
                    MedicationTypeItem(
                        type = type,
                        isSelected = type.id == selectedTypeId,
                        onClick = { /* onTypeSelected(type.id) */ },
                        selectedColor = selectedColor,
                        cornerRadius = cornerShape
                    )
                }
            }
        }
    }
}

@Composable
fun MedicationTypeItem(
    type: MedicationType,
    isSelected: Boolean,
    selectedColor: MedicationColor,
    cornerRadius: RoundedCornerShape,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(4.dp) // Outer padding for spacing between items
            .fillMaxWidth() // Item takes full width of its grid cell
            .aspectRatio(1f) // Make item square or desired aspect ratio
            .background(
                color = if (isSelected) selectedColor.backgroundColor else MaterialTheme.colorScheme.surfaceVariant, // Use surfaceVariant for non-selected
                shape = cornerRadius
            )
            .clickable(onClick = onClick)
            .padding(8.dp), // Inner padding for content
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Center content vertically
    ) {
        AsyncImage(
            model = type.imageUrl,
            contentDescription = type.name,
            modifier = Modifier
                .fillMaxSize(0.6f) // Image takes a portion of the item size
                .aspectRatio(1f),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = type.name,
            style = MaterialTheme.typography.bodySmall, // Adjusted for potentially smaller space
            color = if (isSelected) selectedColor.textColor else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}