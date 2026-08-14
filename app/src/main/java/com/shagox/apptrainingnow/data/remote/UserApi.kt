package com.shagox.apptrainingnow.data.remote

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
 * API del microservicio user-service (puerto 8081).
 * Incluye: usuarios, trainer-clients, notificaciones.
 */
interface UserApi {

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

    /** Creación de usuarios con privilegios: requiere token JWT de admin (interceptor lo agrega). */
    @POST("api/users/admin-create")
    suspend fun createUserByAdmin(@Body user: UserDto): Response<UserDto>

    // ==================== Sanciones (token de admin vía interceptor) ====================

    @PATCH("api/users/{id}/ban")
    suspend fun banUser(@Path("id") id: Int, @Body body: Map<String, String>): Response<UserDto>

    @PATCH("api/users/{id}/unban")
    suspend fun unbanUser(@Path("id") id: Int): Response<UserDto>

    @PATCH("api/users/{id}/suspend")
    suspend fun suspendUser(@Path("id") id: Int, @Body body: Map<String, Any>): Response<UserDto>

    @PATCH("api/users/{id}/unsuspend")
    suspend fun unsuspendUser(@Path("id") id: Int): Response<UserDto>

    @PUT("api/users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: UserDto): Response<UserDto>

    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<Unit>

    /** Ping de presencia: se llama periódicamente mientras la app está en primer plano. */
    @PATCH("api/users/{id}/heartbeat")
    suspend fun heartbeat(@Path("id") id: Int): Response<Unit>

    @GET("api/users/trainers")
    suspend fun getTrainers(): List<UserDto>

    @GET("api/users/trainers/search")
    suspend fun searchTrainers(@Query("q") q: String): List<UserDto>

    @GET("api/users/clients")
    suspend fun getClients(): List<UserDto>

    @GET("api/users/clients/search")
    suspend fun searchClients(@Query("q") q: String): List<UserDto>

    // Trainer-clients (mismo microservicio 8081)
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


    // ==================== Recuperación de contraseña ====================

    @POST("api/users/password-reset/request")
    suspend fun requestPasswordReset(@Body body: Map<String, String>): Response<Map<String, String>>

    @POST("api/users/password-reset/verify")
    suspend fun verifyPasswordReset(@Body body: Map<String, String>): Response<Map<String, String>>

    @POST("api/users/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body body: Map<String, String>): Response<Map<String, String>>
}
