package com.d4viddf.medicationreminder.ui.features.todayschedules.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.ui.components.getMedicationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationFilterBottomSheet(
    allMedications: List<Medication>,
    selectedMedicationIds: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    var tempSelectedIds by remember { mutableStateOf(selectedMedicationIds.toSet()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.filter_by_medication),
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = {
                    tempSelectedIds = if (tempSelectedIds.size == allMedications.size) {
                        emptySet()
                    } else {
                        allMedications.map { it.id }.toSet()
                    }
                }) {
                    Text(text = stringResource(id = R.string.select_all))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allMedications) { medication ->
                    MedicationFilterItem(
                        medication = medication,
                        isSelected = medication.id in tempSelectedIds,
                        onSelectionChanged = {
                            tempSelectedIds = if (medication.id in tempSelectedIds) {
                                tempSelectedIds - medication.id
                            } else {
                                tempSelectedIds + medication.id
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onConfirm(tempSelectedIds.toList()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.confirm))
            }
        }
    }
}

@Composable
private fun MedicationFilterItem(
    medication: Medication,
    isSelected: Boolean,
    onSelectionChanged: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = getMedicationIcon(medicationType = medication.type),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = medication.name,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onSelectionChanged() }
        )
    }
}