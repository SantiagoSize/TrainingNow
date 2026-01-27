package com.shagox.apptrainingnow.data.local.trainer

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.shagox.apptrainingnow.data.local.user.UserEntity

/**
 * Entidad que representa la relación Entrenador-Cliente.
 * 
 * Esta tabla intermedia permite:
 * - Un entrenador puede tener múltiples clientes
 * - Un cliente puede tener múltiples entrenadores
 * - Seguimiento del estado de la suscripción/relación
 * - Historial de notas y observaciones del entrenador
 * 
 * @property trainerId ID del entrenador (FK → UserEntity)
 * @property clientId ID del cliente (FK → UserEntity)
 * @property status Estado de la relación: PENDING, ACTIVE, PAUSED, CANCELLED
 * @property startDate Fecha de inicio de la relación (timestamp)
 * @property endDate Fecha de finalización (null si está activa)
 * @property trainerNotes Notas privadas del entrenador sobre el cliente
 * @property clientGoals Objetivos que el cliente comunicó al entrenador
 * @property sessionPrice Precio por sesión acordado (opcional)
 * @property sessionsPerWeek Cantidad de sesiones semanales acordadas
 */
@Entity(
    tableName = "trainer_clients",
    primaryKeys = ["trainerId", "clientId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["trainerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trainerId"]),
        Index(value = ["clientId"]),
        Index(value = ["status"])
    ]
)
data class TrainerClientEntity(
    val trainerId: Int,
    val clientId: Int,
    val status: String = TrainerClientStatus.PENDING.name,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val trainerNotes: String? = null,
    val clientGoals: String? = null,
    val sessionPrice: Double? = null,
    val sessionsPerWeek: Int = 3,
    val lastInteractionDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Estados posibles de la relación Entrenador-Cliente.
 */
enum class TrainerClientStatus {
    /** Solicitud pendiente de aprobación */
    PENDING,
    /** Relación activa */
    ACTIVE,
    /** Relación pausada temporalmente */
    PAUSED,
    /** Relación cancelada/finalizada */
    CANCELLED
}
