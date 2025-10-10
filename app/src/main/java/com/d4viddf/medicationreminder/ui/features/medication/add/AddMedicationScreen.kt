package com.d4viddf.medicationreminder.ui.features.medication.add

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.FrequencyType
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.MedicationSearchResult
import com.d4viddf.medicationreminder.data.model.ScheduleType
import com.d4viddf.medicationreminder.ui.features.medication.add.components.*
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import com.d4viddf.medicationreminder.workers.WorkerScheduler
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    navController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
    medicationViewModel: MedicationViewModel = hiltViewModel(),
    medicationScheduleViewModel: MedicationScheduleViewModel = hiltViewModel()
) {
    val isTablet = widthSizeClass >= WindowWidthSizeClass.Medium

    val stepDetailsList = listOf(
        StepDetails(1, stringResource(R.string.step_title_medication_name), R.drawable.ic_pill_placeholder),
        StepDetails(2, stringResource(R.string.step_title_type_color), R.drawable.rounded_palette_24),
        StepDetails(3, stringResource(R.string.step_title_dosage_package), R.drawable.ic_inventory),
        StepDetails(4, stringResource(R.string.step_title_frequency), R.drawable.ic_access_time),
        StepDetails(5, stringResource(R.string.step_title_summary), R.drawable.ic_check)
    )

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var progress by rememberSaveable { mutableFloatStateOf(0f) }
    var selectedTypeId by rememberSaveable { mutableIntStateOf(1) }
    var selectedColor by rememberSaveable { mutableStateOf(MedicationColor.LIGHT_ORANGE) }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var showColorSheet by remember { mutableStateOf(false) }

    val selectStartDatePlaceholder = stringResource(id = R.string.select_start_date_placeholder)
    val selectEndDatePlaceholder = stringResource(id = R.string.select_end_date_placeholder)

    var frequency by rememberSaveable { mutableStateOf(FrequencyType.ONCE_A_DAY) }
    var selectedDays by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var onceADayTime by rememberSaveable { mutableStateOf<LocalTime?>(null) }
    var selectedTimes by rememberSaveable { mutableStateOf<List<LocalTime>>(emptyList()) }
    var intervalHours by rememberSaveable { mutableIntStateOf(1) }
    var intervalMinutes by rememberSaveable { mutableIntStateOf(0) }
    var intervalStartTime by rememberSaveable { mutableStateOf<LocalTime?>(null) }
    var intervalEndTime by rememberSaveable { mutableStateOf<LocalTime?>(null) }

    var medicationName by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var packageSize by rememberSaveable { mutableStateOf("") }
    var saveRemainingFraction by rememberSaveable { mutableStateOf(false) }
    var medicationSearchResult by rememberSaveable { mutableStateOf<MedicationSearchResult?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val timeFormatter = remember { DateTimeFormatter.ISO_LOCAL_TIME }
    val localContext = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "Name: ",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (medicationName.isBlank() || currentStep == 0) "" else medicationName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        if (currentStep != 0) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                            )
                        } else {
                            Spacer(Modifier.height(4.dp + 8.dp))
                        }
                    }
                },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = {
                            if (currentStep == 2) {
                                dosage = ""
                            }
                            currentStep--
                            progress = (currentStep + 1) / 5f
                        }) {
                            Icon(painterResource(id = R.drawable.rounded_arrow_back_ios_24), contentDescription = stringResource(id = R.string.back))
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(painterResource(id = R.drawable.rounded_arrow_back_ios_24), contentDescription = stringResource(id = R.string.back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(id = R.drawable.rounded_close_24), contentDescription = stringResource(id = R.string.close))
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (currentStep < 4 && medicationName.isNotBlank()) {
                        currentStep++
                        progress = (currentStep + 1) / 5f
                    } else if (currentStep == 4) {
                        coroutineScope.launch {
                            val currentRegistrationDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val finalStartDate: String = if (startDate.isNotBlank() && startDate != selectStartDatePlaceholder) {
                                startDate
                            } else {
                                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE).also {
                                    Log.d("AddMedScreen", "User did not select a start date. Defaulting to today: $it")
                                }
                            }

                            val medicationToInsert = Medication(
                                name = medicationName,
                                typeId = selectedTypeId,
                                color = selectedColor.toString(),
                                packageSize = packageSize.toIntOrNull() ?: 0,
                                remainingDoses = packageSize.toIntOrNull() ?: 0,
                                saveRemainingFraction = saveRemainingFraction,
                                startDate = finalStartDate,
                                endDate = if (endDate.isNotBlank() && endDate != selectEndDatePlaceholder) endDate else null,
                                reminderTime = null,
                                registrationDate = currentRegistrationDate,
                                nregistro = medicationSearchResult?.nregistro
                            )

                            val scheduleType = when (frequency) {
                                FrequencyType.ONCE_A_DAY -> ScheduleType.DAILY
                                FrequencyType.MULTIPLE_TIMES_A_DAY -> ScheduleType.CUSTOM_ALARMS
                                FrequencyType.INTERVAL -> ScheduleType.INTERVAL
                            }

                            val scheduleToInsert = MedicationSchedule(
                                medicationId = 0,
                                scheduleType = scheduleType,
                                startDate = finalStartDate,
                                intervalHours = if (scheduleType == ScheduleType.INTERVAL) intervalHours else null,
                                intervalMinutes = if (scheduleType == ScheduleType.INTERVAL) intervalMinutes else null,
                                daysOfWeek = if (scheduleType == ScheduleType.DAILY) selectedDays.map { DayOfWeek.of(it) } else null,
                                specificTimes = when (scheduleType) {
                                    ScheduleType.DAILY -> onceADayTime?.let { listOf(it) }
                                    ScheduleType.CUSTOM_ALARMS -> selectedTimes
                                    else -> null
                                },
                                intervalStartTime = if (scheduleType == ScheduleType.INTERVAL) intervalStartTime?.format(timeFormatter) else null,
                                intervalEndTime = if (scheduleType == ScheduleType.INTERVAL) intervalEndTime?.format(timeFormatter) else null
                            )

                            val (medicationId, _) = medicationViewModel.insertMedicationAndDosage(
                                medication = medicationToInsert,
                                schedule = scheduleToInsert,
                                dosage = dosage
                            )

                            medicationId.let { medId ->
                                val finalSchedule = scheduleToInsert.copy(medicationId = medId)
                                medicationScheduleViewModel.insertSchedule(finalSchedule)

                                val appContext = localContext.applicationContext
                                WorkerScheduler.scheduleRemindersForMedication(appContext, medId)
                                Log.d("AddMedScreen", "Called WorkerScheduler.scheduleRemindersForMedication for medId: $medId after inserting medication and schedule.")
                            }

                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier
                    .then(if (isTablet) Modifier.widthIn(max = 300.dp) else Modifier.fillMaxWidth())
                    .padding(16.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .height(60.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = medicationName.isNotBlank() && (currentStep != 2 || (packageSize.isNotBlank() && packageSize.toIntOrNull() != null))
            ) {
                Text(if (currentStep == 4) stringResource(id = R.string.confirm) else stringResource(id = R.string.next))
            }
        }
    ) { innerPadding ->
        if (isTablet) {
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.3f)
                        .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
                        .fillMaxHeight()
                ) {
                    AddMedicationStepsIndicator(
                        currentStep = currentStep,
                        totalSteps = stepDetailsList.size,
                        stepDetails = stepDetailsList
                    )
                }

                val rightColumnModifier = if (currentStep == 0) {
                    Modifier
                        .weight(0.7f)
                        .padding(16.dp)
                        .fillMaxHeight()
                } else {
                    Modifier
                        .weight(0.7f)
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                }

                Column(modifier = rightColumnModifier) {
                    CurrentStepContent(
                        currentStep = currentStep,
                        medicationName = medicationName,
                        onMedicationNameChange = { medicationName = it },
                        onMedicationSelected = { result ->
                            medicationSearchResult = result
                            if (result != null) {
                                medicationName = result.name
                            }
                        },
                        selectedTypeId = selectedTypeId,
                        onTypeSelected = { selectedTypeId = it },
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it },
                        dosage = dosage,
                        onDosageChange = { dosage = it },
                        packageSize = packageSize,
                        onPackageSizeChange = { packageSize = it },
                        saveRemainingFraction = saveRemainingFraction,
                        onSaveRemainingFractionChange = { saveRemainingFraction = it },
                        medicationSearchResult = medicationSearchResult,
                        startDate = if (startDate.isBlank()) selectStartDatePlaceholder else startDate,
                        onStartDateSelected = { startDate = it },
                        endDate = if (endDate.isBlank()) selectEndDatePlaceholder else endDate,
                        onEndDateSelected = { endDate = it },
                        frequency = frequency,
                        onFrequencySelected = { newFrequency ->
                            frequency = newFrequency
                            if (newFrequency != FrequencyType.ONCE_A_DAY) {
                                onceADayTime = null
                                selectedDays = emptyList()
                            }
                            if (newFrequency != FrequencyType.MULTIPLE_TIMES_A_DAY) {
                                selectedTimes = emptyList()
                            }
                            if (newFrequency != FrequencyType.INTERVAL) {
                                intervalHours = 1
                                intervalMinutes = 0
                                intervalStartTime = null
                                intervalEndTime = null
                            }
                        },
                        selectedDays = selectedDays,
                        onDaysSelected = { selectedDays = it },
                        onceADayTime = onceADayTime,
                        onOnceADayTimeSelected = { onceADayTime = it },
                        selectedTimes = selectedTimes,
                        onTimesSelected = { selectedTimes = it },
                        intervalHours = intervalHours,
                        onIntervalHoursChanged = { intervalHours = it },
                        intervalMinutes = intervalMinutes,
                        onIntervalMinutesChanged = { intervalMinutes = it },
                        intervalStartTime = intervalStartTime,
                        onIntervalStartTimeSelected = { intervalStartTime = it },
                        intervalEndTime = intervalEndTime,
                        onIntervalEndTimeSelected = { intervalEndTime = it },
                        selectStartDatePlaceholder = selectStartDatePlaceholder,
                        selectEndDatePlaceholder = selectEndDatePlaceholder,
                        isTablet = isTablet,
                        onShowColorSheet = { showColorSheet = true },
                        onDismissColorSheet = { showColorSheet = false },
                        showColorSheet = showColorSheet
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
            ) {
                CurrentStepContent(
                    currentStep = currentStep,
                    medicationName = medicationName,
                    onMedicationNameChange = { medicationName = it },
                    onMedicationSelected = { result ->
                        medicationSearchResult = result
                        if (result != null) {
                            medicationName = result.name
                        }
                    },
                    selectedTypeId = selectedTypeId,
                    onTypeSelected = { selectedTypeId = it },
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                    dosage = dosage,
                    onDosageChange = { dosage = it },
                    packageSize = packageSize,
                    onPackageSizeChange = { packageSize = it },
                    saveRemainingFraction = saveRemainingFraction,
                    onSaveRemainingFractionChange = { saveRemainingFraction = it },
                    medicationSearchResult = medicationSearchResult,
                    startDate = if (startDate.isBlank()) selectStartDatePlaceholder else startDate,
                    onStartDateSelected = { startDate = it },
                    endDate = if (endDate.isBlank()) selectEndDatePlaceholder else endDate,
                    onEndDateSelected = { endDate = it },
                    frequency = frequency,
                    onFrequencySelected = { newFrequency ->
                        frequency = newFrequency
                        if (newFrequency != FrequencyType.ONCE_A_DAY) {
                            onceADayTime = null
                            selectedDays = emptyList()
                        }
                        if (newFrequency != FrequencyType.MULTIPLE_TIMES_A_DAY) {
                            selectedTimes = emptyList()
                        }
                        if (newFrequency != FrequencyType.INTERVAL) {
                            intervalHours = 1
                            intervalMinutes = 0
                            intervalStartTime = null
                            intervalEndTime = null
                        }
                    },
                    selectedDays = selectedDays,
                    onDaysSelected = { selectedDays = it },
                    onceADayTime = onceADayTime,
                    onOnceADayTimeSelected = { onceADayTime = it },
                    selectedTimes = selectedTimes,
                    onTimesSelected = { selectedTimes = it },
                    intervalHours = intervalHours,
                    onIntervalHoursChanged = { intervalHours = it },
                    intervalMinutes = intervalMinutes,
                    onIntervalMinutesChanged = { intervalMinutes = it },
                    intervalStartTime = intervalStartTime,
                    onIntervalStartTimeSelected = { intervalStartTime = it },
                    intervalEndTime = intervalEndTime,
                    onIntervalEndTimeSelected = { intervalEndTime = it },
                    selectStartDatePlaceholder = selectStartDatePlaceholder,
                    selectEndDatePlaceholder = selectEndDatePlaceholder,
                    isTablet = isTablet,
                    onShowColorSheet = { showColorSheet = true },
                    onDismissColorSheet = { showColorSheet = false },
                    showColorSheet = showColorSheet
                )
            }
        }
    }
}

