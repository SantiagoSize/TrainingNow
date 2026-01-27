package com.shagox.apptrainingnow.data.local.progress

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.local.workout.WorkoutSessionEntity

/**
 * Entidad que almacena los récords personales (PRs) del usuario.
 * 
 * Mantiene un historial de los mejores rendimientos en cada ejercicio,
 * permitiendo el seguimiento de la progresión de fuerza a lo largo del tiempo.
 * 
 * @property id Identificador único autoincremental
 * @property userId ID del usuario (FK → UserEntity)
 * @property exerciseId ID del ejercicio (FK → ExerciseEntity)
 * @property sessionId ID de la sesión donde se logró (FK → WorkoutSessionEntity)
 * @property recordType Tipo de récord: MAX_WEIGHT, MAX_REPS, MAX_VOLUME, FASTEST_TIME
 * @property value Valor del récord
 * @property unit Unidad (kg, reps, segundos, etc.)
 * @property reps Repeticiones (para récords de peso máximo)
 * @property previousValue Valor del récord anterior (para mostrar mejora)
 * @property achievedAt Timestamp cuando se logró
 * @property notes Notas sobre el récord
 * @property isCurrentRecord ¿Es el récord actual? (false si fue superado)
 */
@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["exerciseId"]),
        Index(value = ["sessionId"]),
        Index(value = ["userId", "exerciseId"]),
        Index(value = ["userId", "exerciseId", "recordType"]),
        Index(value = ["isCurrentRecord"])
    ]
)
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val exerciseId: Int,
    val sessionId: Int? = null,
    
    // Tipo y valor del récord
    val recordType: String = RecordType.MAX_WEIGHT.name,
    val value: Double,
    val unit: String = "kg",
    
    // Contexto del récord
    val reps: Int? = null,        // Para 1RM, 5RM, etc.
    val sets: Int? = null,        // Número de series
    val totalVolume: Double? = null, // peso × reps × sets
    
    // Comparación con récord anterior
    val previousValue: Double? = null,
    val improvementPercentage: Double? = null,
    
    // Estado
    val isCurrentRecord: Boolean = true,
    val verifiedByTrainer: Boolean = false,
    val trainerId: Int? = null,
    
    // Metadatos
    val achievedAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val videoUrl: String? = null, // Video del logro
    
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Tipos de récords personales.
 */
enum class RecordType {
    MAX_WEIGHT,     // Peso máximo levantado (1RM)
    MAX_REPS,       // Máximas repeticiones con un peso específico
    MAX_VOLUME,     // Volumen total máximo (peso × reps × sets)
    FASTEST_TIME,   // Tiempo más rápido
    LONGEST_TIME,   // Mayor tiempo sostenido
    MAX_DISTANCE,   // Mayor distancia
    BEST_FORM       // Mejor técnica (calificación del entrenador)
}
