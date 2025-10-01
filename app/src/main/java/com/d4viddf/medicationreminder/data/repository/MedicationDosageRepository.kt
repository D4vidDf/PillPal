package com.d4viddf.medicationreminder.data.repository

import com.d4viddf.medicationreminder.data.model.MedicationDosage
import com.d4viddf.medicationreminder.data.source.local.MedicationDosageDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationDosageRepository @Inject constructor(
    private val medicationDosageDao: MedicationDosageDao
) {
    suspend fun insert(dosage: MedicationDosage): Long {
        return medicationDosageDao.insert(dosage)
    }

    suspend fun getActiveDosage(medicationId: Int): MedicationDosage? {
        return medicationDosageDao.getActiveDosage(medicationId)
    }

    suspend fun deactivateDosage(dosageId: Int, endDate: String) {
        medicationDosageDao.deactivateDosage(dosageId, endDate)
    }

    suspend fun getDosageForDate(medicationId: Int, date: String): com.d4viddf.medicationreminder.data.model.MedicationDosage? {
        return medicationDosageDao.getDosageForDate(medicationId, date)
    }
}