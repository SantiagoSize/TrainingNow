package com.shagox.apptrainingnow.data.local.progress

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shagox.apptrainingnow.data.local.user.UserEntity

/**
 * Entidad que representa los objetivos de entrenamiento del usuario.
 * 
 * Permite definir y seguir metas específicas, medibles y con fecha límite.
 * Los objetivos pueden ser creados por el usuario o asignados por el entrenador.
 * 
 * @property id Identificador único autoincremental
 * @property userId ID del usuario dueño del objetivo (FK → UserEntity)
 * @property createdByTrainerId ID del entrenador que creó el objetivo (null si lo creó el usuario)
 * @property title Título descriptivo del objetivo
 * @property description Descripción detallada
 * @property category Categoría: WEIGHT_LOSS, MUSCLE_GAIN, STRENGTH, ENDURANCE, FLEXIBILITY, CUSTOM
 * @property targetValue Valor objetivo numérico (ej: 75 kg)
 * @property currentValue Valor actual
 * @property unit Unidad de medida (kg, %, reps, km, etc.)
 * @property startDate Fecha de inicio
 * @property targetDate Fecha objetivo para lograr la meta
 * @property completedDate Fecha en que se completó (null si no está completado)
 * @property status Estado: ACTIVE, COMPLETED, ABANDONED, PAUSED
 * @property priority Prioridad: HIGH, MEDIUM, LOW
 * @property milestones Hitos intermedios (JSON string)
 * @property reminderEnabled ¿Recordatorios activos?
 * @property reminderFrequency Frecuencia de recordatorios
 */
@Entity(
    tableName = "goals",
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
            childColumns = ["createdByTrainerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["createdByTrainerId"]),
        Index(value = ["status"]),
        Index(value = ["category"]),
        Index(value = ["userId", "status"])
    ]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val createdByTrainerId: Int? = null,
    
    // Información del objetivo
    val title: String,
    val description: String? = null,
    val category: String = GoalCategory.CUSTOM.name,
    
    // Valores de seguimiento
    val targetValue: Double? = null,
    val currentValue: Double? = null,
    val startValue: Double? = null,
    val unit: String? = null,
    
    // Fechas
    val startDate: Long = System.currentTimeMillis(),
    val targetDate: Long? = null,
    val completedDate: Long? = null,
    
    // Estado y prioridad
    val status: String = GoalStatus.ACTIVE.name,
    val priority: String = GoalPriority.MEDIUM.name,
    
    // Hitos y progreso
    val milestones: String? = null, // JSON: [{"value": 80, "date": 123456, "reached": true}]
    val progressPercentage: Double = 0.0,
    
    // Recordatorios
    val reminderEnabled: Boolean = false,
    val reminderFrequency: String? = null, // DAILY, WEEKLY, CUSTOM
    
    // Notas y feedback
    val notes: String? = null,
    val trainerFeedback: String? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Categorías de objetivos disponibles.
 */
enum class GoalCategory {
    WEIGHT_LOSS,      // Pérdida de peso
    MUSCLE_GAIN,      // Ganancia muscular
    STRENGTH,         // Fuerza (PRs)
    ENDURANCE,        // Resistencia cardiovascular
    FLEXIBILITY,      // Flexibilidad
    BODY_COMPOSITION, // Composición corporal
    HABIT,            // Hábitos (entrenar X veces por semana)
    NUTRITION,        // Objetivos nutricionales
    CUSTOM            // Personalizado
}

/**
 * Estados de un objetivo.
 */
enum class GoalStatus {
    ACTIVE,     // En progreso
    COMPLETED,  // Logrado
    ABANDONED,  // Abandonado
    PAUSED,     // Pausado temporalmente
    OVERDUE     // Vencido sin completar
}

/**
 * Niveles de prioridad.
 */
enum class GoalPriority {
    HIGH,
    MEDIUM,
    LOW
}
