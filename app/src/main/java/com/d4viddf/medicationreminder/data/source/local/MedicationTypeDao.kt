package com.d4viddf.medicationreminder.data.source.local

import androidx.room.*
import com.d4viddf.medicationreminder.data.model.MedicationType
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationType(medicationType: MedicationType)

    @Query("SELECT * FROM medication_types")
    fun getAllMedicationTypes(): Flow<List<MedicationType>>

    @Query("SELECT * FROM medication_types WHERE id = :typeId")
    suspend fun getMedicationTypeById(typeId: Int): MedicationType?

    @Update
    suspend fun updateMedicationType(medicationType: MedicationType)

    @Delete
    suspend fun deleteMedicationType(medicationType: MedicationType)

    @Query("DELETE FROM medication_types")
    suspend fun deleteAllMedicationTypes()

    @Query("SELECT * FROM medication_types WHERE name = :name")
    suspend fun getMedicationTypeByName(name: String): MedicationType?

    @Query("SELECT * FROM medication_types WHERE id IN (:ids)")
    suspend fun getMedicationTypesByIds(ids: List<Int>): List<MedicationType>
}
