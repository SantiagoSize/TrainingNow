package com.shagox.apptrainingnow.data.local.notification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int, // Para saber a quién le llegó (Relación)
    val title: String,
    val message: String,
    val date: Long = System.currentTimeMillis(),
    val isRead: Boolean = false // Para marcar si ya la leyó
)