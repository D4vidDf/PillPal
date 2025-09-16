package com.d4viddf.medicationreminder.data.repository

import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.WaterPreset
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.source.local.HealthDataDao
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDataRepository @Inject constructor(
    private val healthDataDao: HealthDataDao
) {

    // --- READ Functions ---
    fun getLatestBodyTemperature(): Flow<BodyTemperature?> = healthDataDao.getLatestBodyTemperature()
    fun getLatestWeight(): Flow<Weight?> = healthDataDao.getLatestWeight()
    fun getTotalWaterIntakeSince(startTime: Long): Flow<Double?> = healthDataDao.getTotalWaterIntakeSince(startTime)
    fun getBodyTemperatureBetween(startTime: Instant, endTime: Instant): Flow<List<BodyTemperature>> =
        healthDataDao.getBodyTemperatureBetween(startTime, endTime)

    fun getWeightBetween(startTime: Instant, endTime: Instant): Flow<List<Weight>> =
        healthDataDao.getWeightBetween(startTime, endTime)

    fun getWaterIntakeBetween(startTime: Instant, endTime: Instant): Flow<List<WaterIntake>> =
        healthDataDao.getWaterIntakeBetween(startTime, endTime)

    // --- WRITE Functions ---
    suspend fun insertBodyTemperature(record: BodyTemperature) {
        healthDataDao.insertBodyTemperature(record)
    }

    suspend fun insertWeight(record: Weight) {
        healthDataDao.insertWeight(record)
    }

    suspend fun insertWaterIntake(record: WaterIntake) {
        healthDataDao.insertWaterIntake(record)
    }

    fun getWaterPresets(): Flow<List<WaterPreset>> = healthDataDao.getWaterPresets()

    suspend fun insertWaterPreset(preset: WaterPreset) {
        healthDataDao.insertWaterPreset(preset)
    }

    suspend fun deleteWaterPreset(id: Int) {
        healthDataDao.deleteWaterPreset(id)
    }

    suspend fun deleteAllWaterIntake() {
        healthDataDao.deleteAllWaterIntake()
    }

    suspend fun deleteAllWeight() {
        healthDataDao.deleteAllWeight()
    }
}