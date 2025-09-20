package com.d4viddf.medicationreminder.data.healthconnect

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.HeartRate
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import android.content.pm.PackageManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val healthConnectCompatible = mutableStateOf(false)
    init {
        healthConnectCompatible.value = isHealthConnectAppInstalled()
    }

    fun isHealthConnectAppInstalled(): Boolean {
        val providerPackageName = "com.google.android.apps.healthdata"
        val status = HealthConnectClient.getSdkStatus(context, providerPackageName)
        return status != HealthConnectClient.SDK_UNAVAILABLE
    }

    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getWritePermission(BodyTemperatureRecord::class),
    )

    suspend fun hasAllPermissions(): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    fun getPermissions(): Set<String> {
        return PERMISSIONS.map { it.toString() }.toSet()
    }

    suspend fun revokeAllPermissions() {
        healthConnectClient.permissionController.revokeAllPermissions()
    }

    suspend fun writeWaterIntake(record: WaterIntake) {
        val hydrationRecord = HydrationRecord(
            startTime = record.time,
            startZoneOffset = null,
            endTime = record.time,
            endZoneOffset = null,
            volume = androidx.health.connect.client.units.Volume.milliliters(record.volumeMilliliters)
        )
        healthConnectClient.insertRecords(listOf(hydrationRecord))
    }

    suspend fun writeWeight(record: Weight) {
        val weightRecord = WeightRecord(
            time = record.time,
            zoneOffset = null,
            weight = androidx.health.connect.client.units.Mass.kilograms(record.weightKilograms)
        )
        healthConnectClient.insertRecords(listOf(weightRecord))
    }

    suspend fun writeBodyTemperature(record: BodyTemperature) {
        val temperatureRecord = BodyTemperatureRecord(
            time = record.time,
            zoneOffset = null,
            temperature = androidx.health.connect.client.units.Temperature.celsius(record.temperatureCelsius)
        )
        healthConnectClient.insertRecords(listOf(temperatureRecord))
    }

    suspend fun writeHeartRate(record: HeartRate) {
        val heartRateRecord = HeartRateRecord(
            startTime = record.time,
            startZoneOffset = null,
            endTime = record.time,
            endZoneOffset = null,
            samples = listOf(
                HeartRateRecord.Sample(
                    time = record.time,
                    beatsPerMinute = record.beatsPerMinute
                )
            )
        )
        healthConnectClient.insertRecords(listOf(heartRateRecord))
    }

    fun getWaterIntake(startTime: Instant, endTime: Instant): Flow<List<WaterIntake>> = flow {
        if (hasAllPermissions()) {
            val request = ReadRecordsRequest(
                recordType = HydrationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val response = healthConnectClient.readRecords(request)
            emit(response.records.map {
                WaterIntake(
                    time = it.startTime,
                    volumeMilliliters = it.volume.inMilliliters,
                    sourceApp = it.metadata.dataOrigin.packageName
                )
            })
        } else {
            emit(emptyList())
        }
    }

    fun getWeight(startTime: Instant, endTime: Instant): Flow<List<Weight>> = flow {
        if (hasAllPermissions()) {
            val request = ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val response = healthConnectClient.readRecords(request)
            emit(response.records.map {
                Weight(
                    time = it.time,
                    weightKilograms = it.weight.inKilograms,
                    sourceApp = it.metadata.dataOrigin.packageName
                )
            })
        } else {
            emit(emptyList())
        }
    }

    fun getBodyTemperature(startTime: Instant, endTime: Instant): Flow<List<BodyTemperature>> = flow {
        if (hasAllPermissions()) {
            val request = ReadRecordsRequest(
                recordType = BodyTemperatureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val response = healthConnectClient.readRecords(request)
            emit(response.records.map {
                BodyTemperature(
                    time = it.time,
                    temperatureCelsius = it.temperature.inCelsius,
                    measurementLocation = it.measurementLocation,
                    sourceApp = it.metadata.dataOrigin.packageName
                )
            })
        } else {
            emit(emptyList())
        }
    }

    fun getHeartRate(startTime: Instant, endTime: Instant): Flow<List<HeartRate>> = flow {
        if (hasAllPermissions()) {
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val response = healthConnectClient.readRecords(request)
            emit(response.records.flatMap { heartRateRecord ->
                heartRateRecord.samples.map { sample ->
                    HeartRate(
                        time = sample.time,
                        beatsPerMinute = sample.beatsPerMinute,
                        sourceApp = heartRateRecord.metadata.dataOrigin.packageName
                    )
                }
            })
        } else {
            emit(emptyList())
        }
    }
}
