// En app/src/main/java/com/d4viddf/medicationreminder/viewmodel/MedicationReminderViewModel.kt
package com.d4viddf.medicationreminder.viewmodel

import android.content.Context // Necesario para @ApplicationContext
import android.util.Log        // Para Log.d y Log.i
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker // Importa tu Worker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext // Para inyectar el contexto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate // Necesario para filtrar los de hoy
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MedicationReminderViewModel @Inject constructor(
    private val reminderRepository: MedicationReminderRepository,
    @ApplicationContext private val appContext: Context // Inyecta el ApplicationContext
) : ViewModel() {

    private val _allRemindersForSelectedMedication = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val allRemindersForSelectedMedication: StateFlow<List<MedicationReminder>> = _allRemindersForSelectedMedication

    private val _todaysRemindersForSelectedMedication = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val todaysRemindersForSelectedMedication: StateFlow<List<MedicationReminder>> = _todaysRemindersForSelectedMedication

    // Formateador consistente para parsear las fechas/horas de la BD
    // Si lo tienes en ReminderCalculator, puedes usar ese. Si no, defínelo aquí o en un companion object.
    private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME


    fun loadRemindersForMedication(medicationId: Int) {
        viewModelScope.launch(Dispatchers.IO) { // Las operaciones de BD en IO
            reminderRepository.getRemindersForMedication(medicationId).collect { allRemindersList ->
                val sortedAllReminders = allRemindersList.sortedBy { it.reminderTime }
                _allRemindersForSelectedMedication.value = sortedAllReminders

                val today = LocalDate.now()
                _todaysRemindersForSelectedMedication.value = sortedAllReminders.filter {
                    try {
                        LocalDateTime.parse(it.reminderTime, storableDateTimeFormatter).toLocalDate().isEqual(today)
                    } catch (e: Exception) {
                        Log.e("MedReminderVM", "Error parsing reminderTime: ${it.reminderTime}", e)
                        false
                    }
                } // Ya están ordenados por `sortedAllReminders`
                Log.d("MedReminderVM", "Loaded ${allRemindersList.size} total reminders, ${todaysRemindersForSelectedMedication.value.size} for today (medId: $medicationId).")
            }
        }
    }

    suspend fun getAllRemindersForMedicationOnce(medicationId: Int): List<MedicationReminder> {
        return withContext(Dispatchers.IO) {
            reminderRepository.getRemindersForMedication(medicationId).firstOrNull()?.sortedBy { it.reminderTime } ?: emptyList()
        }
    }

    fun markReminderAsTaken(reminderId: Int, takenAt: String) {

        viewModelScope.launch {

            reminderRepository.markReminderAsTaken(reminderId, takenAt)

        }

    }

    fun markReminderAsTakenAndUpdateLists(reminderId: Int, medicationId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val nowString = LocalDateTime.now().format(storableDateTimeFormatter)
            Log.d("MedReminderVM", "Marking reminderId $reminderId (medId $medicationId) as taken at $nowString")
            reminderRepository.markReminderAsTaken(reminderId, nowString)
            triggerNextReminderScheduling(medicationId) // Llama sin pasar el contexto
        }
    }

    internal fun triggerNextReminderScheduling(medicationId: Int) {
        Log.d("MedReminderVM", "Triggering next reminder scheduling for med ID: $medicationId using injected appContext")
        val workManager = WorkManager.getInstance(this.appContext) // Usa this.appContext
        val data = Data.Builder()
            .putInt(ReminderSchedulingWorker.KEY_MEDICATION_ID, medicationId)
            .putBoolean(ReminderSchedulingWorker.KEY_IS_DAILY_REFRESH, false)
            .build()
        val scheduleNextWorkRequest =
            OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
                .setInputData(data)
                .addTag("${ReminderSchedulingWorker.WORK_NAME_PREFIX}NextFromDetail_${medicationId}")
                .build()
        workManager.enqueueUniqueWork(
            "${ReminderSchedulingWorker.WORK_NAME_PREFIX}NextScheduledFromDetail_${medicationId}",
            ExistingWorkPolicy.REPLACE,
            scheduleNextWorkRequest
        )
        Log.i("MedReminderVM", "Enqueued ReminderSchedulingWorker for med ID $medicationId.")
    }
}