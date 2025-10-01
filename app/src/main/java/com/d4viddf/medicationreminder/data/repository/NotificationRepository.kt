package com.d4viddf.medicationreminder.data.repository

import com.d4viddf.medicationreminder.data.model.Notification
import com.d4viddf.medicationreminder.data.source.local.NotificationDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(private val notificationDao: NotificationDao) {

    fun getAllNotifications(): Flow<List<Notification>> = notificationDao.getAllNotifications()

    suspend fun insert(notification: Notification) {
        notificationDao.insert(notification)
    }

    suspend fun delete(notification: Notification) {
        notificationDao.delete(notification)
    }

    suspend fun deleteAll() {
        notificationDao.deleteAll()
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    fun getUnreadNotificationCount(): Flow<Int> = notificationDao.getUnreadNotificationCount()
}