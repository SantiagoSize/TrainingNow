package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.local.routine.RoutineDao
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val routineDao: RoutineDao) {

    // Obtener mis rutinas (Propias + Globales)
    fun getMyRoutines(userId: Int): Flow<List<RoutineEntity>> {
        return routineDao.getMyRoutines(userId)
    }

    // Obtener los ejercicios de una rutina específica (Para cuando le des click)
    fun getExercisesForRoutine(routineId: Int): Flow<List<ExerciseEntity>> {
        return routineDao.getExercisesForRoutine(routineId)
    }
}