package com.d4viddf.medicationreminder.ui.features.medication.add.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DosageEditor(
    initialDosage: String,
    unit: String,
    onSave: (String) -> Unit
) {
    var integerPart by remember { mutableStateOf("1") }
    var selectedFraction by remember { mutableStateOf(PillFraction.WHOLE) }
    var showFractionMenu by remember { mutableStateOf(false) }

    LaunchedEffect(initialDosage) {
        if (initialDosage.isBlank() || initialDosage == "0") {
            integerPart = "1"
            selectedFraction = PillFraction.WHOLE
            return@LaunchedEffect
        }
        val parts = initialDosage.split(".")
        integerPart = parts.getOrNull(0)?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() } ?: "0"
        val fractionPart = parts.getOrNull(1)
        selectedFraction = when (fractionPart) {
            "5" -> PillFraction.HALF
            "33" -> PillFraction.THIRD
            "25" -> PillFraction.QUARTER
            else -> PillFraction.WHOLE
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface // Match bottom sheet background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                "Dosage",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = integerPart,
                    onValueChange = { newValue ->
                        integerPart = (newValue.filter { it.isDigit() }.toIntOrNull() ?: 0).toString()
                    },
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface // Ensure visibility in dark mode
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Box {
                    Text(
                        text = selectedFraction.display,
                        style = if (selectedFraction == PillFraction.WHOLE) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface, // Ensure visibility in dark mode
                        modifier = Modifier.clickable { showFractionMenu = true }
                    )
                    DropdownMenu(
                        expanded = showFractionMenu,
                        onDismissRequest = { showFractionMenu = false }
                    ) {
                        PillFraction.values().forEach { fraction ->
                            DropdownMenuItem(
                                text = { Text(fraction.display) },
                                onClick = {
                                    selectedFraction = fraction
                                    showFractionMenu = false
                                },
                                colors = androidx.compose.material3.DropdownMenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            Text(
                text = unit,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val current = integerPart.toIntOrNull() ?: 0
                        if (current > 0) {
                            integerPart = (current - 1).toString()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease dosage",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer // Ensure visibility
                    )
                }
                Spacer(modifier = Modifier.width(32.dp))
                IconButton(
                    onClick = {
                        val current = integerPart.toIntOrNull() ?: 0
                        integerPart = (current + 1).toString()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase dosage",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer // Ensure visibility
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp)) // Placeholder for the checkbox space

            Button(
                onClick = {
                    val intPart = integerPart.toIntOrNull() ?: 0
                    val dosageValue = if (selectedFraction == PillFraction.WHOLE) {
                        intPart.toString()
                    } else {
                        val fractionValue = when(selectedFraction) {
                            PillFraction.HALF -> "5"
                            PillFraction.THIRD -> "33"
                            PillFraction.QUARTER -> "25"
                            PillFraction.WHOLE -> ""
                        }
                        "$intPart.$fractionValue"
                    }
                    onSave(dosageValue)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Save")
            }
        }
    }
}