package com.shagox.apptrainingnow.data.local.workout

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.local.user.UserEntity

/**
 * Entidad que representa una sesión de entrenamiento realizada.
 * 
 * Registra cada vez que un usuario completa (o intenta) una rutina.
 * Permite el seguimiento histórico del progreso del usuario.
 * 
 * @property id Identificador único autoincremental
 * @property userId ID del usuario que realizó la sesión (FK → UserEntity)
 * @property routineId ID de la rutina ejecutada (FK → RoutineEntity, nullable para entrenamientos libres)
 * @property startTime Timestamp de inicio de la sesión
 * @property endTime Timestamp de fin de la sesión (null si en progreso)
 * @property status Estado: IN_PROGRESS, COMPLETED, ABANDONED
 * @property totalDurationMinutes Duración total en minutos
 * @property caloriesBurned Calorías quemadas estimadas
 * @property notes Notas del usuario sobre la sesión
 * @property rating Calificación del usuario (1-5)
 * @property perceivedDifficulty Dificultad percibida (1-10, escala RPE)
 * @property mood Estado de ánimo antes del entrenamiento
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["routineId"]),
        Index(value = ["startTime"]),
        Index(value = ["status"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val routineId: Int? = null,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val status: String = WorkoutSessionStatus.IN_PROGRESS.name,
    val totalDurationMinutes: Int? = null,
    val caloriesBurned: Int? = null,
    val notes: String? = null,
    val rating: Int? = null,
    val perceivedDifficulty: Int? = null,
    val mood: String? = null,
    val location: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Estados posibles de una sesión de entrenamiento.
 */
enum class WorkoutSessionStatus {
    /** Sesión en progreso */
    IN_PROGRESS,
    /** Sesión completada exitosamente */
    COMPLETED,
    /** Sesión abandonada antes de completar */
    ABANDONED,
    /** Sesión programada para el futuro */
    SCHEDULED
}

/**
 * Estados de ánimo para registrar antes del entrenamiento.
 */
enum class WorkoutMood {
    ENERGIZED,
    NORMAL,
    TIRED,
    STRESSED,
    MOTIVATED,
    UNMOTIVATED
}
