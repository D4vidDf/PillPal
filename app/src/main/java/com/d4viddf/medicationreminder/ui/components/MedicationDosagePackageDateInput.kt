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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.ui.theme.AppTheme
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

    // Resolve strings in Composable context
    val strDosagePackageDatesTitle = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_package_dates_title)
    val strDosageWholePill = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_whole_pill)
    val strDosageMg = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_mg) // Assuming R.string.dosage_mg exists for "mg"
    val strDosageMl = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_ml) // Assuming R.string.dosage_ml exists for "ml"
    val strDosageSprays = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_sprays)
    val strDosageSuppositories = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_suppositories)
    val strDosagePatches = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_patches)
    val strDosageTapToSet = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_tap_to_set)
    val strDosageLabel = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_label)
    val strDosagePrefilledInfo = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_prefilled_info)
    val strPackageSizeLabel = stringResource(id = com.d4viddf.medicationreminder.R.string.package_size_label)
    val strPackageUnitsLabel = stringResource(id = com.d4viddf.medicationreminder.R.string.package_units_label)
    val strStartDateLabel = stringResource(id = com.d4viddf.medicationreminder.R.string.start_date_label)
    val strSelectStartDatePlaceholder = stringResource(id = com.d4viddf.medicationreminder.R.string.select_start_date_placeholder)
    val strEndDateLabel = stringResource(id = com.d4viddf.medicationreminder.R.string.end_date_label)
    val strSelectEndDatePlaceholder = stringResource(id = com.d4viddf.medicationreminder.R.string.select_end_date_placeholder)
    val strSelectDosageDialogTitle = stringResource(id = com.d4viddf.medicationreminder.R.string.select_dosage_dialog_title)
    val strDosageWheelPickerNotAvailable = stringResource(id = com.d4viddf.medicationreminder.R.string.dosage_wheel_picker_not_available)
    val strDialogDoneButton = stringResource(id = com.d4viddf.medicationreminder.R.string.dialog_done_button)
    val strDialogOkButton = stringResource(id = com.d4viddf.medicationreminder.R.string.dialog_ok_button)
    val strDialogCancelButton = stringResource(id = com.d4viddf.medicationreminder.R.string.dialog_cancel_button)

    // Pre-resolve PillFraction display values
    // The 'pillFractionDisplayValues' using context = null was part of an intermediate step and is not needed.
    // 'resolvedPillFractionDisplayValues' is the correct one.
    val resolvedPillFractionDisplayValues = PillFraction.values().map {
        it to PillFraction.displayValue(it) // Call the composable function directly
    }.toMap()


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
                dosage.ifEmpty {
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
                } ?: strDosageTapToSet
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
        val configuration = LocalConfiguration.current
        ModalBottomSheet(
            onDismissRequest = { showDosageModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text(
                    strSelectDosageDialogTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (medicationType?.name) { // medicationType.name is English, used for logic
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
                            LaunchedEffect(numPills, pillFraction) {
                                val fractionDisplay = resolvedPillFractionDisplayValues[pillFraction] ?: ""
                                onDosageChange("${numPills}${fractionDisplay}")
                            }

                            IOSWheelPicker(
                                items = (0..10).toList(), selectedItem = numPills, onItemSelected = { numPills = it },
                                modifier = Modifier.width(80.dp).height(150.dp),
                                displayTransform = { it.toString() }
                            )
                            IOSWheelPicker(
                                items = PillFraction.values().toList(), selectedItem = pillFraction, onItemSelected = { pillFraction = it },
                                modifier = Modifier.padding(start = 16.dp).width(120.dp).height(150.dp),
                                displayTransform = { resolvedPillFractionDisplayValues[it] ?: it.name }
                            )
                        }
                        "Cream", "Creme" -> {
                            var creamAmount by remember(dosage) { mutableStateOf(dosage.substringBefore(" ") ?: "10") }
                            var creamUnit by remember(dosage) { mutableStateOf(CreamUnit.values().find { dosage.endsWith(it.displayValue) } ?: CreamUnit.MG) }
                            LaunchedEffect(creamAmount, creamUnit) { onDosageChange("${creamAmount} ${creamUnit.displayValue}") }

                            IOSWheelPicker(items = (0..100 step 5).map { it.toString() }, selectedItem = creamAmount, onItemSelected = { creamAmount = it }, modifier = Modifier.width(100.dp).height(150.dp), displayTransform = { it })
                            IOSWheelPicker(items = CreamUnit.values().toList(), selectedItem = creamUnit, onItemSelected = { creamUnit = it }, modifier = Modifier.padding(start = 16.dp).width(100.dp).height(150.dp), displayTransform = { it.displayValue })
                        }
                        "Liquid" -> {
                            var liquidAmount by remember(dosage) { mutableStateOf(dosage.substringBefore(" ") ?: "10") }
                            var liquidUnit by remember(dosage) { mutableStateOf(LiquidUnit.values().find { dosage.endsWith(it.displayValue) } ?: LiquidUnit.ML) }
                            LaunchedEffect(liquidAmount, liquidUnit) { onDosageChange("${liquidAmount} ${liquidUnit.displayValue}") }

                            IOSWheelPicker(items = (0..1000 step 10).map { it.toString() }, selectedItem = liquidAmount, onItemSelected = { liquidAmount = it }, modifier = Modifier.width(100.dp).height(150.dp), displayTransform = { it })
                            IOSWheelPicker(items = LiquidUnit.values().toList(), selectedItem = liquidUnit, onItemSelected = { liquidUnit = it }, modifier = Modifier.padding(start = 16.dp).width(100.dp).height(150.dp), displayTransform = { it.displayValue })
                        }
                        "Powder" -> {
                            var powderAmount by remember(dosage) { mutableStateOf(dosage.substringBefore(" ") ?: "100") }
                            var powderUnit by remember(dosage) { mutableStateOf(PowderUnit.values().find { dosage.endsWith(it.displayValue) } ?: PowderUnit.MG) }
                            LaunchedEffect(powderAmount, powderUnit) { onDosageChange("${powderAmount} ${powderUnit.displayValue}") }

                            IOSWheelPicker(items = (0..1000 step 10).map { it.toString() }, selectedItem = powderAmount, onItemSelected = { powderAmount = it }, modifier = Modifier.width(100.dp).height(150.dp), displayTransform = { it })
                            IOSWheelPicker(items = PowderUnit.values().toList(), selectedItem = powderUnit, onItemSelected = { powderUnit = it }, modifier = Modifier.padding(start = 16.dp).width(100.dp).height(150.dp), displayTransform = { it.displayValue })
                        }
                        "Syringe" -> {
                            var syringeAmount by remember(dosage) { mutableStateOf(dosage.substringBefore(" ") ?: "1") }
                            LaunchedEffect(syringeAmount) { onDosageChange("${syringeAmount} ${SyringeUnit.ML.displayValue}") } // displayValue is "ml"

                            IOSWheelPicker(items = (0..100 step 1).map { it.toString() }, selectedItem = syringeAmount, onItemSelected = { syringeAmount = it }, modifier = Modifier.width(80.dp).height(150.dp), displayTransform = { it })
                            Text(SyringeUnit.ML.displayValue, modifier = Modifier.padding(start = 16.dp)) // "ml"
                        }
                        "Spray" -> {
                            var numSprays by remember(dosage) { mutableStateOf(dosage.filter { it.isDigit() }.toIntOrNull() ?: 1) }
                            LaunchedEffect(numSprays, strDosageSprays) { onDosageChange("$numSprays $strDosageSprays") }

                            IOSWheelPicker(items = (1..10).toList(), selectedItem = numSprays, onItemSelected = { numSprays = it }, modifier = Modifier.width(80.dp).height(150.dp), displayTransform = { it.toString() })
                            Text(strDosageSprays, modifier = Modifier.padding(start = 16.dp))
                        }
                        "Suppository", "Suppositorium" -> {
                            var numSuppositories by remember(dosage) { mutableStateOf(dosage.filter { it.isDigit() }.toIntOrNull() ?: 1) }
                            LaunchedEffect(numSuppositories, strDosageSuppositories) { onDosageChange("$numSuppositories $strDosageSuppositories") }

                            IOSWheelPicker(items = (1..10).toList(), selectedItem = numSuppositories, onItemSelected = { numSuppositories = it }, modifier = Modifier.width(80.dp).height(150.dp), displayTransform = { it.toString() })
                            Text(strDosageSuppositories, modifier = Modifier.padding(start = 16.dp))
                        }
                        "Patch" -> {
                            var numPatches by remember(dosage) { mutableStateOf(dosage.filter { it.isDigit() }.toIntOrNull() ?: 1) }
                            LaunchedEffect(numPatches, strDosagePatches) { onDosageChange("$numPatches $strDosagePatches") }

                            IOSWheelPicker(items = (1..10).toList(), selectedItem = numPatches, onItemSelected = { numPatches = it }, modifier = Modifier.width(80.dp).height(150.dp), displayTransform = { it.toString() })
                            Text(strDosagePatches, modifier = Modifier.padding(start = 16.dp))
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
        val okText = stringResource(id = com.d4viddf.medicationreminder.R.string.dialog_ok_button)
        val cancelText = stringResource(id = com.d4viddf.medicationreminder.R.string.dialog_cancel_button)

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

@Composable
fun <T> IOSWheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    displayTransform: (T) -> String // Kept as (T) -> String, resolution happens before calling
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(selectedItem).coerceIn(0, items.size.coerceAtLeast(1) - 1)
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

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isActuallySelected = item == selectedItem

                Text(
                    text = displayTransform(item), // displayTransform is now @Composable
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


enum class PillFraction(val stringRes: Int) {
    WHOLE(com.d4viddf.medicationreminder.R.string.dosage_whole_pill),
    HALF(com.d4viddf.medicationreminder.R.string.dosage_half_pill),
    QUARTER(com.d4viddf.medicationreminder.R.string.dosage_quarter_pill);

    companion object {
        @Composable
        fun displayValue(fraction: PillFraction): String { // Removed unused context parameter
            return stringResource(id = fraction.stringRes)
        }
    }
}

// Assuming CreamUnit, LiquidUnit, PowderUnit, SyringeUnit enums have `displayValue`
// that are either already non-translatable (like "ml", "mg") or their `toString`
// / `displayTransform` in `IOSWheelPicker` would handle stringResource if they were for display.
// For units like "sprays", "suppositories", "patches", these were directly handled with stringResource
// in the `when` block for `medicationType?.name`.
// If these enums were to be displayed directly via their `toString` or a `displayValue` property
// in a generic way, those methods/properties would need to become @Composable or accept a Composable lambda
// to use stringResource, similar to PillFraction.
// For now, existing structure for these specific units seems to handle i18n where the text is generated.

enum class CreamUnit(val displayValue: String) { // displayValue seems to be for units like "mg", "g" - these are often not translated
    MG("mg"), G("g"), ML("ml");
    override fun toString(): String = displayValue
}
enum class LiquidUnit(val displayValue: String) { // "ml", "l" - often not translated
    ML("ml"), L("l");
    override fun toString(): String = displayValue
}
enum class PowderUnit(val displayValue: String) { // "mg", "g" - often not translated
    MG("mg"), G("g");
    override fun toString(): String = displayValue
}
enum class SyringeUnit(val displayValue: String) { // "ml" - often not translated
    ML("ml");
    override fun toString(): String = displayValue
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun MedicationDosagePackageDateInputPreview() {
    AppTheme {
        // This preview might have limited functionality for the dosage modal
        // as the ViewModel won't be properly injected in a preview.
        // The medicationType will likely be null.
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
            // viewModel = // Cannot easily provide a fake ViewModel here for preview
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun M3StyledDatePickerButtonPreview() {
    AppTheme {
        M3StyledDatePickerButton(
            label = "Start Date",
            dateString = "",
            placeholder = "Select date",
            onDateSelected = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun IOSWheelPickerPreview() {
    AppTheme {
        Box(modifier = Modifier.height(150.dp).width(100.dp)) { // Provide size for the picker
            IOSWheelPicker(
                items = (0..10).toList(),
                selectedItem = 5,
                onItemSelected = {},
                displayTransform = { it.toString() }
            )
        }
    }
}