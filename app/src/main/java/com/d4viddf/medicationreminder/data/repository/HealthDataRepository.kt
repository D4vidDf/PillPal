package com.d4viddf.medicationreminder.data.repository

import com.d4viddf.medicationreminder.data.healthconnect.HealthConnectManager
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.HeartRate
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.WaterPreset
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.source.local.HealthDataDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDataRepository @Inject constructor(
    private val healthDataDao: HealthDataDao,
    private val healthConnectManager: HealthConnectManager,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    // --- READ Functions ---
    fun getLatestBodyTemperature(): Flow<BodyTemperature?> = healthDataDao.getLatestBodyTemperature()
    fun getLatestWeight(): Flow<Weight?> = healthDataDao.getLatestWeight()
    fun getLatestHeartRate(): Flow<HeartRate?> = healthDataDao.getLatestHeartRate()
    fun getTotalWaterIntakeSince(startTime: Long): Flow<Double?> = healthDataDao.getTotalWaterIntakeSince(startTime)
    fun getBodyTemperatureBetween(startTime: Instant, endTime: Instant): Flow<List<BodyTemperature>> {
        return userPreferencesRepository.showHealthConnectDataFlow.flatMapLatest { showHealthConnectData ->
            val localData = healthDataDao.getBodyTemperatureBetween(startTime, endTime)
            if (showHealthConnectData) {
                val healthConnectData = healthConnectManager.getBodyTemperature(startTime, endTime)
                localData.combine(healthConnectData) { local, healthConnect ->
                    (local + healthConnect).distinct()
                }
            } else {
                localData
            }
        }
    }

    fun getWeightBetween(startTime: Instant, endTime: Instant): Flow<List<Weight>> {
        return userPreferencesRepository.showHealthConnectDataFlow.flatMapLatest { showHealthConnectData ->
            val localData = healthDataDao.getWeightBetween(startTime, endTime)
            if (showHealthConnectData) {
                val healthConnectData = healthConnectManager.getWeight(startTime, endTime)
                localData.combine(healthConnectData) { local, healthConnect ->
                    (local + healthConnect).distinct()
                }
            } else {
                localData
            }
        }
    }

    fun getLatestWeightBefore(date: Instant): Flow<Weight?> = healthDataDao.getLatestWeightBefore(date)

    fun getWaterIntakeBetween(startTime: Instant, endTime: Instant): Flow<List<WaterIntake>> {
        return userPreferencesRepository.showHealthConnectDataFlow.flatMapLatest { showHealthConnectData ->
            val localData = healthDataDao.getWaterIntakeBetween(startTime, endTime)
            if (showHealthConnectData) {
                val healthConnectData = healthConnectManager.getWaterIntake(startTime, endTime)
                localData.combine(healthConnectData) { local, healthConnect ->
                    (local + healthConnect).distinct()
                }
            } else {
                localData
            }
        }
    }

    fun getHeartRateBetween(startTime: Instant, endTime: Instant): Flow<List<HeartRate>> {
        return userPreferencesRepository.showHealthConnectDataFlow.flatMapLatest { showHealthConnectData ->
            val localData = healthDataDao.getHeartRateBetween(startTime, endTime)
            if (showHealthConnectData) {
                val healthConnectData = healthConnectManager.getHeartRate(startTime, endTime)
                localData.combine(healthConnectData) { local, healthConnect ->
                    (local + healthConnect).distinct()
                }
            } else {
                localData
            }
        }
    }

    // --- WRITE Functions ---
    suspend fun insertBodyTemperature(record: BodyTemperature) {
        healthDataDao.insertBodyTemperature(record)
        healthConnectManager.writeBodyTemperature(record)
    }

    suspend fun insertWeight(record: Weight) {
        healthDataDao.insertWeight(record)
        healthConnectManager.writeWeight(record)
    }

    suspend fun insertWaterIntake(record: WaterIntake) {
        healthDataDao.insertWaterIntake(record)
        healthConnectManager.writeWaterIntake(record)
    }

    suspend fun insertHeartRate(record: HeartRate) {
        healthDataDao.insertHeartRate(record)
        healthConnectManager.writeHeartRate(record)
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