package com.d4viddf.medicationreminder.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.d4viddf.medicationreminder.data.converters.DateTimeConverters

@Database(
    entities = [Medication::class, MedicationType::class, MedicationSchedule::class, MedicationReminder::class, MedicationInfo::class, FirebaseSync::class],
    version = 6, // Incremented version to 6
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class MedicationDatabase : RoomDatabase() {

    companion object {
        // Add a new migration for version 5 to 6.
        // Since the changes (String to List with TypeConverter) alter how data is stored
        // but the underlying column type (TEXT) likely remains the same, an empty migration
        // might be sufficient if Room handles the data conversion automatically.
        // However, if existing data needs transformation, specific SQL would be required.
        // For now, assuming Room handles it or new data format doesn't conflict with old string format.
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
}

