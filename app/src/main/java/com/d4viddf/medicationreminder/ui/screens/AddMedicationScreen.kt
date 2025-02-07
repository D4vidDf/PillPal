package com.d4viddf.medicationreminder.ui.screens

import MedicationSearchResult
import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationInfo
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.*
import com.d4viddf.medicationreminder.viewmodel.MedicationInfoViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    onNavigateBack: () -> Unit,
    medicationViewModel: MedicationViewModel = hiltViewModel(),
    medicationScheduleViewModel: MedicationScheduleViewModel = hiltViewModel(),
    medicationInfoViewModel: MedicationInfoViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var selectedTypeId by remember { mutableStateOf(1) }
    var selectedColor by remember { mutableStateOf(MedicationColor.LIGHT_ORANGE) }
    var startDate by remember { mutableStateOf("Select Start Date") }
    var endDate by remember { mutableStateOf("Select End Date") }
    var frequency by remember { mutableStateOf("Once a day") }
    var medicationName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var packageSize by remember { mutableStateOf("") }
    var selectedTimes by remember { mutableStateOf(listOf<LocalTime>()) }
    var intervalHours by remember { mutableStateOf(0) }
    var intervalMinutes by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf(listOf<Int>()) }

    // Medication search result to be saved
    var medicationSearchResult by remember { mutableStateOf<MedicationSearchResult?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            // Capture the width of the title
            var titleWidth by remember { mutableStateOf(0) }

            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally // Center the title horizontally
                        ) {
                            Text(
                                text = if (medicationName.isBlank() || currentStep==0) "New medication" else medicationName.substringBefore(" "),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .onGloballyPositioned {
                                        titleWidth = it.size.width
                                    }
                            )

                            // Progress bar
                            if (currentStep!=0) LinearProgressIndicator(
                                progress = {progress},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 10.dp, end = 10.dp) // Add some space between the title and the progress bar
                            )
                            else Spacer(modifier = Modifier.fillMaxWidth())
                        }
                    },
                    navigationIcon = {
                        if (currentStep > 0) {
                            IconButton(onClick = {
                                currentStep--
                                progress = (currentStep + 1) / 5f
                            },modifier = Modifier.width(64.dp)) {
                                Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "Back", )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateBack,modifier = Modifier.width(64.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        },
        bottomBar = {
            Button(
                onClick = {
                    if (currentStep < 4 && medicationName.isNotBlank()) { // Check if medication name is not blank
                        currentStep++
                        progress = (currentStep + 1) / 5f
                    } else if (currentStep == 4) {
                        coroutineScope.launch {
                            val medicationId = medicationViewModel.insertMedication(
                                Medication(
                                    name = medicationName,
                                    typeId = selectedTypeId,
                                    color = selectedColor.toString(),
                                    dosage = if (dosage.isNotEmpty()) dosage else null,
                                    packageSize = packageSize.toInt(),
                                    remainingDoses = packageSize.toInt(),
                                    startDate = if (startDate != "Select Start Date") startDate else null,
                                    endDate = if (endDate != "Select End Date") endDate else null,
                                    reminderTime = null
                                )
                            )

                            medicationId.let {
                                val scheduleType = when (frequency) {
                                    "Once a day" -> ScheduleType.DAILY
                                    "Weekly" -> ScheduleType.WEEKLY
                                    "As Needed" -> ScheduleType.AS_NEEDED
                                    "Interval" -> ScheduleType.INTERVAL
                                    "Multiple times a day" -> ScheduleType.CUSTOM_ALARMS
                                    else -> ScheduleType.DAILY
                                }

                                medicationScheduleViewModel.insertSchedule(
                                    MedicationSchedule(
                                        medicationId = it,
                                        scheduleType = scheduleType,
                                        intervalHours = if (frequency == "Interval") intervalHours else null,
                                        intervalMinutes = if (frequency == "Interval") intervalMinutes else null,
                                        daysOfWeek = if (frequency == "Once a day" || frequency == "Weekly") selectedDays.joinToString(",") else null,
                                        specificTimes = if (frequency == "Multiple times a day") selectedTimes.joinToString(",") { time ->
                                            time.toString()
                                        } else null
                                    )
                                )

                                medicationSearchResult?.let { result ->
                                    medicationInfoViewModel.insertMedicationInfo(
                                        MedicationInfo(
                                            medicationId = it,
                                            description = result.description,
                                            atcCode = result.atcCode,
                                            safetyNotes = result.safetyNotes,
                                            administrationRoutes = result.administrationRoutes.joinToString(","),
                                            dosage = result.dosage,
                                            documentUrls = result.documentUrls.joinToString(","),
                                            nregistro = result.nregistro,
                                            labtitular = result.labtitular,
                                            comercializado = result.comercializado,
                                            requiereReceta = result.requiereReceta,
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
                    .padding(16.dp).height(60.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = medicationName.isNotBlank() // Button enabled only if medication name is not blank
            ) {
                Text("Confirm")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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
                    Column(modifier = Modifier.weight(1f)) {
                        MedicationTypeSelector(
                            selectedTypeId = selectedTypeId,
                            onTypeSelected = { selectedTypeId = it },
                            modifier = Modifier.fillMaxSize(), // Make it fill the available space,
                                    selectedColor = selectedColor
                        )
                    }
                    // Color selection is now below the medication type grid
                    ColorSelector(
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }

                2 -> {
                    GenericTextFieldInput(
                        label = "Dosage",
                        value = dosage,
                        onValueChange = { dosage = it },
                        description = "Enter the dosage as indicated by your healthcare provider.",
                        isError = dosage.isBlank()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GenericTextFieldInput(
                        label = "Package Size",
                        value = packageSize,
                        onValueChange = { packageSize = it },
                        description = "Enter the number of doses available in the package.",
                        isError = packageSize.toIntOrNull() == null || packageSize.toInt() <= 0
                    )
                }

                3 -> {
                    FrequencySelector(
                        selectedFrequency = frequency,
                        onFrequencySelected = { frequency = it },
                        selectedDays = selectedDays,
                        onDaysSelected = { selectedDays = it },
                        selectedTimes = selectedTimes,
                        onTimesSelected = { selectedTimes = it },
                        onIntervalChanged = { hours, minutes ->
                            intervalHours = hours
                            intervalMinutes = minutes
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DatePickerButton(
                        label = "Start Date",
                        date = startDate,
                        onDateSelected = { startDate = it },
                        context = context
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DatePickerButton(
                        label = "End Date",
                        date = endDate,
                        onDateSelected = { endDate = it },
                        context = context
                    )
                }

                4 -> {
                    MedicationSummary(
                        typeId = selectedTypeId,
                        medicationName = medicationName,
                        color = MedicationColor.valueOf(selectedColor.toString()).backgroundColor,
                        dosage = dosage,
                        packageSize = packageSize,
                        frequency = frequency,
                        startDate = startDate,
                        endDate = endDate
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ColorSelector(
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }
            }
        }
    }
}

@Composable
fun DatePickerButton(
    label: String,
    date: String,
    onDateSelected: (String) -> Unit,
    context: Context
) {
    val calendar = Calendar.getInstance()

    Button(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDateSelected("$dayOfMonth/${month + 1}/$year")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text = "$label: $date")
    }
}

@Composable
fun MedicationSummary(
    typeId: Int,
    medicationName: String,
    color: Color,
    dosage: String,
    packageSize: String,
    frequency: String,
    startDate: String,
    endDate: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Medication Summary", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Medication Name: $medicationName")
        Text(text = "Medication Type ID: $typeId")
        Text(text = "Dosage: $dosage")
        Text(text = "Package Size: $packageSize")
        Text(text = "Frequency: $frequency")
        Text(text = "Start Date: $startDate")
        Text(text = "End Date: $endDate")
    }
}