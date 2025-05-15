@file:OptIn(ExperimentalMaterial3Api::class)

package com.d4viddf.medicationreminder.ui.components

import MedicationSearchResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll // Import for sheet content scroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration // To get screen height
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDosagePackageDateInput(
    selectedTypeId: Int,
    dosage: String,
    onDosageChange: (String) -> Unit,
    packageSize: String,
    onPackageSizeChange: (String) -> Unit,
    medicationSearchResult: MedicationSearchResult?,
    startDate: String,
    onStartDateSelected: (String) -> Unit,
    endDate: String,
    onEndDateSelected: (String) -> Unit,
    viewModel: MedicationTypeViewModel = hiltViewModel()
) {
    val medicationTypes by viewModel.medicationTypes.collectAsState(initial = emptyList())
    val medicationType = medicationTypes.find { it.id == selectedTypeId }
    var showDosageModal by remember { mutableStateOf(false) }

    // This Column is part of the main screen's scrollable area (from AddMedicationScreen)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp) // Ensure consistent padding
    ) {
        Text(
            "Dosage, Package & Dates",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Surface(
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { showDosageModal = true }
        ) {
            val displayDosage = dosage.ifEmpty {
                if (medicationSearchResult?.dosage != null) medicationSearchResult.dosage
                else {
                    when (medicationType?.name) {
                        "Tablet", "Pill" -> "1${PillFraction.WHOLE.displayValue}"
                        "Cream", "Creme" -> "10 ${CreamUnit.MG.displayValue}"
                        "Liquid" -> "10 ${LiquidUnit.ML.displayValue}"
                        "Powder" -> "100 ${PowderUnit.MG.displayValue}"
                        "Syringe" -> "1 ${SyringeUnit.ML.displayValue}"
                        "Spray" -> "1 sprays"
                        "Suppository", "Suppositorium" -> "1 suppositories"
                        "Patch" -> "1 patches"
                        else -> "Tap to set dosage"
                    }
                }
            } ?: "Tap to set dosage"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Dosage", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = displayDosage, style = MaterialTheme.typography.headlineMedium)
                if (medicationSearchResult?.dosage != null) {
                    Text(
                        text = "(Pre-filled from CIMA, tap to edit)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Surface(
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Package Size", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = packageSize,
                    onValueChange = onPackageSizeChange,
                    label = { Text("Units") },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.width(120.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3StyledDatePickerButton(
                label = "Start Date",
                dateString = startDate,
                onDateSelected = onStartDateSelected,
                modifier = Modifier.weight(1f),
                placeholder = "Select Start Date"
            )
            M3StyledDatePickerButton(
                label = "End Date",
                dateString = endDate,
                onDateSelected = onEndDateSelected,
                modifier = Modifier.weight(1f),
                placeholder = "Select End Date",
                minSelectableDateMillis = if (startDate.isNotEmpty() && startDate != "Select Start Date") {
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(startDate)?.time
                    } catch (e: Exception) { null }
                } else null
            )
        }
        Spacer(modifier = Modifier.height(16.dp)) // Space at the bottom of Step 2 content
    }

    if (showDosageModal) {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val sheetScrollState = rememberScrollState()

        ModalBottomSheet(
            onDismissRequest = { showDosageModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false), // Typically better to force full expansion or handle skip carefully
            // dragHandle = null, // To remove the default drag handle if you want a cleaner look
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                // No verticalScroll or heightIn here; let sheet wrap content or provide its own scroll if content is too tall.
                // The fixed height of the Row of pickers is key.
            ) {
                Text(
                    "Select Dosage",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                )

                // This Row contains pickers. Its height is determined by the pickers (150.dp).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp) // Fixed height for the picker area (e.g. 150dp for picker + 30dp padding)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (medicationType?.name) {
                        "Tablet", "Pill" -> {
                            var numPills by remember(dosage) { mutableStateOf(dosage.substringBefore(".").toIntOrNull() ?: dosage.substringBefore(",").toIntOrNull() ?: dosage.filter { it.isDigit() }.toIntOrNull() ?: 1) }
                            var pillFraction by remember(dosage) {
                                mutableStateOf(
                                    when {
                                        dosage.contains(".5") || dosage.contains(",5") -> PillFraction.HALF
                                        dosage.contains(".25") || dosage.contains(",25") -> PillFraction.QUARTER
                                        else -> PillFraction.WHOLE
                                    }
                                )
                            }
                            LaunchedEffect(numPills, pillFraction) { onDosageChange("${numPills}${pillFraction.displayValue}") }

                            IOSWheelPicker(
                                items = (0..10).toList(),
                                selectedItem = numPills,
                                onItemSelected = { numPills = it },
                                modifier = Modifier.width(80.dp).height(150.dp)
                            )
                            IOSWheelPicker(
                                items = PillFraction.values().toList(),
                                selectedItem = pillFraction,
                                onItemSelected = { pillFraction = it },
                                modifier = Modifier.padding(start = 16.dp).width(120.dp).height(150.dp),
                                displayTransform = { it.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
                            )
                        }
                        "Cream", "Creme" -> {
                            var creamAmount by remember(dosage) { mutableStateOf(dosage.substringBefore(" ") ?: "10") }
                            var creamUnit by remember(dosage) { mutableStateOf(CreamUnit.values().find { dosage.endsWith(it.displayValue) } ?: CreamUnit.MG) }
                            LaunchedEffect(creamAmount, creamUnit) { onDosageChange("${creamAmount} ${creamUnit.displayValue}") }

                            IOSWheelPicker(
                                items = (0..100 step 5).map { it.toString() },
                                selectedItem = creamAmount,
                                onItemSelected = { creamAmount = it },
                                modifier = Modifier.width(100.dp).height(150.dp)
                            )
                            IOSWheelPicker(
                                items = CreamUnit.values().toList(),
                                selectedItem = creamUnit,
                                onItemSelected = { creamUnit = it },
                                modifier = Modifier.padding(start = 16.dp).width(100.dp).height(150.dp),
                                displayTransform = { it.displayValue }
                            )
                        }
                        "Liquid" -> {
                            var liquidAmount by remember(dosage) { mutableStateOf(dosage.substringBefore(" ") ?: "10") }
                            var liquidUnit by remember(dosage) { mutableStateOf(LiquidUnit.values().find { dosage.endsWith(it.displayValue) } ?: LiquidUnit.ML) }
                            LaunchedEffect(liquidAmount, liquidUnit) { onDosageChange("${liquidAmount} ${liquidUnit.displayValue}") }

                            IOSWheelPicker(
                                items = (0..1000 step 10).map { it.toString() },
                                selectedItem = liquidAmount,
                                onItemSelected = { liquidAmount = it },
                                modifier = Modifier.width(100.dp).height(150.dp)
                            )
                            IOSWheelPicker(
                                items = LiquidUnit.values().toList(),
                                selectedItem = liquidUnit,
                                onItemSelected = { liquidUnit = it },
                                modifier = Modifier.padding(start = 16.dp).width(100.dp).height(150.dp),
                                displayTransform = { it.displayValue }
                            )
                        }
                        "Powder" -> {
                            var powderAmount by remember(dosage) { mutableStateOf(dosage.substringBefore(" ") ?: "100") }
                            var powderUnit by remember(dosage) { mutableStateOf(PowderUnit.values().find { dosage.endsWith(it.displayValue) } ?: PowderUnit.MG) }
                            LaunchedEffect(powderAmount, powderUnit) { onDosageChange("${powderAmount} ${powderUnit.displayValue}") }

                            IOSWheelPicker(
                                items = (0..1000 step 10).map { it.toString() },
                                selectedItem = powderAmount,
                                onItemSelected = { powderAmount = it },
                                modifier = Modifier.width(100.dp).height(150.dp)
                            )
                            IOSWheelPicker(
                                items = PowderUnit.values().toList(),
                                selectedItem = powderUnit,
                                onItemSelected = { powderUnit = it },
                                modifier = Modifier.padding(start = 16.dp).width(100.dp).height(150.dp),
                                displayTransform = { it.displayValue }
                            )
                        }
                        "Syringe" -> {
                            var syringeAmount by remember(dosage) { mutableStateOf(dosage.substringBefore(" ") ?: "1") }
                            LaunchedEffect(syringeAmount) { onDosageChange("${syringeAmount} ${SyringeUnit.ML.displayValue}") }

                            IOSWheelPicker(
                                items = (0..100 step 1).map { it.toString() },
                                selectedItem = syringeAmount,
                                onItemSelected = { syringeAmount = it },
                                modifier = Modifier.width(80.dp).height(150.dp)
                            )
                            Text(SyringeUnit.ML.displayValue, modifier = Modifier.padding(start = 16.dp))
                        }
                        "Spray" -> {
                            var numSprays by remember(dosage) { mutableStateOf(dosage.filter { it.isDigit() }.toIntOrNull() ?: 1) }
                            LaunchedEffect(numSprays) { onDosageChange("$numSprays sprays") }

                            IOSWheelPicker(
                                items = (1..10).toList(),
                                selectedItem = numSprays,
                                onItemSelected = { numSprays = it },
                                modifier = Modifier.width(80.dp).height(150.dp)
                            )
                            Text("sprays", modifier = Modifier.padding(start = 16.dp))
                        }
                        "Suppository", "Suppositorium" -> {
                            var numSuppositories by remember(dosage) { mutableStateOf(dosage.filter { it.isDigit() }.toIntOrNull() ?: 1) }
                            LaunchedEffect(numSuppositories) { onDosageChange("$numSuppositories suppositories") }

                            IOSWheelPicker(
                                items = (1..10).toList(),
                                selectedItem = numSuppositories,
                                onItemSelected = { numSuppositories = it },
                                modifier = Modifier.width(80.dp).height(150.dp)
                            )
                            Text("suppositories", modifier = Modifier.padding(start = 16.dp))
                        }
                        "Patch" -> {
                            var numPatches by remember(dosage) { mutableStateOf(dosage.filter { it.isDigit() }.toIntOrNull() ?: 1) }
                            LaunchedEffect(numPatches) { onDosageChange("$numPatches patches") }

                            IOSWheelPicker(
                                items = (1..10).toList(),
                                selectedItem = numPatches,
                                onItemSelected = { numPatches = it },
                                modifier = Modifier.width(80.dp).height(150.dp)
                            )
                            Text("patches", modifier = Modifier.padding(start = 16.dp))
                        }
                        else -> { // Fallback for other types
                            Text("Dosage selection for this type is not available via wheel picker. Please enter manually if needed.",
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp)) // More space before the button

                Button(
                    onClick = { showDosageModal = false },
                    modifier = Modifier.fillMaxWidth() // Button takes full width
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3StyledDatePickerButton(
    label: String,
    dateString: String,
    placeholder: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    minSelectableDateMillis: Long? = null
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val currentDisplayDate = if (dateString.isNotEmpty() && dateString != placeholder) dateString else placeholder

    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentDisplayDate,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (currentDisplayDate != placeholder) FontWeight.Medium else FontWeight.Normal,
                color = if (currentDisplayDate != placeholder) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        if (currentDisplayDate != placeholder) {
            try {
                calendar.time = dateFormatter.parse(currentDisplayDate) ?: Date()
            } catch (_: Exception) { /* Default to today if parse fails */ }
        }
        val initialSelectedMillis = if (datePickerIsValidMillis(calendar.timeInMillis, minSelectableDateMillis)) {
            calendar.timeInMillis
        } else {
            minSelectableDateMillis ?: Calendar.getInstance().timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis,
            yearRange = (Calendar.getInstance().get(Calendar.YEAR) - 100)..(Calendar.getInstance().get(Calendar.YEAR) + 100),
            selectableDates = remember(minSelectableDateMillis) {
                if (minSelectableDateMillis != null) MinDateSelectable(minSelectableDateMillis) else AllDatesSelectable
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(dateFormatter.format(Date(millis)))
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null &&
                            (minSelectableDateMillis == null || datePickerState.selectedDateMillis!! >= minSelectableDateMillis)
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = true
            )
        }
    }
}

private fun datePickerIsValidMillis(millisToCheck: Long, minMillis: Long?): Boolean {
    return minMillis?.let { millisToCheck >= it } ?: true
}

@ExperimentalMaterial3Api
private object AllDatesSelectable : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
    override fun isSelectableYear(year: Int): Boolean = true
}

@ExperimentalMaterial3Api
private class MinDateSelectable(private val minUtcTimeMillis: Long) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis >= minUtcTimeMillis
    }
    override fun isSelectableYear(year: Int): Boolean {
        val calMin = Calendar.getInstance().apply { timeInMillis = minUtcTimeMillis }
        return year >= calMin.get(Calendar.YEAR)
    }
}

@Composable
fun <T> IOSWheelPicker( // Ensure this is the version you are using
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier, // This will be .width(X.dp).height(150.dp)
    displayTransform: (T) -> String = { it.toString() }
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(selectedItem).coerceIn(0, items.size.coerceAtLeast(1) -1)
    )

    LaunchedEffect(selectedItem, items) {
        val index = items.indexOf(selectedItem)
        if (index != -1 && index < items.size) {
            listState.scrollToItem(index)
        }
    }

    LaunchedEffect(listState.isScrollInProgress, items) {
        if (!listState.isScrollInProgress && items.isNotEmpty()) {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isNotEmpty()) {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val centralItem = visibleItemsInfo.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - viewportCenter) }
                centralItem?.index?.let { newIndex ->
                    if (newIndex >= 0 && newIndex < items.size && items[newIndex] != selectedItem) {
                        onItemSelected(items[newIndex])
                    }
                }
            }
        }
    }

    // CRITICAL: The Box takes the explicit size from the modifier passed to IOSWheelPicker.
    // The LazyColumn fills this constrained Box.
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(), // LazyColumn fills the sized Box
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isActuallySelected = item == selectedItem

                Text(
                    text = displayTransform(item),
                    style = if (isActuallySelected) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .clickable { onItemSelected(item) }
                )
            }
        }
    }
}

enum class PillFraction(val displayValue: String) {
    WHOLE(""), HALF(".5"), QUARTER(".25");
    override fun toString(): String {
        return when(this) {
            WHOLE -> "Whole"
            HALF -> "½"
            QUARTER -> "¼"
        }
    }
}
enum class CreamUnit(val displayValue: String) {
    MG("mg"), G("g"), ML("ml");
    override fun toString(): String = displayValue
}
enum class LiquidUnit(val displayValue: String) {
    ML("ml"), L("l");
    override fun toString(): String = displayValue
}
enum class PowderUnit(val displayValue: String) {
    MG("mg"), G("g");
    override fun toString(): String = displayValue
}
enum class SyringeUnit(val displayValue: String) {
    ML("ml");
    override fun toString(): String = displayValue
}