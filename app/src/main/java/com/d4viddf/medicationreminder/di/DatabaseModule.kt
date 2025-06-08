package com.d4viddf.medicationreminder.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration // Import Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.d4viddf.medicationreminder.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Llamar a un método que inserte los tipos de medicamentos predeterminados
                    val defaultTypes = listOf(
                        MedicationType(name = "Tablet", imageUrl = "https://placehold.co/600x400.png"),
                        MedicationType(name = "Pill", imageUrl = "https://placehold.co/600x400.png"),
                        MedicationType(name = "Powder", imageUrl = "https://placehold.co/600x400.png"),
                        MedicationType(name = "Syringe", imageUrl = "https://placehold.co/600x400.png"),
                        MedicationType(name = "Creme", imageUrl = "https://placehold.co/600x400.png"),
                        MedicationType(name = "Spray", imageUrl = "https://placehold.co/600x400.png"),
                        MedicationType(name = "Liquid", imageUrl = "https://placehold.co/600x400.png"),
                        MedicationType(name = "Suppositoriun", imageUrl = "https://placehold.co/600x400.png"),
                        MedicationType(name = "Patch", imageUrl = "https://placehold.co/600x400.png"),
                    )
                    // Iniciar una coroutine para insertar los datos predeterminados'
                    CoroutineScope(Dispatchers.IO).launch {
                        db.execSQL(
                            "INSERT INTO medication_types (name, imageUrl) VALUES " +
                                    defaultTypes.joinToString(", ") { "('${it.name}', '${it.imageUrl}')" }
                        )
                    }
                }
            })
            .addMigrations(MIGRATION_2_3, MedicationDatabase.MIGRATION_3_4, MedicationDatabase.MIGRATION_4_5) // Added MIGRATION_4_5
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
    fun provideMedicationInfoDao(database: MedicationDatabase): MedicationInfoDao = database.medicationInfoDao()

    @Provides
    fun provideFirebaseSyncDao(database: MedicationDatabase): FirebaseSyncDao = database.firebaseSyncDao()

}
