package com.shagox.apptrainingnow.data.remote

import com.shagox.apptrainingnow.data.remote.dto.ExerciseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API del microservicio exercise-service (puerto 8082).
 */
interface ExerciseApi {

    @GET("api/exercises")
    suspend fun getExercises(): List<ExerciseDto>

    @GET("api/exercises/{id}")
    suspend fun getExerciseById(@Path("id") id: Int): ExerciseDto

    @GET("api/exercises/category/{category}")
    suspend fun getExercisesByCategory(@Path("category") category: String): List<ExerciseDto>

    @GET("api/exercises/search")
    suspend fun searchExercises(@Query("q") q: String): List<ExerciseDto>

    @POST("api/exercises")
    suspend fun createExercise(@Body exercise: ExerciseDto): Response<ExerciseDto>

    @PUT("api/exercises/{id}")
    suspend fun updateExercise(@Path("id") id: Int, @Body exercise: ExerciseDto): Response<ExerciseDto>

    @DELETE("api/exercises/{id}")
    suspend fun deleteExercise(@Path("id") id: Int): Response<Unit>
}
