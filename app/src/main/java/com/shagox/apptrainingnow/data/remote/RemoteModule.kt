package com.shagox.apptrainingnow.data.remote

import com.shagox.apptrainingnow.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Módulo de conexión con los 4 microservicios TrainingNow (Spring Boot).
 * Cada servicio tiene su propia URL base y cliente Retrofit.
 *
 * El host se configura en gradle.properties (API_HOST):
 * - 10.0.2.2 = emulador (localhost del PC)
 * - 192.168.x.x = IP del PC en red WiFi para dispositivo físico
 */
object RemoteModule {

    private val HOST: String = BuildConfig.API_HOST
    private val USER_BASE_URL = "http://$HOST:8081/"
    private val EXERCISE_BASE_URL = "http://$HOST:8082/"
    private val ROUTINE_BASE_URL = "http://$HOST:8083/"
    private val WORKOUT_BASE_URL = "http://$HOST:8084/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
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

    fun userApi(): UserApi = userRetrofit.create(UserApi::class.java)
    fun exerciseApi(): ExerciseApi = exerciseRetrofit.create(ExerciseApi::class.java)
    fun routineApi(): RoutineApi = routineRetrofit.create(RoutineApi::class.java)
    fun workoutApi(): WorkoutApi = workoutRetrofit.create(WorkoutApi::class.java)

    /** Genérico por si se necesita otro servicio sobre alguna base URL. */
    fun <T> create(baseUrl: String, service: Class<T>): T = retrofit(baseUrl).create(service)
}
