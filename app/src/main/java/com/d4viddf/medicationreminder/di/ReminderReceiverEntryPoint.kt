package com.d4viddf.medicationreminder.di // Or your chosen package

import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationTypeRepository
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent // Or ApplicationComponent if older Hilt

@EntryPoint
@InstallIn(SingletonComponent::class) // Or ApplicationComponent
interface ReminderReceiverEntryPoint {
    fun reminderRepository(): MedicationReminderRepository
    fun medicationRepository(): MedicationRepository
    fun medicationTypeRepository(): MedicationTypeRepository
    fun notificationScheduler(): NotificationScheduler
}