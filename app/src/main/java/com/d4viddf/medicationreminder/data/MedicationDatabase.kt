package com.d4viddf.medicationreminder.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Medication::class, MedicationType::class, MedicationSchedule::class, MedicationReminder::class, MedicationInfo::class, FirebaseSync::class],
    version = 5, // Incremented version to 5
    exportSchema = false
)
abstract class MedicationDatabase : RoomDatabase() {

    companion object {
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

