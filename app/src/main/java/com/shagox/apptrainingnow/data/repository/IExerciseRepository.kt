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

    /** Crear ejercicio (solo admin, validado por el backend). */
    suspend fun createExercise(exercise: ExerciseEntity) = insertExercises(listOf(exercise))

    /** Editar ejercicio (solo admin). */
    suspend fun updateExercise(exercise: ExerciseEntity) { }

    /** Eliminar ejercicio (solo admin). */
    suspend fun deleteExercise(exerciseId: Int) { }

    /** Crear una categoría nueva, vacía (sin ejercicios todavía). Solo admin. */
    suspend fun createCategory(name: String) { }

    /** Renombrar una categoría: actualiza también todos los ejercicios que la usan. Solo admin. */
    suspend fun renameCategory(oldName: String, newName: String) { }

    /** Eliminar una categoría junto con todos sus ejercicios. Solo admin. */
    suspend fun deleteCategory(name: String) { }
}
