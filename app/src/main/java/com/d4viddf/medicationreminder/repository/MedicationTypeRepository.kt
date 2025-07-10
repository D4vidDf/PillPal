package com.d4viddf.medicationreminder.repository

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MedicationTypeRepository @Inject constructor(
    private val medicationTypeDao: MedicationTypeDao
) {

    fun getAllMedicationTypes(): Flow<List<MedicationType>> = medicationTypeDao.getAllMedicationTypes()

    suspend fun insertMedicationType(medicationType: MedicationType) {
        medicationTypeDao.insertMedicationType(medicationType)
    }
    suspend fun updateMedicationType(medicationType: MedicationType) {
        medicationTypeDao.updateMedicationType(medicationType)
    }
    suspend fun deleteMedicationType(medicationType: MedicationType) {
        medicationTypeDao.deleteMedicationType(medicationType)
    }
    open suspend fun getMedicationTypeById(id: Int): MedicationType? {
        return medicationTypeDao.getMedicationTypeById(id)
    }
}
