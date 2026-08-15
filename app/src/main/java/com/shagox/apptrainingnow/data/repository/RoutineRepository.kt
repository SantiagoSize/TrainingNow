package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.domain.MAX_EXERCISES_PER_DAY
import com.shagox.apptrainingnow.data.domain.RoutineDayView
import com.shagox.apptrainingnow.data.domain.RoutineHeader
import com.shagox.apptrainingnow.data.domain.RoutineWithDays
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.local.exercise.ExerciseDao
import com.shagox.apptrainingnow.data.local.routine.RoutineDao
import com.shagox.apptrainingnow.data.local.routine.RoutineDayEntity
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.dto.NotificationDto
import com.shagox.apptrainingnow.data.remote.dto.RoutineDto
import com.shagox.apptrainingnow.data.remote.dto.RoutineExerciseDto
import com.shagox.apptrainingnow.data.local.routine.RoutineExerciseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Datos de un día de la rutina semanal (nombre del día, actividad, ejercicios por nombre).
 */
data class DayRoutineInput(
    val dayLabel: String,
    val activityName: String,
    val exerciseNames: List<String>
)

/**
 * Interfaz del repositorio de rutinas para integración con backend (Spring Boot).
 * CRUD de rutinas, días (RoutineEntity por día) y ejercicios por día.
 * Permisos: Admin/Entrenador → rutinas globales; Usuario → rutinas privadas (ownerId = userId).
 */
