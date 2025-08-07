package com.d4viddf.medicationreminder.data.repository

import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.source.local.HealthDataDao
import kotlinx.coroutines.flow.Flow
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
}