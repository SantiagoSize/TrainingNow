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

    override fun getCategoryStats(): Flow<List<CategoryCount>> = flow {
        val exercises = api.getExercises()
        val byCategory = exercises.groupBy { it.category }
        emit(byCategory.map { (category, list) -> CategoryCount(category, list.size) }.sortedByDescending { it.count })
    }.catch { emit(emptyList()) }

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
        instructions = instructions,
        tips = tips,
        commonMistakes = commonMistakes,
        recommendedSets = recommendedSets,
        recommendedReps = recommendedReps,
        restSeconds = restSeconds,
        isSystemDefault = isSystemDefault
    )
}
