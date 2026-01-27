package com.shagox.apptrainingnow.data.local.progress

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shagox.apptrainingnow.data.local.user.UserEntity

/**
 * Entidad que almacena las medidas corporales del usuario.
 * 
 * Permite el seguimiento histórico del progreso físico con medidas detalladas.
 * Cada registro representa un punto en el tiempo, permitiendo ver la evolución.
 * 
 * @property id Identificador único autoincremental
 * @property userId ID del usuario (FK → UserEntity)
 * @property measurementDate Fecha de la medición
 * @property weightKg Peso en kilogramos
 * @property bodyFatPercentage Porcentaje de grasa corporal
 * @property muscleMassKg Masa muscular en kg
 * @property waterPercentage Porcentaje de agua corporal
 * @property bmi Índice de Masa Corporal (calculado)
 * @property chestCm Circunferencia del pecho en cm
 * @property waistCm Circunferencia de cintura en cm
 * @property hipsCm Circunferencia de cadera en cm
 * @property rightArmCm Circunferencia brazo derecho en cm
 * @property leftArmCm Circunferencia brazo izquierdo en cm
 * @property rightThighCm Circunferencia muslo derecho en cm
 * @property leftThighCm Circunferencia muslo izquierdo en cm
 * @property rightCalfCm Circunferencia pantorrilla derecha en cm
 * @property leftCalfCm Circunferencia pantorrilla izquierda en cm
 * @property neckCm Circunferencia del cuello en cm
 * @property shouldersCm Circunferencia de hombros en cm
 * @property notes Notas adicionales
 * @property photoUrl URL de foto de progreso (opcional)
 */
@Entity(
    tableName = "body_measurements",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["measurementDate"]),
        Index(value = ["userId", "measurementDate"])
    ]
)
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val measurementDate: Long = System.currentTimeMillis(),
    
    // Métricas principales
    val weightKg: Double? = null,
    val bodyFatPercentage: Double? = null,
    val muscleMassKg: Double? = null,
    val waterPercentage: Double? = null,
    val bmi: Double? = null, // Se puede calcular si tenemos altura del usuario
    
    // Circunferencias (en centímetros)
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val rightArmCm: Double? = null,
    val leftArmCm: Double? = null,
    val rightThighCm: Double? = null,
    val leftThighCm: Double? = null,
    val rightCalfCm: Double? = null,
    val leftCalfCm: Double? = null,
    val neckCm: Double? = null,
    val shouldersCm: Double? = null,
    val forearmCm: Double? = null,
    
    // Adicional
    val notes: String? = null,
    val photoUrl: String? = null,
    val measuredBy: Int? = null, // ID del entrenador si fue él quien midió
    
    val createdAt: Long = System.currentTimeMillis()
)
