package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel

@Composable
fun MedicationTypeSelector(
    selectedTypeId: Int,
    onTypeSelected: (Int) -> Unit,
    viewModel: MedicationTypeViewModel = hiltViewModel(),
    selectedColor: MedicationColor,
    modifier: Modifier = Modifier
) {
    val medicationTypes by viewModel.medicationTypes.collectAsState(initial = emptyList())

    if (medicationTypes.isEmpty()) {
        // Display loading indicator
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select type of medication",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )

            // Make the LazyVerticalGrid fill the available space
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), // 3 columns
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Fill available space
            ) {
                items(medicationTypes.size) { type ->
                    val cornerShape = when (type) {
                        0 -> RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 8.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp
                        )
                        2 -> RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp,
                            topEnd = 16.dp,
                            bottomEnd = 8.dp
                        )
                        6 -> RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 16.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp
                        )
                        8 -> RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp,
                            topEnd = 8.dp,
                            bottomEnd = 16.dp
                        )
                        else -> RoundedCornerShape(8.dp)
                    }
                    MedicationTypeItem(
                        type = medicationTypes[type],
                        isSelected = medicationTypes[type].id == selectedTypeId,
                        onClick = { onTypeSelected(medicationTypes[type].id) },
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
            .padding(4.dp)
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) selectedColor.backgroundColor else MaterialTheme.colorScheme.background,
                shape = cornerRadius
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = type.imageUrl,
            contentDescription = type.name,
            modifier = Modifier
                .size(120.dp)
                .aspectRatio(1f),
            contentScale = ContentScale.Fit // Adjust how the image is scaled to fit the box
        )
        Text(
            text = type.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) selectedColor.textColor else MaterialTheme.colorScheme.onBackground
        )
    }
}