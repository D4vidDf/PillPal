@file:OptIn(ExperimentalMaterial3Api::class)

package com.d4viddf.medicationreminder.ui.features.medication.add.components

import android.content.res.Configuration
import com.d4viddf.medicationreminder.data.model.MedicationSearchResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.features.medication.add.MedicationTypeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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

    // Resolve strings in Composable context
    val strDosagePackageDatesTitle = stringResource(id = R.string.dosage_package_dates_title)
    val strDosageWholePill = stringResource(id = R.string.dosage_whole_pill)
    val strDosageMg = stringResource(id = R.string.dosage_mg) // Assuming R.string.dosage_mg exists for "mg"
    val strDosageMl = stringResource(id = R.string.dosage_ml) // Assuming R.string.dosage_ml exists for "ml"
    val strDosageSprays = stringResource(id = R.string.dosage_sprays)
    val strDosageSuppositories = stringResource(id = R.string.dosage_suppositories)
    val strDosagePatches = stringResource(id = R.string.dosage_patches)
    val strDosageTapToSet = stringResource(id = R.string.dosage_tap_to_set)
    val strDosageLabel = stringResource(id = R.string.dosage_label)
    val strDosagePrefilledInfo = stringResource(id = R.string.dosage_prefilled_info)
    val strPackageSizeLabel = stringResource(id = R.string.package_size_label)
    val strPackageUnitsLabel = stringResource(id = R.string.package_units_label)
    val strStartDateLabel = stringResource(id = R.string.start_date_label)
    val strSelectStartDatePlaceholder = stringResource(id = R.string.select_start_date_placeholder)
    val strEndDateLabel = stringResource(id = R.string.end_date_label)
    val strSelectEndDatePlaceholder = stringResource(id = R.string.select_end_date_placeholder)
    val strSelectDosageDialogTitle = stringResource(id = R.string.select_dosage_dialog_title)
    val strDosageWheelPickerNotAvailable = stringResource(id = R.string.dosage_wheel_picker_not_available)
    val strDialogDoneButton = stringResource(id = R.string.dialog_done_button)
    val strDialogOkButton = stringResource(id = R.string.dialog_ok_button)
    val strDialogCancelButton = stringResource(id = R.string.dialog_cancel_button)

    // Pre-resolve PillFraction display values
    // The 'pillFractionDisplayValues' using context = null was part of an intermediate step and is not needed.
    // 'resolvedPillFractionDisplayValues' is the correct one.
    val resolvedPillFractionDisplayValues = PillFraction.entries.associate {
        it to PillFraction.displayValue(it) // Call the composable function directly
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Text(
            strDosagePackageDatesTitle,
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
            val displayDosage = remember(dosage, medicationSearchResult, medicationType, strDosageWholePill, strDosageMg, strDosageMl, strDosageSprays, strDosageSuppositories, strDosagePatches, strDosageTapToSet) {
                val formattedDosage = dosage
                    .replace(".5", " ½")
                    .replace(".33", " ⅓")
                    .replace(".25", " ¼")
                    .replace(".0", "")

                formattedDosage.ifEmpty {
                    if (medicationSearchResult?.dosage != null) medicationSearchResult.dosage
                    else {
                        when (medicationType?.name) { // medicationType.name is English, used for logic
                            "Tablet", "Pill" -> strDosageWholePill
                            "Cream", "Creme" -> "10 $strDosageMg"
                            "Liquid" -> "10 $strDosageMl"
                            "Powder" -> "100 $strDosageMg"
                            "Syringe" -> "1 $strDosageMl"
                            "Spray" -> "1 $strDosageSprays"
                            "Suppository", "Suppositorium" -> "1 $strDosageSuppositories"
                            "Patch" -> "1 $strDosagePatches"
                            else -> strDosageTapToSet
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(strDosageLabel, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = displayDosage, style = MaterialTheme.typography.headlineMedium)
                if (medicationSearchResult?.dosage != null) {
                    Text(
                        text = strDosagePrefilledInfo,
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
                Text(strPackageSizeLabel, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = packageSize,
                    onValueChange = onPackageSizeChange,
                    label = { Text(strPackageUnitsLabel) },
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
                label = strStartDateLabel,
                dateString = startDate,
                onDateSelected = onStartDateSelected,
                modifier = Modifier.weight(1f),
                placeholder = strSelectStartDatePlaceholder
            )
            M3StyledDatePickerButton(
                label = strEndDateLabel,
                dateString = endDate,
                onDateSelected = onEndDateSelected,
                modifier = Modifier.weight(1f),
                placeholder = strSelectEndDatePlaceholder,
                minSelectableDateMillis = if (startDate.isNotEmpty() && startDate != strSelectStartDatePlaceholder) {
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(startDate)?.time
                    } catch (e: Exception) { null }
                } else null
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDosageModal) {
        ModalBottomSheet(
            onDismissRequest = { showDosageModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    strSelectDosageDialogTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (medicationType?.name) {
                        "Tablet", "Pill" -> {
                            var numPills by remember(dosage) {
                                mutableStateOf(
                                    dosage.split(".").first().filter { it.isDigit() }.ifEmpty { "" }
                                )
                            }
                            var selectedFraction by remember(dosage) {
                                mutableStateOf(
                                    when {
                                        dosage.endsWith(".5") -> PillFraction.HALF
                                        dosage.endsWith(".33") -> PillFraction.THIRD
                                        dosage.endsWith(".25") -> PillFraction.QUARTER
                                        else -> PillFraction.WHOLE
                                    }
                                )
                            }

                            LaunchedEffect(numPills, selectedFraction) {
                                val dosageValue = if (selectedFraction == PillFraction.WHOLE) {
                                    if (numPills.isNotEmpty()) (numPills.toIntOrNull() ?: 0).toString() else ""
                                } else {
                                    val intValue = numPills.toIntOrNull() ?: 0
                                    when (selectedFraction) {
                                        PillFraction.HALF -> "$intValue.5"
                                        PillFraction.THIRD -> "$intValue.33"
                                        PillFraction.QUARTER -> "$intValue.25"
                                        else -> intValue.toString()
                                    }
                                }
                                onDosageChange(dosageValue)
                            }

                            OutlinedTextField(
                                value = numPills,
                                onValueChange = { numPills = it.filter { c -> c.isDigit() } },
                                label = { Text("Pills") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            UnitDropdown(
                                items = PillFraction.values().toList(),
                                selected = selectedFraction,
                                onSelected = { selectedFraction = it },
                                displayTransform = { it.display },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        "Cream", "Creme" -> {
                            val parts = dosage.split(" ")
                            var amount by remember { mutableStateOf(parts.firstOrNull() ?: "") }
                            var unit by remember { mutableStateOf(CreamUnit.values().find { it.displayValue == parts.getOrNull(1) } ?: CreamUnit.MG) }

                            LaunchedEffect(amount, unit) {
                                if (amount.isNotBlank()) {
                                    onDosageChange("$amount ${unit.displayValue}")
                                } else {
                                    onDosageChange("")
                                }
                            }

                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it },
                                label = { Text("Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            UnitDropdown(
                                items = CreamUnit.values().toList(),
                                selected = unit,
                                onSelected = { unit = it },
                                displayTransform = { it.displayValue },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        "Liquid" -> {
                            val parts = dosage.split(" ")
                            var amount by remember { mutableStateOf(parts.firstOrNull() ?: "") }
                            var unit by remember { mutableStateOf(LiquidUnit.values().find { it.displayValue == parts.getOrNull(1) } ?: LiquidUnit.ML) }

                            LaunchedEffect(amount, unit) {
                                if (amount.isNotBlank()) {
                                    onDosageChange("$amount ${unit.displayValue}")
                                } else {
                                    onDosageChange("")
                                }
                            }

                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it },
                                label = { Text("Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            UnitDropdown(
                                items = LiquidUnit.values().toList(),
                                selected = unit,
                                onSelected = { unit = it },
                                displayTransform = { it.displayValue },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        "Powder" -> {
                            val parts = dosage.split(" ")
                            var amount by remember { mutableStateOf(parts.firstOrNull() ?: "") }
                            var unit by remember { mutableStateOf(PowderUnit.values().find { it.displayValue == parts.getOrNull(1) } ?: PowderUnit.MG) }

                            LaunchedEffect(amount, unit) {
                                if (amount.isNotBlank()) {
                                    onDosageChange("$amount ${unit.displayValue}")
                                } else {
                                    onDosageChange("")
                                }
                            }

                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it },
                                label = { Text("Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            UnitDropdown(
                                items = PowderUnit.values().toList(),
                                selected = unit,
                                onSelected = { unit = it },
                                displayTransform = { it.displayValue },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        "Syringe", "Suppository", "Suppositorium", "Spray", "Patch" -> {
                            val (unitLabel, unitSuffix) = when(medicationType?.name) {
                                "Syringe" -> SyringeUnit.ML.displayValue to " ${SyringeUnit.ML.displayValue}"
                                "Spray" -> strDosageSprays to " $strDosageSprays"
                                "Suppository", "Suppositorium" -> strDosageSuppositories to " $strDosageSuppositories"
                                "Patch" -> strDosagePatches to " $strDosagePatches"
                                else -> "" to ""
                            }
                            var amount by remember(dosage) { mutableStateOf(dosage.removeSuffix(unitSuffix)) }

                            LaunchedEffect(amount) {
                                if (amount.isNotBlank()) {
                                    onDosageChange("$amount$unitSuffix")
                                } else {
                                    onDosageChange("")
                                }
                            }

                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it.filter { c -> c.isDigit() } },
                                label = { Text(unitLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            Text(strDosageWheelPickerNotAvailable,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showDosageModal = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strDialogDoneButton)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3StyledDatePickerButton(
    label: String, // stringResource
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
        val okText = stringResource(id = R.string.dialog_ok_button)
        val cancelText = stringResource(id = R.string.dialog_cancel_button)

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
                ) { Text(okText) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(cancelText) }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = null, // Title can be set here if needed, using stringResource
                headline = null, // Headline can be set here if needed, using stringResource
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> UnitDropdown(
    items: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    displayTransform: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayTransform(selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(displayTransform(item)) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

enum class PillFraction(val value: Float, val display: String) {
    WHOLE(0.0f, " "),
    HALF(0.5f, "½"),
    THIRD(0.33f, "⅓"),
    QUARTER(0.25f, "¼");

    companion object {
        @Composable
        fun displayValue(fraction: PillFraction): String {
            return fraction.display
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

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationDosagePackageDateInputPreview() {
    AppTheme(dynamicColor = false) {
        MedicationDosagePackageDateInput(
            selectedTypeId = 1,
            dosage = "1 pill",
            onDosageChange = {},
            packageSize = "30",
            onPackageSizeChange = {},
            medicationSearchResult = null,
            startDate = "",
            onStartDateSelected = {},
            endDate = "",
            onEndDateSelected = {}
        )
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun M3StyledDatePickerButtonPreview() {
    AppTheme(dynamicColor = false) {
        M3StyledDatePickerButton(
            label = "Start Date",
            dateString = "",
            placeholder = "Select date",
            onDateSelected = {}
        )
    }
}