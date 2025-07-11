package com.d4viddf.medicationreminder.wear.tile

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import com.d4viddf.medicationreminder.wear.persistence.MedicationSyncDao
import com.d4viddf.medicationreminder.wear.persistence.WearAppDatabase
import com.d4viddf.medicationreminder.wear.presentation.WearActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.util.Log
import androidx.core.content.ContextCompat // Added import for ContextCompat
// Jetpack Compose imports for Previews
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column as ComposeColumn
import androidx.compose.foundation.layout.Spacer as ComposeSpacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight as ComposeFontWeight
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.wear.compose.material.Text as ComposeText

private const val RESOURCES_VERSION = "1"
private const val TILE_ID = "medication_tile"

// --- Data structure for displaying reminders on the tile ---
internal data class TileReminderInfo(
    val name: String,
    val dosage: String?,
    val time: LocalTime // Using LocalTime for easier comparison
)

// Using LocalTime for time representation
fun formatLocalTime(localTime: LocalTime): String {
    return localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

class MedicationTileService : TileService() {

    // Scope for coroutines launched from this service.
    // Use Dispatchers.IO for database operations.
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var medicationSyncDao: MedicationSyncDao
    private val gson = Gson() // For parsing JSON from Room entities

    override fun onCreate() {
        super.onCreate()
        medicationSyncDao = WearAppDatabase.getDatabase(this).medicationSyncDao()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        // This method is called on the main thread, so offload DB access.
        return Futures.transform(
            serviceScope.launchListenableFuture {
                fetchNextReminder()
            },
            { nextReminderInfo ->
                TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCES_VERSION)
                    .setTimeline( // Corrected method name: setTimeline
                        TileBuilders.Timeline.Builder().addTimelineEntry(
                            TileBuilders.TimelineEntry.Builder().setLayout(
                                LayoutElementBuilders.Layout.Builder().setRoot(
                                    tileLayout(this, nextReminderInfo, requestParams.deviceConfiguration)
                                ).build()
                            ).build()
                        ).build()
                    )
                    .build()
            },
            ContextCompat.getMainExecutor(this) // Corrected executor usage
        )
    }

    // Helper to launch a coroutine and return ListenableFuture
    private fun <T> CoroutineScope.launchListenableFuture(
        block: suspend CoroutineScope.() -> T
    ): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        this.launch {
            try {
                future.set(block())
            } catch (e: Exception) {
                future.setException(e)
                Log.e("MedicationTileService", "Error in launchListenableFuture", e)
            }
        }
        return future
    }


    private suspend fun fetchNextReminder(): TileReminderInfo? {
        val allMedicationsWithSchedules = medicationSyncDao.getAllMedicationsWithSchedules().firstOrNull() ?: emptyList()
        if (allMedicationsWithSchedules.isEmpty()) {
            Log.d("MedicationTileService", "No medications found in Room for tile.")
            return null
        }
        Log.d("MedicationTileService", "Fetched ${allMedicationsWithSchedules.size} medications from Room for tile.")


        val today = LocalDate.now()
        val now = LocalTime.now()
        val allPotentialReminders = mutableListOf<TileReminderInfo>()

        allMedicationsWithSchedules.forEach { medPojo ->
            val medication = medPojo.medication
            val startDate = medication.startDate?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
            val endDate = medication.endDate?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }

            if (startDate != null && startDate.isAfter(today)) return@forEach
            if (endDate != null && endDate.isBefore(today)) return@forEach

            medPojo.schedules.forEach { scheduleEntity ->
                val specificTimes: List<LocalTime>? = scheduleEntity.specificTimesJson?.let { json ->
                    val typeToken = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(json, typeToken).map { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) }
                }
                val dailyRepetitionDays: List<String>? = scheduleEntity.dailyRepetitionDaysJson?.let { json ->
                    val typeToken = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(json, typeToken)
                }

                when (scheduleEntity.scheduleType) {
                    "DAILY_SPECIFIC_TIMES" -> {
                        if (dailyRepetitionDays?.contains(today.dayOfWeek.name) == true || dailyRepetitionDays.isNullOrEmpty()) {
                            specificTimes?.forEach { time ->
                                allPotentialReminders.add(
                                    TileReminderInfo(
                                        name = medication.name,
                                        dosage = medication.dosage,
                                        time = time
                                    )
                                )
                            }
                        }
                    }
                    "INTERVAL" -> {
                        val intervalStartTimeStr = scheduleEntity.intervalStartTime
                        val intervalEndTimeStr = scheduleEntity.intervalEndTime
                        val intervalHours = scheduleEntity.intervalHours
                        val intervalMinutes = scheduleEntity.intervalMinutes

                        if (intervalStartTimeStr != null && intervalHours != null && intervalMinutes != null) {
                            var currentTimeSlot = LocalTime.parse(intervalStartTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
                            val intervalEndTime = intervalEndTimeStr?.let { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) } ?: LocalTime.MAX
                            val intervalDuration = java.time.Duration.ofHours(intervalHours.toLong()).plusMinutes(intervalMinutes.toLong())

                            while (!currentTimeSlot.isAfter(intervalEndTime) && intervalDuration.seconds > 0) {
                                if (currentTimeSlot.isAfter(LocalTime.MIDNIGHT) || currentTimeSlot == LocalTime.MIDNIGHT) {
                                    allPotentialReminders.add(
                                        TileReminderInfo(
                                            name = medication.name,
                                            dosage = medication.dosage,
                                            time = currentTimeSlot
                                        )
                                    )
                                }
                                val nextTimeSlot = currentTimeSlot.plus(intervalDuration)
                                if (nextTimeSlot == currentTimeSlot) break
                                currentTimeSlot = nextTimeSlot
                                if (currentTimeSlot.isBefore(LocalTime.parse(intervalStartTimeStr, DateTimeFormatter.ofPattern("HH:mm")))) break
                            }
                        }
                    }
                }
            }
        }
        // Filter for upcoming reminders today and sort
        val upcomingReminders = allPotentialReminders
            .filter { it.time.isAfter(now) }
            .sortedBy { it.time }

        Log.d("MedicationTileService", "Found ${upcomingReminders.size} upcoming reminders for the tile.")
        return upcomingReminders.firstOrNull()
    }


    private fun tileLayout(
        context: Context,
        nextReminder: TileReminderInfo?,
        deviceParameters: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {

        val launchAppClickable = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setClassName(WearActivity::class.java.name)
                    .setPackageName(context.packageName)
                    .build()
            ).build()

        if (nextReminder == null) {
            return PrimaryLayout.Builder(deviceParameters)
                .setContent(
                    Text.Builder(context, "No upcoming doses.")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(Color.White.toArgb()))
                        .build()
                )
                .setPrimaryChipContent(
                    CompactChip.Builder(
                        context,
                        "Open App",
                        ModifiersBuilders.Clickable.Builder().setOnClick(launchAppClickable).build(),
                        deviceParameters
                    ).build()
                )
                .build()
        }

        return PrimaryLayout.Builder(deviceParameters)
            .setPrimaryLabelTextContent(
                Text.Builder(context, "Next: ${formatLocalTime(nextReminder.time)}")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(ColorBuilders.argb(Color(0xFFB0BEC5).toArgb()))
                    .build()
            )
            .setContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(
                        Text.Builder(context, nextReminder.name)
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .setColor(ColorBuilders.argb(Color.White.toArgb()))
                            .build()
                    )
                    .addContent(
                        Text.Builder(context, nextReminder.dosage ?: "") // Handle null dosage
                            .setTypography(Typography.TYPOGRAPHY_BODY1)
                            .setColor(ColorBuilders.argb(Color(0xFFCFD8DC).toArgb()))
                            .build()
                    )
                    .build()
            )
            .setPrimaryChipContent(
                CompactChip.Builder(
                    context,
                    "Open App",
                    ModifiersBuilders.Clickable.Builder().setOnClick(launchAppClickable).build(),
                    deviceParameters
                ).build()
            )
            .build()
    }
}


// --- Previews for Tile ---
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun TilePreview() {
    // Use TileReminderInfo and LocalTime for the preview
    val nextReminder = TileReminderInfo(
        name = "Mestinon",
        dosage = "1 tablet",
        time = LocalTime.of(9, 0)
    )

    MaterialTheme {
        ComposeColumn(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Use formatLocalTime for LocalTime
            ComposeText("Next: ${formatLocalTime(nextReminder.time)}", fontSize = 14.sp, color = Color.LightGray)
            ComposeSpacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
            ComposeText(nextReminder.name, fontSize = 20.sp, fontWeight = ComposeFontWeight.Bold, color = Color.White)
            ComposeText(nextReminder.dosage ?: "", fontSize = 14.sp, color = Color.Gray) // Handle null dosage
        }
    }
}

@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun NoDoseTilePreview() {
    MaterialTheme {
        ComposeColumn(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ComposeText("No upcoming doses.", fontSize = 14.sp, color = Color.White)
        }
    }
}