interface RoutineRepositoryContract {
    fun getMyRoutines(userId: Int): Flow<List<RoutineEntity>>
    fun getGlobalRoutines(): Flow<List<RoutineEntity>>
    fun observeRoutine(routineId: Int): Flow<RoutineEntity?>
    fun getRoutineWithDays(routineId: Int, userId: Int): Flow<RoutineWithDays?>
    suspend fun getRoutineById(routineId: Int): RoutineEntity?
    suspend fun insertRoutine(routine: RoutineEntity): Long
    suspend fun updateRoutine(routine: RoutineEntity)
    suspend fun deleteRoutine(routine: RoutineEntity)
    suspend fun getExercisesForDay(dayId: Int): List<ExerciseEntity>
    suspend fun addExerciseToDay(dayId: Int, exerciseId: Int)
    suspend fun removeExerciseFromDay(dayId: Int, exerciseId: Int)
    suspend fun canAddExerciseToDay(dayId: Int): Boolean
    suspend fun updateDayActivity(dayId: Int, dayLabel: String, activityName: String)
}

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseDao
) : RoutineRepositoryContract {

    // ==================== JERARQUÍA: Rutina → Días → Ejercicios ====================

    /**
     * Observa la rutina completa: cabecera + lista de días con ejercicios (0-10 por día).
     * Para UI: Lista de días → Vista de detalle de ejercicios.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getRoutineWithDays(routineId: Int, userId: Int): Flow<RoutineWithDays?> {
        return routineDao.observeRoutine(routineId).flatMapLatest { routine ->
            if (routine == null) flowOf(null)
            else flow { emit(buildRoutineWithDays(routine, userId)) }
        }
    }

    private suspend fun buildRoutineWithDays(routine: RoutineEntity, userId: Int): RoutineWithDays {
        val days = routineDao.getDaysOfRoutine(routine.id)
        val dayViews = days.map { day ->
            val exercises = routineDao.getExercisesForDaySync(day.id)
            RoutineDayView(
                routineId = day.id, // id del día (identifica al día dentro de la rutina)
                dayLabel = day.dayLabel,
                activityName = day.activityName,
                exercises = exercises,
                exerciseCount = exercises.size
            )
        }
        return RoutineWithDays(
            header = RoutineHeader(
                id = routine.id,
                name = routine.name,
                ownerId = routine.ownerId,
                creatorId = routine.creatorId,
                creationDate = routine.creationDate
            ),
            days = dayViews
        )
    }

    private fun parseDayInfo(dayInfo: String): Pair<String, String> {
        val idx = dayInfo.indexOf(" - ")
        return if (idx >= 0) {
            dayInfo.substring(0, idx).trim() to dayInfo.substring(idx + 3).trim()
        } else {
            dayInfo.trim() to ""
        }
    }

    /**
     * Indica si se puede añadir otro ejercicio al día (límite 10 por día).
     */
    override suspend fun canAddExerciseToDay(dayId: Int): Boolean {
        return routineDao.countExercisesInDay(dayId) < MAX_EXERCISES_PER_DAY
    }

    override suspend fun getRoutineById(routineId: Int): RoutineEntity? =
        routineDao.getRoutineById(routineId)

    override suspend fun getExercisesForDay(dayId: Int): List<ExerciseEntity> =
        routineDao.getExercisesForDaySync(dayId)

    override suspend fun addExerciseToDay(dayId: Int, exerciseId: Int) {
        if (!canAddExerciseToDay(dayId)) return
        val count = routineDao.countExercisesInDay(dayId)
        routineDao.insertRoutineExercise(
            RoutineExerciseEntity(dayId = dayId, exerciseId = exerciseId, order = count + 1)
        )
    }

    override suspend fun removeExerciseFromDay(dayId: Int, exerciseId: Int) {
        routineDao.removeExerciseFromDay(dayId, exerciseId)
    }

    /** Fija (o quita, con null) la hora del recordatorio de un día. */
    suspend fun setDayReminder(dayId: Int, hora: Int?, minuto: Int?) {
        routineDao.updateDayReminder(dayId, hora, minuto)
    }

    /** Hora propia del día, o null si usa la general. */
    suspend fun getDayReminder(dayId: Int): Pair<Int, Int>? {
        val dia = routineDao.getDayById(dayId) ?: return null
        val hora = dia.reminderHour ?: return null
        return hora to (dia.reminderMinute ?: 0)
    }

    /** Días del usuario que tienen recordatorio propio configurado. */
    suspend fun getDaysWithReminder(userId: Int) = routineDao.getDaysWithReminder(userId)

    /** Cambia el nombre de la sesión de un día (ej. "Pecho y Tríceps"). */
    override suspend fun updateDayActivity(dayId: Int, dayLabel: String, activityName: String) {
        val day = routineDao.getDayById(dayId) ?: return
        routineDao.updateDay(day.copy(dayLabel = dayLabel, activityName = activityName))
    }

    override suspend fun insertRoutine(routine: RoutineEntity): Long =
        routineDao.insertRoutine(routine)

    override suspend fun updateRoutine(routine: RoutineEntity) {
        routineDao.updateRoutine(routine)
    }

    override suspend fun deleteRoutine(routine: RoutineEntity) {
        routineDao.deleteRoutine(routine)
    }

    // ==================== QUERIES ====================

    override fun getMyRoutines(userId: Int): Flow<List<RoutineEntity>> {
        return routineDao.getMyRoutines(userId)
    }

    override fun getGlobalRoutines(): Flow<List<RoutineEntity>> {
        return routineDao.getGlobalRoutines()
    }

    // Obtener solo rutinas propias del usuario (para sección "MIS RUTINAS")
    fun getUserOwnRoutines(userId: Int): Flow<List<RoutineEntity>> {
        return routineDao.getUserOwnRoutines(userId)
    }

    /** Rutinas que un entrenador compartió con el usuario, pendientes de aceptar/rechazar. */
    fun getPendingSharedRoutines(userId: Int): Flow<List<RoutineEntity>> {
        return routineDao.getPendingSharedRoutines(userId)
    }

    /** Plantillas reutilizables del entrenador. */
    fun getTemplates(trainerId: Int): Flow<List<RoutineEntity>> {
        return routineDao.getTemplates(trainerId)
    }

    override fun observeRoutine(routineId: Int): Flow<RoutineEntity?> {
        return routineDao.observeRoutine(routineId)
    }

    // Obtener los ejercicios de una rutina específica (Para cuando le des click)
    fun getExercisesForRoutine(dayId: Int): Flow<List<ExerciseEntity>> {
        return routineDao.getExercisesForDay(dayId)
    }

    /**
     * Guarda una rutina personal: 7 días (cajas), cada uno con 0–10 ejercicios.
     * La rutina es solo del usuario (ownerId = creatorId = userId).
     */
    suspend fun savePersonalRoutine(
        userId: Int,
        routineName: String,
        days: List<DayRoutineInput>
    ): Long {
        require(days.size == 7) { "Se requieren exactamente 7 días" }
        val now = System.currentTimeMillis()

        // Una sola rutina; sus días van en la tabla routine_days
        val routineId = routineDao.insertRoutine(
            RoutineEntity(
                ownerId = userId,
                creatorId = userId,
                name = routineName,
                dayInfo = resumenDeDias(days),
                creationDate = now,
                scheduledTime = now
            )
        ).toInt()

        guardarDias(routineId, days)
        return routineId.toLong()
    }

    /** Crea los días de la rutina con sus ejercicios (resolviendo o creando cada ejercicio). */
    private suspend fun guardarDias(routineId: Int, days: List<DayRoutineInput>) {
        days.forEachIndexed { indice, day ->
            val dayId = routineDao.insertDay(
                RoutineDayEntity(
                    routineId = routineId,
                    dayLabel = day.dayLabel,
                    activityName = day.activityName.trim(),
                    dayOrder = indice
                )
            ).toInt()

            val nombres = day.exerciseNames.map { it.trim() }
                .filter { it.isNotBlank() }
                .take(MAX_EXERCISES_PER_DAY)

            val crossRefs = nombres.mapIndexed { index, nombre ->
                val existente = exerciseDao.getExerciseByName(nombre)
                val exerciseId = existente?.id ?: exerciseDao.insertExercise(
                    ExerciseEntity(
                        name = nombre,
                        category = "Personalizado",
                        description = "",
                        videoUrl = "",
                        isSystemDefault = false
                    )
                ).toInt()
                RoutineExerciseEntity(dayId = dayId, exerciseId = exerciseId, order = index + 1)
            }
            if (crossRefs.isNotEmpty()) routineDao.insertRoutineExercises(crossRefs)
        }
    }

    /** Resumen para la lista: días que tienen ejercicios ("Lunes, Miércoles, Viernes"). */
    private fun resumenDeDias(days: List<DayRoutineInput>): String {
        val conEjercicios = days.filter { d -> d.exerciseNames.any { it.isNotBlank() } }
        return if (conEjercicios.isEmpty()) "Sin días asignados"
        else conEjercicios.joinToString(", ") { it.dayLabel }
    }

    /**
     * Guarda una rutina global/recomendada (ownerId = null), publicada por un admin.
     * Visible para todos los usuarios en "Rutinas recomendadas".
     */
    suspend fun saveGlobalRoutine(
        adminId: Int,
        routineName: String,
        days: List<DayRoutineInput>
    ): Long {
        require(days.size == 7) { "Se requieren exactamente 7 días" }
        val now = System.currentTimeMillis()

        val routineId = routineDao.insertRoutine(
            RoutineEntity(
                ownerId = null,
                creatorId = adminId,
                name = routineName,
                dayInfo = resumenDeDias(days),
                creationDate = now,
                scheduledTime = now
            )
        ).toInt()

        guardarDias(routineId, days)
        return routineId.toLong()
    }

    /**
     * Guarda una rutina creada por el entrenador para un cliente.
     * ownerId = clientId (la ve el usuario como "su" rutina), creatorId = trainerId.
     * @return ID del primer día de la rutina (para notificación / abrir detalle).
     */
    suspend fun saveRoutineForClient(
        trainerId: Int,
        clientId: Int,
        routineName: String,
        days: List<DayRoutineInput>
    ): Long {
        require(days.size == 7) { "Se requieren exactamente 7 días" }
        val now = System.currentTimeMillis()

        // Una sola rutina para el cliente, con sus días
        val routineId = routineDao.insertRoutine(
            RoutineEntity(
                ownerId = clientId,
                creatorId = trainerId,
                name = routineName,
                dayInfo = resumenDeDias(days),
                creationDate = now,
                scheduledTime = now
            )
        ).toInt()

        guardarDias(routineId, days)
        val firstRoutineId = routineId.toLong()

        // Enviar al backend (TrainNow-Rutinas) y notificar al cliente (TrainNow-Comunicaciones).
        // Best-effort: si no hay conexión, la rutina queda local y se puede reintentar.
        try {
            pushRoutineDaysToBackend(trainerId, clientId, routineName, days, now)
            RemoteModule.notificationApi().createNotification(
                NotificationDto(
                    userId = clientId,
                    title = "Nueva rutina asignada",
                    message = "Tu entrenador te asignó la rutina \"$routineName\". ¡A entrenar!",
                    type = "ROUTINE",
                    priority = "HIGH",
                    senderId = trainerId
                )
            )
        } catch (_: Exception) {
            // Sin conexión con los microservicios: la rutina queda guardada localmente.
        }

        return firstRoutineId
    }

    /** Publica cada día de la rutina en TrainNow-Rutinas, resolviendo ejercicios por nombre. */
    private suspend fun pushRoutineDaysToBackend(
        trainerId: Int,
        clientId: Int?,
        routineName: String,
        days: List<DayRoutineInput>,
        creationDate: Long,
        pendingShare: Boolean = false,
        isTemplate: Boolean = false
    ) {
        val routineApi = RemoteModule.routineApi()
        val exerciseApi = RemoteModule.exerciseApi()
        // Catálogo backend por nombre (crea los que falten)
        val backendByName = exerciseApi.getExercises().associateBy { it.name.lowercase() }.toMutableMap()

        for (day in days) {
            val dayInfo = if (day.activityName.isNotBlank()) "${day.dayLabel} - ${day.activityName}" else day.dayLabel
            val created = routineApi.createRoutine(
                RoutineDto(
                    ownerId = clientId,
                    creatorId = trainerId,
                    name = routineName,
                    dayInfo = dayInfo,
                    creationDate = creationDate,
                    scheduledTime = creationDate,
                    pendingShare = pendingShare,
                    isTemplate = isTemplate
                )
            ).body() ?: continue

            val names = day.exerciseNames.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_EXERCISES_PER_DAY)
            val refs = mutableListOf<RoutineExerciseDto>()
            names.forEachIndexed { index, name ->
                val backendExercise = backendByName[name.lowercase()] ?: run {
                    val nuevo = exerciseApi.createExercise(
                        com.shagox.apptrainingnow.data.remote.dto.ExerciseDto(
                            name = name, category = "Personalizado", isSystemDefault = false
                        )
                    ).body()
                    if (nuevo != null) backendByName[name.lowercase()] = nuevo
                    nuevo
                }
                if (backendExercise != null) {
                    refs.add(RoutineExerciseDto(routineId = created.id, exerciseId = backendExercise.id, order = index + 1))
                }
            }
            if (refs.isNotEmpty()) {
                routineApi.setRoutineExercises(created.id, refs)
            }
        }
    }

    /**
     * Sincroniza las rutinas asignadas al usuario desde el backend hacia Room.
     * Identidad: (name + dayInfo + creationDate). Best-effort, tolerante a estar offline.
     */
    suspend fun syncRoutinesFromBackend(userId: Int) {
        try {
            val routineApi = RemoteModule.routineApi()
            val exerciseApi = RemoteModule.exerciseApi()

            // El backend guarda un registro por día; se agrupan por nombre de rutina
            val asignadas = if (userId > 0) routineApi.getRoutinesByOwner(userId) else emptyList()
            val publicas = routineApi.getPublicRoutines()
            val remotas = asignadas + publicas
            if (remotas.isEmpty()) return

            val locales = routineDao.getRoutinesByOwnerOnce(userId) + routineDao.getGlobalRoutinesOnce()
            val nombresLocales = locales.map { it.name to it.ownerId }.toHashSet()
            val catalogo = exerciseApi.getExercises().associateBy { it.id }

            remotas.groupBy { it.name to it.ownerId }.forEach { (clave, diasRemotos) ->
                if (clave in nombresLocales) return@forEach
                val primero = diasRemotos.first()

                val routineId = routineDao.insertRoutine(
                    RoutineEntity(
                        ownerId = primero.ownerId,
                        creatorId = primero.creatorId,
                        name = primero.name,
                        dayInfo = diasRemotos.mapNotNull { it.dayInfo?.substringBefore(" - ") }
                            .distinct().joinToString(", "),
                        creationDate = primero.creationDate ?: System.currentTimeMillis(),
                        scheduledTime = primero.scheduledTime ?: System.currentTimeMillis()
                    )
                ).toInt()

                diasRemotos.forEachIndexed { indice, remota ->
                    val info = remota.dayInfo.orEmpty()
                    val etiqueta = info.substringBefore(" - ").ifBlank { "Día ${indice + 1}" }
                    val actividad = if (info.contains(" - ")) info.substringAfter(" - ") else ""

                    val dayId = routineDao.insertDay(
                        RoutineDayEntity(
                            routineId = routineId,
                            dayLabel = etiqueta,
                            activityName = actividad,
                            dayOrder = indice
                        )
                    ).toInt()

                    val refs = routineApi.getRoutineExercises(remota.id)
                    val crossRefs = refs.mapNotNull { ref ->
                        val nombre = catalogo[ref.exerciseId]?.name ?: return@mapNotNull null
                        val local = exerciseDao.getExerciseByName(nombre)
                            ?: run {
                                exerciseDao.insertExercise(
                                    ExerciseEntity(
                                        name = nombre,
                                        category = catalogo[ref.exerciseId]?.category ?: "Personalizado",
                                        description = catalogo[ref.exerciseId]?.description.orEmpty(),
                                        videoUrl = catalogo[ref.exerciseId]?.videoUrl.orEmpty(),
                                        isSystemDefault = false
                                    )
                                )
                                exerciseDao.getExerciseByName(nombre)
                            } ?: return@mapNotNull null
                        RoutineExerciseEntity(dayId = dayId, exerciseId = local.id, order = ref.order)
                    }
                    if (crossRefs.isNotEmpty()) routineDao.insertRoutineExercises(crossRefs)
                }
            }
        } catch (_: Exception) {
            // Offline o backend caído: se reintenta la próxima vez que se abra la pantalla.
        }
    }

    // ==================== COMPARTIR RUTINAS (entrenador → usuario, con aceptación) ====================

    /**
     * El entrenador comparte una rutina recién creada con un usuario específico (id ya
     * resuelto por la UI a partir del correo o id que escribió). Queda pendiente de
     * aceptación: no aparece en "Mis rutinas" del usuario hasta que la acepte.
     */
    suspend fun shareRoutineWithUser(
        trainerId: Int,
        targetUserId: Int,
        routineName: String,
        days: List<DayRoutineInput>
    ): Long {
        require(days.size == 7) { "Se requieren exactamente 7 días" }
        val now = System.currentTimeMillis()

        val routineId = routineDao.insertRoutine(
            RoutineEntity(
                ownerId = targetUserId,
                creatorId = trainerId,
                name = routineName,
                dayInfo = resumenDeDias(days),
                creationDate = now,
                scheduledTime = now,
                pendingShare = true
            )
        ).toInt()
        guardarDias(routineId, days)

        try {
            pushRoutineDaysToBackend(trainerId, targetUserId, routineName, days, now, pendingShare = true)
        } catch (_: Exception) {
            // Sin conexión: queda local; se reintenta al reabrir la pantalla.
        }
        return routineId.toLong()
    }

    /**
     * Guarda una plantilla reutilizable del entrenador (sin dueño todavía). Se comparte
     * después con distintos usuarios vía [shareTemplateWithUser], sin volver a crearla.
     */
    suspend fun saveAsTemplate(trainerId: Int, routineName: String, days: List<DayRoutineInput>): Long {
        require(days.size == 7) { "Se requieren exactamente 7 días" }
        val now = System.currentTimeMillis()

        val routineId = routineDao.insertRoutine(
            RoutineEntity(
                ownerId = null,
                creatorId = trainerId,
                name = routineName,
                dayInfo = resumenDeDias(days),
                creationDate = now,
                scheduledTime = now,
                isTemplate = true
            )
        ).toInt()
        guardarDias(routineId, days)

        try {
            pushRoutineDaysToBackend(trainerId, null, routineName, days, now, isTemplate = true)
        } catch (_: Exception) {
            // Sin conexión: la plantilla queda guardada localmente.
        }
        return routineId.toLong()
    }

    /** Comparte una plantilla ya guardada con un usuario nuevo (duplica sus días/ejercicios). */
    suspend fun shareTemplateWithUser(templateRoutineId: Int, trainerId: Int, targetUserId: Int): Long {
        val template = routineDao.getRoutineById(templateRoutineId) ?: return -1
        val templateDays = routineDao.getDaysOfRoutine(templateRoutineId)
        if (templateDays.size != 7) return -1
        val dayInputs = templateDays.map { day ->
            val exercises = routineDao.getExercisesForDaySync(day.id)
            DayRoutineInput(
                dayLabel = day.dayLabel,
                activityName = day.activityName,
                exerciseNames = exercises.map { it.name }
            )
        }
        return shareRoutineWithUser(trainerId, targetUserId, template.name, dayInputs)
    }

    /**
     * El usuario acepta una rutina compartida: si todavía no está sincronizada localmente
     * la trae del backend (agrupando sus días, igual que [syncRoutinesFromBackend]), y marca
     * pendingShare = false en ambos lados para que ya aparezca en "Mis rutinas".
     */
    suspend fun acceptSharedRoutine(userId: Int, routineName: String) {
        try {
            val routineApi = RemoteModule.routineApi()
            val remotas = routineApi.getRoutinesByOwner(userId)
                .filter { it.name == routineName && it.pendingShare == true }
            if (remotas.isEmpty()) return
            val primero = remotas.first()

            val yaLocal = routineDao.getRoutinesByOwnerOnce(userId)
                .firstOrNull { it.name == routineName && it.pendingShare }

            val routineId = yaLocal?.id ?: run {
                val exerciseApi = RemoteModule.exerciseApi()
                val catalogo = exerciseApi.getExercises().associateBy { it.id }
                val nuevoId = routineDao.insertRoutine(
                    RoutineEntity(
                        ownerId = userId,
                        creatorId = primero.creatorId,
                        name = primero.name,
                        dayInfo = remotas.mapNotNull { it.dayInfo?.substringBefore(" - ") }
                            .distinct().joinToString(", "),
                        creationDate = primero.creationDate ?: System.currentTimeMillis(),
                        scheduledTime = primero.scheduledTime ?: System.currentTimeMillis(),
                        pendingShare = true
                    )
                ).toInt()
                remotas.forEachIndexed { indice, remota ->
                    val info = remota.dayInfo.orEmpty()
                    val etiqueta = info.substringBefore(" - ").ifBlank { "Día ${indice + 1}" }
                    val actividad = if (info.contains(" - ")) info.substringAfter(" - ") else ""
                    val dayId = routineDao.insertDay(
                        RoutineDayEntity(routineId = nuevoId, dayLabel = etiqueta, activityName = actividad, dayOrder = indice)
                    ).toInt()
                    val refs = routineApi.getRoutineExercises(remota.id)
                    val crossRefs = refs.mapNotNull { ref ->
                        val nombre = catalogo[ref.exerciseId]?.name ?: return@mapNotNull null
                        val local = exerciseDao.getExerciseByName(nombre) ?: run {
                            exerciseDao.insertExercise(
                                ExerciseEntity(
                                    name = nombre,
                                    category = catalogo[ref.exerciseId]?.category ?: "Personalizado",
                                    description = catalogo[ref.exerciseId]?.description.orEmpty(),
                                    videoUrl = catalogo[ref.exerciseId]?.videoUrl.orEmpty(),
                                    isSystemDefault = false
                                )
                            )
                            exerciseDao.getExerciseByName(nombre)
                        } ?: return@mapNotNull null
                        RoutineExerciseEntity(dayId = dayId, exerciseId = local.id, order = ref.order)
                    }
                    if (crossRefs.isNotEmpty()) routineDao.insertRoutineExercises(crossRefs)
                }
                nuevoId
            }

            routineDao.getRoutineById(routineId)?.let { routineDao.updateRoutine(it.copy(pendingShare = false)) }
            remotas.forEach { row -> routineApi.updateRoutine(row.id, row.copy(pendingShare = false)) }
        } catch (_: Exception) {
            // Sin conexión: se puede reintentar aceptar más tarde.
        }
    }

    /** El usuario rechaza una rutina compartida: se borra en el backend y localmente si llegó a bajarse. */
    suspend fun declineSharedRoutine(userId: Int, routineName: String) {
        try {
            val routineApi = RemoteModule.routineApi()
            routineApi.getRoutinesByOwner(userId)
                .filter { it.name == routineName && it.pendingShare == true }
                .forEach { row -> routineApi.deleteRoutine(row.id) }
        } catch (_: Exception) {
            // Sin conexión: nada que borrar remotamente todavía.
        }
        routineDao.getRoutinesByOwnerOnce(userId)
            .filter { it.name == routineName && it.pendingShare }
            .forEach { routineDao.deleteRoutine(it) }
    }
}