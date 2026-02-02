package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.exercise.CategoryCount
import com.shagox.apptrainingnow.data.local.exercise.ExerciseDao
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val exerciseDao: ExerciseDao) {

    fun getAllExercises(): Flow<List<ExerciseEntity>> {
        return exerciseDao.getAllExercises()
    }

    fun getCategoryStats(): Flow<List<CategoryCount>> {
        return exerciseDao.getCategoryStats()
    }

    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>> {
        return exerciseDao.getExercisesByCategory(category)
    }

    fun observeExercise(id: Int): Flow<ExerciseEntity?> {
        return exerciseDao.observeExercise(id)
    }

    suspend fun insertExercises(exercises: List<ExerciseEntity>) {
        exerciseDao.insertExercises(exercises)
    }
}