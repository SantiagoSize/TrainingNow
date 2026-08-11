package com.shagox.apptrainingnow.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shagox.apptrainingnow.data.local.chat.ChatDao
import com.shagox.apptrainingnow.data.local.chat.MessageEntity
import com.shagox.apptrainingnow.data.local.exercise.ExerciseDao
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.local.notification.NotificationDao
import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import com.shagox.apptrainingnow.data.local.notification.NotificationType
import com.shagox.apptrainingnow.data.local.progress.BodyMeasurementEntity
import com.shagox.apptrainingnow.data.local.progress.GoalCategory
import com.shagox.apptrainingnow.data.local.progress.GoalEntity
import com.shagox.apptrainingnow.data.local.progress.PersonalRecordEntity
import com.shagox.apptrainingnow.data.local.progress.ProgressDao
import com.shagox.apptrainingnow.data.local.routine.RoutineDao
import com.shagox.apptrainingnow.data.local.routine.RoutineDayEntity
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.local.routine.RoutineExerciseEntity
import com.shagox.apptrainingnow.data.local.trainer.TrainerClientDao
import com.shagox.apptrainingnow.data.local.trainer.TrainerClientEntity
import com.shagox.apptrainingnow.data.local.trainer.TrainerClientStatus
import com.shagox.apptrainingnow.data.local.user.UserDao
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.local.workout.ExerciseLogEntity
import com.shagox.apptrainingnow.data.local.workout.WorkoutDao
import com.shagox.apptrainingnow.data.local.workout.WorkoutSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base de datos principal de la aplicación TrainingNow.
 * 
 * Implementa el patrón Singleton para garantizar una única instancia.
 * Utiliza Room Database con soporte para:
 * 
 * - Gestión de usuarios (clientes, entrenadores, administradores)
 * - Catálogo de ejercicios con categorías
 * - Rutinas de entrenamiento personalizadas y globales
 * - Sistema de mensajería/chat en tiempo real
 * - Notificaciones push y recordatorios
 * - Seguimiento de progreso (medidas corporales, objetivos, récords)
 * - Sesiones de entrenamiento con registro detallado
 * - Relaciones entrenador-cliente
 * 
 * @version 9 - Días de rutina con hora de recordatorio propia
 */
