package com.d4viddf.medicationreminder.ui.screens

import MedicationSearchResult
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// import androidx.compose.ui.platform.LocalContext // Only if child components truly need it
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.FrequencyType
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationInfo
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.*
import com.d4viddf.medicationreminder.viewmodel.MedicationInfoViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import kotlinx.coroutines.launch
import java.time.LocalDate // Added import
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    onNavigateBack: () -> Unit,
    medicationViewModel: MedicationViewModel = hiltViewModel(),
    medicationScheduleViewModel: MedicationScheduleViewModel = hiltViewModel(),
    medicationInfoViewModel: MedicationInfoViewModel = hiltViewModel()
) {
    var currentStep by rememberSaveable { mutableStateOf(0) }
    var progress by rememberSaveable { mutableStateOf(0f) }
    var selectedTypeId by rememberSaveable { mutableStateOf(1) }
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
    var intervalHours by rememberSaveable { mutableStateOf(1) } // Default to 1 hour
    var intervalMinutes by rememberSaveable { mutableStateOf(0) }
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
                            text = if (medicationName.isBlank() || currentStep == 0) stringResource(id = com.d4viddf.medicationreminder.R.string.new_medication_toolbar_title) else medicationName.substringBefore(" "),
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
                            Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = stringResource(id = com.d4viddf.medicationreminder.R.string.back))
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = com.d4viddf.medicationreminder.R.string.close))
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

                            val medicationId = medicationViewModel.insertMedication(
                                Medication(
                                    name = medicationName, typeId = selectedTypeId, color = selectedColor.toString(),
                                    dosage = dosage.ifEmpty { null }, packageSize = packageSize.toIntOrNull() ?: 0,
                                    remainingDoses = packageSize.toIntOrNull() ?: 0,
                                    startDate = if (startDate.isNotBlank() && startDate != selectStartDatePlaceholder) startDate else null,
                                    endDate = if (endDate.isNotBlank() && endDate != selectEndDatePlaceholder) endDate else null,
                                    reminderTime = null, // This seems to be consistently null here
                                    registrationDate = currentRegistrationDate // Set the new field
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
                                    daysOfWeek = if (scheduleType == ScheduleType.DAILY) selectedDays.joinToString(",") else null,
                                    specificTimes = when (scheduleType) {
                                        ScheduleType.DAILY -> onceADayTime?.format(timeFormatter)?.let { listOf(it) }?.joinToString(",")
                                        ScheduleType.CUSTOM_ALARMS -> selectedTimes.map { it.format(timeFormatter) }.joinToString(",")
                                        else -> null
                                    },
                                    // Assuming you add these to MedicationSchedule data class and DB
                                    intervalStartTime = if (scheduleType == ScheduleType.INTERVAL) intervalStartTime?.format(timeFormatter) else null,
                                    intervalEndTime = if (scheduleType == ScheduleType.INTERVAL) intervalEndTime?.format(timeFormatter) else null
                                )
                                medicationScheduleViewModel.insertSchedule(schedule)

                                // Get ApplicationContext from LocalContext for WorkManager
                                val appContext = localContext.applicationContext
                                val workManager = WorkManager.getInstance(appContext) // Get context carefully
                                val data = Data.Builder()
                                    .putInt(ReminderSchedulingWorker.KEY_MEDICATION_ID, medId) // medId is the new medication's ID
                                    .build()

                                val scheduleRemindersWorkRequest = OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
                                    .setInputData(data)
                                    .addTag("${ReminderSchedulingWorker.WORK_NAME_PREFIX}${medId}") // Tag for observation/cancellation
                                    .build()

                                workManager.enqueueUniqueWork(
                                    "${ReminderSchedulingWorker.WORK_NAME_PREFIX}${medId}",
                                    ExistingWorkPolicy.REPLACE, // Or APPEND, depending on desired behavior if already scheduled
                                    scheduleRemindersWorkRequest
                                )
                                Log.d("AddMedScreen", "Enqueued ReminderSchedulingWorker for medId: $medId")

                                medicationSearchResult?.let { result ->
                                    medicationInfoViewModel.insertMedicationInfo(
                                        MedicationInfo(
                                            medicationId = medId, description = result.description, atcCode = result.atcCode,
                                            safetyNotes = result.safetyNotes, administrationRoutes = result.administrationRoutes.joinToString(","),
                                            dosage = result.dosage, documentUrls = result.documentUrls.joinToString(","),
                                            nregistro = result.nregistro, labtitular = result.labtitular,
                                            comercializado = result.comercializado, requiereReceta = result.requiereReceta,
                                            generico = result.generico
                                        )
                                    )
                                }
                            }
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .height(60.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = medicationName.isNotBlank() && (currentStep != 2 || (packageSize.isNotBlank() && packageSize.toIntOrNull() != null)) // Add more validation
            ) {
                Text(if (currentStep == 4) stringResource(id = com.d4viddf.medicationreminder.R.string.confirm) else stringResource(id = com.d4viddf.medicationreminder.R.string.next))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(WindowInsets.safeDrawing.asPaddingValues())
        ) {
            when (currentStep) {
                0 -> {
                    MedicationNameInput(
                        medicationName = medicationName,
                        onMedicationNameChange = { medicationName = it },
                        onMedicationSelected = { result ->
                            medicationSearchResult = result
                            if (result != null) {
                                medicationName = result.name
                                dosage = result.dosage ?: ""
                            }
                        }
                    )
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 500.dp)) {
                        MedicationTypeSelector(
                            selectedTypeId = selectedTypeId,
                            onTypeSelected = { selectedTypeId = it },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            selectedColor = selectedColor
                        )
                        ColorSelector(
                            selectedColor = selectedColor,
                            onColorSelected = { selectedColor = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
                2 -> {
                    MedicationDosagePackageDateInput(
                        selectedTypeId = selectedTypeId,
                        dosage = dosage, onDosageChange = { dosage = it },
                        packageSize = packageSize, onPackageSizeChange = { packageSize = it },
                        medicationSearchResult = medicationSearchResult,
                        startDate = if (startDate.isBlank()) selectStartDatePlaceholder else startDate,
                        onStartDateSelected = { startDate = it },
                        endDate = if (endDate.isBlank()) selectEndDatePlaceholder else endDate,
                        onEndDateSelected = { endDate = it }
                    )
                }
                3 -> {
                    FrequencySelector(
                        selectedFrequency = frequency,
                        onFrequencySelected = { newFrequency ->
                            frequency = newFrequency
                            // Reset states when frequency changes to avoid carrying over inappropriate data
                            if (newFrequency != FrequencyType.ONCE_A_DAY) {
                                onceADayTime = null
                                selectedDays = emptyList()
                            }
                            if (newFrequency != FrequencyType.MULTIPLE_TIMES_A_DAY) {
                                selectedTimes = emptyList()
                            }
                            if (newFrequency != FrequencyType.INTERVAL) {
                                intervalHours = 1 // Reset to default
                                intervalMinutes = 0 // Reset to default
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
                        onIntervalEndTimeSelected = { intervalEndTime = it }
                    )
                }
                4 -> {
                    val summaryStartDate = if (startDate.isBlank()) selectStartDatePlaceholder else startDate
                    val summaryEndDate = if (endDate.isBlank()) selectEndDatePlaceholder else endDate
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
                    ColorSelector(selectedColor = selectedColor, onColorSelected = { selectedColor = it }) // ColorSelector doesn't depend on frequency string
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddMedicationScreenPreview() {
    AppTheme {
        AddMedicationScreen(onNavigateBack = {})
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
        Divider(Modifier.padding(vertical = 8.dp))
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