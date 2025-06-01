package com.d4viddf.medicationreminder.repository

import com.d4viddf.medicationreminder.data.FirebaseSync
import com.d4viddf.medicationreminder.data.FirebaseSyncDao
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.MedicationScheduleDao
import com.d4viddf.medicationreminder.data.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationScheduleRepository @Inject constructor(
    private val medicationScheduleDao: MedicationScheduleDao,
    private val firebaseSyncDao: FirebaseSyncDao
) {

    fun getSchedulesForMedication(medicationId: Int): Flow<List<MedicationSchedule>> =
        medicationScheduleDao.getSchedulesForMedication(medicationId)

    // Add this function to MedicationScheduleRepository
    fun getAllSchedules(): Flow<List<MedicationSchedule>> {
        return medicationScheduleDao.getAllSchedules()
    }

    suspend fun insertSchedule(schedule: MedicationSchedule) {
        medicationScheduleDao.insertSchedule(schedule)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "MedicationSchedule", entityId = schedule.id, syncStatus = SyncStatus.PENDING)
        )
    }

    suspend fun updateSchedule(schedule: MedicationSchedule) {
        medicationScheduleDao.updateSchedule(schedule)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "MedicationSchedule", entityId = schedule.id, syncStatus = SyncStatus.PENDING)
        )
    }

    suspend fun deleteSchedule(schedule: MedicationSchedule) {
        medicationScheduleDao.deleteSchedule(schedule)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(
                entityName = "MedicationSchedule",
                entityId = schedule.id,
                syncStatus = SyncStatus.PENDING
            )
        )
    }
}