@Database(
    entities = [
        // Usuarios y autenticación
        UserEntity::class,
        
        // Ejercicios y rutinas
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineDayEntity::class,
        RoutineExerciseEntity::class,
        
        // Comunicación
        MessageEntity::class,
        NotificationEntity::class,
        
        // Entrenador-Cliente
        TrainerClientEntity::class,
        
        // Sesiones de entrenamiento
        WorkoutSessionEntity::class,
        ExerciseLogEntity::class,
        
        // Progreso y seguimiento
        BodyMeasurementEntity::class,
        GoalEntity::class,
        PersonalRecordEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    // ==================== DATA ACCESS OBJECTS ====================
    
    /** DAO para gestión de usuarios */
    abstract fun userDao(): UserDao
    
    /** DAO para catálogo de ejercicios */
    abstract fun exerciseDao(): ExerciseDao
    
    /** DAO para rutinas de entrenamiento */
    abstract fun routineDao(): RoutineDao
    
    /** DAO para sistema de chat */
    abstract fun chatDao(): ChatDao
    
    /** DAO para notificaciones */
    abstract fun notificationDao(): NotificationDao
    
    /** DAO para relaciones entrenador-cliente */
    abstract fun trainerClientDao(): TrainerClientDao
    
    /** DAO para sesiones de entrenamiento */
    abstract fun workoutDao(): WorkoutDao
    
    /** DAO para progreso (medidas, objetivos, récords) */
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "training_now.db"
        private const val TAG = "AppDatabase"

        /**
         * Obtiene la instancia única de la base de datos.
         * Implementa double-checked locking para thread safety.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Garantiza que existan los datos base (ejercicios y rutinas recomendadas).
         * Es idempotente: cada prepopulate se salta si su tabla ya tiene registros.
         * Se llama en cada arranque, de modo que también repuebla si la base quedó
         * vacía tras una migración destructiva.
         */
        fun asegurarDatosBase(database: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    prepopulateExercises(database.exerciseDao())
                    prepopulateRoutines(database.routineDao())
                    // Limpieza de rutinas predeterminadas descontinuadas (ej: "Full Body Principiante").
                    // Corre siempre, a diferencia de prepopulateRoutines que solo siembra con la tabla vacía.
                    database.routineDao().deleteRutinasDescontinuadas()
                    android.util.Log.d(TAG, "Datos base verificados")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error asegurando datos base", e)
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration(true)
                .build()
        }

        /**
         * Callback para inicialización de la base de datos.
         * Se ejecuta al crear la BD por primera vez.
         */
        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                poblarDatosIniciales()
            }

            /**
             * Al subir de versión con migración destructiva la BD se recrea vacía:
             * hay que volver a poblar los datos base (rutinas públicas, ejercicios, etc.).
             */
            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                poblarDatosIniciales()
            }

            private fun poblarDatosIniciales() {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        kotlinx.coroutines.delay(200)
                        val database = INSTANCE ?: return@launch
                        android.util.Log.d(TAG, "Iniciando prepopulate de datos...")
                        
                        // Poblar en orden de dependencias
                        prepopulateUsers(database.userDao())
                        prepopulateExercises(database.exerciseDao())
                        prepopulateRoutines(database.routineDao())
                        prepopulateTrainerClientRelations(database.trainerClientDao())
                        prepopulateGoals(database.progressDao())
                        prepopulateNotifications(database.notificationDao())
                        prepopulateSampleWorkout(database.workoutDao(), database.progressDao())
                        
                        android.util.Log.d(TAG, "Prepopulate completado exitosamente")
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error en prepopulate", e)
                    }
                }
            }
        }

        // ==================== PREPOPULATE DATA ====================

        /**
         * Crea usuarios iniciales del sistema.
         * 
         * Usuarios creados:
         * - ID 1: Admin del sistema
         * - ID 2: Entrenador Santiago (especialidad: Hipertrofia)
         * - ID 3: Entrenadora María (especialidad: Funcional)
         * - ID 4: Cliente Juan (usuario de prueba)
         * - ID 5: Cliente Ana (usuaria de prueba)
         */
        private suspend fun prepopulateUsers(dao: UserDao) {
            if (dao.count() > 0) return
            
            val users = listOf(
                // Administrador del sistema (@admin.tn para rol ADMIN al iniciar sesión)
                UserEntity(
                    role = "ADMIN",
                    name = "Super",
                    lastName = "Admin",
                    email = "admin@admin.tn",
                    phone = "+56900000000",
                    password = "admin123",
                    specializations = "Gestión del Sistema"
                ),
                // Entrenador 1 - Especialista en Hipertrofia
                UserEntity(
                    role = "TRAINER",
                    name = "Santiago",
                    lastName = "Rodríguez",
                    email = "santiago@coach.tn",
                    phone = "+56912345678",
                    password = "coach123",
                    specializations = "Hipertrofia, Fuerza, Powerlifting",
                    gender = "M"
                ),
                // Entrenador 2 - Especialista en Funcional
                UserEntity(
                    role = "TRAINER",
                    name = "María",
                    lastName = "González",
                    email = "maria@coach.tn",
                    phone = "+56923456789",
                    password = "coach123",
                    specializations = "Entrenamiento Funcional, CrossFit, HIIT",
                    gender = "F"
                ),
                // Cliente 1
                UserEntity(
                    role = "USER",
                    name = "Juan",
                    lastName = "Pérez",
                    email = "juan@gmail.com",
                    phone = "+56987654321",
                    password = "user123",
                    height = 175f,
                    weight = 78f,
                    gender = "M",
                    birthDate = 631152000000 // 01/01/1990
                ),
                // Cliente 2
                UserEntity(
                    role = "USER",
                    name = "Ana",
                    lastName = "Martínez",
                    email = "ana@gmail.com",
                    phone = "+56976543210",
                    password = "user123",
                    height = 165f,
                    weight = 62f,
                    gender = "F",
                    birthDate = 694224000000 // 01/01/1992
                )
            )
            users.forEach { dao.insertUser(it) }
            android.util.Log.d(TAG, "Usuarios creados: ${users.size}")
        }

        /**
         * Crea el catálogo inicial de ejercicios.
         * Organizados por grupo muscular con descripciones detalladas.
         */
        private suspend fun prepopulateExercises(dao: ExerciseDao) {
            if (dao.count() > 0) return
            
            val exercises = listOf(
                // === PECTORALES ===
                ExerciseEntity(
                    name = "Press Banca",
                    category = "Pectorales",
                    description = "Acostado en banco plano, baja la barra al pecho y empuja hacia arriba. Mantén los pies firmes en el suelo y la espalda ligeramente arqueada.",
                    videoUrl = "https://youtube.com/watch?v=press_banca",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Press Inclinado",
                    category = "Pectorales",
                    description = "Similar al press banca pero en banco inclinado a 30-45°. Enfoca el trabajo en la parte superior del pectoral.",
                    videoUrl = "https://youtube.com/watch?v=press_inclinado",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Aperturas con Mancuernas",
                    category = "Pectorales",
                    description = "Acostado en banco, brazos extendidos con mancuernas. Baja los brazos en arco hasta sentir estiramiento y vuelve a la posición inicial.",
                    videoUrl = "https://youtube.com/watch?v=aperturas",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Fondos en Paralelas",
                    category = "Pectorales",
                    description = "En barras paralelas, baja el cuerpo flexionando los codos hasta 90° y empuja hacia arriba. Inclínate hacia adelante para mayor activación pectoral.",
                    videoUrl = "https://youtube.com/watch?v=fondos",
                    isSystemDefault = true
                ),
                
                // === ESPALDA ===
                ExerciseEntity(
                    name = "Dominadas",
                    category = "Espalda",
                    description = "Cuelga de la barra con agarre prono (palmas hacia adelante), tira del cuerpo hacia arriba hasta que la barbilla supere la barra.",
                    videoUrl = "https://youtube.com/watch?v=dominadas",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Remo con Barra",
                    category = "Espalda",
                    description = "Inclinado hacia adelante con espalda recta, tira la barra hacia el abdomen inferior. Mantén los codos cerca del cuerpo.",
                    videoUrl = "https://youtube.com/watch?v=remo_barra",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Jalón al Pecho",
                    category = "Espalda",
                    description = "En máquina de poleas, tira la barra hacia el pecho superior mientras contraes los dorsales. Evita balancear el torso.",
                    videoUrl = "https://youtube.com/watch?v=jalon",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Peso Muerto",
                    category = "Espalda",
                    description = "Con barra en el suelo, levántala manteniendo la espalda recta y empujando con las piernas. Ejercicio compuesto fundamental.",
                    videoUrl = "https://youtube.com/watch?v=peso_muerto",
                    isSystemDefault = true
                ),
                
                // === PIERNAS ===
                ExerciseEntity(
                    name = "Sentadilla",
                    category = "Piernas",
                    description = "Con barra en la espalda alta, baja flexionando rodillas y caderas hasta que los muslos estén paralelos al suelo. Mantén la espalda recta.",
                    videoUrl = "https://youtube.com/watch?v=sentadilla",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Prensa de Piernas",
                    category = "Piernas",
                    description = "En máquina, empuja la plataforma con los pies separados al ancho de hombros. No bloquees las rodillas al extender.",
                    videoUrl = "https://youtube.com/watch?v=prensa",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Extensión de Cuádriceps",
                    category = "Piernas",
                    description = "En máquina, extiende las piernas contra la resistencia. Ejercicio de aislamiento para cuádriceps.",
                    videoUrl = "https://youtube.com/watch?v=extension_quad",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Curl Femoral",
                    category = "Piernas",
                    description = "En máquina, flexiona las piernas llevando los talones hacia los glúteos. Trabaja los isquiotibiales.",
                    videoUrl = "https://youtube.com/watch?v=curl_femoral",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Zancadas",
                    category = "Piernas",
                    description = "Da un paso al frente y baja hasta que ambas rodillas formen 90°. Alterna piernas. Excelente para equilibrio y fuerza unilateral.",
                    videoUrl = "https://youtube.com/watch?v=zancadas",
                    isSystemDefault = true
                ),
                
                // === HOMBROS ===
                ExerciseEntity(
                    name = "Press Militar",
                    category = "Hombros",
                    description = "De pie o sentado, empuja la barra desde los hombros hacia arriba. Mantén el core activado para estabilidad.",
                    videoUrl = "https://youtube.com/watch?v=press_militar",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Elevaciones Laterales",
                    category = "Hombros",
                    description = "Con mancuernas a los lados, eleva los brazos lateralmente hasta la altura de los hombros. Controla el movimiento.",
                    videoUrl = "https://youtube.com/watch?v=elevaciones_lat",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Elevaciones Frontales",
                    category = "Hombros",
                    description = "Eleva las mancuernas al frente hasta la altura de los ojos. Trabaja el deltoides anterior.",
                    videoUrl = "https://youtube.com/watch?v=elevaciones_front",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Pájaros",
                    category = "Hombros",
                    description = "Inclinado hacia adelante, eleva las mancuernas lateralmente. Trabaja el deltoides posterior.",
                    videoUrl = "https://youtube.com/watch?v=pajaros",
                    isSystemDefault = true
                ),
                
                // === BRAZOS ===
                ExerciseEntity(
                    name = "Curl de Bíceps",
                    category = "Brazos",
                    description = "Con mancuernas o barra, flexiona los codos llevando el peso hacia los hombros. No balancees el cuerpo.",
                    videoUrl = "https://youtube.com/watch?v=curl_biceps",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Curl Martillo",
                    category = "Brazos",
                    description = "Similar al curl pero con agarre neutro (palmas enfrentadas). Trabaja bíceps y braquial.",
                    videoUrl = "https://youtube.com/watch?v=curl_martillo",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Extensión de Tríceps",
                    category = "Brazos",
                    description = "Con mancuerna sobre la cabeza, baja el peso detrás de la cabeza y extiende. Mantén los codos fijos.",
                    videoUrl = "https://youtube.com/watch?v=extension_triceps",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Fondos en Banco",
                    category = "Brazos",
                    description = "Manos en banco detrás, baja el cuerpo flexionando los codos y empuja hacia arriba. Excelente para tríceps.",
                    videoUrl = "https://youtube.com/watch?v=fondos_banco",
                    isSystemDefault = true
                ),
                
                // === CORE/ABDOMINALES ===
                ExerciseEntity(
                    name = "Plancha",
                    category = "Core",
                    description = "Apoyado en antebrazos y puntas de pies, mantén el cuerpo recto. Activa abdominales y glúteos. Aguanta el tiempo indicado.",
                    videoUrl = "https://youtube.com/watch?v=plancha",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Crunch Abdominal",
                    category = "Core",
                    description = "Acostado boca arriba, eleva los hombros del suelo contrayendo los abdominales. No tires del cuello.",
                    videoUrl = "https://youtube.com/watch?v=crunch",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Elevación de Piernas",
                    category = "Core",
                    description = "Colgado de barra o acostado, eleva las piernas rectas hasta 90°. Trabaja abdominales inferiores.",
                    videoUrl = "https://youtube.com/watch?v=elevacion_piernas",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Russian Twist",
                    category = "Core",
                    description = "Sentado con torso inclinado, gira el torso de lado a lado con o sin peso. Trabaja oblicuos.",
                    videoUrl = "https://youtube.com/watch?v=russian_twist",
                    isSystemDefault = true
                ),
                
                // === CARDIO ===
                ExerciseEntity(
                    name = "Burpees",
                    category = "Cardio",
                    description = "Ejercicio de cuerpo completo: sentadilla, plancha, flexión, salto. Alta intensidad cardiovascular.",
                    videoUrl = "https://youtube.com/watch?v=burpees",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Mountain Climbers",
                    category = "Cardio",
                    description = "En posición de plancha, alterna llevando rodillas al pecho rápidamente. Cardio y core.",
                    videoUrl = "https://youtube.com/watch?v=mountain_climbers",
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Jumping Jacks",
                    category = "Cardio",
                    description = "Salta abriendo piernas y brazos simultáneamente. Ejercicio de calentamiento y cardio básico.",
                    videoUrl = "https://youtube.com/watch?v=jumping_jacks",
                    isSystemDefault = true
                ),

                // === BÁSQUETBOL (29-36) ===
                ExerciseEntity(
                    name = "Dribbling con Conos",
                    category = "Básquetbol",
                    description = "Recorrido en zigzag entre conos manteniendo el balón bajo control con ambas manos. Mejora el manejo y el cambio de dirección.",
                    videoUrl = "",
                    muscles = "Antebrazo, Core, Piernas",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Balón y conos",
                    recommendedSets = 4,
                    recommendedReps = "30 seg",
                    restSeconds = 45,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Tiro Libre",
                    category = "Básquetbol",
                    description = "Serie de lanzamientos desde la línea de tiros libres con rutina previa constante. Trabaja técnica y concentración.",
                    videoUrl = "",
                    muscles = "Hombros, Tríceps, Core",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Balón y aro",
                    recommendedSets = 5,
                    recommendedReps = "10 tiros",
                    restSeconds = 60,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Salto al Cajón",
                    category = "Básquetbol",
                    description = "Salto explosivo con ambos pies sobre un cajón, aterrizando suave con rodillas flexionadas. Desarrolla potencia para el salto vertical.",
                    videoUrl = "",
                    muscles = "Cuádriceps, Glúteos, Gemelos",
                    difficulty = "INTERMEDIO",
                    equipment = "Cajón pliométrico",
                    recommendedSets = 4,
                    recommendedReps = "8-10",
                    restSeconds = 90,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Desplazamiento Defensivo",
                    category = "Básquetbol",
                    description = "Movimiento lateral en posición defensiva, sin cruzar los pies y manteniendo la cadera baja. Base de la defensa individual.",
                    videoUrl = "",
                    muscles = "Cuádriceps, Glúteos, Aductores",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Peso corporal",
                    recommendedSets = 4,
                    recommendedReps = "30 seg",
                    restSeconds = 45,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Entrada a Canasta",
                    category = "Básquetbol",
                    description = "Aproximación en dos tiempos finalizando en bandeja, alternando lado derecho e izquierdo.",
                    videoUrl = "",
                    muscles = "Piernas, Core",
                    difficulty = "INTERMEDIO",
                    equipment = "Balón y aro",
                    recommendedSets = 4,
                    recommendedReps = "10 por lado",
                    restSeconds = 60,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Sprint de Cancha",
                    category = "Básquetbol",
                    description = "Carrera a máxima velocidad de fondo a fondo, con cambio de sentido. Mejora la resistencia específica del partido.",
                    videoUrl = "",
                    muscles = "Piernas, Sistema cardiovascular",
                    difficulty = "INTERMEDIO",
                    equipment = "Cancha",
                    recommendedSets = 6,
                    recommendedReps = "1 largo",
                    restSeconds = 60,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Rebote y Salida",
                    category = "Básquetbol",
                    description = "Captura del rebote con ambas manos, protección del balón y primer pase de salida rápida.",
                    videoUrl = "",
                    muscles = "Espalda, Hombros, Piernas",
                    difficulty = "INTERMEDIO",
                    equipment = "Balón y aro",
                    recommendedSets = 4,
                    recommendedReps = "8-10",
                    restSeconds = 60,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Tiro de Media Distancia",
                    category = "Básquetbol",
                    description = "Lanzamientos desde distintos puntos del perímetro tras recepción o autopase, cuidando el equilibrio.",
                    videoUrl = "",
                    muscles = "Hombros, Tríceps, Piernas",
                    difficulty = "INTERMEDIO",
                    equipment = "Balón y aro",
                    recommendedSets = 5,
                    recommendedReps = "10 tiros",
                    restSeconds = 45,
                    isSystemDefault = true
                ),

                // === PILATES (37-45) ===
                ExerciseEntity(
                    name = "The Hundred",
                    category = "Pilates",
                    description = "Tumbado boca arriba, piernas en mesa y tronco elevado, bombea los brazos 100 tiempos coordinando la respiración.",
                    videoUrl = "",
                    muscles = "Core profundo, Abdominales",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Colchoneta",
                    recommendedSets = 1,
                    recommendedReps = "100 tiempos",
                    restSeconds = 30,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Roll Up",
                    category = "Pilates",
                    description = "Desde tumbado, enrolla la columna vértebra a vértebra hasta sentarte y desciende con el mismo control.",
                    videoUrl = "",
                    muscles = "Recto abdominal, Columna",
                    difficulty = "INTERMEDIO",
                    equipment = "Colchoneta",
                    recommendedSets = 2,
                    recommendedReps = "8-10",
                    restSeconds = 30,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Single Leg Stretch",
                    category = "Pilates",
                    description = "Alterna el estiramiento de una pierna mientras la otra se acerca al pecho, manteniendo la zona lumbar apoyada.",
                    videoUrl = "",
                    muscles = "Core, Oblicuos, Flexores de cadera",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Colchoneta",
                    recommendedSets = 2,
                    recommendedReps = "10 por lado",
                    restSeconds = 30,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Puente de Hombros",
                    category = "Pilates",
                    description = "Eleva la cadera desde el suelo desenrollando la columna, aprieta glúteos arriba y baja vértebra a vértebra.",
                    videoUrl = "",
                    muscles = "Glúteos, Isquiotibiales, Core",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Colchoneta",
                    recommendedSets = 3,
                    recommendedReps = "10-12",
                    restSeconds = 30,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Swan Dive",
                    category = "Pilates",
                    description = "Boca abajo, extiende la columna elevando el pecho con los brazos apoyados, sin forzar la zona lumbar.",
                    videoUrl = "",
                    muscles = "Erectores espinales, Glúteos",
                    difficulty = "INTERMEDIO",
                    equipment = "Colchoneta",
                    recommendedSets = 2,
                    recommendedReps = "8-10",
                    restSeconds = 30,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Side Kick",
                    category = "Pilates",
                    description = "De lado, con el cuerpo alineado, lleva la pierna superior adelante y atrás controlando el movimiento desde el core.",
                    videoUrl = "",
                    muscles = "Glúteo medio, Oblicuos, Aductores",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Colchoneta",
                    recommendedSets = 2,
                    recommendedReps = "12 por lado",
                    restSeconds = 30,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Teaser",
                    category = "Pilates",
                    description = "Desde tumbado, sube tronco y piernas formando una V manteniendo el equilibrio sobre los isquiones.",
                    videoUrl = "",
                    muscles = "Core completo, Flexores de cadera",
                    difficulty = "AVANZADO",
                    equipment = "Colchoneta",
                    recommendedSets = 2,
                    recommendedReps = "6-8",
                    restSeconds = 45,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Estiramiento del Gato",
                    category = "Pilates",
                    description = "En cuadrupedia, alterna arquear y redondear la columna al ritmo de la respiración. Movilidad para la espalda.",
                    videoUrl = "",
                    muscles = "Columna, Core",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Colchoneta",
                    recommendedSets = 2,
                    recommendedReps = "10",
                    restSeconds = 20,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Criss Cross",
                    category = "Pilates",
                    description = "Rotación del tronco llevando el codo hacia la rodilla contraria, con movimiento lento y controlado.",
                    videoUrl = "",
                    muscles = "Oblicuos, Recto abdominal",
                    difficulty = "INTERMEDIO",
                    equipment = "Colchoneta",
                    recommendedSets = 2,
                    recommendedReps = "10 por lado",
                    restSeconds = 30,
                    isSystemDefault = true
                ),

                // === FÚTBOL (46-53) ===
                ExerciseEntity(
                    name = "Conducción con Balón",
                    category = "Fútbol",
                    description = "Recorrido en zigzag entre conos golpeando el balón con el interior y exterior del pie. Mejora el control en carrera.",
                    videoUrl = "",
                    muscles = "Cuádriceps, Gemelos, Core",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Balón y conos",
                    recommendedSets = 4,
                    recommendedReps = "30 seg",
                    restSeconds = 45,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Pase y Control",
                    category = "Fútbol",
                    description = "Pases a corta y media distancia con control orientado, alternando ambos perfiles. Base técnica del juego colectivo.",
                    videoUrl = "",
                    muscles = "Piernas, Core",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Balón y compañero o pared",
                    recommendedSets = 4,
                    recommendedReps = "20 pases",
                    restSeconds = 45,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Definición a Portería",
                    category = "Fútbol",
                    description = "Remates desde el borde del área tras conducción o pase, buscando colocación antes que potencia.",
                    videoUrl = "",
                    muscles = "Cuádriceps, Isquiotibiales, Core",
                    difficulty = "INTERMEDIO",
                    equipment = "Balón y portería",
                    recommendedSets = 5,
                    recommendedReps = "8 remates",
                    restSeconds = 60,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Sprint con Cambio de Dirección",
                    category = "Fútbol",
                    description = "Carreras cortas a máxima intensidad con giros de 90 y 180 grados. Reproduce las acciones reales del partido.",
                    videoUrl = "",
                    muscles = "Piernas, Sistema cardiovascular",
                    difficulty = "INTERMEDIO",
                    equipment = "Conos",
                    recommendedSets = 6,
                    recommendedReps = "20 metros",
                    restSeconds = 60,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Escalera de Agilidad",
                    category = "Fútbol",
                    description = "Series de apoyos rápidos dentro de la escalera, variando el patrón de pisada. Mejora la coordinación y la frecuencia de zancada.",
                    videoUrl = "",
                    muscles = "Gemelos, Tibial, Core",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Escalera de agilidad",
                    recommendedSets = 4,
                    recommendedReps = "3 pasadas",
                    restSeconds = 45,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Juego de Cabeza",
                    category = "Fútbol",
                    description = "Remates de cabeza tras centro o autopase, golpeando con la frente y acompañando con el tronco.",
                    videoUrl = "",
                    muscles = "Cuello, Core, Piernas",
                    difficulty = "INTERMEDIO",
                    equipment = "Balón",
                    recommendedSets = 3,
                    recommendedReps = "10 remates",
                    restSeconds = 60,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Carrera Continua",
                    category = "Fútbol",
                    description = "Trote sostenido a ritmo moderado para construir base aeróbica y recuperar entre sesiones intensas.",
                    videoUrl = "",
                    muscles = "Piernas, Sistema cardiovascular",
                    difficulty = "PRINCIPIANTE",
                    equipment = "Peso corporal",
                    recommendedSets = 1,
                    recommendedReps = "25-35 min",
                    restSeconds = 0,
                    isSystemDefault = true
                ),
                ExerciseEntity(
                    name = "Rondo 4 contra 1",
                    category = "Fútbol",
                    description = "Juego de posesión en círculo: cuatro jugadores conservan el balón a uno o dos toques mientras uno presiona.",
                    videoUrl = "",
                    muscles = "Piernas, Core",
                    difficulty = "INTERMEDIO",
                    equipment = "Balón y compañeros",
                    recommendedSets = 4,
                    recommendedReps = "3 min",
                    restSeconds = 60,
                    isSystemDefault = true
                )
            )
            dao.insertExercises(exercises)
            android.util.Log.d(TAG, "Ejercicios creados: ${exercises.size}")
        }

        /**
         * Crea rutinas de ejemplo para demostración.
         */
        private suspend fun prepopulateRoutines(dao: RoutineDao) {
            if (dao.count() > 0) return

            val semana = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

            /** Crea un día con su sesión y ejercicios. Sin ejercicios = día de descanso. */
            suspend fun dia(
                routineId: Int,
                orden: Int,
                actividad: String,
                ejercicios: List<Int>,
                hora: Int? = null
            ) {
                val dayId = dao.insertDay(
                    RoutineDayEntity(
                        routineId = routineId,
                        dayLabel = semana[orden],
                        activityName = actividad,
                        dayOrder = orden,
                        reminderHour = hora,
                        reminderMinute = if (hora != null) 0 else null
                    )
                ).toInt()
                if (ejercicios.isNotEmpty()) {
                    dao.insertRoutineExercises(
                        ejercicios.mapIndexed { i, ejercicioId ->
                            RoutineExerciseEntity(dayId = dayId, exerciseId = ejercicioId, order = i + 1)
                        }
                    )
                }
            }

            // ==================== BÁSQUETBOL ====================
            // Pretemporada: técnica individual + fuerza + acondicionamiento
            val basquetId = dao.insertRoutine(
                RoutineEntity(
                    name = "Pretemporada de Básquetbol",
                    dayInfo = "Lunes, Martes, Jueves, Viernes, Sábado",
                    ownerId = null,
                    creatorId = 1
                )
            ).toInt()
            dia(basquetId, 0, "Manejo de balón y tiro", listOf(29, 30, 33, 36))
            dia(basquetId, 1, "Fuerza de tren inferior", listOf(9, 10, 8, 13, 22))
            dia(basquetId, 2, "", emptyList())
            dia(basquetId, 3, "Salto y potencia", listOf(31, 26, 13, 35))
            dia(basquetId, 4, "Defensa y agilidad", listOf(32, 27, 29, 34))
            dia(basquetId, 5, "Partido y tiro libre", listOf(34, 30, 36, 28))
            dia(basquetId, 6, "", emptyList())

            // ==================== HIPERTROFIA ====================
            // Rutina clásica de 5 días con división por grupos musculares
            val hipertrofiaId = dao.insertRoutine(
                RoutineEntity(
                    name = "Hipertrofia - 5 días",
                    dayInfo = "Lunes, Martes, Miércoles, Jueves, Viernes",
                    ownerId = null,
                    creatorId = 2
                )
            ).toInt()
            dia(hipertrofiaId, 0, "Pecho y Tríceps", listOf(1, 2, 3, 20, 21))
            dia(hipertrofiaId, 1, "Espalda y Bíceps", listOf(5, 6, 7, 18, 19))
            dia(hipertrofiaId, 2, "Piernas", listOf(9, 10, 11, 12, 8))
            dia(hipertrofiaId, 3, "Hombros y Core", listOf(14, 15, 16, 17, 22))
            dia(hipertrofiaId, 4, "Full Body y Brazos", listOf(1, 5, 9, 18, 20))
            dia(hipertrofiaId, 5, "", emptyList())
            dia(hipertrofiaId, 6, "", emptyList())

            // ==================== PILATES ====================
            // Mat Pilates progresivo: control, fuerza del centro y movilidad
            val pilatesId = dao.insertRoutine(
                RoutineEntity(
                    name = "Pilates Mat - Semana completa",
                    dayInfo = "Lunes, Miércoles, Viernes, Domingo",
                    ownerId = null,
                    creatorId = 3
                )
            ).toInt()
            dia(pilatesId, 0, "Centro y respiración", listOf(37, 38, 39, 45))
            dia(pilatesId, 1, "", emptyList())
            dia(pilatesId, 2, "Fuerza y estabilidad", listOf(37, 40, 42, 41))
            dia(pilatesId, 3, "", emptyList())
            dia(pilatesId, 4, "Core avanzado", listOf(37, 43, 45, 39))
            dia(pilatesId, 5, "", emptyList())
            dia(pilatesId, 6, "Movilidad y estiramiento", listOf(44, 41, 40, 38))

            // ==================== FÚTBOL ====================
            // Semana tipo: técnica, fuerza, velocidad y partido el fin de semana
            val futbolId = dao.insertRoutine(
                RoutineEntity(
                    name = "Preparación de Fútbol",
                    dayInfo = "Lunes, Martes, Miércoles, Viernes, Sábado",
                    ownerId = null,
                    creatorId = 1
                )
            ).toInt()
            dia(futbolId, 0, "Técnica y pase", listOf(46, 47, 50, 53))
            dia(futbolId, 1, "Fuerza de piernas", listOf(9, 13, 12, 8, 22))
            dia(futbolId, 2, "Velocidad y agilidad", listOf(49, 50, 46, 27))
            dia(futbolId, 3, "", emptyList())
            dia(futbolId, 4, "Definición y remate", listOf(48, 51, 47, 53))
            dia(futbolId, 5, "Partido", listOf(53, 48, 49))
            dia(futbolId, 6, "Recuperación activa", listOf(52, 44))

            android.util.Log.d(TAG, "Rutinas creadas: 4 (Básquetbol, Fútbol, Hipertrofia, Pilates)")
        }

        /**
         * Crea relaciones entrenador-cliente de ejemplo.
         */
        private suspend fun prepopulateTrainerClientRelations(dao: TrainerClientDao) {
            val relations = listOf(
                // Santiago (ID 2) entrena a Juan (ID 4)
                TrainerClientEntity(
                    trainerId = 2,
                    clientId = 4,
                    status = TrainerClientStatus.ACTIVE.name,
                    trainerNotes = "Cliente dedicado, objetivo: ganar masa muscular",
                    clientGoals = "Aumentar 5kg de músculo en 3 meses",
                    sessionsPerWeek = 4
                ),
                // María (ID 3) entrena a Ana (ID 5)
                TrainerClientEntity(
                    trainerId = 3,
                    clientId = 5,
                    status = TrainerClientStatus.ACTIVE.name,
                    trainerNotes = "Enfocada en tonificación y resistencia",
                    clientGoals = "Tonificar piernas y mejorar condición física",
                    sessionsPerWeek = 3
                ),
                // Santiago también tiene a Ana como clienta (PENDING)
                TrainerClientEntity(
                    trainerId = 2,
                    clientId = 5,
                    status = TrainerClientStatus.PENDING.name,
                    clientGoals = "Consulta inicial sobre entrenamiento de fuerza"
                )
            )
            relations.forEach { dao.insertTrainerClient(it) }
            android.util.Log.d(TAG, "Relaciones entrenador-cliente creadas: ${relations.size}")
        }

        /**
         * Crea objetivos de ejemplo para los clientes.
         */
        private suspend fun prepopulateGoals(dao: ProgressDao) {
            val goals = listOf(
                // Objetivo de Juan: Ganar músculo
                GoalEntity(
                    userId = 4,
                    createdByTrainerId = 2,
                    title = "Aumentar masa muscular",
                    description = "Ganar 5kg de masa muscular magra en 3 meses mediante entrenamiento de hipertrofia",
                    category = GoalCategory.MUSCLE_GAIN.name,
                    targetValue = 83.0,
                    currentValue = 78.0,
                    startValue = 78.0,
                    unit = "kg",
                    targetDate = System.currentTimeMillis() + 7776000000, // 90 días
                    progressPercentage = 0.0
                ),
                // Objetivo de Juan: Mejorar press banca
                GoalEntity(
                    userId = 4,
                    createdByTrainerId = 2,
                    title = "Press Banca 100kg",
                    description = "Alcanzar 100kg en press banca para 1 repetición",
                    category = GoalCategory.STRENGTH.name,
                    targetValue = 100.0,
                    currentValue = 80.0,
                    startValue = 80.0,
                    unit = "kg",
                    progressPercentage = 0.0
                ),
                // Objetivo de Ana: Perder grasa
                GoalEntity(
                    userId = 5,
                    createdByTrainerId = 3,
                    title = "Reducir porcentaje de grasa",
                    description = "Bajar de 25% a 20% de grasa corporal",
                    category = GoalCategory.BODY_COMPOSITION.name,
                    targetValue = 20.0,
                    currentValue = 25.0,
                    startValue = 25.0,
                    unit = "%",
                    targetDate = System.currentTimeMillis() + 5184000000, // 60 días
                    progressPercentage = 0.0
                ),
                // Objetivo de Ana: Consistencia
                GoalEntity(
                    userId = 5,
                    createdByTrainerId = 3,
                    title = "Entrenar 4 veces por semana",
                    description = "Mantener consistencia de 4 entrenamientos semanales durante 2 meses",
                    category = GoalCategory.HABIT.name,
                    targetValue = 32.0, // 4 x 8 semanas
                    currentValue = 0.0,
                    startValue = 0.0,
                    unit = "sesiones",
                    progressPercentage = 0.0
                )
            )
            goals.forEach { dao.insertGoal(it) }
            android.util.Log.d(TAG, "Objetivos creados: ${goals.size}")
        }

        /**
         * Crea notificaciones de bienvenida.
         */
        private suspend fun prepopulateNotifications(dao: NotificationDao) {
            val notifications = listOf(
                NotificationEntity(
                    userId = 4,
                    title = "¡Bienvenido a TrainingNow!",
                    message = "Tu entrenador Santiago te ha asignado tu primera rutina. ¡Comienza tu transformación hoy!",
                    type = NotificationType.SYSTEM.name,
                    senderId = 2
                ),
                NotificationEntity(
                    userId = 5,
                    title = "¡Bienvenida a TrainingNow!",
                    message = "María te da la bienvenida. Tienes una rutina de piernas lista para empezar.",
                    type = NotificationType.SYSTEM.name,
                    senderId = 3
                ),
                NotificationEntity(
                    userId = 4,
                    title = "Nueva rutina asignada",
                    message = "Santiago te ha asignado 'Plan Hipertrofia - Semana 1'. ¡A entrenar!",
                    type = NotificationType.ROUTINE_ASSIGNED.name,
                    senderId = 2
                )
            )
            notifications.forEach { dao.insertNotification(it) }
            android.util.Log.d(TAG, "Notificaciones creadas: ${notifications.size}")
        }

        /**
         * Crea una sesión de entrenamiento de ejemplo con sus logs.
         */
        private suspend fun prepopulateSampleWorkout(workoutDao: WorkoutDao, progressDao: ProgressDao) {
            // Crear una sesión completada para Juan
            val sessionId = workoutDao.insertSession(
                WorkoutSessionEntity(
                    userId = 4,
                    routineId = 4, // Su rutina de pecho
                    startTime = System.currentTimeMillis() - 86400000, // Ayer
                    endTime = System.currentTimeMillis() - 86400000 + 3600000, // 1 hora después
                    status = "COMPLETED",
                    totalDurationMinutes = 60,
                    caloriesBurned = 350,
                    rating = 4,
                    perceivedDifficulty = 7,
                    mood = "MOTIVATED",
                    notes = "Buen entrenamiento, me sentí con energía"
                )
            ).toInt()

            // Registrar ejercicios de esa sesión
            workoutDao.insertExerciseLogs(listOf(
                ExerciseLogEntity(
                    sessionId = sessionId,
                    exerciseId = 1, // Press Banca
                    orderInSession = 1,
                    plannedSets = 4,
                    plannedReps = 10,
                    completedSets = 4,
                    actualReps = "[10, 10, 9, 8]",
                    weightKg = 70.0,
                    rpe = 8
                ),
                ExerciseLogEntity(
                    sessionId = sessionId,
                    exerciseId = 2, // Press Inclinado
                    orderInSession = 2,
                    plannedSets = 3,
                    plannedReps = 12,
                    completedSets = 3,
                    actualReps = "[12, 11, 10]",
                    weightKg = 50.0,
                    rpe = 7
                )
            ))

            // Crear una medida corporal para Juan
            progressDao.insertMeasurement(
                BodyMeasurementEntity(
                    userId = 4,
                    weightKg = 78.0,
                    bodyFatPercentage = 18.0,
                    chestCm = 100.0,
                    waistCm = 82.0,
                    rightArmCm = 35.0,
                    leftArmCm = 34.5,
                    notes = "Medición inicial"
                )
            )

            android.util.Log.d(TAG, "Sesión de ejemplo y medidas creadas")
        }
    }
}