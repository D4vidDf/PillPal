package com.d4viddf.medicationreminder.di

import android.content.Context
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationScheduleRepository
import com.d4viddf.medicationreminder.data.SimplestDependency // Asegúrate que esta clase existe y es @Singleton @Inject constructor()
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent // Usa ApplicationComponent si es una versión muy vieja de Hilt

@EntryPoint
@InstallIn(SingletonComponent::class) // O ApplicationComponent
interface WorkerDependenciesEntryPoint {
    fun medicationRepository(): MedicationRepository
    fun medicationScheduleRepository(): MedicationScheduleRepository
    fun medicationReminderRepository(): MedicationReminderRepository
    fun notificationScheduler(): NotificationScheduler
    fun simplestDependency(): SimplestDependency // Para TestSimpleWorker
}