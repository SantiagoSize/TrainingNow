package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.exercise.CategoryCount
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.ExerciseApi
import com.shagox.apptrainingnow.data.remote.dto.ExerciseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class ExerciseApiRepository(
    private val api: ExerciseApi = RemoteModule.exerciseApi()
) : IExerciseRepository {

    override fun getAllExercises(): Flow<List<ExerciseEntity>> = flow {
        emit(api.getExercises().map { it.toEntity() })
    }

    override fun getCategoryStats(): Flow<List<CategoryCount>> = flow {
        val exercises = api.getExercises()
        val byCategory = exercises.groupBy { it.category }
        emit(byCategory.map { (category, list) -> CategoryCount(category, list.size) }.sortedByDescending { it.count })
    }

    override fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>> = flow {
        emit(api.getExercisesByCategory(category).map { it.toEntity() })
    }

    override fun observeExercise(id: Int): Flow<ExerciseEntity?> = flow {
        try {
            emit(api.getExerciseById(id).toEntity())
        } catch (e: HttpException) {
            if (e.code() == 404) emit(null) else throw e
        }
    }

    override suspend fun insertExercises(exercises: List<ExerciseEntity>) {
        for (e in exercises) {
            val response = api.createExercise(e.toDto())
            if (!response.isSuccessful) throw HttpException(response)
        }
    }

    private fun ExerciseDto.toEntity(): ExerciseEntity = ExerciseEntity(
        id = id,
        name = name,
        category = category,
        description = description.orEmpty(),
        videoUrl = videoUrl.orEmpty(),
        isSystemDefault = isSystemDefault
    )

    private fun ExerciseEntity.toDto(): ExerciseDto = ExerciseDto(
        id = id,
        name = name,
        category = category,
        description = description,
        videoUrl = videoUrl,
        isSystemDefault = isSystemDefault
    )
}
