package com.d4viddf.medicationreminder.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.d4viddf.medicationreminder.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton
import androidx.room.migration.Migration

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
                        MedicationType(name = "Cápsula", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Pastilla", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Líquido", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Tópico", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Aerosol", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Crema", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Dispositivo", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Espuma", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Gel", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Gotas", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Inhalador", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Inyección", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Loción", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Parche", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Polvo", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Pomada", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Supositorio", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Compresa", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Tableta", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Gránulos", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Bucal/Sublingual", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Oftálmico", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Ótico", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Nasal", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Vaginal", imageUrl = "https://example.com/placeholder.png"),
                        MedicationType(name = "Incontinencia", imageUrl = "https://example.com/placeholder.png"),
                    )
                    // Iniciar una coroutine para insertar los datos predeterminados
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
