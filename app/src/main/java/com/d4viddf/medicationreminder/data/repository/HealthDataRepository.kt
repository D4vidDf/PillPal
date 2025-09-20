package com.d4viddf.medicationreminder.data.repository

import com.d4viddf.medicationreminder.data.healthconnect.HealthConnectManager
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.HeartRate
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
    private val healthDataDao: HealthDataDao,
    private val healthConnectManager: HealthConnectManager
) {

    // --- READ Functions ---
    fun getLatestBodyTemperature(): Flow<BodyTemperature?> = healthDataDao.getLatestBodyTemperature()
    fun getLatestWeight(): Flow<Weight?> = healthDataDao.getLatestWeight()
    fun getLatestHeartRate(): Flow<HeartRate?> = healthDataDao.getLatestHeartRate()
    fun getTotalWaterIntakeSince(startTime: Long): Flow<Double?> = healthDataDao.getTotalWaterIntakeSince(startTime)

    fun getBodyTemperatureBetween(startTime: Instant, endTime: Instant, useHealthConnect: Boolean): Flow<List<BodyTemperature>> {
        return if (useHealthConnect) {
            healthConnectManager.getBodyTemperature(startTime, endTime)
        } else {
            healthDataDao.getBodyTemperatureBetween(startTime, endTime)
        }
    }

    fun getWeightBetween(startTime: Instant, endTime: Instant, useHealthConnect: Boolean): Flow<List<Weight>> {
        return if (useHealthConnect) {
            healthConnectManager.getWeight(startTime, endTime)
        } else {
            healthDataDao.getWeightBetween(startTime, endTime)
        }
    }

    fun getHeartRateBetween(startTime: Instant, endTime: Instant, useHealthConnect: Boolean): Flow<List<HeartRate>> {
        return if (useHealthConnect) {
            healthConnectManager.getHeartRate(startTime, endTime)
        } else {
            healthDataDao.getHeartRateBetween(startTime, endTime)
        }
    }

    fun getLatestWeightBefore(date: Instant): Flow<Weight?> = healthDataDao.getLatestWeightBefore(date)

    fun getWaterIntakeBetween(startTime: Instant, endTime: Instant, useHealthConnect: Boolean): Flow<List<WaterIntake>> {
        return if (useHealthConnect) {
            healthConnectManager.getWaterIntake(startTime, endTime)
        } else {
            healthDataDao.getWaterIntakeBetween(startTime, endTime)
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