// In wear/src/main/java/com/d4viddf/medicationreminder/wear/tile/MedicationTileService.kt

package com.d4viddf.medicationreminder.wear.tile

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TimelineBuilders
import com.d4viddf.medicationreminder.wear.persistence.MedicationSyncDao
import com.d4viddf.medicationreminder.wear.persistence.WearAppDatabase
import com.d4viddf.medicationreminder.wear.presentation.WearActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val RESOURCES_VERSION = "1"

// Data class remains the same
internal data class TileReminderInfo(
    val medicationId: Int,
    val name: String,
    val dosage: String?,
    val time: LocalTime
)

class MedicationTileService : CoroutinesTileService() {

    private lateinit var medicationSyncDao: MedicationSyncDao
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        medicationSyncDao = WearAppDatabase.getDatabase(this).medicationSyncDao()
    }

    // This is the new, cleaner way to handle tile requests.
    // It's a suspend function, so you can call your DAO directly.
    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val nextReminderInfo = fetchNextReminder()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(
                                        tileLayout(
                                            this@MedicationTileService,
                                            nextReminderInfo,
                                            requestParams.deviceConfiguration
                                        )
                                    ).build()
                            ).build()
                    ).build()
            ).build()
    }

    // No changes needed to your data fetching or layout functions
    private suspend fun fetchNextReminder(): TileReminderInfo? {
        val allMedicationsWithSchedules = medicationSyncDao.getAllMedicationsWithSchedules().firstOrNull() ?: emptyList()
        if (allMedicationsWithSchedules.isEmpty()) {
            return null
        }

        val today = LocalDate.now()
        val now = LocalTime.now()
        val allPotentialReminders = mutableListOf<TileReminderInfo>()

        allMedicationsWithSchedules.forEach { medPojo ->
            val medication = medPojo.medication
            val startDate = medication.startDate?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
            val endDate = medication.endDate?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }

            if ((startDate != null && startDate.isAfter(today)) || (endDate != null && endDate.isBefore(today))) {
                return@forEach
            }

            medPojo.schedules.forEach { scheduleEntity ->
                val specificTimes: List<LocalTime>? = scheduleEntity.specificTimesJson?.let { json ->
                    val typeToken = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(json, typeToken).map { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) }
                }

                if (scheduleEntity.scheduleType == "DAILY_SPECIFIC_TIMES") {
                    specificTimes?.forEach { time ->
                        allPotentialReminders.add(
                            TileReminderInfo(
                                medicationId = medication.medicationId,
                                name = medication.name,
                                dosage = medication.dosage,
                                time = time
                            )
                        )
                    }
                }
            }
        }

        return allPotentialReminders
            .filter { it.time.isAfter(now) }
            .minByOrNull { it.time }
    }

    private fun tileLayout(
        context: Context,
        nextReminder: TileReminderInfo?,
        deviceParameters: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        val launchMainAppClickable = ModifiersBuilders.Clickable.Builder()
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setClassName(WearActivity::class.java.name)
                            .setPackageName(context.packageName)
                            .build()
                    )
            )
            .build()

        if (nextReminder == null) {
            return PrimaryLayout.Builder(deviceParameters)
                .setPrimaryLabelTextContent(
                    Text.Builder(context, "Medication")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(Color(0xFFB0BEC5).toArgb()))
                        .build()
                )
                .setContent(
                    Text.Builder(context, "No upcoming doses.")
                        .setTypography(Typography.TYPOGRAPHY_BODY1)
                        .setColor(ColorBuilders.argb(Color.White.toArgb()))
                        .build()
                )
                .setPrimaryChipContent(
                    CompactChip.Builder(
                        context,
                        "Open App",
                        launchMainAppClickable,
                        deviceParameters
                    ).build()
                )
                .build()
        }

        return PrimaryLayout.Builder(deviceParameters)
            .setPrimaryLabelTextContent(
                Text.Builder(context, "Next: ${nextReminder.time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
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
                        Text.Builder(context, nextReminder.dosage ?: "")
                            .setTypography(Typography.TYPOGRAPHY_BODY1)
                            .setColor(ColorBuilders.argb(Color(0xFFCFD8DC).toArgb()))
                            .build()
                    )
                    .build()
            )
            .setPrimaryChipContent(
                CompactChip.Builder(
                    context,
                    "View Details",
                    launchMainAppClickable,
                    deviceParameters
                ).build()
            )
            .build()
    }

    // The resourcesRequest function is also simpler
    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): TileBuilders.Resources {
        return TileBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }
}

// The @Preview code for Compose does not need to change.