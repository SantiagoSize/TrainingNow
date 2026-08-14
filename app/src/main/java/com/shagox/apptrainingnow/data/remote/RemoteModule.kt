package com.shagox.apptrainingnow.data.remote

import com.shagox.apptrainingnow.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Módulo de conexión con los 4 microservicios TrainingNow (Spring Boot).
 *
 * Mapa de puertos (alineado al backend):
 * - 8081 tn-usuarios        → usuarios, login, trainer-clients
 * - 8082 tn-biblioteca      → ejercicios
 * - 8083 tn-rutinas         → rutinas + sesiones de entrenamiento (workouts)
 * - 8084 tn-comunicaciones  → notificaciones
 *
 * El host se configura en gradle.properties (API_HOST):
 * - 10.0.2.2 = emulador (localhost del PC)
 * - 192.168.x.x = IP del PC en red WiFi para dispositivo físico
 */
object RemoteModule {

    /** Token JWT de la sesión actual (se setea al hacer login, se limpia al hacer logout). */
    @Volatile
    var authToken: String? = null

    private val HOST: String = BuildConfig.API_HOST
    private val USER_BASE_URL = "http://$HOST:8081/"
    private val EXERCISE_BASE_URL = "http://$HOST:8082/"
    private val ROUTINE_BASE_URL = "http://$HOST:8083/"
    private val WORKOUT_BASE_URL = "http://$HOST:8083/"      // workouts viven en tn-rutinas
    private val NOTIFICATION_BASE_URL = "http://$HOST:8084/" // tn-comunicaciones

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = authToken
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else chain.request()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val userRetrofit = retrofit(USER_BASE_URL)
    private val exerciseRetrofit = retrofit(EXERCISE_BASE_URL)
    private val routineRetrofit = retrofit(ROUTINE_BASE_URL)
    private val workoutRetrofit = retrofit(WORKOUT_BASE_URL)
    private val notificationRetrofit = retrofit(NOTIFICATION_BASE_URL)

    fun userApi(): UserApi = userRetrofit.create(UserApi::class.java)
    fun exerciseApi(): ExerciseApi = exerciseRetrofit.create(ExerciseApi::class.java)
    fun routineApi(): RoutineApi = routineRetrofit.create(RoutineApi::class.java)
    fun workoutApi(): WorkoutApi = workoutRetrofit.create(WorkoutApi::class.java)
    fun attendanceApi(): AttendanceApi = routineRetrofit.create(AttendanceApi::class.java)
    fun notificationApi(): NotificationApi = notificationRetrofit.create(NotificationApi::class.java)
    fun chatApi(): ChatApi = notificationRetrofit.create(ChatApi::class.java)

    /** Base URL de tn-comunicaciones, para armar la URL completa de un adjunto de chat (ej. NOTIFICATION_BASE_URL + "/uploads/chat/xxx.jpg"). */
    fun chatBaseUrl(): String = NOTIFICATION_BASE_URL

    /** Genérico por si se necesita otro servicio sobre alguna base URL. */
    fun <T> create(baseUrl: String, service: Class<T>): T = retrofit(baseUrl).create(service)
}
