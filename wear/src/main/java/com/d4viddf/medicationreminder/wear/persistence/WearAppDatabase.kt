package com.d4viddf.medicationreminder.wear.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MedicationSyncEntity::class, ScheduleDetailSyncEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WearAppDatabase : RoomDatabase() {

    abstract fun medicationSyncDao(): MedicationSyncDao

    companion object {
        @Volatile
        private var INSTANCE: WearAppDatabase? = null

        fun getDatabase(context: Context): WearAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WearAppDatabase::class.java,
                    "wear_medication_sync_db"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
