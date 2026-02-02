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
 * @version 5 - Usuarios: suspendedUntil, suspendReason, isBanned, banReason (sanciones admin)
 */
@Database(
    entities = [
        // Usuarios y autenticación
        UserEntity::class,
        
        // Ejercicios y rutinas
        ExerciseEntity::class,
        RoutineEntity::class,
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
    version = 5,
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
            
            // === RUTINA GLOBAL: Básicos para principiantes ===
            val rutinaPrincipiantes = RoutineEntity(
                name = "Rutina Full Body - Principiantes",
                dayInfo = "Lunes, Miércoles, Viernes",
                ownerId = null, // Pública
                creatorId = 1   // Admin
            )
            val principiantesId = dao.insertRoutine(rutinaPrincipiantes).toInt()
            dao.insertRoutineExercises(listOf(
                RoutineExerciseEntity(routineId = principiantesId, exerciseId = 9, order = 1),  // Sentadilla
                RoutineExerciseEntity(routineId = principiantesId, exerciseId = 1, order = 2),  // Press Banca
                RoutineExerciseEntity(routineId = principiantesId, exerciseId = 5, order = 3),  // Dominadas
                RoutineExerciseEntity(routineId = principiantesId, exerciseId = 22, order = 4), // Plancha
                RoutineExerciseEntity(routineId = principiantesId, exerciseId = 18, order = 5)  // Curl Bíceps
            ))

            // === RUTINA GLOBAL: Push (Empuje) ===
            val rutinaPush = RoutineEntity(
                name = "Push Day - Pecho, Hombros, Tríceps",
                dayInfo = "Día de Empuje",
                ownerId = null,
                creatorId = 2 // Santiago
            )
            val pushId = dao.insertRoutine(rutinaPush).toInt()
            dao.insertRoutineExercises(listOf(
                RoutineExerciseEntity(routineId = pushId, exerciseId = 1, order = 1),  // Press Banca
                RoutineExerciseEntity(routineId = pushId, exerciseId = 2, order = 2),  // Press Inclinado
                RoutineExerciseEntity(routineId = pushId, exerciseId = 14, order = 3), // Press Militar
                RoutineExerciseEntity(routineId = pushId, exerciseId = 15, order = 4), // Elevaciones Lat
                RoutineExerciseEntity(routineId = pushId, exerciseId = 20, order = 5), // Extensión Tríceps
                RoutineExerciseEntity(routineId = pushId, exerciseId = 4, order = 6)   // Fondos
            ))

            // === RUTINA GLOBAL: Pull (Tirón) ===
            val rutinaPull = RoutineEntity(
                name = "Pull Day - Espalda, Bíceps",
                dayInfo = "Día de Tirón",
                ownerId = null,
                creatorId = 2
            )
            val pullId = dao.insertRoutine(rutinaPull).toInt()
            dao.insertRoutineExercises(listOf(
                RoutineExerciseEntity(routineId = pullId, exerciseId = 8, order = 1),  // Peso Muerto
                RoutineExerciseEntity(routineId = pullId, exerciseId = 5, order = 2),  // Dominadas
                RoutineExerciseEntity(routineId = pullId, exerciseId = 6, order = 3),  // Remo Barra
                RoutineExerciseEntity(routineId = pullId, exerciseId = 7, order = 4),  // Jalón
                RoutineExerciseEntity(routineId = pullId, exerciseId = 18, order = 5), // Curl Bíceps
                RoutineExerciseEntity(routineId = pullId, exerciseId = 19, order = 6)  // Curl Martillo
            ))

            // === RUTINA PRIVADA para Juan (Cliente ID 4) ===
            val rutinaJuan = RoutineEntity(
                name = "Plan Hipertrofia - Semana 1",
                dayInfo = "Lunes - Pecho",
                ownerId = 4,
                creatorId = 2,
                scheduledTime = System.currentTimeMillis() + 86400000
            )
            val juanId = dao.insertRoutine(rutinaJuan).toInt()
            dao.insertRoutineExercises(listOf(
                RoutineExerciseEntity(routineId = juanId, exerciseId = 1, order = 1),
                RoutineExerciseEntity(routineId = juanId, exerciseId = 2, order = 2),
                RoutineExerciseEntity(routineId = juanId, exerciseId = 3, order = 3),
                RoutineExerciseEntity(routineId = juanId, exerciseId = 4, order = 4)
            ))

            // === RUTINA PRIVADA para Ana (Cliente ID 5) ===
            val rutinaAna = RoutineEntity(
                name = "Glúteos y Piernas - Intensivo",
                dayInfo = "Martes y Jueves",
                ownerId = 5,
                creatorId = 3, // María
                scheduledTime = System.currentTimeMillis() + 172800000
            )
            val anaId = dao.insertRoutine(rutinaAna).toInt()
            dao.insertRoutineExercises(listOf(
                RoutineExerciseEntity(routineId = anaId, exerciseId = 9, order = 1),  // Sentadilla
                RoutineExerciseEntity(routineId = anaId, exerciseId = 10, order = 2), // Prensa
                RoutineExerciseEntity(routineId = anaId, exerciseId = 13, order = 3), // Zancadas
                RoutineExerciseEntity(routineId = anaId, exerciseId = 12, order = 4), // Curl Femoral
                RoutineExerciseEntity(routineId = anaId, exerciseId = 22, order = 5)  // Plancha
            ))

            android.util.Log.d(TAG, "Rutinas creadas: 5")
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