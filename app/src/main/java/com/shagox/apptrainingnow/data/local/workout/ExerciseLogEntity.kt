package com.shagox.apptrainingnow.data.local.workout

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity

/**
 * Entidad que registra el detalle de cada ejercicio realizado en una sesión.
 * 
 * Permite un seguimiento granular del progreso:
 * - Series completadas vs planificadas
 * - Peso utilizado
 * - Repeticiones por serie
 * - Tiempo de descanso
 * - Notas específicas del ejercicio
 * 
 * @property id Identificador único autoincremental
 * @property sessionId ID de la sesión de entrenamiento (FK → WorkoutSessionEntity)
 * @property exerciseId ID del ejercicio realizado (FK → ExerciseEntity)
 * @property orderInSession Orden del ejercicio en la sesión
 * @property plannedSets Series planificadas
 * @property completedSets Series completadas
 * @property plannedReps Repeticiones planificadas por serie
 * @property actualReps Lista de reps reales por serie (JSON string: "[12,10,8]")
 * @property weightKg Peso utilizado en kg
 * @property restTimeSeconds Tiempo de descanso entre series
 * @property durationSeconds Duración total del ejercicio
 * @property notes Notas específicas del ejercicio
 * @property rpe Rating de Esfuerzo Percibido (1-10)
 * @property isPersonalRecord ¿Es un récord personal?
 */
@Entity(
    tableName = "exercise_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"]),
        Index(value = ["sessionId", "exerciseId"])
    ]
)
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int,
    val exerciseId: Int,
    val orderInSession: Int = 0,
    
    // Planificación
    val plannedSets: Int = 3,
    val plannedReps: Int = 12,
    val plannedWeightKg: Double? = null,
    
    // Ejecución real
    val completedSets: Int = 0,
    val actualReps: String? = null, // JSON array: "[12, 10, 8]"
    val weightKg: Double? = null,
    
    // Métricas de tiempo
    val restTimeSeconds: Int = 60,
    val durationSeconds: Int? = null,
    
    // Feedback
    val notes: String? = null,
    val rpe: Int? = null, // Rating of Perceived Exertion (1-10)
    val isPersonalRecord: Boolean = false,
    
    // Técnica
    val formRating: Int? = null, // 1-5 estrellas
    val tempo: String? = null, // Ej: "3-1-2-0" (excéntrico-pausa-concéntrico-arriba)
    
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Valor de [notes] usado como marca cuando un ejercicio se marcó "terminado" en el
         * checklist sin que el usuario haya registrado series (reps/carga) manualmente. Permite
         * que el detalle del día en el reporte mensual lo muestre como completado en vez de
         * ignorarlo (antes solo se mostraban ejercicios con series reales).
         */
        const val NOTA_TERMINADO_SIN_SERIE = "terminado_sin_serie"
    }
}
