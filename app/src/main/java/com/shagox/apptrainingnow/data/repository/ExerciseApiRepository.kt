package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.exercise.CategoryCount
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.ExerciseApi
import com.shagox.apptrainingnow.data.remote.dto.ExerciseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class ExerciseApiRepository(
    private val api: ExerciseApi = RemoteModule.exerciseApi()
) : IExerciseRepository {

    override fun getAllExercises(): Flow<List<ExerciseEntity>> = flow {
        emit(api.getExercises().map { it.toEntity() })
    }.catch { emit(emptyList()) }

    /**
     * Categorías reales (GET /api/categories): incluye categorías recién creadas sin
     * ejercicios todavía. Antes esto se calculaba agrupando los ejercicios existentes, por lo
     * que una categoría vacía era imposible de representar.
     */
    override fun getCategoryStats(): Flow<List<CategoryCount>> = flow {
        val categories = api.getCategories()
        emit(categories.map { CategoryCount(it.name, it.exerciseCount) }.sortedBy { it.category })
    }.catch { emit(emptyList()) }

    /** Crear categoría vacía: POST /api/categories (requiere token de admin). */
    override suspend fun createCategory(name: String) {
        val response = api.createCategory(mapOf("name" to name))
        if (!response.isSuccessful) throw Exception(categoryError(response.errorBody()?.string(), response.code()))
    }

    /** Renombrar categoría: PUT /api/categories/{oldName} (requiere token de admin). */
    override suspend fun renameCategory(oldName: String, newName: String) {
        val response = api.renameCategory(oldName, mapOf("name" to newName))
        if (!response.isSuccessful) throw Exception(categoryError(response.errorBody()?.string(), response.code()))
    }

    /** Eliminar categoría (y sus ejercicios): DELETE /api/categories/{name} (requiere token de admin). */
    override suspend fun deleteCategory(name: String) {
        val response = api.deleteCategory(name)
        if (!response.isSuccessful) throw Exception(categoryError(response.errorBody()?.string(), response.code()))
    }

    private fun categoryError(cuerpoError: String?, code: Int): String {
        val mensaje = Regex("\"error\":\"([^\"]*)\"").find(cuerpoError.orEmpty())?.groupValues?.get(1)
        return mensaje ?: adminError(code)
    }

    override fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>> = flow {
        emit(api.getExercisesByCategory(category).map { it.toEntity() })
    }.catch { emit(emptyList()) }

    override fun observeExercise(id: Int): Flow<ExerciseEntity?> = flow {
        try {
            emit(api.getExerciseById(id).toEntity())
        } catch (e: HttpException) {
            if (e.code() == 404) emit(null) else throw e
        }
    }.catch { emit(null) }

    override suspend fun insertExercises(exercises: List<ExerciseEntity>) {
        for (e in exercises) {
            val response = api.createExercise(e.toDto())
            if (!response.isSuccessful) throw HttpException(response)
        }
    }

    /** Crear ejercicio: POST /api/exercises (requiere token de admin). */
    override suspend fun createExercise(exercise: ExerciseEntity) {
        val response = api.createExercise(exercise.toDto())
        if (!response.isSuccessful) throw Exception(adminError(response.code()))
    }

    /** Editar ejercicio: PUT /api/exercises/{id} (requiere token de admin). */
    override suspend fun updateExercise(exercise: ExerciseEntity) {
        val response = api.updateExercise(exercise.id, exercise.toDto())
        if (!response.isSuccessful) throw Exception(adminError(response.code()))
    }

    /** Eliminar ejercicio: DELETE /api/exercises/{id} (requiere token de admin). */
    override suspend fun deleteExercise(exerciseId: Int) {
        val response = api.deleteExercise(exerciseId)
        if (!response.isSuccessful) throw Exception(adminError(response.code()))
    }

    private fun adminError(code: Int): String = when (code) {
        403 -> "Solo un administrador con sesión activa puede modificar la biblioteca"
        404 -> "Ejercicio no encontrado"
        else -> "Error del servidor ($code)"
    }

    private fun ExerciseDto.toEntity(): ExerciseEntity = ExerciseEntity(
        id = id,
        name = name,
        category = category,
        description = description.orEmpty(),
        videoUrl = videoUrl.orEmpty(),
        imageUrl = imageUrl,
        muscles = muscles,
        difficulty = difficulty,
        equipment = equipment,
        alternatives = alternatives,
        instructions = instructions,
        tips = tips,
        commonMistakes = commonMistakes,
        recommendedSets = recommendedSets,
        recommendedReps = recommendedReps,
        restSeconds = restSeconds,
        isSystemDefault = isSystemDefault
    )

    private fun ExerciseEntity.toDto(): ExerciseDto = ExerciseDto(
        id = id,
        name = name,
        category = category,
        description = description,
        videoUrl = videoUrl,
        imageUrl = imageUrl,
        muscles = muscles,
        difficulty = difficulty,
        equipment = equipment,
        alternatives = alternatives,
        instructions = instructions,
        tips = tips,
        commonMistakes = commonMistakes,
        recommendedSets = recommendedSets,
        recommendedReps = recommendedReps,
        restSeconds = restSeconds,
        isSystemDefault = isSystemDefault
    )
}
