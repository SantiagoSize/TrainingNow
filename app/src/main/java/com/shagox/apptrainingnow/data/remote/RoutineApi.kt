package com.shagox.apptrainingnow.data.remote

import com.shagox.apptrainingnow.data.remote.dto.RoutineDto
import com.shagox.apptrainingnow.data.remote.dto.RoutineExerciseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * API del microservicio routine-service (puerto 8083).
 */
interface RoutineApi {

    @GET("api/routines")
    suspend fun getRoutines(): List<RoutineDto>

    @GET("api/routines/{id}")
    suspend fun getRoutineById(@Path("id") id: Int): RoutineDto

    @GET("api/routines/{id}/exercises")
    suspend fun getRoutineExercises(@Path("id") id: Int): List<RoutineExerciseDto>

    @GET("api/routines/owner/{ownerId}")
    suspend fun getRoutinesByOwner(@Path("ownerId") ownerId: Int): List<RoutineDto>

    @GET("api/routines/creator/{creatorId}")
    suspend fun getRoutinesByCreator(@Path("creatorId") creatorId: Int): List<RoutineDto>

    @GET("api/routines/public")
    suspend fun getPublicRoutines(): List<RoutineDto>

    @POST("api/routines")
    suspend fun createRoutine(@Body routine: RoutineDto): Response<RoutineDto>

    @PUT("api/routines/{id}")
    suspend fun updateRoutine(@Path("id") id: Int, @Body routine: RoutineDto): Response<RoutineDto>

    @POST("api/routines/{id}/exercises")
    suspend fun setRoutineExercises(@Path("id") id: Int, @Body exercises: List<RoutineExerciseDto>): Response<Unit>

    @DELETE("api/routines/{id}")
    suspend fun deleteRoutine(@Path("id") id: Int): Response<Unit>
}
