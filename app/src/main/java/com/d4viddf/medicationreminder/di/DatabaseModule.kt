package com.d4viddf.medicationreminder.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration // Import Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationType
import com.d4viddf.medicationreminder.data.source.local.FirebaseSyncDao
import com.d4viddf.medicationreminder.data.source.local.MedicationDao
import com.d4viddf.medicationreminder.data.source.local.MedicationDatabase
import com.d4viddf.medicationreminder.data.source.local.MedicationDosageDao
import com.d4viddf.medicationreminder.data.source.local.MedicationInfoDao
import com.d4viddf.medicationreminder.data.source.local.MedicationReminderDao
import com.d4viddf.medicationreminder.data.source.local.MedicationScheduleDao
import com.d4viddf.medicationreminder.data.source.local.MedicationTypeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add the new column, allowing NULLs initially
            // Note: Table name is "medications" as defined in Medication.kt Entity annotation
            db.execSQL("ALTER TABLE medications ADD COLUMN registrationDate TEXT")

            // Populate registrationDate for existing rows
            // Use startDate if it's not null and not empty, otherwise use current date
            db.execSQL("""
                UPDATE medications
                SET registrationDate = CASE
                    WHEN startDate IS NOT NULL AND startDate != '' THEN startDate
                    ELSE date('now')
                END
            """)
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MedicationDatabase {
        return Room.databaseBuilder(
            context,
            MedicationDatabase::class.java,
            "medications.db"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)

                    val cursor = db.query("SELECT COUNT(*) FROM medication_types")
                    val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                    cursor.close()

                    if (count == 0) {
                        val defaultTypes = listOf(
                            MedicationType(nameResId = R.string.medication_type_tablet, imageUrl = "file:///android_asset/medication_types/tablet.webp"),
                            MedicationType(nameResId = R.string.medication_type_pill, imageUrl = "file:///android_asset/medication_types/pill.webp"),
                            MedicationType(nameResId = R.string.medication_type_powder, imageUrl = "file:///android_asset/medication_types/powder.webp"),
                            MedicationType(nameResId = R.string.medication_type_syringe, imageUrl = "file:///android_asset/medication_types/syringe.webp"),
                            MedicationType(nameResId = R.string.medication_type_creme, imageUrl = "file:///android_asset/medication_types/creme.webp"),
                            MedicationType(nameResId = R.string.medication_type_spray, imageUrl = "file:///android_asset/medication_types/spray.webp"),
                            MedicationType(nameResId = R.string.medication_type_liquid, imageUrl = "file:///android_asset/medication_types/liquid.webp"),
                            MedicationType(nameResId = R.string.medication_type_suppositorium, imageUrl = "file:///android_asset/medication_types/suppositorium.webp"),
                            MedicationType(nameResId = R.string.medication_type_patch, imageUrl = "file:///android_asset/medication_types/patch.webp"),
                            MedicationType(nameResId = R.string.medication_type_inhaler, imageUrl = "file:///android_asset/medication_types/inhaler.webp"),
                            MedicationType(nameResId = R.string.medication_type_drops, imageUrl = "file:///android_asset/medication_types/drops.webp"),
                            MedicationType(nameResId = R.string.medication_type_other, imageUrl = "file:///android_asset/medication_types/other.webp"),
                        )
                        db.execSQL(
                            "INSERT INTO medication_types (nameResId, imageUrl) VALUES " +
                                    defaultTypes.joinToString(", ") { "(${it.nameResId}, '${it.imageUrl}')" }
                        )
                    }
                }
            })
            .addMigrations(MIGRATION_2_3, MedicationDatabase.MIGRATION_3_4, MedicationDatabase.MIGRATION_4_5, MedicationDatabase.MIGRATION_5_6, MedicationDatabase.MIGRATION_6_7, MedicationDatabase.MIGRATION_8_9, MedicationDatabase.MIGRATION_9_10, MedicationDatabase.MIGRATION_10_11, MedicationDatabase.MIGRATION_11_12, MedicationDatabase.MIGRATION_12_13, MedicationDatabase.MIGRATION_13_14, MedicationDatabase.MIGRATION_14_15)
            .fallbackToDestructiveMigration(false) // Added this line
            .build()
    }

    @Provides
    fun provideMedicationDao(database: MedicationDatabase): MedicationDao = database.medicationDao()

    @Provides
    fun provideMedicationTypeDao(database: MedicationDatabase): MedicationTypeDao = database.medicationTypeDao()

    @Provides
    fun provideMedicationScheduleDao(database: MedicationDatabase): MedicationScheduleDao = database.medicationScheduleDao()

    @Provides
    fun provideMedicationReminderDao(database: MedicationDatabase): MedicationReminderDao = database.medicationReminderDao()

    @Provides
    fun provideMedicationDosageDao(database: MedicationDatabase): MedicationDosageDao = database.medicationDosageDao()

    @Provides
    fun provideMedicationInfoDao(database: MedicationDatabase): MedicationInfoDao = database.medicationInfoDao()

    @Provides
    fun provideFirebaseSyncDao(database: MedicationDatabase): FirebaseSyncDao = database.firebaseSyncDao()
    @Provides
    fun provideHealthDataDao(database: MedicationDatabase) = database.healthDataDao()

    @Provides
    fun provideNotificationDao(database: MedicationDatabase) = database.notificationDao()

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.IO)

    @Provides
    @Singleton // Dispatchers are singletons
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
