package com.d4viddf.medicationreminder.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.d4viddf.medicationreminder.data.model.FirebaseSync
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationDosage
import com.d4viddf.medicationreminder.data.model.MedicationInfo
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.MedicationType
import com.d4viddf.medicationreminder.data.model.Notification
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.HeartRate
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.WaterPreset
import com.d4viddf.medicationreminder.data.model.healthdata.Weight

@Database(
    entities = [Medication::class, MedicationType::class, MedicationSchedule::class, MedicationReminder::class, MedicationInfo::class, FirebaseSync::class, BodyTemperature::class, Weight::class,
        WaterIntake::class, WaterPreset::class, HeartRate::class, MedicationDosage::class, Notification::class],
    version = 15,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class MedicationDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN lowStockThreshold INTEGER")
                db.execSQL("ALTER TABLE medications ADD COLUMN lowStockReminderDays INTEGER")
                db.execSQL("ALTER TABLE medications ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medications ADD COLUMN isSuspended INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `timestamp` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `color` TEXT,
                        `isRead` INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN saveRemainingFraction INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medication_schedule ADD COLUMN startDate TEXT")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // We are performing major schema changes, so we'll disable foreign keys for the transaction.
                db.execSQL("PRAGMA foreign_keys=OFF;")

                // Step 1: Create the new medication_dosages table.
                db.execSQL("""
                    CREATE TABLE `medication_dosages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `medicationId` INTEGER NOT NULL,
                        `dosage` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT,
                        FOREIGN KEY(`medicationId`) REFERENCES `medications`(`id`) ON DELETE CASCADE
                    )
                """)

                // Step 2: Populate the new medication_dosages table from the old medications table.
                // Use COALESCE to find a valid start date for the dosage.
                db.execSQL("""
                    INSERT INTO medication_dosages (medicationId, dosage, startDate)
                    SELECT id, dosage, COALESCE(startDate, registrationDate, '${java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}')
                    FROM medications
                    WHERE dosage IS NOT NULL AND dosage != ''
                """)

                // Step 3: Recreate the medications table without the 'dosage' column.
                db.execSQL("""
                    CREATE TABLE `medications_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `typeId` INTEGER,
                        `color` TEXT NOT NULL,
                        `packageSize` INTEGER NOT NULL,
                        `remainingDoses` INTEGER NOT NULL,
                        `startDate` TEXT,
                        `endDate` TEXT,
                        `reminderTime` TEXT,
                        `registrationDate` TEXT,
                        `nregistro` TEXT
                    )
                """)
                db.execSQL("""
                    INSERT INTO `medications_new` (id, name, typeId, color, packageSize, remainingDoses, startDate, endDate, reminderTime, registrationDate, nregistro)
                    SELECT id, name, typeId, color, packageSize, remainingDoses, startDate, endDate, reminderTime, registrationDate, nregistro FROM `medications`
                """)
                db.execSQL("DROP TABLE `medications`")
                db.execSQL("ALTER TABLE `medications_new` RENAME TO `medications`")


                // Step 4: Create a new medication_reminder table with the required medicationDosageId.
                db.execSQL("""
                    CREATE TABLE `medication_reminder_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `medicationId` INTEGER NOT NULL,
                        `medicationScheduleId` INTEGER,
                        `medicationDosageId` INTEGER NOT NULL,
                        `reminderTime` TEXT NOT NULL,
                        `isTaken` INTEGER NOT NULL DEFAULT 0,
                        `takenAt` TEXT,
                        `notificationId` INTEGER,
                        FOREIGN KEY(`medicationId`) REFERENCES `medications`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`medicationScheduleId`) REFERENCES `medication_schedule`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`medicationDosageId`) REFERENCES `medication_dosages`(`id`) ON DELETE CASCADE
                    )
                """)

                // Step 5: Populate the new reminder table, linking to the new dosage table.
                // This preserves all reminders for medications that had a dosage.
                db.execSQL("""
                    INSERT INTO medication_reminder_new (id, medicationId, medicationScheduleId, medicationDosageId, reminderTime, isTaken, takenAt, notificationId)
                    SELECT
                        mr.id,
                        mr.medicationId,
                        mr.medicationScheduleId,
                        md.id,
                        mr.reminderTime,
                        mr.isTaken,
                        mr.takenAt,
                        mr.notificationId
                    FROM medication_reminder AS mr
                    JOIN medication_dosages AS md ON mr.medicationId = md.medicationId
                """)

                // Step 6: Drop the old reminder table and rename the new one.
                db.execSQL("DROP TABLE `medication_reminder`")
                db.execSQL("ALTER TABLE `medication_reminder_new` RENAME TO `medication_reminder`")

                // Step 7: Recreate all necessary indices for the new tables.
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_dosages_medicationId` ON `medication_dosages` (`medicationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_reminder_medicationId` ON `medication_reminder` (`medicationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_reminder_medicationScheduleId` ON `medication_reminder` (`medicationScheduleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_reminder_medicationDosageId` ON `medication_reminder` (`medicationDosageId`)")

                // Finally, re-enable foreign key constraints.
                db.execSQL("PRAGMA foreign_keys=ON;")
            }
        }


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
    abstract fun medicationDosageDao(): MedicationDosageDao
    abstract fun medicationInfoDao(): MedicationInfoDao
    abstract fun firebaseSyncDao(): FirebaseSyncDao
    abstract fun healthDataDao(): HealthDataDao
    abstract fun notificationDao(): NotificationDao
}