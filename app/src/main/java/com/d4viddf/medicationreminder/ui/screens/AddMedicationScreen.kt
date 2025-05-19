package com.d4viddf.medicationreminder.ui.screens

import MedicationSearchResult
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationInfo
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.ColorSelector
import com.d4viddf.medicationreminder.ui.components.FrequencySelector
import com.d4viddf.medicationreminder.ui.components.MedicationDosagePackageDateInput
import com.d4viddf.medicationreminder.ui.components.MedicationNameInput
import com.d4viddf.medicationreminder.ui.components.MedicationTypeSelector
import com.d4viddf.medicationreminder.viewmodel.MedicationInfoViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    var startDate by rememberSaveable { mutableStateOf("Select Start Date") }
    var endDate by rememberSaveable { mutableStateOf("Select End Date") }

    // Frequency related states
    var frequency by rememberSaveable { mutableStateOf("Once a day") }
    var selectedDays by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) } // Default to empty

    // "Once a day" specific state
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
            var titleWidth by remember { mutableStateOf(0) }
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (medicationName.isBlank() || currentStep == 0) "New medication" else medicationName.substringBefore(" "),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.onGloballyPositioned { titleWidth = it.size.width }
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
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Back")
                            }
                        } else {
                            Spacer(Modifier.width(48.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        },
        bottomBar = {
            Button(
                onClick = {
                    if (currentStep < 4 && medicationName.isNotBlank()) { // TODO: Add validation for each step
                        currentStep++
                        progress = (currentStep + 1) / 5f
                    } else if (currentStep == 4) {
                        coroutineScope.launch {
                            val medicationId = medicationViewModel.insertMedication(
                                Medication(
                                    name = medicationName, typeId = selectedTypeId, color = selectedColor.toString(),
                                    dosage = dosage.ifEmpty { null }, packageSize = packageSize.toIntOrNull() ?: 0,
                                    remainingDoses = packageSize.toIntOrNull() ?: 0,
                                    startDate = if (startDate != "Select Start Date") startDate else null,
                                    endDate = if (endDate != "Select End Date") endDate else null,
                                    reminderTime = null // This might be derived or deprecated
                                )
                            )
                            medicationId.let { medId ->
                                val scheduleType = when (frequency) {
                                    "Once a day" -> ScheduleType.DAILY // Or a new type if needed for single specific time
                                    "Multiple times a day" -> ScheduleType.CUSTOM_ALARMS
                                    "Interval" -> ScheduleType.INTERVAL
                                    // "Weekly" was removed from options, handle if re-added
                                    else -> ScheduleType.DAILY // Default or handle error
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
                Text(if (currentStep == 4) "Confirm" else "Next")
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
                        startDate = startDate, onStartDateSelected = { startDate = it },
                        endDate = endDate, onEndDateSelected = { endDate = it }
                    )
                }
                3 -> {
                    FrequencySelector(
                        selectedFrequency = frequency,
                        onFrequencySelected = { newFrequency ->
                            frequency = newFrequency
                            // Reset states when frequency changes to avoid carrying over inappropriate data
                            if (newFrequency != "Once a day") {
                                onceADayTime = null
                                selectedDays = emptyList() // Typically days are for "Once a day" or "Weekly"
                            }
                            if (newFrequency != "Multiple times a day") {
                                selectedTimes = emptyList()
                            }
                            if (newFrequency != "Interval") {
                                intervalHours = 1 // Reset to default
                                intervalMinutes = 0 // Reset to default
                                intervalStartTime = null
                                intervalEndTime = null
                            }
                        },
                        // "Once a day" props
                        selectedDays = selectedDays,
                        onDaysSelected = { selectedDays = it },
                        onceADayTime = onceADayTime,
                        onOnceADayTimeSelected = { onceADayTime = it },
                        // "Multiple times a day" props
                        selectedTimes = selectedTimes,
                        onTimesSelected = { selectedTimes = it },
                        // "Interval" props
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
                    MedicationSummary(
                        typeId = selectedTypeId, medicationName = medicationName, color = selectedColor.backgroundColor,
                        dosage = dosage, packageSize = packageSize, frequency = frequency,
                        startDate = startDate, endDate = endDate,
                        // Pass new schedule details for summary
                        onceADayTime = onceADayTime,
                        selectedTimes = selectedTimes,
                        intervalHours = intervalHours,
                        intervalMinutes = intervalMinutes,
                        intervalStartTime = intervalStartTime,
                        intervalEndTime = intervalEndTime,
                        selectedDays = selectedDays
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ColorSelector(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MedicationSummary(
    typeId: Int, medicationName: String, color: Color, dosage: String, packageSize: String,
    frequency: String, startDate: String, endDate: String,
    // Added for more detailed summary based on frequency
    onceADayTime: LocalTime?,
    selectedTimes: List<LocalTime>,
    intervalHours: Int,
    intervalMinutes: Int,
    intervalStartTime: LocalTime?,
    intervalEndTime: LocalTime?,
    selectedDays: List<Int>
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val daysOfWeekMap = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Medication Summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        InfoRow("Name:", medicationName)
        // InfoRow("Type ID:", typeId.toString()) // Optional to show type ID
        InfoRow("Dosage:", dosage.ifEmpty { "Not set" })
        InfoRow("Package Size:", packageSize.ifEmpty { "Not set" })
        InfoRow("Start Date:", if (startDate == "Select Start Date") "Not set" else startDate)
        InfoRow("End Date:", if (endDate == "Select End Date") "Not set" else endDate)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text("Color:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(24.dp).background(color, CircleShape))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Reminder Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        InfoRow("Frequency:", frequency)

        when (frequency) {
            "Once a day" -> {
                InfoRow("Time:", onceADayTime?.format(timeFormatter) ?: "Not set")
                if (selectedDays.isNotEmpty()) {
                    InfoRow("Days:", selectedDays.sorted().mapNotNull { daysOfWeekMap.getOrNull(it - 1) }.joinToString(", "))
                }
            }
            "Multiple times a day" -> {
                if (selectedTimes.isNotEmpty()) {
                    selectedTimes.sorted().forEachIndexed { index, time ->
                        InfoRow("Alarm ${index + 1}:", time.format(timeFormatter))
                    }
                } else {
                    InfoRow("Alarms:", "None set")
                }
            }
            "Interval" -> {
                InfoRow("Interval:", "Every $intervalHours hrs $intervalMinutes mins")
                InfoRow("Daily Start:", intervalStartTime?.format(timeFormatter) ?: "Not set")
                InfoRow("Daily End:", intervalEndTime?.format(timeFormatter) ?: "Not set")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) { // Reduced vertical padding for more info
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}