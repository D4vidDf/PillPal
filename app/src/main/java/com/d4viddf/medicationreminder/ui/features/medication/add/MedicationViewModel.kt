package com.d4viddf.medicationreminder.ui.features.medication.add

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationDosage
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import com.d4viddf.medicationreminder.data.repository.MedicationDosageRepository
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.ui.features.medication.details.components.ProgressDetails
import com.d4viddf.medicationreminder.workers.WorkerScheduler
import android.content.Context
import com.d4viddf.medicationreminder.data.repository.MedicationReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import java.time.format.DateTimeParseException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.d4viddf.medicationreminder.R

sealed class CounterInfo {
    data class Dose(val value: String, val unit: String) : CounterInfo()
    data class RemainingDoses(val value: String, val labelResId: Int) : CounterInfo()
    data class Frequency(val value: String, val unit: String) : CounterInfo()
    data class Weekly(val days: List<Boolean>, val labelResId: Int) : CounterInfo()
    data class Duration(val value: String, val unit: String) : CounterInfo()
    data class Status(val iconResId: Int, val labelResId: Int) : CounterInfo()
}

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val reminderRepository: MedicationReminderRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val dosageRepository: MedicationDosageRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _medications = MutableStateFlow<List<Medication>>(emptyList()) // This will hold the unfiltered list directly
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _medicationProgressDetails = MutableStateFlow<ProgressDetails?>(null)
    val medicationProgressDetails: StateFlow<ProgressDetails?> = _medicationProgressDetails.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Medication>>(emptyList())
    val searchResults: StateFlow<List<Medication>> = _searchResults.asStateFlow()

    private val _activeDosage = MutableStateFlow<MedicationDosage?>(null)
    val activeDosage: StateFlow<MedicationDosage?> = _activeDosage.asStateFlow()

    private val _counterInfo = MutableStateFlow<List<CounterInfo>>(emptyList())
    val counterInfo: StateFlow<List<CounterInfo>> = _counterInfo.asStateFlow()

    init {
        observeMedications()
        observeSearchQueryAndMedications()
    }

    private fun observeMedications() {
        viewModelScope.launch {
            medicationRepository.getAllMedications().collect { medications ->
                _medications.value = medications // Directly update _medications
            }
        }
    }

    private fun observeSearchQueryAndMedications() {
        viewModelScope.launch {
            combine(_searchQuery, _medications) { query, medications -> // _medications is now the unfiltered list
                if (query.isBlank()) {
                    emptyList() // Return empty list if query is blank
                } else {
                    medications.filter { medication ->
                        medication.name.contains(query, ignoreCase = true)
                    }
                }
            }.collect { filteredMedications ->
                _searchResults.value = filteredMedications
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshMedications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Perform the actual data refresh logic here.
                // This might involve a specific repository call to fetch fresh data,
                // or if your `getAllMedications()` flow re-fetches on new collection,
                // you could potentially take the first emission.
                // For simplicity, if your repository automatically updates subscribers
                // when data changes, this refresh might just be about ensuring
                // any cached/stale data source is invalidated or re-queried.
                // If repository.getAllMedications() is a cold flow that fetches on collection:
                medicationRepository.getAllMedications().firstOrNull() // Ensure it re-fetches, result updates _medications via observeMedications
                // Add a small delay if needed to simulate work if the operation is too fast, for testing UI
                // kotlinx.coroutines.delay(1000)
            } catch (e: Exception) {
                // Handle error, log it (e.g., Log.e("MedicationViewModel", "Error refreshing medications", e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun observeMedicationAndRemindersForDailyProgress(medicationId: Int) {
        viewModelScope.launch { // Mover a IO para operaciones de BD y cálculo
            // Observar cambios en los reminders y recalcular
            // También necesitamos observar el medicationState y scheduleState por si cambian (ej. edición)
            // Una forma más robusta podría ser combinar Flows, pero por ahora recolectaremos reminders
            // y obtendremos medication/schedule cada vez.
            reminderRepository.getRemindersForMedication(medicationId).collect { remindersList ->
                val currentMedication = medicationRepository.getMedicationById(medicationId)
                val currentSchedule = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()?.firstOrNull()
                val currentDosage = dosageRepository.getActiveDosage(medicationId)
                calculateAndSetDailyProgressDetails(currentMedication, currentSchedule, remindersList)
                updateCounterInfo(currentMedication, currentSchedule, currentDosage)
            }
        }
    }

    suspend fun getMedicationById(medicationId: Int): Medication? {
        return withContext(Dispatchers.IO) {
            medicationRepository.getMedicationById(medicationId)
        }
    }

    fun loadActiveDosage(medicationId: Int) {
        viewModelScope.launch {
            _activeDosage.value = dosageRepository.getActiveDosage(medicationId)
        }
    }

    private suspend fun calculateAndSetDailyProgressDetails(
        medication: Medication?,
        schedule: MedicationSchedule?,
        allRemindersForMedication: List<MedicationReminder> // Pasar la lista actual de reminders
    ) {
        if (medication == null || schedule == null) {
            _medicationProgressDetails.value = ProgressDetails(0,0,0,0f, "N/A")
            return
        }

        val today = LocalDate.now()

        // 1. Calcular dosis totales programadas para HOY
        // Replace calculateReminderDateTimes with generateRemindersForPeriod
        val remindersMapForToday = ReminderCalculator.generateRemindersForPeriod(
            medication = medication,
            schedule = schedule,
            periodStartDate = today,
            periodEndDate = today
        )
        val scheduledTimesTodayList = remindersMapForToday[today] ?: emptyList()
        val totalDosesScheduledToday = scheduledTimesTodayList.size
        Log.d("ProgressCalc", "Med: ${medication.name}, Schedule: ${schedule.scheduleType}, Scheduled for $today: $totalDosesScheduledToday doses (using generateRemindersForPeriod). Times: $scheduledTimesTodayList")


        // 2. Calcular dosis tomadas HOY
        val dosesTakenToday = allRemindersForMedication.count { reminder ->
            try {
                val reminderDateTime = LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                reminderDateTime.toLocalDate().isEqual(today) && reminder.isTaken
            } catch (e: DateTimeParseException) {
                Log.e("ProgressCalc", "Error parsing reminderTime: ${reminder.reminderTime}", e)
                false
            }
        }
        Log.d("ProgressCalc", "Doses taken today for ${medication.name}: $dosesTakenToday")


        // 3. Calcular la fracción de progreso para la barra (Tomadas Hoy / Programadas Hoy)
        val progressFraction = if (totalDosesScheduledToday > 0) {
            dosesTakenToday.toFloat() / totalDosesScheduledToday.toFloat()
        } else {
            0f // Si no hay dosis programadas para hoy, el progreso es 0 o 100% si tampoco hay tomadas?
            // Mejor 0 si no hay programadas. Si se tomaron sin estar programadas, es otro caso.
        }

        // 4. Determinar el texto a mostrar
        val displayText = "$dosesTakenToday / $totalDosesScheduledToday"

        _medicationProgressDetails.value = ProgressDetails(
            taken = dosesTakenToday, // Tomadas hoy
            remaining = (totalDosesScheduledToday - dosesTakenToday).coerceAtLeast(0), // Restantes para hoy
            totalFromPackage = totalDosesScheduledToday, // "Total" en este contexto son las programadas hoy
            progressFraction = progressFraction.coerceIn(0f, 1f),
            displayText = displayText
        )
    }

    // Mantener la función anterior si la necesitas para un progreso general basado en paquete o período completo
    // o refactorizarla completamente si el progreso diario es el único que se mostrará.
    // Por ahora, la comento para evitar confusión, ya que la nueva es calculateAndSetDailyProgressDetails
    /*
    suspend fun calculateAndSetOverallProgressDetails(medication: Medication?, schedule: MedicationSchedule?) {
        // ... lógica anterior que calculaba el progreso general del tratamiento o paquete ...
    }
    */

    suspend fun insertMedicationAndDosage(medication: Medication, schedule: MedicationSchedule, dosage: String): Pair<Int, Long?> {
        return withContext(Dispatchers.IO) {
            val medId = medicationRepository.insertMedication(medication)
            var dosageId: Long? = null
            if (dosage.isNotBlank()) {
                val dosageStartDate = schedule.startDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                dosageId = dosageRepository.insert(
                    MedicationDosage(
                        medicationId = medId.toInt(),
                        dosage = dosage,
                        startDate = dosageStartDate
                    )
                )
            }
            medId.toInt() to dosageId
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.updateMedication(medication)
            WorkerScheduler.scheduleRemindersForMedication(appContext, medication.id)
            Log.i("MedicationViewModel", "Scheduled medication-specific reminder scheduling for medId ${medication.id} after updating medication.")
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.deleteMedication(medication)
            WorkerScheduler.scheduleRemindersForMedication(appContext, medication.id)
            Log.i("MedicationViewModel", "Scheduled medication-specific reminder scheduling for medId ${medication.id} after deleting medication.")
        }
    }

    fun updateCounterInfo(medication: Medication?, schedule: MedicationSchedule?, dosage: MedicationDosage?) {
        val counters = mutableListOf<CounterInfo>()
        if (medication == null || schedule == null || dosage == null) {
            _counterInfo.value = emptyList()
            return
        }

        // Slot 1: Dose
        val medicationFormName = appContext.resources.getQuantityString(
            medication.medicationForm.getPluralResId(),
            if (dosage.dosage.toFloatOrNull() == 1f) 1 else 2,
            dosage.dosage
        )
        counters.add(CounterInfo.Dose(dosage.dosage, medicationFormName))

        // Slot 2: Remaining Doses
        if (medication.packageSize > 0) {
            val doseValue = dosage.dosage.toFloatOrNull() ?: 1f
            if (doseValue > 0) {
                val remainingDoses = (medication.stock / doseValue).toInt()
                counters.add(CounterInfo.RemainingDoses(remainingDoses.toString(), R.string.remaining_doses))
            }
        }

        // Slot 3: Schedule, Frequency, or Status
        var slot3Info: CounterInfo? = null
        if (medication.isSuspended) {
            slot3Info = CounterInfo.Status(R.drawable.ic_suspended, R.string.suspended)
        } else {
            when (schedule.scheduleType) {
                "DAILY" -> {
                    val timesPerDay = schedule.timesPerDay ?: 0
                    slot3Info = CounterInfo.Frequency(
                        timesPerDay.toString(),
                        appContext.resources.getQuantityString(R.plurals.times_a_day, timesPerDay, timesPerDay)
                    )
                }
                "EVERY_X_HOURS" -> {
                    val hours = schedule.hoursBetweenDoses ?: 0
                    slot3Info = CounterInfo.Frequency(
                        hours.toString(),
                        appContext.resources.getQuantityString(R.plurals.every_x_hours, hours, hours)
                    )
                }
                "WEEKLY" -> {
                    val days = listOf(
                        schedule.monday, schedule.tuesday, schedule.wednesday,
                        schedule.thursday, schedule.friday, schedule.saturday, schedule.sunday
                    )
                    slot3Info = CounterInfo.Weekly(days, R.string.days)
                }
                "AS_NEEDED" -> slot3Info = CounterInfo.Status(R.drawable.ic_medication, R.string.as_needed)
            }
        }

        // Fallback for Slot 3 if not yet filled
        if (slot3Info == null) {
            slot3Info = if (medication.endDate == null) {
                CounterInfo.Status(R.drawable.ic_infinity, R.string.ongoing)
            } else {
                try {
                    val endDate = LocalDate.parse(medication.endDate, DateTimeFormatter.ISO_LOCAL_DATE)
                    val remainingDays = LocalDate.now().until(endDate).days
                    CounterInfo.Duration(remainingDays.toString(), "days left")
                } catch (e: Exception) {
                    null // or a default/error state
                }
            }
        }

        if (slot3Info != null) {
            counters.add(slot3Info)
        }

        _counterInfo.value = counters.take(3)
    }
}