package com.d4viddf.medicationreminder.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.d4viddf.medicationreminder.data.model.MedicationDosage

@Dao
interface MedicationDosageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dosage: MedicationDosage): Long

    @Query("SELECT * FROM medication_dosages WHERE medicationId = :medicationId AND endDate IS NULL")
    suspend fun getActiveDosage(medicationId: Int): MedicationDosage?

    @Query("UPDATE medication_dosages SET endDate = :endDate WHERE id = :dosageId")
    suspend fun deactivateDosage(dosageId: Int, endDate: String)

    @Query("SELECT * FROM medication_dosages WHERE medicationId = :medicationId AND startDate <= :date AND (endDate IS NULL OR endDate >= :date) ORDER BY startDate DESC LIMIT 1")
    suspend fun getDosageForDate(medicationId: Int, date: String): MedicationDosage?
}