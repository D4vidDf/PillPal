package com.d4viddf.medicationreminder.ui.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.Notification
import com.d4viddf.medicationreminder.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<Notification>> = notificationRepository.getAllNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        markAllAsRead()
        // TODO: Remove this mock data generator before final production release
        addMockData()
    }

    private fun addMockData() {
        viewModelScope.launch {
            // Clear previous mocks to avoid duplicates on each screen view
            notificationRepository.deleteAll()

            val mockNotifications = listOf(
                Notification(title = "Ibuprofen", message = "Time to refill your Ibuprofen", timestamp = LocalDateTime.now(), type = "low_medication", icon = "pill", color = "LIGHT_BLUE"),
                Notification(title = "Stay Hydrated!", message = "Time to drink a glass of water.", timestamp = LocalDateTime.now().minusMinutes(30), type = "water_reminder", icon = "water", color = null),
                Notification(title = "Security Note", message = "La AEMPS informa que se ha retirado un lote de Paracetamol...", timestamp = LocalDateTime.now().minusHours(1), type = "security_alert", icon = "security", color = null),
                Notification(title = "Amoxicillin", message = "Time to refill your Amoxicillin", timestamp = LocalDateTime.now().minusDays(1), type = "low_medication", icon = "capsule", color = "LIGHT_GREEN"),
                Notification(title = "Security Note", message = "Actualización de seguridad importante sobre su tratamiento.", timestamp = LocalDateTime.now().minusDays(1).minusHours(2), type = "security_alert", icon = "security", color = null),
                Notification(title = "Cough Syrup", message = "Your syrup is running low.", timestamp = LocalDateTime.now().minusDays(2), type = "low_medication", icon = "syrup", color = "LIGHT_PURPLE")
            )
            mockNotifications.forEach { notificationRepository.insert(it) }
            // Mark the new mock data as read since we are viewing the screen
            markAllAsRead()
        }
    }

    fun onNotificationSwiped(notification: Notification) {
        viewModelScope.launch {
            notificationRepository.delete(notification)
        }
    }

    fun onClearAllConfirmed() {
        viewModelScope.launch {
            notificationRepository.deleteAll()
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }
}