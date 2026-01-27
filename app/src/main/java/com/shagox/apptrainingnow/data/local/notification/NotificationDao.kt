package com.shagox.apptrainingnow.data.local.notification

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar notificaciones del sistema.
 * 
 * Proporciona operaciones CRUD completas y queries especializadas para:
 * - Crear y gestionar notificaciones
 * - Marcar como leídas
 * - Filtrar por tipo y prioridad
 * - Limpieza automática de notificaciones antiguas
 */
@Dao
interface NotificationDao {

    // ==================== OPERACIONES CRUD ====================

    /**
     * Inserta una nueva notificación.
     * @return ID de la notificación creada
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    /**
     * Inserta múltiples notificaciones.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    /**
     * Actualiza una notificación.
     */
    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    /**
     * Elimina una notificación.
     */
    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)

    /**
     * Elimina una notificación por ID.
     */
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Int)

    // ==================== QUERIES GENERALES ====================

    /**
     * Obtiene una notificación por ID.
     */
    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    suspend fun getNotificationById(notificationId: Int): NotificationEntity?

    /**
     * Obtiene todas las notificaciones de un usuario.
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY date DESC")
    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>>

    /**
     * Obtiene notificaciones paginadas.
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getNotificationsPaginated(userId: Int, limit: Int, offset: Int): List<NotificationEntity>

    /**
     * Obtiene las últimas N notificaciones.
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentNotifications(userId: Int, limit: Int = 20): List<NotificationEntity>

    // ==================== QUERIES DE ESTADO (LEÍDO/NO LEÍDO) ====================

    /**
     * Cuenta notificaciones no leídas.
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCount(userId: Int): Flow<Int>

    /**
     * Cuenta notificaciones no leídas de forma síncrona.
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    suspend fun getUnreadCountSync(userId: Int): Int

    /**
     * Obtiene solo notificaciones no leídas.
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0 ORDER BY date DESC")
    fun getUnreadNotifications(userId: Int): Flow<List<NotificationEntity>>

    /**
     * Obtiene solo notificaciones leídas.
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 1 ORDER BY date DESC")
    fun getReadNotifications(userId: Int): Flow<List<NotificationEntity>>

    /**
     * Marca una notificación como leída.
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Int)

    /**
     * Marca todas las notificaciones de un usuario como leídas.
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId AND isRead = 0")
    suspend fun markAllAsRead(userId: Int)

    /**
     * Marca notificaciones de un tipo específico como leídas.
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId AND type = :type AND isRead = 0")
    suspend fun markTypeAsRead(userId: Int, type: String)

    // ==================== QUERIES POR TIPO ====================

    /**
     * Obtiene notificaciones por tipo.
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId AND type = :type ORDER BY date DESC")
    fun getNotificationsByType(userId: Int, type: String): Flow<List<NotificationEntity>>

    /**
     * Obtiene notificaciones de alta prioridad.
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId AND priority = 'HIGH' ORDER BY date DESC")
    fun getHighPriorityNotifications(userId: Int): Flow<List<NotificationEntity>>

    /**
     * Cuenta notificaciones no leídas por tipo.
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND type = :type AND isRead = 0")
    suspend fun getUnreadCountByType(userId: Int, type: String): Int

    // ==================== QUERIES PARA ENTRENADORES ====================

    /**
     * Obtiene notificaciones enviadas por un entrenador específico.
     */
    @Query("SELECT * FROM notifications WHERE userId = :clientId AND senderId = :trainerId ORDER BY date DESC")
    fun getNotificationsFromTrainer(clientId: Int, trainerId: Int): Flow<List<NotificationEntity>>

    /**
     * Cuenta solicitudes de clientes pendientes (para entrenadores).
     */
    @Query("""
        SELECT COUNT(*) FROM notifications 
        WHERE userId = :trainerId 
        AND type = 'CLIENT_REQUEST' 
        AND isRead = 0
    """)
    fun getPendingClientRequestCount(trainerId: Int): Flow<Int>

    // ==================== LIMPIEZA Y MANTENIMIENTO ====================

    /**
     * Elimina notificaciones antiguas (leídas y con más de X días).
     */
    @Query("DELETE FROM notifications WHERE userId = :userId AND isRead = 1 AND date < :thresholdTime")
    suspend fun deleteOldReadNotifications(userId: Int, thresholdTime: Long)

    /**
     * Elimina notificaciones expiradas.
     */
    @Query("DELETE FROM notifications WHERE expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun deleteExpiredNotifications(currentTime: Long = System.currentTimeMillis())

    /**
     * Elimina todas las notificaciones de un usuario.
     */
    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int)

    /**
     * Elimina notificaciones leídas de un usuario.
     */
    @Query("DELETE FROM notifications WHERE userId = :userId AND isRead = 1")
    suspend fun deleteReadNotifications(userId: Int)

    /**
     * Cuenta el total de notificaciones de un usuario.
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId")
    suspend fun countNotifications(userId: Int): Int

    // ==================== QUERIES DE BÚSQUEDA ====================

    /**
     * Busca notificaciones por contenido.
     */
    @Query("""
        SELECT * FROM notifications 
        WHERE userId = :userId 
        AND (LOWER(title) LIKE '%' || LOWER(:query) || '%' 
             OR LOWER(message) LIKE '%' || LOWER(:query) || '%')
        ORDER BY date DESC
    """)
    suspend fun searchNotifications(userId: Int, query: String): List<NotificationEntity>

    /**
     * Obtiene notificaciones en un rango de fechas.
     */
    @Query("""
        SELECT * FROM notifications 
        WHERE userId = :userId 
        AND date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getNotificationsInDateRange(userId: Int, startDate: Long, endDate: Long): Flow<List<NotificationEntity>>
}