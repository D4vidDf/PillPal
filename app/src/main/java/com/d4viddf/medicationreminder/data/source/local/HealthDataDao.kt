package com.d4viddf.medicationreminder.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDataDao {

    // --- Body Temperature ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyTemperature(record: BodyTemperature)

    @Query("SELECT * FROM body_temperature_records ORDER BY time DESC LIMIT 1")
    fun getLatestBodyTemperature(): Flow<BodyTemperature?>

    // --- Weight ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(record: Weight)

    @Query("SELECT * FROM weight_records ORDER BY time DESC LIMIT 1")
    fun getLatestWeight(): Flow<Weight?>

    // --- Water Intake ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterIntake(record: WaterIntake)

    // Note: Water intake is often summed over a period, so we'll add a query for that.
    @Query("SELECT SUM(volumeMilliliters) FROM water_intake_records WHERE time >= :startTime")
    fun getTotalWaterIntakeSince(startTime: Long): Flow<Double?>

    @Query("SELECT * FROM water_intake_records ORDER BY time DESC LIMIT 1")
    fun getLatestWaterIntake(): Flow<WaterIntake?>
}