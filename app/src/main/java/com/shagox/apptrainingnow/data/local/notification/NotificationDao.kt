package com.shagox.apptrainingnow.data.local.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    // Guardar una notificación nueva
    @Insert
    suspend fun insertNotification(notification: NotificationEntity)

    // Obtener todas las notificaciones de un usuario específico
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY date DESC")
    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>>

    // Contar cuántas no ha leído (para el puntito rojo)
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCount(userId: Int): Flow<Int>
}