@Composable
private fun CurrentStepContent(
    currentStep: Int,
    medicationName: String,
    onMedicationNameChange: (String) -> Unit,
    onMedicationSelected: (MedicationSearchResult?) -> Unit,
    selectedTypeId: Int,
    onTypeSelected: (Int) -> Unit,
    selectedColor: MedicationColor,
    onColorSelected: (MedicationColor) -> Unit,
    dosage: String,
    onDosageChange: (String) -> Unit,
    packageSize: String,
    onPackageSizeChange: (String) -> Unit,
    saveRemainingFraction: Boolean,
    onSaveRemainingFractionChange: (Boolean) -> Unit,
    medicationSearchResult: MedicationSearchResult?,
    startDate: String,
    onStartDateSelected: (String) -> Unit,
    endDate: String,
    onEndDateSelected: (String) -> Unit,
    frequency: FrequencyType,
    onFrequencySelected: (FrequencyType) -> Unit,
    selectedDays: List<Int>,
    onDaysSelected: (List<Int>) -> Unit,
    onceADayTime: LocalTime?,
    onOnceADayTimeSelected: (LocalTime?) -> Unit,
    selectedTimes: List<LocalTime>,
    onTimesSelected: (List<LocalTime>) -> Unit,
    intervalHours: Int,
    onIntervalHoursChanged: (Int) -> Unit,
    intervalMinutes: Int,
    onIntervalMinutesChanged: (Int) -> Unit,
    intervalStartTime: LocalTime?,
    onIntervalStartTimeSelected: (LocalTime?) -> Unit,
    intervalEndTime: LocalTime?,
    onIntervalEndTimeSelected: (LocalTime?) -> Unit,
    selectStartDatePlaceholder: String,
    selectEndDatePlaceholder: String,
    isTablet: Boolean,
    onShowColorSheet: () -> Unit,
    onDismissColorSheet: () -> Unit,
    showColorSheet: Boolean
) {
    when (currentStep) {
        0 -> {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            val searchResultsListModifier = if (isTablet) {
                Modifier.fillMaxHeight()
            } else if (isLandscape) {
                Modifier.heightIn(max = 300.dp)
            } else {
                Modifier.heightIn(min = 200.dp, max = 600.dp)
            }

            MedicationNameInput(
                medicationName = medicationName,
                onMedicationNameChange = onMedicationNameChange,
                onMedicationSelected = onMedicationSelected,
                searchResultsListModifier = searchResultsListModifier
            )
        }
        1 -> {
            Column(modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 600.dp)) {
                MedicationTypeSelector(
                    selectedTypeId = selectedTypeId,
                    onTypeSelected = onTypeSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    selectedColor = selectedColor
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { onShowColorSheet() }
                ) {
                    ColorSelector(
                        selectedColor = selectedColor,
                        onColorSelected = onColorSelected,
                        showBottomSheet = showColorSheet,
                        onDismiss = onDismissColorSheet
                    )
                }
            }
        }
        2 -> {
            MedicationDosagePackageDateInput(
                selectedTypeId = selectedTypeId,
                dosage = dosage, onDosageChange = onDosageChange,
                packageSize = packageSize, onPackageSizeChange = onPackageSizeChange,
                saveRemainingFraction = saveRemainingFraction,
                onSaveRemainingFractionChange = onSaveRemainingFractionChange,
                medicationSearchResult = medicationSearchResult,
                startDate = startDate,
                onStartDateSelected = onStartDateSelected,
                endDate = endDate,
                onEndDateSelected = onEndDateSelected
            )
        }
        3 -> {
            FrequencySelector(
                selectedFrequency = frequency,
                onFrequencySelected = onFrequencySelected,
                selectedDays = selectedDays,
                onDaysSelected = onDaysSelected,
                onceADayTime = onceADayTime,
                onOnceADayTimeSelected = onOnceADayTimeSelected,
                selectedTimes = selectedTimes,
                onTimesSelected = onTimesSelected,
                intervalHours = intervalHours,
                onIntervalHoursChanged = onIntervalHoursChanged,
                intervalMinutes = intervalMinutes,
                onIntervalMinutesChanged = onIntervalMinutesChanged,
                intervalStartTime = intervalStartTime,
                onIntervalStartTimeSelected = onIntervalStartTimeSelected,
                intervalEndTime = intervalEndTime,
                onIntervalEndTimeSelected = onIntervalEndTimeSelected
            )
        }
        4 -> {
            val summaryStartDate = if (startDate == selectStartDatePlaceholder) "" else startDate
            val summaryEndDate = if (endDate == selectEndDatePlaceholder) "" else endDate
            MedicationSummary(
                typeId = selectedTypeId, medicationName = medicationName, color = selectedColor.backgroundColor,
                dosage = dosage, packageSize = packageSize, frequency = frequency,
                startDate = summaryStartDate, endDate = summaryEndDate,
                onceADayTime = onceADayTime,
                selectedTimes = selectedTimes,
                intervalHours = intervalHours,
                intervalMinutes = intervalMinutes,
                intervalStartTime = intervalStartTime,
                intervalEndTime = intervalEndTime,
                selectedDays = selectedDays
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowColorSheet() }
            ) {
                ColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = onColorSelected,
                    showBottomSheet = showColorSheet,
                    onDismiss = onDismissColorSheet
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddMedicationScreenPreview() {
    AppTheme {
        AddMedicationScreen(
            navController = rememberNavController(),
            widthSizeClass = WindowWidthSizeClass.Compact
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MedicationSummaryPreview() {
    AppTheme {
        MedicationSummary(
            typeId = 1,
            medicationName = "Medication Name",
            color = Color.Cyan,
            dosage = "1 pill",
            packageSize = "30",
            frequency = FrequencyType.ONCE_A_DAY,
            startDate = "2024-01-01",
            endDate = "2024-01-30",
            onceADayTime = LocalTime.of(9, 0),
            selectedTimes = listOf(LocalTime.of(9, 0), LocalTime.of(18, 0)),
            intervalHours = 8,
            intervalMinutes = 0,
            intervalStartTime = LocalTime.of(8, 0),
            intervalEndTime = LocalTime.of(20, 0),
            selectedDays = listOf(1, 2, 3, 4, 5)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InfoRowPreview() {
    AppTheme {
        InfoRow(label = "Label", value = "Value")
    }
}

@Composable
fun MedicationSummary(
    typeId: Int, medicationName: String, color: Color, dosage: String, packageSize: String,
    frequency: FrequencyType,
    startDate: String, endDate: String,
    onceADayTime: LocalTime?,
    selectedTimes: List<LocalTime>,
    intervalHours: Int,
    intervalMinutes: Int,
    intervalStartTime: LocalTime?,
    intervalEndTime: LocalTime?,
    selectedDays: List<Int>
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val daysOfWeekMap = listOf(
        stringResource(id = R.string.day_mon), stringResource(id = R.string.day_tue),
        stringResource(id = R.string.day_wed), stringResource(id = R.string.day_thu),
        stringResource(id = R.string.day_fri), stringResource(id = R.string.day_sat),
        stringResource(id = R.string.day_sun)
    )
    val notSet = stringResource(id = R.string.not_set)
    val noneSet = stringResource(id = R.string.none_set)
    val selectStartDatePlaceholder = stringResource(id = R.string.select_start_date_placeholder)
    val selectEndDatePlaceholder = stringResource(id = R.string.select_end_date_placeholder)

    val formattedDosage = dosage
        .replace(".5", " ½")
        .replace(".33", " ⅓")
        .replace(".25", " ¼")
        .replace(".0", "")
        .trim()

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)) {
        Text(stringResource(id = R.string.medication_summary_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        InfoRow(stringResource(id = R.string.label_name), medicationName)
        InfoRow(stringResource(id = R.string.label_dosage), formattedDosage.ifEmpty { notSet })
        InfoRow(stringResource(id = R.string.label_package_size), packageSize.ifEmpty { notSet })
        InfoRow(stringResource(id = R.string.label_start_date), if (startDate.isBlank() || startDate == selectStartDatePlaceholder) notSet else startDate)
        InfoRow(stringResource(id = R.string.label_end_date), if (endDate.isBlank() || endDate == selectEndDatePlaceholder) notSet else endDate)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text(stringResource(id = R.string.label_color), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Box(Modifier
                .size(24.dp)
                .background(color, CircleShape))
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(id = R.string.reminder_details_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        InfoRow(stringResource(id = R.string.label_frequency), stringResource(id = frequency.stringResId))

        when (frequency) {
            FrequencyType.ONCE_A_DAY -> {
                InfoRow(stringResource(id = R.string.label_time), onceADayTime?.format(timeFormatter) ?: notSet)
                if (selectedDays.isNotEmpty()) {
                    InfoRow(stringResource(id = R.string.label_days), selectedDays.sorted().mapNotNull { daysOfWeekMap.getOrNull(it - 1) }.joinToString(", "))
                }
            }
            FrequencyType.MULTIPLE_TIMES_A_DAY -> {
                if (selectedTimes.isNotEmpty()) {
                    selectedTimes.sorted().forEachIndexed { index, time ->
                        InfoRow(stringResource(id = R.string.label_alarm_numbered, index + 1), time.format(timeFormatter))
                    }
                } else {
                    InfoRow(stringResource(id = R.string.label_alarms), noneSet)
                }
            }
            FrequencyType.INTERVAL -> {
                InfoRow(stringResource(id = R.string.label_interval), stringResource(id = R.string.interval_details, intervalHours, intervalMinutes))
                InfoRow(stringResource(id = R.string.label_daily_start_time), intervalStartTime?.format(timeFormatter) ?: notSet)
                InfoRow(stringResource(id = R.string.label_daily_end_time), intervalEndTime?.format(timeFormatter) ?: notSet)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}