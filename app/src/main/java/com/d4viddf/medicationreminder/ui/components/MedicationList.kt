package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.d4viddf.medicationreminder.data.Medication

@Composable
fun MedicationList(
    medications: List<Medication>,
    onItemClick: (Medication) -> Unit,
    modifier: Modifier = Modifier // Add modifier here
) {
    Column(modifier = modifier) {
        LazyColumn {
            items(medications) { medication ->
                MedicationCard(
                    medication = medication,
                    onClick = { onItemClick(medication) }
                )
            }
        }
    }
}
