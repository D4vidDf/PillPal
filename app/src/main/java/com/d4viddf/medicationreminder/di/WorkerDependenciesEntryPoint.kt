package com.d4viddf.medicationreminder.di

import com.d4viddf.medicationreminder.data.repository.MedicationDosageRepository
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.data.model.SimplestDependency // Asegúrate que esta clase existe y es @Singleton @Inject constructor()
import com.d4viddf.medicationreminder.data.repository.MedicationReminderRepository
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent // Usa ApplicationComponent si es una versión muy vieja de Hilt

@EntryPoint
@InstallIn(SingletonComponent::class) // O ApplicationComponent
interface WorkerDependenciesEntryPoint {
    fun medicationRepository(): MedicationRepository
    fun medicationScheduleRepository(): MedicationScheduleRepository
    fun medicationReminderRepository(): MedicationReminderRepository
    fun medicationDosageRepository(): MedicationDosageRepository
    fun notificationScheduler(): NotificationScheduler
    fun simplestDependency(): SimplestDependency // Para TestSimpleWorker
}