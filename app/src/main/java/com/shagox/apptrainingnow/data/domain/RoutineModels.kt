package com.shagox.apptrainingnow.data.domain

import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity

/**
 * Modelo de dominio: cabecera de una rutina (nombre global).
 * Refleja la jerarquía: Rutina → Días → Ejercicios (0-10 por día).
 */
data class RoutineHeader(
    val id: Int,
    val name: String,
    val ownerId: Int?,
    val creatorId: Int
)

/**
 * Un día de la rutina con su nombre, "Actividad de hoy" y lista de ejercicios.
 * Cada día tiene 0..10 ejercicios.
 */
data class RoutineDayView(
    val routineId: Int,
    val dayLabel: String,
    val activityName: String,
    val exercises: List<ExerciseEntity>,
    val exerciseCount: Int
) {
    /** Un día sin ejercicios es día de descanso, tenga o no nombre de sesión. */
    val esDescanso: Boolean get() = exercises.isEmpty()

    /** Texto a mostrar: el nombre de la sesión o "Descanso" si no tiene ejercicios. */
    val displayActivity: String
        get() = when {
            activityName.isNotBlank() -> activityName
            esDescanso -> "Descanso"
            else -> "-"
        }
}

/**
 * Rutina completa: cabecera + lista de días con sus ejercicios.
 * Usado para UI: Lista de días → Vista de detalle de ejercicios.
 */
data class RoutineWithDays(
    val header: RoutineHeader,
    val days: List<RoutineDayView>
) {
    val routineName: String get() = header.name
}

/** Límite máximo de ejercicios por día (requisito de negocio). */
const val MAX_EXERCISES_PER_DAY = 10
