package com.shagox.apptrainingnow.data.local.exercise

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ejercicio de la biblioteca.
 * imageUrl admite URL pública o data URI comprimido (JPEG base64).
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val description: String,
    val videoUrl: String,
    val imageUrl: String? = null,
    val muscles: String? = null,
    val difficulty: String? = "PRINCIPIANTE",
    val equipment: String? = null,
    /** Formas alternativas de hacerlo (ej. "Mancuernas, Barra, Máquina"), separadas por coma. */
    val alternatives: String? = null,
    /** Pasos de ejecución separados por "|". */
    val instructions: String? = null,
    /** Consejos de técnica separados por "|". */
    val tips: String? = null,
    /** Errores comunes separados por "|". */
    val commonMistakes: String? = null,
    val recommendedSets: Int? = null,
    val recommendedReps: String? = null,
    val restSeconds: Int? = null,
    val isSystemDefault: Boolean = true
)
