package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.exercise.CategoryCount
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import kotlinx.coroutines.flow.Flow

interface IExerciseRepository {
    fun getAllExercises(): Flow<List<ExerciseEntity>>
    fun getCategoryStats(): Flow<List<CategoryCount>>
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>
    fun observeExercise(id: Int): Flow<ExerciseEntity?>
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
}
