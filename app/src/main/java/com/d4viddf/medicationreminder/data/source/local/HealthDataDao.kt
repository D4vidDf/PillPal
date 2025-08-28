package com.d4viddf.medicationreminder.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.WaterPreset
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface HealthDataDao {

    // --- Body Temperature ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyTemperature(record: BodyTemperature)

    @Query("SELECT * FROM body_temperature_records ORDER BY time DESC LIMIT 1")
    fun getLatestBodyTemperature(): Flow<BodyTemperature?>

    @Query("SELECT * FROM body_temperature_records WHERE time BETWEEN :startTime AND :endTime ORDER BY time DESC")
    fun getBodyTemperatureBetween(startTime: Instant, endTime: Instant): Flow<List<BodyTemperature>>

    // --- Weight ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(record: Weight)

    @Query("SELECT * FROM weight_records ORDER BY time DESC LIMIT 1")
    fun getLatestWeight(): Flow<Weight?>

    @Query("SELECT * FROM weight_records WHERE time BETWEEN :startTime AND :endTime ORDER BY time DESC")
    fun getWeightBetween(startTime: Instant, endTime: Instant): Flow<List<Weight>>

    // --- Water Intake ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterIntake(record: WaterIntake)

    // Note: Water intake is often summed over a period, so we'll add a query for that.
    @Query("SELECT SUM(volumeMilliliters) FROM water_intake_records WHERE time >= :startTime")
    fun getTotalWaterIntakeSince(startTime: Long): Flow<Double?>

    @Query("SELECT * FROM water_intake_records ORDER BY time DESC LIMIT 1")
    fun getLatestWaterIntake(): Flow<WaterIntake?>

    @Query("SELECT * FROM water_intake_records WHERE time BETWEEN :startTime AND :endTime ORDER BY time DESC")
    fun getWaterIntakeBetween(startTime: Instant, endTime: Instant): Flow<List<WaterIntake>>

    @Query("DELETE FROM water_intake_records")
    suspend fun deleteAllWaterIntake()

    // --- Water Presets ---

    @Query("SELECT * FROM water_presets")
    fun getWaterPresets(): Flow<List<WaterPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterPreset(preset: WaterPreset)

    @Query("DELETE FROM water_presets WHERE id = :id")
    suspend fun deleteWaterPreset(id: Int)
}