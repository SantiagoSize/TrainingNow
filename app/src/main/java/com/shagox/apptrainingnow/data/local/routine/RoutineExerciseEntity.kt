package com.shagox.apptrainingnow.data.local.routine

import androidx.room.Entity
import androidx.room.ForeignKey
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity

/**
 * Ejercicio asignado a un día concreto de una rutina.
 */
@Entity(
    tableName = "routine_exercise",
    primaryKeys = ["dayId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("exerciseId")]
)
data class RoutineExerciseEntity(
    val dayId: Int,
    val exerciseId: Int,
    val order: Int
)
