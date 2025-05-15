package com.d4viddf.medicationreminder.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
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
