package com.shagox.apptrainingnow.data.remote

import com.shagox.apptrainingnow.data.remote.dto.ExerciseLogDto
import com.shagox.apptrainingnow.data.remote.dto.WorkoutSessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * API del microservicio workout-service (puerto 8084).
 * Sesiones de entrenamiento y logs de ejercicios.
 */
interface WorkoutApi {

    @GET("api/workouts/sessions/{id}")
    suspend fun getSessionById(@Path("id") id: Int): WorkoutSessionDto

    @GET("api/workouts/sessions/user/{userId}")
    suspend fun getSessionsByUser(@Path("userId") userId: Int): List<WorkoutSessionDto>

    @GET("api/workouts/sessions/user/{userId}/status/{status}")
    suspend fun getSessionsByUserAndStatus(
        @Path("userId") userId: Int,
        @Path("status") status: String
    ): List<WorkoutSessionDto>

    @GET("api/workouts/sessions/{sessionId}/logs")
    suspend fun getLogsBySession(@Path("sessionId") sessionId: Int): List<ExerciseLogDto>

    @POST("api/workouts/sessions")
    suspend fun createSession(@Body session: WorkoutSessionDto): Response<WorkoutSessionDto>

    @PUT("api/workouts/sessions/{id}")
    suspend fun updateSession(@Path("id") id: Int, @Body session: WorkoutSessionDto): Response<WorkoutSessionDto>

    @POST("api/workouts/logs")
    suspend fun createLog(@Body log: ExerciseLogDto): Response<ExerciseLogDto>

    @DELETE("api/workouts/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: Int): Response<Unit>
}
