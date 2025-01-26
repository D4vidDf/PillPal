package com.d4viddf.medicationreminder.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Medication::class, MedicationType::class, MedicationSchedule::class, MedicationReminder::class, MedicationInfo::class, FirebaseSync::class],
    version = 2,
    exportSchema = false
)
abstract class MedicationDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationTypeDao(): MedicationTypeDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun medicationInfoDao(): MedicationInfoDao
    abstract fun firebaseSyncDao(): FirebaseSyncDao
}

