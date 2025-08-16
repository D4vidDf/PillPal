package com.d4viddf.medicationreminder.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.d4viddf.medicationreminder.data.model.FirebaseSync
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationInfo
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.MedicationType
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.WaterPreset
import com.d4viddf.medicationreminder.data.model.healthdata.Weight

@Database(
    entities = [Medication::class, MedicationType::class, MedicationSchedule::class, MedicationReminder::class, MedicationInfo::class, FirebaseSync::class, BodyTemperature::class, Weight::class,
        WaterIntake::class, WaterPreset::class],
    version = 9, // Incremented version to 9
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class MedicationDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `water_presets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` REAL NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE water_intake_records ADD COLUMN type TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create BodyTemperature table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `body_temperature_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `time` INTEGER NOT NULL, 
                        `temperatureCelsius` REAL NOT NULL, 
                        `measurementLocation` INTEGER, 
                        `sourceApp` TEXT, 
                        `device` TEXT
                    )
                """)
                // Create Weight table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `weight_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `time` INTEGER NOT NULL, 
                        `weightKilograms` REAL NOT NULL, 
                        `sourceApp` TEXT, 
                        `device` TEXT
                    )
                """)
                // Create WaterIntake table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `water_intake_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `time` INTEGER NOT NULL, 
                        `volumeMilliliters` REAL NOT NULL, 
                        `sourceApp` TEXT, 
                        `device` TEXT
                    )
                """)
            }
        }
        // ... (rest of the migrations are the same)
    }

    abstract fun medicationDao(): MedicationDao
    abstract fun medicationTypeDao(): MedicationTypeDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun medicationInfoDao(): MedicationInfoDao
    abstract fun firebaseSyncDao(): FirebaseSyncDao
    abstract fun healthDataDao(): HealthDataDao
}
