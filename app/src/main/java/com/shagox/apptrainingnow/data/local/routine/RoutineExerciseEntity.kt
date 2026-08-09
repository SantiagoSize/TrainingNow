package com.shagox.apptrainingnow.data.local.routine

import androidx.room.Entity
import androidx.room.ForeignKey
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity

@Entity(
    tableName = "routine_exercise",
    primaryKeys = ["routineId", "exerciseId"],
    foreignKeys = [
        ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [androidx.room.Index("exerciseId")]
)
data class RoutineExerciseEntity(
    val routineId: Int,
    val exerciseId: Int,
    val order: Int
)