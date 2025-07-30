package com.d4viddf.medicationreminder.ui.features.medication.add

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.features.medication.add.components.AddMedicationStepsIndicator
import com.d4viddf.medicationreminder.ui.features.medication.add.components.ColorSelector
import com.d4viddf.medicationreminder.ui.features.medication.add.components.FrequencySelector
import com.d4viddf.medicationreminder.ui.features.medication.add.components.MedicationDosagePackageDateInput
import com.d4viddf.medicationreminder.ui.features.medication.add.components.MedicationNameInput
import com.d4viddf.medicationreminder.ui.features.medication.add.components.MedicationTypeSelector
import com.d4viddf.medicationreminder.ui.features.medication.add.components.StepDetails
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.workers.WorkerScheduler
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    // onNavigateBack: () -> Unit, // Remove this
    navController: NavHostController, // Add this
    widthSizeClass: WindowWidthSizeClass,
    medicationViewModel: MedicationViewModel = hiltViewModel(),
    medicationScheduleViewModel: MedicationScheduleViewModel = hiltViewModel()
    // medicationInfoViewModel: MedicationInfoViewModel = hiltViewModel() // Removed parameter
) {
    // val windowSizeClass = LocalWindowSizeClass.current // REMOVE THIS
    val isTablet = widthSizeClass >= WindowWidthSizeClass.Medium // Ensure this uses the parameter

    val stepDetailsList = listOf(
        StepDetails(
            1,
            stringResource(R.string.step_title_medication_name),
            R.drawable.ic_pill_placeholder
        ),
        StepDetails(
            2,
            stringResource(R.string.step_title_type_color),
            R.drawable.rounded_palette_24
        ), // Use new icon
        StepDetails(3, stringResource(R.string.step_title_dosage_package), R.drawable.ic_inventory), // Use new icon
        StepDetails(4, stringResource(R.string.step_title_frequency), R.drawable.ic_access_time), // Use new icon
        StepDetails(
            5,
            stringResource(R.string.step_title_summary),
            R.drawable.ic_check
        ) // Use new icon
    )

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var progress by rememberSaveable { mutableFloatStateOf(0f) }
    var selectedTypeId by rememberSaveable { mutableIntStateOf(1) }
    var selectedColor by rememberSaveable { mutableStateOf(MedicationColor.LIGHT_ORANGE) }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }

    // Resolve placeholder strings once
    val selectStartDatePlaceholder = stringResource(id = R.string.select_start_date_placeholder)
    val selectEndDatePlaceholder = stringResource(id = R.string.select_end_date_placeholder)

    // Frequency related states
    var frequency by rememberSaveable { mutableStateOf(FrequencyType.ONCE_A_DAY) }
    var selectedDays by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }

    // FrequencyType.ONCE_A_DAY specific state
    var onceADayTime by rememberSaveable { mutableStateOf<LocalTime?>(null) }

    // "Multiple times a day" specific state
    var selectedTimes by rememberSaveable { mutableStateOf<List<LocalTime>>(emptyList()) }

    // "Interval" specific states
    var intervalHours by rememberSaveable { mutableIntStateOf(1) } // Default to 1 hour
    var intervalMinutes by rememberSaveable { mutableIntStateOf(0) }
    var intervalStartTime by rememberSaveable { mutableStateOf<LocalTime?>(null) }
    var intervalEndTime by rememberSaveable { mutableStateOf<LocalTime?>(null) }


    var medicationName by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var packageSize by rememberSaveable { mutableStateOf("") }
    var medicationSearchResult by rememberSaveable { mutableStateOf<MedicationSearchResult?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val timeFormatter = remember { DateTimeFormatter.ISO_LOCAL_TIME } // For storing LocalTime as String

    val localContext = LocalContext.current // Get the current context

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (medicationName.isBlank() || currentStep == 0) stringResource(id = R.string.new_medication_toolbar_title) else medicationName.substringBefore(" "),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                            // Removed onGloballyPositioned as titleWidth is not used
                        )
                        if (currentStep != 0) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                            )
                        } else {
                            Spacer(Modifier.height(4.dp + 8.dp)) // Placeholder for progress bar height
                        }
                    }
                },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = {
                            currentStep--
                            progress = (currentStep + 1) / 5f
                        }) {
                            Icon(painterResource(id = R.drawable.rounded_arrow_back_ios_24), contentDescription = stringResource(id = R.string.back))
                        }
                    } else {
                        // If on step 0, this back button could also navigate back past the choice screen
                        // Or simply pop AddMedicationScreen, leading to AddMedicationChoiceScreen
                        // For consistency with the close button, let's make it pop AddMedicationScreen only.
                        // The user would then be on AddMedicationChoiceScreen and can use its new close button.
                         IconButton(onClick = { navController.popBackStack() }) { // Pops AddMedicationScreen
                             Icon(painterResource(id = R.drawable.rounded_arrow_back_ios_24), contentDescription = stringResource(id = R.string.back))
                         }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // This should take us back to the screen before AddMedicationChoiceScreen
                         navController.popBackStack(Screen.AddMedicationChoice.route, inclusive = true)
                    }) {
                        Icon(painterResource(id = R.drawable.rounded_close_24), contentDescription = stringResource(id = R.string.close))
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (currentStep < 4 && medicationName.isNotBlank()) { // TODO: Add validation for each step
                        currentStep++
                        progress = (currentStep + 1) / 5f
                    } else if (currentStep == 4) {
                        coroutineScope.launch {
                            val currentRegistrationDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) // "yyyy-MM-dd"
                            val finalStartDate: String?
                            if (startDate.isNotBlank() && startDate != selectStartDatePlaceholder) {
                                finalStartDate = startDate
                            } else {
                                finalStartDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                Log.d("AddMedScreen", "User did not select a start date. Defaulting to today: $finalStartDate")
                            }

                            val medicationId = medicationViewModel.insertMedication(
                                Medication(
                                    name = medicationName, typeId = selectedTypeId, color = selectedColor.toString(),
                                    dosage = dosage.ifEmpty { null }, packageSize = packageSize.toIntOrNull() ?: 0,
                                    remainingDoses = packageSize.toIntOrNull() ?: 0,
                                    startDate = finalStartDate, // Use the new finalStartDate
                                    endDate = if (endDate.isNotBlank() && endDate != selectEndDatePlaceholder) endDate else null,
                                    reminderTime = null, // This seems to be consistently null here
                                    registrationDate = currentRegistrationDate, // Set the new field
                                    nregistro = medicationSearchResult?.nregistro // Populate nregistro from search result
                                )
                            )
                            medicationId.let { medId ->
                                val scheduleType = when (frequency) {
                                    FrequencyType.ONCE_A_DAY -> ScheduleType.DAILY
                                    FrequencyType.MULTIPLE_TIMES_A_DAY -> ScheduleType.CUSTOM_ALARMS
                                    FrequencyType.INTERVAL -> ScheduleType.INTERVAL
                                    // else case is not strictly needed if all FrequencyType cases are handled and frequency is non-nullable.
                                    // However, to be safe or if new types are added without updating this `when`, a default is good.
                                }

                                val schedule = MedicationSchedule(
                                    medicationId = medId,
                                    scheduleType = scheduleType,
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
                                medicationScheduleViewModel.insertSchedule(schedule)

                                // Get ApplicationContext from LocalContext for WorkerScheduler
                                val appContext = localContext.applicationContext
                                WorkerScheduler.scheduleRemindersForMedication(appContext, medId)
                                Log.d("AddMedScreen", "Called WorkerScheduler.scheduleRemindersForMedication for medId: $medId after inserting medication and schedule.")

                                // Removed medicationInfoViewModel.insertMedicationInfo call block
                            }
                            // onNavigateBack() // Remove this line

                            // New navigation logic:
                            // Pop AddMedicationScreen AND AddMedicationChoiceScreen from the back stack.
                            // This effectively returns to the screen that was active before AddMedicationChoiceScreen.
                            navController.popBackStack(Screen.AddMedicationChoice.route, inclusive = true)
                        }
                    }
                },
                modifier = Modifier
                    .then(if (isTablet) Modifier.widthIn(max = 300.dp) else Modifier.fillMaxWidth()) // Apply conditional width
                    .padding(16.dp) // Keep overall padding
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
                    // .verticalScroll(scrollState) // Vertical scroll might need to be applied to individual columns if they overflow
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
            ) {
                // Left Column (Steps Indicator)
                Column(
                    modifier = Modifier
                        .weight(0.3f) // Adjust weight as needed
                        .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp) // Adjusted padding
                        .fillMaxHeight() // Ensure it takes full height for the line connectors
                    // .verticalScroll(rememberScrollState()) // Removed as the indicator itself should manage its height or be non-scrolling if designed to fit
                ) {
                    AddMedicationStepsIndicator(
                        currentStep = currentStep,
                        totalSteps = stepDetailsList.size,
                        stepDetails = stepDetailsList
                    )
                }

                // Right Column (Content based on currentStep)
                val rightColumnModifier = if (currentStep == 0) { // Step 0 is MedicationNameInput with LazyColumn
                    Modifier
                        .weight(0.7f)
                        .padding(16.dp)
                        // NO verticalScroll for step 0 on tablet, LazyColumn in MedicationNameInput will handle scroll
                        .fillMaxHeight() // Ensure this column takes up available height for LazyColumn to fill
                } else {
                    Modifier
                        .weight(0.7f)
                        .padding(16.dp)
                        .verticalScroll(scrollState) // Existing scrollState for other steps
                }

                Column(
                    modifier = rightColumnModifier
                ) {
                    CurrentStepContent( // Extracted the when block into a new Composable
                        currentStep = currentStep,
                        medicationName = medicationName,
                        onMedicationNameChange = { medicationName = it },
                        onMedicationSelected = { result ->
                            medicationSearchResult = result
                            if (result != null) {
                                medicationName = result.name
                                dosage = result.dosage ?: ""
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
                        medicationSearchResult = medicationSearchResult,
                        startDate = if (startDate.isBlank()) selectStartDatePlaceholder else startDate,
                        onStartDateSelected = { startDate = it },
                        endDate = if (endDate.isBlank()) selectEndDatePlaceholder else endDate,
                        onEndDateSelected = { endDate = it },
                        frequency = frequency,
                        onFrequencySelected = { newFrequency ->
                            frequency = newFrequency
                            // Reset states logic...
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
                        isTablet = isTablet // Pass isTablet here
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            // Phone layout (existing Column structure)
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
            ) {
                CurrentStepContent( // Use the extracted Composable here too
                    currentStep = currentStep,
                    medicationName = medicationName,
                    onMedicationNameChange = { medicationName = it },
                    onMedicationSelected = { result ->
                        medicationSearchResult = result
                        if (result != null) {
                            medicationName = result.name
                            dosage = result.dosage ?: ""
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
                    medicationSearchResult = medicationSearchResult,
                    startDate = if (startDate.isBlank()) selectStartDatePlaceholder else startDate,
                    onStartDateSelected = { startDate = it },
                    endDate = if (endDate.isBlank()) selectEndDatePlaceholder else endDate,
                    onEndDateSelected = { endDate = it },
                    frequency = frequency,
                    onFrequencySelected = { newFrequency ->
                        frequency = newFrequency
                        // Reset states logic...
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
                        isTablet = isTablet // Pass isTablet here
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
    // Add placeholders passed from parent
    selectStartDatePlaceholder: String,
    selectEndDatePlaceholder: String,
    isTablet: Boolean // Add this
) {
    when (currentStep) {
        0 -> {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            val searchResultsListModifier = if (isTablet) {
                Modifier.fillMaxHeight()
            } else if (isLandscape) {
                // More constrained height for phone landscape to avoid conflict with TextField & outer scroll
                Modifier.heightIn(max = 300.dp) // Adjusted from 100dp to give a bit more space
            } else { // Phone Portrait
                Modifier.heightIn(min = 200.dp, max = 600.dp) // Adjusted from 200dp
            }

            MedicationNameInput(
                medicationName = medicationName,
                onMedicationNameChange = onMedicationNameChange,
                onMedicationSelected = onMedicationSelected,
                searchResultsListModifier = searchResultsListModifier
            )
        }
        1 -> {
             Column(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 600.dp)) {
                MedicationTypeSelector(
                    selectedTypeId = selectedTypeId,
                    onTypeSelected = onTypeSelected,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    selectedColor = selectedColor
                )
                 ColorSelector(
                     selectedColor = selectedColor,
                     onColorSelected = onColorSelected,
                     modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                 )
            }
        }
        2 -> {
            MedicationDosagePackageDateInput(
                selectedTypeId = selectedTypeId,
                dosage = dosage, onDosageChange = onDosageChange,
                packageSize = packageSize, onPackageSizeChange = onPackageSizeChange,
                medicationSearchResult = medicationSearchResult,
                startDate = startDate, // Pass the potentially placeholder-containing state
                onStartDateSelected = onStartDateSelected,
                endDate = endDate, // Pass the potentially placeholder-containing state
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
            // Ensure placeholders are correctly passed if MedicationSummary uses them
            val summaryStartDate = if (startDate == selectStartDatePlaceholder) "" else startDate
            val summaryEndDate = if (endDate == selectEndDatePlaceholder) "" else endDate
            MedicationSummary(
                typeId = selectedTypeId, medicationName = medicationName, color = selectedColor.backgroundColor,
                dosage = dosage, packageSize = packageSize, frequency = frequency,
                startDate = summaryStartDate, endDate = summaryEndDate, // Use resolved values
                onceADayTime = onceADayTime,
                selectedTimes = selectedTimes,
                intervalHours = intervalHours,
                intervalMinutes = intervalMinutes,
                intervalStartTime = intervalStartTime,
                intervalEndTime = intervalEndTime,
                selectedDays = selectedDays
            )
            Spacer(modifier = Modifier.height(16.dp))
            ColorSelector(selectedColor = selectedColor, onColorSelected = onColorSelected)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddMedicationScreenPreview() {
    AppTheme {
        AddMedicationScreen(
            // onNavigateBack = {}, // Remove
            navController = rememberNavController(), // Add
            widthSizeClass = WindowWidthSizeClass.Compact // Provide a default for preview
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
    frequency: FrequencyType, // Changed to FrequencyType
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

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(stringResource(id = R.string.medication_summary_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        InfoRow(stringResource(id = R.string.label_name), medicationName)
        InfoRow(stringResource(id = R.string.label_dosage), dosage.ifEmpty { notSet })
        InfoRow(stringResource(id = R.string.label_package_size), packageSize.ifEmpty { notSet })
        InfoRow(stringResource(id = R.string.label_start_date), if (startDate.isBlank() || startDate == selectStartDatePlaceholder) notSet else startDate)
        InfoRow(stringResource(id = R.string.label_end_date), if (endDate.isBlank() || endDate == selectEndDatePlaceholder) notSet else endDate)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text(stringResource(id = R.string.label_color), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(24.dp).background(color, CircleShape))
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(id = R.string.reminder_details_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        InfoRow(stringResource(id = R.string.label_frequency), stringResource(id = frequency.stringResId)) // Display localized frequency name

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
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}