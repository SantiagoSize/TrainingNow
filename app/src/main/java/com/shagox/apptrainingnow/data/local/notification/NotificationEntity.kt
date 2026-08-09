package com.shagox.apptrainingnow.data.local.notification

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shagox.apptrainingnow.data.local.user.UserEntity

/**
 * Entidad que representa las notificaciones del sistema.
 * 
 * Almacena notificaciones push, recordatorios y alertas para los usuarios.
 * Soporta diferentes tipos de notificaciones con acciones opcionales.
 * 
 * @property id Identificador único autoincremental
 * @property userId ID del usuario destinatario (FK → UserEntity)
 * @property title Título de la notificación
 * @property message Mensaje/contenido de la notificación
 * @property type Tipo de notificación: REMINDER, MESSAGE, ACHIEVEMENT, SYSTEM, TRAINER_FEEDBACK
 * @property date Fecha de creación
 * @property isRead ¿Ha sido leída?
 * @property actionType Tipo de acción al hacer clic (opcional)
 * @property actionData Datos para la acción (ej: ID de chat, ID de rutina)
 * @property priority Prioridad: HIGH, NORMAL, LOW
 * @property expiresAt Fecha de expiración (opcional)
 * @property senderId ID del remitente si aplica (ej: entrenador)
 */
@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["senderId"]),
        Index(value = ["isRead"]),
        Index(value = ["type"]),
        Index(value = ["userId", "isRead"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Int = 0,
    val userId: Int,
    val title: String,
    val message: String,
    val type: String = NotificationType.SYSTEM.name,
    val date: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    
    // Acciones
    val actionType: String? = null, // OPEN_CHAT, OPEN_ROUTINE, OPEN_GOAL, etc.
    val actionData: String? = null, // JSON o ID relacionado
    
    // Prioridad y expiración
    val priority: String = NotificationPriority.NORMAL.name,
    val expiresAt: Long? = null,
    
    // Remitente (para notificaciones de entrenadores)
    val senderId: Int? = null,
    
    // Metadatos
    val iconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Tipos de notificaciones disponibles.
 */
enum class NotificationType {
    /** Recordatorio de entrenamiento */
    REMINDER,
    /** Nuevo mensaje en chat */
    MESSAGE,
    /** Logro desbloqueado */
    ACHIEVEMENT,
    /** Notificación del sistema */
    SYSTEM,
    /** Feedback del entrenador */
    TRAINER_FEEDBACK,
    /** Nueva rutina asignada */
    ROUTINE_ASSIGNED,
    /** Objetivo actualizado */
    GOAL_UPDATE,
    /** Récord personal */
    PERSONAL_RECORD,
    /** Solicitud de cliente */
    CLIENT_REQUEST,
    /** Promoción o información */
    PROMOTIONAL
}

/**
 * Niveles de prioridad de notificaciones.
 */
enum class NotificationPriority {
    HIGH,
    NORMAL,
    LOW
}

/**
 * Tipos de acciones al hacer clic en la notificación.
 */
enum class NotificationAction {
    OPEN_CHAT,
    OPEN_ROUTINE,
    OPEN_GOAL,
    OPEN_PROFILE,
    OPEN_WORKOUT,
    OPEN_CLIENT_LIST,
    OPEN_URL,
    NONE
}