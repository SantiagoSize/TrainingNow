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
 * Host: se detecta solo en tiempo de ejecución (ver [detectarHost]), así el mismo build
 * sirve para emulador y teléfono físico sin tocar gradle.properties ni recompilar:
 * - Emulador Android → 10.0.2.2 (localhost del PC visto desde el emulador)
 * - Teléfono físico → 127.0.0.1 + "adb reverse" (funciona en cualquier PC por USB, ver
 *   gradle.properties para los comandos)
 * Si en gradle.properties (API_HOST) se deja un valor explícito distinto de esos dos
 * (por ejemplo una IP de red 192.168.x.x), se respeta tal cual en vez de autodetectar.
 */
object RemoteModule {

    /** Token JWT de la sesión actual (se setea al hacer login, se limpia al hacer logout). */
    @Volatile
    var authToken: String? = null

    private val HOST: String = detectarHost()

    /**
     * true si la app corre en un emulador Android. Build.HARDWARE = "goldfish"/"ranchu" es el
     * motor virtual del propio emulador (Google) y es el marcador más confiable — cubre tanto
     * las imágenes viejas ("google_sdk") como las actuales de Android Studio ("sdk_gphone...",
     * que NO contienen "generic" ni "google_sdk" y por eso las heurísticas viejas las pasaban
     * por alto).
     */
    private fun esEmulador(): Boolean {
        val hardware = android.os.Build.HARDWARE
        val b = android.os.Build.FINGERPRINT
        val modelo = android.os.Build.MODEL
        val producto = android.os.Build.PRODUCT
        return hardware.contains("goldfish") || hardware.contains("ranchu") ||
            b.startsWith("generic") || b.startsWith("unknown") ||
            modelo.contains("google_sdk") || modelo.contains("sdk_gphone") ||
            modelo.contains("Emulator") || modelo.contains("Android SDK built for") ||
            producto.contains("sdk_gphone") || producto.contains("sdk_gapps") ||
            android.os.Build.MANUFACTURER.contains("Genymotion") ||
            (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")) ||
            producto == "google_sdk"
    }

    private fun detectarHost(): String {
        val configurado = BuildConfig.API_HOST
        if (configurado != "10.0.2.2" && configurado != "127.0.0.1") return configurado
        return if (esEmulador()) "10.0.2.2" else "127.0.0.1"
    }
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
