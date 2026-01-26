package com.shagox.apptrainingnow.data.local.routine

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_exercises")
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routineId: Int,
    val exerciseId: Int,
    val dayOfWeek: Int,
    val orderIndex: Int,
    val notes: String? = null
)