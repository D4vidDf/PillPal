package com.d4viddf.medicationreminder.data.repository

import android.content.Context
import android.util.Log
import com.d4viddf.medicationreminder.data.model.FirebaseSync
import com.d4viddf.medicationreminder.data.source.local.FirebaseSyncDao
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.source.local.MedicationDao
import com.d4viddf.medicationreminder.data.source.local.MedicationReminderDao
import com.d4viddf.medicationreminder.data.model.SyncStatus
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Intent
import com.d4viddf.medicationreminder.utils.constants.IntentActionConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MedicationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationDao: MedicationDao,
    private val medicationReminderDao: MedicationReminderDao,
    private val notificationScheduler: NotificationScheduler,
    private val firebaseSyncDao: FirebaseSyncDao
) {

    fun getAllMedications(): Flow<List<Medication>> = medicationDao.getAllMedications()

    suspend fun insertMedication(medication: Medication): Int {
        val newId = medicationDao.insertMedication(medication).toInt()
        // Record sync request for Firebase
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(
                entityName = "Medication",
                entityId = newId,
                syncStatus = SyncStatus.PENDING
            )
        )
        sendDataChangedBroadcast()
        return newId
    }

    suspend fun updateMedication(medication: Medication) {
        medicationDao.updateMedication(medication)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "Medication", entityId = medication.id, syncStatus = SyncStatus.PENDING)
        )
        sendDataChangedBroadcast()
    }

    suspend fun deleteMedication(medication: Medication) {
        Log.d("MedicationRepository", "Deleting medication: ${medication.name} (ID: ${medication.id})")

        // 1. Obtener y cancelar todos los recordatorios futuros no tomados para esta medicación
        val currentTimeIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val futureReminders = medicationReminderDao.getFutureUntakenRemindersForMedication(medication.id, currentTimeIso)
            .firstOrNull()

        futureReminders?.forEach { reminder ->
            Log.d("MedicationRepository", "Cancelling alarm for reminder ID: ${reminder.id} due to medication deletion.")
            notificationScheduler.cancelAllAlarmsForReminder(context, reminder.id)
            // No es necesario borrar el reminder de la BD aquí, CASCADE lo hará
        }

        // 2. Borrar la medicación de la BD (CASCADE se encargará de horarios y recordatorios)
        medicationDao.deleteMedication(medication)
        Log.d("MedicationRepository", "Medication ${medication.name} deleted from DB.")

        // 3. Registrar para Firebase Sync (si aplica)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "Medication", entityId = medication.id, syncStatus = SyncStatus.PENDING)
        )
        sendDataChangedBroadcast()
    }

    suspend fun updateDoseCount(medicationId: Int, remainingDoses: Int) {
        val medication = medicationDao.getMedicationById(medicationId)
        if (medication != null) {
            val updatedMedication = medication.copy(remainingDoses = remainingDoses)
            updateMedication(updatedMedication)
        }
    }

    open suspend fun getMedicationById(medicationId: Int): Medication? {
        return medicationDao.getMedicationById(medicationId)
    }
    fun getMedicationByIdFlow(medicationId: Int): Flow<Medication?> {
        return medicationDao.getMedicationByIdFlow(medicationId)
    }

    private fun sendDataChangedBroadcast() {
        val intent = Intent(IntentActionConstants.ACTION_DATA_CHANGED)
        context.sendBroadcast(intent)
    }
}

