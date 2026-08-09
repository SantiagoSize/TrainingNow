package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.exercise.CategoryCount
import com.shagox.apptrainingnow.data.local.exercise.ExerciseDao
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val exerciseDao: ExerciseDao) : IExerciseRepository {

    override fun getAllExercises(): Flow<List<ExerciseEntity>> {
        return exerciseDao.getAllExercises()
    }

    override fun getCategoryStats(): Flow<List<CategoryCount>> {
        return exerciseDao.getCategoryStats()
    }

    override fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>> {
        return exerciseDao.getExercisesByCategory(category)
    }

    override fun observeExercise(id: Int): Flow<ExerciseEntity?> {
        return exerciseDao.observeExercise(id)
    }

    override suspend fun insertExercises(exercises: List<ExerciseEntity>) {
        exerciseDao.insertExercises(exercises)
    }
}