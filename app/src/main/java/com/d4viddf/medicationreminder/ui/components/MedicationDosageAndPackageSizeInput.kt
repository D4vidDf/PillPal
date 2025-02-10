@file:OptIn(ExperimentalMaterial3Api::class)

package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MedicationDosageAndPackageSizeInput(
    selectedTypeId: Int,
    dosage: String,
    onDosageChange: (String) -> Unit,
    packageSize: String,
    onPackageSizeChange: (String) -> Unit,
    viewModel: MedicationTypeViewModel = hiltViewModel()
) {
    val medicationTypes by viewModel.medicationTypes.collectAsState(initial = emptyList())
    val medicationType = medicationTypes.find { it.id == selectedTypeId }
    var showDosageModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Dosage Input
        Surface(
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { showDosageModal = true }
        ) {
            when (medicationType?.name) {
                "Tablet", "Pill" -> {
                    var numPills by remember { mutableStateOf(0) }
                    var pillFraction by remember { mutableStateOf(PillFraction.WHOLE) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dosage", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${numPills}${pillFraction.displayValue}",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                "Cream", "Creme" -> {
                    var creamAmount by remember { mutableStateOf("") }
                    var creamUnit by remember { mutableStateOf(CreamUnit.MG) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dosage", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${creamAmount} ${creamUnit.displayValue}",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                "Liquid" -> {
                    var liquidAmount by remember { mutableStateOf("") }
                    var liquidUnit by remember { mutableStateOf(LiquidUnit.ML) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dosage", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${liquidAmount} ${liquidUnit.displayValue}",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                "Powder" -> {
                    var powderAmount by remember { mutableStateOf("") }
                    var powderUnit by remember { mutableStateOf(PowderUnit.MG) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dosage", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${powderAmount} ${powderUnit.displayValue}",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                "Syringe" -> {
                    var syringeAmount by remember { mutableStateOf("") }
                    var syringeUnit by remember { mutableStateOf(SyringeUnit.ML) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dosage", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${syringeAmount} ${syringeUnit.displayValue}",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                "Spray" -> {
                    var numSprays by remember { mutableStateOf(0) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dosage", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$numSprays sprays",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                "Suppository", "Suppositorium" -> {
                    var numSuppositories by remember { mutableStateOf(0) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dosage", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$numSuppositories suppositories",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                "Patch" -> {
                    var numPatches by remember { mutableStateOf(0) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dosage", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$numPatches patches",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                else -> {
                    // Default input for unknown types
                    GenericTextFieldInput(
                        label = "Dosage",
                        value = dosage,
                        onValueChange = onDosageChange,
                        description = "Enter the dosage as indicated by your healthcare provider.",
                        isError = dosage.isBlank()
                    )
                }
            }
        }

        // Package Size Input
        Surface(
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Package Size", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = packageSize,
                    onValueChange = onPackageSizeChange,
                    textStyle = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.width(120.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // ModalBottomSheet for dosage selection
    if (showDosageModal) {
        ModalBottomSheet(
            onDismissRequest = { showDosageModal = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Select Dosage",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Scrollable Row for dosage selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    when (medicationType?.name) {
                        "Tablet", "Pill" -> {
                            var numPills by remember { mutableStateOf(1) }
                            var pillFraction by remember { mutableStateOf(PillFraction.WHOLE) }

                            // Left column: Number of pills
                            IOSWheelPicker(
                                items = (0..10).toList(),
                                selectedItem = numPills,
                                onItemSelected = { numPills = it },
                                modifier = Modifier.width(80.dp)
                            )

                            // Right column: Pill fraction
                            IOSWheelPicker(
                                items = PillFraction.values().toList(),
                                selectedItem = pillFraction,
                                onItemSelected = { pillFraction = it },
                                modifier = Modifier.padding(start = 16.dp)
                            )

                            // Update the dosage input field when the modal is dismissed
                            LaunchedEffect(showDosageModal) {
                                if (!showDosageModal) {
                                    onDosageChange("${numPills}${pillFraction.displayValue}")
                                }
                            }
                        }
                        "Cream", "Creme" -> {
                            var creamAmount by remember { mutableStateOf(1) }
                            var creamUnit by remember { mutableStateOf(CreamUnit.MG) }

                            // Left column: Cream amount
                            IOSWheelPicker(
                                items = (0..100).toList(),
                                selectedItem = creamAmount,
                                onItemSelected = { creamAmount = it },
                                modifier = Modifier.width(80.dp)
                            )

                            // Right column: Cream unit
                            IOSWheelPicker(
                                items = CreamUnit.values().toList(),
                                selectedItem = creamUnit,
                                onItemSelected = { creamUnit = it },
                                modifier = Modifier.padding(start = 16.dp)
                            )

                            // Update the dosage input field when the modal is dismissed
                            LaunchedEffect(showDosageModal) {
                                if (!showDosageModal) {
                                    onDosageChange("${creamAmount} ${creamUnit.displayValue}")
                                }
                            }
                        }
                        "Liquid" -> {
                            var liquidAmount by remember { mutableStateOf(1) }
                            var liquidUnit by remember { mutableStateOf(LiquidUnit.ML) }

                            // Left column: Liquid amount
                            IOSWheelPicker(
                                items = (0..1000).toList(), // Adjust range as needed
                                selectedItem = liquidAmount,
                                onItemSelected = { liquidAmount = it },
                                modifier = Modifier.width(80.dp)
                            )

                            // Right column: Liquid unit
                            IOSWheelPicker(
                                items = LiquidUnit.values().toList(),
                                selectedItem = liquidUnit,
                                onItemSelected = { liquidUnit = it },
                                modifier = Modifier.padding(start = 16.dp)
                            )

                            // Update the dosage input field when the modal is dismissed
                            LaunchedEffect(showDosageModal) {
                                if (!showDosageModal) {
                                    onDosageChange("${liquidAmount} ${liquidUnit.displayValue}")
                                }
                            }
                        }
                        "Powder" -> {
                            var powderAmount by remember { mutableStateOf(1) }
                            var powderUnit by remember { mutableStateOf(PowderUnit.MG) }

                            // Left column: Powder amount
                            IOSWheelPicker(
                                items = (0..1000).toList(), // Adjust range as needed
                                selectedItem = powderAmount,
                                onItemSelected = { powderAmount = it },
                                modifier = Modifier.width(80.dp)
                            )

                            // Right column: Powder unit
                            IOSWheelPicker(
                                items = PowderUnit.values().toList(),
                                selectedItem = powderUnit,
                                onItemSelected = { powderUnit = it },
                                modifier = Modifier.padding(start = 16.dp)
                            )

                            // Update the dosage input field when the modal is dismissed
                            LaunchedEffect(showDosageModal) {
                                if (!showDosageModal) {
                                    onDosageChange("${powderAmount} ${powderUnit.displayValue}")
                                }
                            }
                        }
                        "Syringe" -> {
                            var syringeAmount by remember { mutableStateOf(1) }

                            // Left column: Syringe amount
                            IOSWheelPicker(
                                items = (0..100).toList(), // Adjust range as needed
                                selectedItem = syringeAmount,
                                onItemSelected = { syringeAmount = it },
                                modifier = Modifier.width(80.dp)
                            )

                            // Right column: Syringe unit (fixed "ml")
                            Text("ml", modifier = Modifier.padding(start = 16.dp))

                            // Update the dosage input field when the modal is dismissed
                            LaunchedEffect(showDosageModal) {
                                if (!showDosageModal) {
                                    onDosageChange("${syringeAmount} ml")
                                }
                            }
                        }
                        "Spray" -> {
                            var numSprays by remember { mutableStateOf(1) }

                            // Left column: Number of sprays
                            IOSWheelPicker(
                                items = (0..10).toList(), // Adjust range as needed
                                selectedItem = numSprays,
                                onItemSelected = { numSprays = it },
                                modifier = Modifier.width(80.dp)
                            )

                            // Right column: Sprays (fixed label)
                            Text("Sprays", modifier = Modifier.padding(start = 16.dp))

                            // Update the dosage input field when the modal is dismissed
                            LaunchedEffect(showDosageModal) {
                                if (!showDosageModal) {
                                    onDosageChange("$numSprays sprays")
                                }
                            }
                        }
                        "Suppository", "Suppositorium" -> {
                            var numSuppositories by remember { mutableStateOf(1) }

                            // Left column: Number of suppositories
                            IOSWheelPicker(
                                items = (0..10).toList(), // Adjust range as needed
                                selectedItem = numSuppositories,
                                onItemSelected = { numSuppositories = it },
                                modifier = Modifier.width(80.dp)
                            )

                            // Right column: Suppositories (fixed label)
                            Text("Suppositories", modifier = Modifier.padding(start = 16.dp))

                            // Update the dosage input field when the modal is dismissed
                            LaunchedEffect(showDosageModal) {
                                if (!showDosageModal) {
                                    onDosageChange("$numSuppositories suppositories")
                                }
                            }
                        }
                        "Patch" -> {
                            var numPatches by remember { mutableStateOf(1) }

                            // Left column: Number of patches
                            IOSWheelPicker(
                                items = (0..10).toList(), // Adjust range as needed
                                selectedItem = numPatches,
                                onItemSelected = { numPatches = it },
                                modifier = Modifier.width(80.dp)
                            )

                            // Right column: Patches (fixed label)
                            Text("Patches", modifier = Modifier.padding(start = 16.dp))

                            // Update the dosage input field when the modal is dismissed
                            LaunchedEffect(showDosageModal) {
                                if (!showDosageModal) {
                                    onDosageChange("$numPatches patches")
                                }
                            }
                        }
                        else -> {
                            // Default content for unknown types
                            Text("Dosage selection not available for this type.")
                        }
                    }
                }
            }
        }
    }
}

// IOSWheelPicker Composable
@Composable
fun <T> IOSWheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex = remember { mutableStateOf(items.indexOf(selectedItem)) }

    // LazyColumn to simulate the wheel picker
    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.value),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = index == currentIndex.value

            Text(
                text = item.toString(),
                style = if (isSelected) {
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clickable {
                        currentIndex.value = index
                        onItemSelected(item)
                    }
            )
        }
    }
}

// Enums for dosage units
enum class PillFraction(val displayValue: String) {
    WHOLE(""),
    HALF(".5"),
    QUARTER(".25")
}

enum class CreamUnit(val displayValue: String) {
    MG("mg"),
    G("g"),
    ML("ml")
}

enum class LiquidUnit(val displayValue: String) {
    ML("ml"),
    L("l")
}

enum class PowderUnit(val displayValue: String) {
    MG("mg"),
    G("g")
}

enum class SyringeUnit(val displayValue: String) {
    ML("ml")
}