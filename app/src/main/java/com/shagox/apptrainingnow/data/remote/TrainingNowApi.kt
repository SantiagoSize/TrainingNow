package com.shagox.apptrainingnow.data.remote

import com.shagox.apptrainingnow.data.remote.dto.ExerciseDto
import com.shagox.apptrainingnow.data.remote.dto.NotificationDto
import com.shagox.apptrainingnow.data.remote.dto.RoutineDto
import com.shagox.apptrainingnow.data.remote.dto.RoutineExerciseDto
import com.shagox.apptrainingnow.data.remote.dto.TrainerClientDto
import com.shagox.apptrainingnow.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interfaz Retrofit con los endpoints de la API trainingnowapi (Spring Boot).
 * Rutas base: /api/users, /api/exercises, /api/notifications, /api/routines.
 */
interface TrainingNowApi {

    // ==================== USUARIOS ====================

    @GET("api/users")
    suspend fun getUsers(): List<UserDto>

    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: Int): UserDto

    @GET("api/users/email/{email}")
    suspend fun getUserByEmail(@Path("email") email: String): UserDto

    @POST("api/users/login")
    suspend fun login(@Body body: Map<String, String>): Response<UserDto>

    @POST("api/users")
    suspend fun createUser(@Body user: UserDto): Response<UserDto>

    @PUT("api/users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: UserDto): Response<UserDto>

    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<Unit>

    @GET("api/users/trainers")
    suspend fun getTrainers(): List<UserDto>

    @GET("api/users/trainers/search")
    suspend fun searchTrainers(@Query("q") q: String): List<UserDto>

    @GET("api/users/clients")
    suspend fun getClients(): List<UserDto>

    @GET("api/users/clients/search")
    suspend fun searchClients(@Query("q") q: String): List<UserDto>

    // ==================== EJERCICIOS ====================

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

    // ==================== NOTIFICACIONES ====================

    @GET("api/notifications/{id}")
    suspend fun getNotificationById(@Path("id") id: Int): NotificationDto

    @GET("api/notifications/user/{userId}")
    suspend fun getNotificationsByUser(@Path("userId") userId: Int): List<NotificationDto>

    @POST("api/notifications")
    suspend fun createNotification(@Body notification: NotificationDto): Response<NotificationDto>

    @PUT("api/notifications/{id}")
    suspend fun updateNotification(@Path("id") id: Int, @Body notification: NotificationDto): Response<NotificationDto>

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: Int): Response<NotificationDto>

    @DELETE("api/notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): Response<Unit>

    // ==================== RUTINAS ====================

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

    // ==================== TRAINER-CLIENTS ====================

    @GET("api/trainer-clients/trainer/{trainerId}")
    suspend fun getTrainerClientsByTrainer(@Path("trainerId") trainerId: Int): List<TrainerClientDto>

    @GET("api/trainer-clients/trainer/{trainerId}/status/{status}")
    suspend fun getTrainerClientsByTrainerAndStatus(
        @Path("trainerId") trainerId: Int,
        @Path("status") status: String
    ): List<TrainerClientDto>

    @GET("api/trainer-clients/client/{clientId}")
    suspend fun getTrainerClientsByClient(@Path("clientId") clientId: Int): List<TrainerClientDto>

    @POST("api/trainer-clients")
    suspend fun createTrainerClient(@Body body: TrainerClientDto): Response<TrainerClientDto>
}
