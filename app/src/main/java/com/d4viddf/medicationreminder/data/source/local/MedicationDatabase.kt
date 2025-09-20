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
import com.d4viddf.medicationreminder.data.model.healthdata.HeartRate
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.WaterPreset
import com.d4viddf.medicationreminder.data.model.healthdata.Weight

@Database(
    entities = [Medication::class, MedicationType::class, MedicationSchedule::class, MedicationReminder::class, MedicationInfo::class, FirebaseSync::class, BodyTemperature::class, Weight::class,
        WaterIntake::class, WaterPreset::class, HeartRate::class],
    version = 10, // Incremented version to 10
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class MedicationDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `heart_rate` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `time` INTEGER NOT NULL,
                        `beatsPerMinute` INTEGER NOT NULL,
                        `sourceApp` TEXT
                    )
                """)
            }
        }

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
                // No schema changes from version 7 to 8.
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
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // No explicit SQL needed if Room handles the TypeConverter changes gracefully
                // for existing data, or if data transformation is complex and handled elsewhere (e.g., manual one-time).
                // If the string format was "1,2,3" for DayOfWeek and "08:00,12:00" for LocalTime,
                // the new TypeConverters should be able to parse these existing strings.
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Create new table with medicationScheduleId as nullable INTEGER
                database.execSQL("""
                    CREATE TABLE medication_reminder_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        medicationId INTEGER NOT NULL,
                        medicationScheduleId INTEGER,
                        reminderTime TEXT NOT NULL,
                        isTaken INTEGER NOT NULL DEFAULT 0,
                        takenAt TEXT,
                        notificationId INTEGER,
                        FOREIGN KEY(medicationId) REFERENCES medications(id) ON DELETE CASCADE,
                        FOREIGN KEY(medicationScheduleId) REFERENCES medication_schedule(id) ON DELETE CASCADE
                    )
                """)

                // 2. Copy data, converting 0 in medicationScheduleId to NULL
                database.execSQL("""
                    INSERT INTO medication_reminder_new (id, medicationId, medicationScheduleId, reminderTime, isTaken, takenAt, notificationId)
                    SELECT id, medicationId, CASE WHEN medicationScheduleId = 0 THEN NULL ELSE medicationScheduleId END, reminderTime, isTaken, takenAt, notificationId
                    FROM medication_reminder
                """)

                // 3. Drop old table
                database.execSQL("DROP TABLE medication_reminder")

                // 4. Rename new table to old table name
                database.execSQL("ALTER TABLE medication_reminder_new RENAME TO medication_reminder")

                // 5. Recreate indices (as they are dropped with the table)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_medication_reminder_medicationId ON medication_reminder (medicationId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_medication_reminder_medicationScheduleId ON medication_reminder (medicationScheduleId)")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE medications ADD COLUMN nregistro TEXT DEFAULT NULL")
            }
        }

    }

    abstract fun medicationDao(): MedicationDao
    abstract fun medicationTypeDao(): MedicationTypeDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun medicationInfoDao(): MedicationInfoDao
    abstract fun firebaseSyncDao(): FirebaseSyncDao
    abstract fun healthDataDao(): HealthDataDao
}
