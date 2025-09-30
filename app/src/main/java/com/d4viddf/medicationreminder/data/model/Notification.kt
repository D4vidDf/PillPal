package com.d4viddf.medicationreminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: LocalDateTime,
    val type: String, // "low_medication" or "security_alert"
    val icon: String,
    val color: String?,
    var isRead: Boolean = false
)