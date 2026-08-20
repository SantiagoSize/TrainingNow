package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Popup
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.domain.RoutineDayView
import com.shagox.apptrainingnow.data.domain.RoutineWithDays
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.local.workout.ExerciseLogEntity
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.SuperficieElevada
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.utils.ReminderHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Variante TN verde más suave para tab Notificaciones seleccionado (mismo tono TN). */
private val VerdeTNMuted = VerdeTN.copy(alpha = 0.85f)

/** Nombres de días en español, semana empezando en Domingo (a pedido: así el domingo ya
 *  se puede planificar/ver la semana que viene, en vez de quedar "colgado" al final). */
private val DIAS_SEMANA_ES = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

private fun nombreDiaHoy(): String {
    val cal = Calendar.getInstance()
    val index = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Domingo, 6=Sábado (Calendar.DAY_OF_WEEK: Domingo=1)
    return DIAS_SEMANA_ES[index]
}

/** Índice del día actual en la semana (0 = Domingo, 6 = Sábado). */
private fun indiceDiaHoy(): Int {
    return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
}

/**
 * Pantalla de detalle de rutina (diseño Full Body: tarjeta verde fecha + Actividad, 2 tabs, Seguimiento).
 */
@Composable
fun RoutineActiveScreen(
    routineRepository: RoutineRepository,
    exerciseRepository: IExerciseRepository? = null,
    workoutRepository: com.shagox.apptrainingnow.data.repository.WorkoutRepository? = null,
    userId: Int,
    routineId: Int,
    initialRoutineName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    // La barra de navegación inferior YA NO pasa por acá: cambiar de tab no abandona la rutina
    // (queda guardada en "active_routine_route" y la barra vuelve directo a ella al tocar
    // "Rutina" de nuevo — ver BottomNavigationBarTN). Solo la flecha del header o el back del
    // sistema hacen un abandono real (onBack() abajo borra esa preferencia).
    androidx.activity.compose.BackHandler(enabled = true) {
        showExitDialog = true
    }
    var notificationsEnabled by remember { mutableStateOf(ReminderHelper.isEnabled(context)) }
    // Aviso flotante breve al activar/desactivar recordatorios
    var avisoTexto by remember { mutableStateOf<String?>(null) }
    var avisoActivado by remember { mutableStateOf(true) }
    val routineWithDays by routineRepository.getRoutineWithDays(routineId, userId).collectAsState(initial = null)
    val days = routineWithDays?.days ?: emptyList()
    // Día actual del sistema; se refresca solo cuando cambia la fecha del teléfono
    var todayDayIndex by remember { mutableStateOf(indiceDiaHoy()) }
    var todayDayName by remember { mutableStateOf(nombreDiaHoy()) }
    LaunchedEffect(Unit) {
        while (true) {
            val nuevoIndice = indiceDiaHoy()
            if (nuevoIndice != todayDayIndex) {
                todayDayIndex = nuevoIndice
                todayDayName = nombreDiaHoy()
            }
            kotlinx.coroutines.delay(60_000L) // comprueba cada minuto
        }
    }
    var selectedDayRoutineId by remember(routineId) { mutableStateOf<Int?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    // Día del que se está configurando la hora propia de recordatorio
    var diaConfigurandoHora by remember { mutableStateOf<RoutineDayView?>(null) }
    var horasPorDia by remember(routineId) { mutableStateOf<Map<Int, Pair<Int, Int>>>(emptyMap()) }

    // Cargar las horas propias ya guardadas
    LaunchedEffect(days) {
        if (days.isEmpty()) return@LaunchedEffect
        horasPorDia = days.mapNotNull { vista ->
            routineRepository.getDayReminder(vista.routineId)?.let { vista.routineId to it }
        }.toMap()
    }

    // Progreso semanal persistido en caché (SharedPreferences), se reinicia cada semana
    val weekProgress = remember(routineId, userId) { WeekProgressStore(context, userId, routineId) }
    var completedDays by remember(routineId, userId) { mutableStateOf(weekProgress.load()) }
    // Sesiones COMPLETED del backend/Room de esta semana (fuente adicional de verdad)
    val weekStart = remember(todayDayIndex) { startOfWeekMillis() }
    val weekSessions by (workoutRepository
        ?.getWorkoutsInDateRange(userId, weekStart, weekStart + 7L * 24 * 60 * 60 * 1000)
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val addExerciseScope = rememberCoroutineScope()
    val allExercisesForPicker by (exerciseRepository?.getAllExercises() ?: flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    LaunchedEffect(days, todayDayName, todayDayIndex) {
        if (days.isEmpty()) return@LaunchedEffect
        val matchByLabel = days.indexOfFirst { day ->
            day.dayLabel.equals(todayDayName, ignoreCase = true) ||
                day.dayLabel.contains(todayDayName, ignoreCase = true)
        }
        val index = if (matchByLabel >= 0) matchByLabel else todayDayIndex.coerceIn(0, days.size - 1)
        selectedDayRoutineId = days[index].routineId
    }
    val selectedDay: RoutineDayView? = days.find { it.routineId == selectedDayRoutineId } ?: days.firstOrNull()
    val routineName = routineWithDays?.routineName ?: initialRoutineName

    var showNotificationTimeDialog by remember { mutableStateOf(false) }
    var notificationHour by remember { mutableStateOf(ReminderHelper.getHour(context)) }
    var notificationMinute by remember { mutableStateOf(ReminderHelper.getMinute(context)) }
    // Los ejercicios marcados se recuerdan por día durante toda la semana
    var checkedExerciseIds by remember(selectedDayRoutineId) {
        mutableStateOf(selectedDayRoutineId?.let { weekProgress.loadChecked(it) } ?: emptySet())
    }
    // Persistir cada cambio de marcado
    LaunchedEffect(checkedExerciseIds, selectedDayRoutineId) {
        selectedDayRoutineId?.let { weekProgress.saveChecked(it, checkedExerciseIds) }
    }
    val allExercisesChecked = selectedDay?.exercises?.isNotEmpty() == true &&
            selectedDay.exercises.all { it.id in checkedExerciseIds }

    // Guardar en caché el día como completado y registrar la asistencia en el backend
    val attendanceRepository = remember { com.shagox.apptrainingnow.data.repository.AttendanceRepository() }
    val selectedIndexForProgress = days.indexOfFirst { it.routineId == selectedDayRoutineId }
    val ejerciciosDelDia = selectedDay?.exercises?.size ?: 0
    val nombreSesionDelDia = selectedDay?.displayActivity ?: "Entrenamiento"
    // Fecha (00:00) del día de la semana seleccionado, no la de "hoy": si se marca el
    // checklist del Martes, debe registrarse como Martes aunque hoy sea Lunes.
    val inicioDelDiaSeleccionado = weekStart + selectedIndexForProgress.coerceAtLeast(0) * (24L * 60 * 60 * 1000)
    // Fecha mostrada en el card verde: la del día tocado en la franja semanal, no siempre
    // "hoy" (antes se mostraba fija en la fecha real aunque tocaras otro día).
    val todayDateLabel = run {
        val cal = Calendar.getInstance().apply { timeInMillis = inicioDelDiaSeleccionado }
        val locale = Locale.forLanguageTag("es")
        SimpleDateFormat("EEEE d", locale).format(cal.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }
    androidx.compose.runtime.LaunchedEffect(allExercisesChecked, selectedIndexForProgress) {
        if (selectedIndexForProgress >= 0) {
            val nuevo = if (allExercisesChecked) completedDays + selectedIndexForProgress
                        else completedDays - selectedIndexForProgress
            if (nuevo != completedDays) {
                completedDays = nuevo
                weekProgress.save(nuevo)
            }
            if (allExercisesChecked && userId > 0) {
                // 1) En la base de datos local, con el nombre de la rutina y de la sesión
                workoutRepository?.registrarDiaCompletado(
                    userId = userId,
                    routineId = routineId,
                    nombreRutina = routineName,
                    nombreSesion = nombreSesionDelDia,
                    ejercicios = ejerciciosDelDia,
                    inicioDia = inicioDelDiaSeleccionado
                )
                // 2) En el backend, para el reporte mensual
                attendanceRepository.registerTrainedToday(
                    userId = userId,
                    routineId = routineId,
                    exercisesCompleted = ejerciciosDelDia
                )
            } else if (!allExercisesChecked && userId > 0) {
                // Se desmarcó algo después de haber completado el día: revertir el registro
                // local para que la franja semanal deje de mostrarlo como completado.
                workoutRepository?.desregistrarDiaCompletado(
                    userId = userId,
                    routineId = routineId,
                    inicioDia = inicioDelDiaSeleccionado
                )
            }
        }
    }
    // Estado del seguimiento del día seleccionado, con el mismo criterio de colores de la franja
    val indiceSeleccionado = days.indexOfFirst { it.routineId == selectedDayRoutineId }
    val estadoDelDia: DayStatus = computeDayStates(
        days, completedDays, weekSessions, weekStart, todayDayIndex,
        creationDate = routineWithDays?.header?.creationDate ?: 0L
    ).getOrElse(indiceSeleccionado.coerceAtLeast(0)) { DayStatus.DESCANSO }
    val seguimientoStatus = when {
        allExercisesChecked -> DayStatus.COMPLETADO.label
        else -> estadoDelDia.label
    }
    val seguimientoColor = if (allExercisesChecked) DayStatus.COMPLETADO.color else estadoDelDia.color

    // Sesión de Room asociada al día seleccionado, para registrar series (reps/carga) por
    // ejercicio. Se crea perezosamente (solo cuando el usuario realmente agrega una serie).
    var sessionIdActual by remember(selectedDayRoutineId) { mutableStateOf<Int?>(null) }
    val obtenerSessionId: suspend () -> Int = {
        sessionIdActual ?: (workoutRepository?.obtenerOCrearSesionDelDia(userId, routineId, inicioDelDiaSeleccionado) ?: -1)
            .also { sessionIdActual = it }
    }
    val unidadesImperiales = com.shagox.apptrainingnow.utils.UnitsPreference.esImperial(context)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            ScreenHeaderTN(
                subtitle = "Rutina",
                title = routineName.uppercase(),
                actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onActionClick = { showExitDialog = true },
                actionTint = Color.White,
                actionBackgroundColor = VerdeTN
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 1) Bloque de actividad: fondo verde #26AB4E, texto negro (efecto recortado). Izq: fecha grande | línea | der: "Actividad de hoy:" + "-"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VerdeTN),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = todayDateLabel,
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .padding(horizontal = 16.dp)
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(
                                text = "Actividad de hoy:",
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = selectedDay?.displayActivity ?: "-",
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Franja semanal: estado de cada día (completado / pendiente / descanso)
                WeekStrip(
                    dayStates = computeDayStates(
                        days, completedDays, weekSessions, weekStart, todayDayIndex,
                        creationDate = routineWithDays?.header?.creationDate ?: 0L
                    ),
                    selectedIndex = days.indexOfFirst { it.routineId == selectedDayRoutineId },
                    onDayClick = { index ->
                        days.getOrNull(index)?.let { selectedDayRoutineId = it.routineId }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))

                // 2) Contenedor con borde verde: Notificaciones (arriba) + línea verde + Seguimiento (abajo)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, VerdeTN, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = GrisFondo
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val viewConfig = LocalViewConfiguration.current
                            CompositionLocalProvider(
                                LocalViewConfiguration provides object : ViewConfiguration {
                                    override val longPressTimeoutMillis: Long get() = 400L
                                    override val touchSlop: Float get() = viewConfig.touchSlop
                                    override val minimumTouchTargetSize: androidx.compose.ui.unit.DpSize get() = viewConfig.minimumTouchTargetSize
                                    override val doubleTapMinTimeMillis: Long get() = viewConfig.doubleTapMinTimeMillis
                                    override val doubleTapTimeoutMillis: Long get() = viewConfig.doubleTapTimeoutMillis
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .then(
                                            if (notificationsEnabled) Modifier.background(VerdeTNMuted)
                                            else Modifier
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    notificationsEnabled = !notificationsEnabled
                                                    if (notificationsEnabled) {
                                                        ReminderHelper.saveUserId(context, userId)
                                                        ReminderHelper.schedule(context, userId)
                                                        avisoActivado = true
                                                        avisoTexto = "Recordatorios activados"
                                                    } else {
                                                        ReminderHelper.cancel(context)
                                                        avisoActivado = false
                                                        avisoTexto = "Recordatorios desactivados"
                                                    }
                                                },
                                                onLongPress = {
                                                    // Configura la hora del día que se está viendo
                                                    val dia = selectedDay
                                                    if (dia != null) {
                                                        val propia = horasPorDia[dia.routineId]
                                                        notificationHour = propia?.first ?: ReminderHelper.getHour(context)
                                                        notificationMinute = propia?.second ?: ReminderHelper.getMinute(context)
                                                        diaConfigurandoHora = dia
                                                    }
                                                    showNotificationTimeDialog = true
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = "Notificaciones: tap activar/desactivar, mantener para hora",
                                            tint = if (notificationsEnabled) NegroFondo else VerdeTN,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Notificaciones",
                                            color = if (notificationsEnabled) NegroFondo else VerdeTN,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(VerdeTN)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Seguimiento",
                                    color = VerdeTN,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(seguimientoColor)
                                    .padding(vertical = 14.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = seguimientoStatus,
                                    color = if (seguimientoColor == DayStatus.NO_ENTRENADO.color) Color.White else NegroFondo,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3) Contenido: lista con checkboxes o placeholder
                selectedDay?.let { day ->
                    if (day.exercises.isEmpty()) {
                        ExerciseListPlaceholder()
                    } else {
                        ExerciseListSection(
                            exercises = day.exercises,
                            exerciseCount = day.exerciseCount,
                            canAddMore = day.exerciseCount < 10,
                            checkedIds = checkedExerciseIds,
                            onCheckChange = { id, checked ->
                                checkedExerciseIds = if (checked) checkedExerciseIds + id
                                else checkedExerciseIds - id
                            },
                            onAddClick = { showAddExerciseDialog = true },
                            workoutRepository = workoutRepository,
                            obtenerSessionId = obtenerSessionId,
                            unidadesImperiales = unidadesImperiales
                        )
                    }
                } ?: ExerciseListPlaceholder()
            }
        }
    }

    // Aviso flotante: aparece y se desvanece solo
    AvisoFlotante(
        texto = avisoTexto,
        activado = avisoActivado,
        onOculto = { avisoTexto = null }
    )

    if (showAddExerciseDialog) {
        val currentDay = selectedDay
        val existingIds = currentDay?.exercises?.map { it.id }?.toSet() ?: emptySet()
        val available = allExercisesForPicker.filter { it.id !in existingIds }
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            containerColor = GrisFondo,
            title = { Text("Agregar ejercicio", color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = {
                if (available.isEmpty()) {
                    Text("No hay más ejercicios disponibles.", color = GrisTexto)
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        available.forEach { exercise ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentDay?.let { day ->
                                            addExerciseScope.launch {
                                                routineRepository.addExerciseToDay(day.routineId, exercise.id)
                                            }
                                        }
                                        showAddExerciseDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Column {
                                    Text(exercise.name, color = TextoPrincipal, fontSize = 15.sp)
                                    Text(exercise.category, color = GrisTexto, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(
                    onClick = { showAddExerciseDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GrisBorde, contentColor = Color.White)
                ) { Text("Cerrar") }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Salir de la rutina") },
            text = { Text("¿Estás seguro de que quieres salir? Se perderá el progreso no guardado.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    }
                ) {
                    Text("Salir", color = VerdeTN)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar", color = VerdeTN)
                }
            },
            containerColor = GrisFondo,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showNotificationTimeDialog) {
        RecordatorioHoraDialog(
            hora = notificationHour,
            minuto = notificationMinute,
            onHoraCambio = { notificationHour = it },
            onMinutoCambio = { notificationMinute = it },
            diaEnEdicion = diaConfigurandoHora,
            onGuardar = {
                val dia = diaConfigurandoHora
                if (dia != null) {
                    // Hora propia de ese día de la semana
                    addExerciseScope.launch {
                        routineRepository.setDayReminder(dia.routineId, notificationHour, notificationMinute)
                        horasPorDia = horasPorDia + (dia.routineId to (notificationHour to notificationMinute))
                    }
                    ReminderHelper.saveUserId(context, userId)
                    ReminderHelper.scheduleForDay(
                        context = context,
                        userId = userId,
                        dayId = dia.routineId,
                        dayOrder = days.indexOfFirst { it.routineId == dia.routineId }.coerceAtLeast(0),
                        hour = notificationHour,
                        minute = notificationMinute
                    )
                    avisoTexto = "Recordatorio del ${dia.dayLabel} guardado"
                } else {
                    ReminderHelper.saveTime(context, notificationHour, notificationMinute)
                    if (notificationsEnabled) {
                        ReminderHelper.saveUserId(context, userId)
                        ReminderHelper.schedule(context, userId)
                    }
                    avisoTexto = "Recordatorio guardado"
                }
                diaConfigurandoHora = null
                showNotificationTimeDialog = false
                avisoActivado = true
            },
            onCancelar = {
                diaConfigurandoHora = null
                showNotificationTimeDialog = false
            }
        )
    }

}


@Composable
private fun TabChipFullBody(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) VerdeTNMuted else GrisBorde
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) VerdeTN else Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = if (isSelected) Color(0xFFB8F0C8) else Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ExerciseListPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp),
        colors = CardDefaults.cardColors(containerColor = GrisBorde),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No hay rutina definida para hoy.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ExerciseListSection(
    exercises: List<ExerciseEntity>,
    exerciseCount: Int,
    canAddMore: Boolean,
    checkedIds: Set<Int>,
    onCheckChange: (Int, Boolean) -> Unit,
    onAddClick: () -> Unit,
    workoutRepository: com.shagox.apptrainingnow.data.repository.WorkoutRepository? = null,
    obtenerSessionId: suspend () -> Int = { -1 },
    unidadesImperiales: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        exercises.forEachIndexed { index, ex ->
            FilaEjercicio(
                ex = ex,
                index = index,
                checked = ex.id in checkedIds,
                onCheckChange = { checked -> onCheckChange(ex.id, checked) },
                workoutRepository = workoutRepository,
                obtenerSessionId = obtenerSessionId,
                unidadesImperiales = unidadesImperiales
            )
        }
        if (canAddMore) {
            Text(
                text = "Máximo 10 ejercicios por día. Añadir ejercicio.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable(onClick = onAddClick)
            )
        } else if (exercises.isNotEmpty()) {
            Text(
                text = "Límite alcanzado (10 ejercicios por día).",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Fila de un ejercicio dentro del checklist del día: nombre, botón "+" para registrar series
 * (reps/carga) y el checkbox de completado. Al tocar "+" se abre un diálogo a pantalla
 * completa con el mismo estilo visual que el carrusel de bienvenida (fondo negro, título
 * centrado, campos redondeados y botón verde inferior) donde se agregan líneas numeradas
 * ("Serie 1", "Serie 2"...) que se guardan en Room al instante.
 */
@Composable
private fun FilaEjercicio(
    ex: ExerciseEntity,
    index: Int,
    checked: Boolean,
    onCheckChange: (Boolean) -> Unit,
    workoutRepository: com.shagox.apptrainingnow.data.repository.WorkoutRepository?,
    obtenerSessionId: suspend () -> Int,
    unidadesImperiales: Boolean
) {
    val scope = rememberCoroutineScope()
    var sessionId by remember(ex.id) { mutableStateOf<Int?>(null) }
    var mostrarDialogoSeries by remember(ex.id) { mutableStateOf(false) }
    val series by (
        if (workoutRepository != null && sessionId != null)
            workoutRepository.getSeriesDeEjercicio(sessionId!!, ex.id)
        else flowOf(emptyList())
    ).collectAsState(initial = emptyList())

    suspend fun agregarSerieNueva() {
        val sid = sessionId ?: obtenerSessionId().also { sessionId = it }
        if (sid >= 0) workoutRepository?.agregarSerie(sid, ex.id, reps = 0, cargaKg = null)
    }

    // Al marcar "terminado" sin haber registrado ninguna serie, se guarda un registro marcador
    // (sin reps/carga reales) para que el ejercicio aparezca en el detalle del día del reporte
    // mensual, que antes solo mostraba ejercicios con series manuales. Si se desmarca, se borra
    // ese marcador (no series reales que el usuario sí haya cargado).
    suspend fun marcarTerminadoSinSerie() {
        val sid = sessionId ?: obtenerSessionId().also { sessionId = it }
        if (sid >= 0) {
            workoutRepository?.agregarSerie(
                sid, ex.id, reps = 0, cargaKg = null,
                notes = ExerciseLogEntity.NOTA_TERMINADO_SIN_SERIE
            )
        }
    }

    // Sin marcar: tarjeta gris. Terminado (checkbox marcado): se pone verde y ya no se pueden
    // sumar más series (se oculta el "+"). Al desmarcar vuelve a gris y el "+" reaparece.
    val colorAcento = if (checked) VerdeTN else Color.Gray
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorAcento.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, colorAcento)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${index + 1}. ${ex.name}",
                    color = TextoPrincipal,
                    fontSize = 15.sp
                )
                val seriesReales = series.filterNot { it.notes == ExerciseLogEntity.NOTA_TERMINADO_SIN_SERIE }
                if (seriesReales.isNotEmpty()) {
                    val etiquetaUnidad = if (unidadesImperiales) "lb" else "kg"
                    seriesReales.forEachIndexed { i, log ->
                        val reps = log.actualReps?.toIntOrNull() ?: 0
                        val carga = log.weightKg?.let { kg ->
                            val valor = if (unidadesImperiales) com.shagox.apptrainingnow.utils.UnitsPreference.kgALibras(kg) else kg
                            if (valor == valor.toInt().toDouble()) valor.toInt().toString()
                            else String.format(Locale.US, "%.1f", valor)
                        }
                        Text(
                            text = if (carga != null) "Serie ${i + 1}  |  $reps reps x $carga $etiquetaUnidad"
                            else "Serie ${i + 1}  |  $reps reps",
                            color = colorAcento,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            if (checked) {
                Spacer(modifier = Modifier.width(48.dp))
            } else {
                IconButton(onClick = {
                    scope.launch {
                        if (series.isEmpty()) agregarSerieNueva()
                        mostrarDialogoSeries = true
                    }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Registrar serie", tint = VerdeTN)
                }
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { isChecked ->
                    onCheckChange(isChecked)
                    scope.launch {
                        if (isChecked) {
                            if (series.isEmpty()) marcarTerminadoSinSerie()
                        } else {
                            // Si lo único guardado era el marcador de "terminado sin serie" (no
                            // series reales cargadas por el usuario), se borra al desmarcar.
                            series.singleOrNull { it.notes == ExerciseLogEntity.NOTA_TERMINADO_SIN_SERIE }
                                ?.let { workoutRepository?.borrarSerie(it) }
                        }
                    }
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = VerdeTN,
                    uncheckedColor = Color.Gray
                )
            )
        }
    }

    if (mostrarDialogoSeries) {
        SeriesDialogEjercicio(
            nombreEjercicio = ex.name,
            series = series,
            unidadesImperiales = unidadesImperiales,
            onAgregarSerie = { scope.launch { agregarSerieNueva() } },
            onEditarSerie = { log, reps, cargaKg ->
                scope.launch { workoutRepository?.updateExerciseLog(log.copy(actualReps = reps, weightKg = cargaKg)) }
            },
            onBorrarSerie = { log -> scope.launch { workoutRepository?.borrarSerie(log) } },
            onDismiss = { mostrarDialogoSeries = false }
        )
    }
}

/**
 * Diálogo para registrar las series (reps/carga) de un ejercicio, con la misma estética que
 * el carrusel de bienvenida: fondo negro a pantalla completa, botón "✕" circular arriba a la
 * derecha, título centrado, campos redondeados con los colores estándar de la app y un botón
 * verde de ancho completo al final.
 */
@Composable
private fun SeriesDialogEjercicio(
    nombreEjercicio: String,
    series: List<com.shagox.apptrainingnow.data.local.workout.ExerciseLogEntity>,
    unidadesImperiales: Boolean,
    onAgregarSerie: () -> Unit,
    onEditarSerie: (com.shagox.apptrainingnow.data.local.workout.ExerciseLogEntity, String, Double?) -> Unit,
    onBorrarSerie: (com.shagox.apptrainingnow.data.local.workout.ExerciseLogEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        cursorColor = VerdeTN,
        focusedLabelColor = VerdeTN,
        unfocusedLabelColor = GrisTexto,
        focusedContainerColor = GrisFondo,
        unfocusedContainerColor = GrisFondo
    )
    val etiquetaPeso = if (unidadesImperiales) "Carga (lb)" else "Carga (kg)"

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NegroFondo)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clip(CircleShape)
                    .background(SuperficieElevada)
                    .clickable { onDismiss() }
                    .padding(10.dp)
            ) {
                Text("✕", color = TextoPrincipal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(top = 80.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nombreEjercicio,
                        color = TextoPrincipal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Registra las repeticiones y la carga de cada serie.",
                        color = GrisTexto,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    series.forEachIndexed { i, log ->
                        var repsTexto by remember(log.id) { mutableStateOf(log.actualReps ?: "") }
                        var cargaTexto by remember(log.id) {
                            mutableStateOf(
                                log.weightKg?.let { kg ->
                                    val valor = if (unidadesImperiales)
                                        com.shagox.apptrainingnow.utils.UnitsPreference.kgALibras(kg)
                                    else kg
                                    if (valor == valor.toInt().toDouble()) valor.toInt().toString()
                                    else String.format(Locale.US, "%.1f", valor)
                                } ?: ""
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Serie ${i + 1}",
                                color = VerdeTN,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.widthIn(min = 58.dp)
                            )
                            OutlinedTextField(
                                value = repsTexto,
                                onValueChange = { nuevo ->
                                    // keyboardType = Number solo sugiere el teclado numérico, no
                                    // bloquea letras/símbolos (se pueden pegar o venir de teclado
                                    // físico). Se filtra a mano: solo dígitos.
                                    repsTexto = nuevo.filter { it.isDigit() }
                                    val reps = repsTexto.toIntOrNull() ?: 0
                                    val cargaKg = cargaTexto.replace(",", ".").toDoubleOrNull()?.let {
                                        if (unidadesImperiales) com.shagox.apptrainingnow.utils.UnitsPreference.librasAKg(it) else it
                                    }
                                    onEditarSerie(log, reps.toString(), cargaKg)
                                },
                                label = { Text("Reps", color = GrisTexto) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = fieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cargaTexto,
                                onValueChange = { nuevo ->
                                    // Igual que Reps: se filtra a mano. Acá se permite además UN
                                    // separador decimal (. o ,), para poder escribir cargas como
                                    // "12.5".
                                    val filtrado = StringBuilder()
                                    var yaTieneSeparador = false
                                    for (c in nuevo) {
                                        if (c.isDigit()) {
                                            filtrado.append(c)
                                        } else if ((c == '.' || c == ',') && !yaTieneSeparador) {
                                            filtrado.append(c)
                                            yaTieneSeparador = true
                                        }
                                    }
                                    cargaTexto = filtrado.toString()
                                    val valor = cargaTexto.replace(",", ".").toDoubleOrNull()
                                    val cargaKg = valor?.let {
                                        if (unidadesImperiales) com.shagox.apptrainingnow.utils.UnitsPreference.librasAKg(it) else it
                                    }
                                    onEditarSerie(log, repsTexto.ifBlank { "0" }, cargaKg)
                                },
                                label = { Text(etiquetaPeso, color = GrisTexto) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = fieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onBorrarSerie(log) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Borrar serie", tint = GrisTexto)
                            }
                        }
                    }

                    Text(
                        text = "+  Agregar otra serie",
                        color = VerdeTN,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clickable { onAgregarSerie() }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LISTO", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector = Icons.Filled.FitnessCenter,
    showIcon: Boolean = false,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) VerdeTN else GrisFondo
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showIcon) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

// ==================== VISTA PREVIA ====================
// Nota: El preview completo requiere RoutineRepository inyectado (MainActivity/AppNavGraph).
// Para previsualizar componentes aislados use Preview de DayChip, TabChip o ExerciseListPlaceholder.

// ==================== PROGRESO SEMANAL ====================

/** Nombres largos de los días, de domingo a sábado. */
private val DIAS_SEMANA_LARGOS = listOf(
    "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
)

/** Estado de un día en la semana de entrenamiento. */
enum class DayStatus(val label: String, val color: Color) {
    /** Entrenó ese día. */
    COMPLETADO("Completado", VerdeTN),
    /** Tiene ejercicios asignados y aún está a tiempo (hoy o días siguientes). */
    PENDIENTE("Pendiente", Color(0xFFFFC107)),
    /** Día pasado con ejercicios que no se completaron. */
    NO_ENTRENADO("No entrenado", Color(0xFFE53935)),
    /** Sin ejercicios asignados. */
    DESCANSO("Descanso", Color(0xFF9E9E9E))
}

/** Domingo 00:00 de la semana actual, en epoch millis (semana Domingo→Sábado). */
private fun startOfWeekMillis(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.firstDayOfWeek = java.util.Calendar.SUNDAY
    cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    if (cal.timeInMillis > System.currentTimeMillis()) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
    }
    return cal.timeInMillis
}

/**
 * Guarda el progreso semanal en caché (SharedPreferences).
 * La clave incluye el número de semana, por lo que cada semana empieza limpia.
 */
class WeekProgressStore(
    context: android.content.Context,
    userId: Int,
    routineId: Int
) {
    private val prefs = context.getSharedPreferences("week_progress", android.content.Context.MODE_PRIVATE)
    private val key: String = run {
        val cal = java.util.Calendar.getInstance()
        val semana = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        val anio = cal.get(java.util.Calendar.YEAR)
        "u${userId}_r${routineId}_${anio}w$semana"
    }

    fun load(): Set<Int> = prefs.getStringSet(key, emptySet())
        ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

    fun save(days: Set<Int>) {
        prefs.edit().putStringSet(key, days.map { it.toString() }.toSet()).apply()
    }

    /** Ejercicios marcados de un día concreto (se conservan al cambiar de día). */
    fun loadChecked(dayId: Int): Set<Int> =
        prefs.getStringSet("${key}_d$dayId", emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

    fun saveChecked(dayId: Int, exerciseIds: Set<Int>) {
        prefs.edit()
            .putStringSet("${key}_d$dayId", exerciseIds.map { it.toString() }.toSet())
            .apply()
    }
}

/** 00:00 del día que contiene [millis]. Se usa para comparar solo por fecha, sin hora. */
private fun startOfDayMillis(millis: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/**
 * Calcula el estado de los 7 días:
 * - COMPLETADO: marcado en caché o con sesión COMPLETED registrada esa jornada.
 * - PENDIENTE: el día tiene ejercicios asignados pero no se completó.
 * - DESCANSO: no hay ejercicios para ese día (o el día es anterior a la creación de la rutina/cuenta).
 * - NO_ENTRENADO: día pasado con ejercicios pendientes, SOLO si la rutina ya existía ese día.
 *
 * [creationDate] evita el bug de mostrar "No entrenado" en días previos a que el usuario
 * creara su cuenta o la rutina (ej: cuenta creada un miércoles no debe mostrar lunes/martes
 * en rojo, ya que es obvio que no pudo entrenar en días que no existía la cuenta).
 */
private fun computeDayStates(
    days: List<RoutineDayView>,
    completedDays: Set<Int>,
    weekSessions: List<com.shagox.apptrainingnow.data.local.workout.WorkoutSessionEntity>,
    weekStart: Long,
    indiceHoy: Int = indiceDiaHoy(),
    creationDate: Long = 0L
): List<DayStatus> {
    val dayMillis = 24L * 60 * 60 * 1000
    val creationDayStart = if (creationDate > 0) startOfDayMillis(creationDate) else 0L
    return (0..6).map { index ->
        val dayView = days.getOrNull(index)
        val tieneEjercicios = dayView != null && dayView.exercises.isNotEmpty()
        val sesionCompletada = weekSessions.any { s ->
            s.status == "COMPLETED" &&
                    (s.startTime - weekStart) in (index * dayMillis) until ((index + 1) * dayMillis)
        }
        val inicioDeEsteDia = weekStart + index * dayMillis
        val esAntesDeExistirLaRutina = creationDayStart > 0 && inicioDeEsteDia < creationDayStart
        when {
            index in completedDays || sesionCompletada -> DayStatus.COMPLETADO
            // Día anterior a que existiera la rutina/cuenta: no cuenta como "no entrenado"
            esAntesDeExistirLaRutina -> DayStatus.DESCANSO
            // Días ya pasados de esta semana con plan sin cumplir
            tieneEjercicios && index < indiceHoy -> DayStatus.NO_ENTRENADO
            tieneEjercicios -> DayStatus.PENDIENTE
            else -> DayStatus.DESCANSO
        }
    }
}

/**
 * Franja semanal de círculos: verde = completado, amarillo = pendiente, gris = descanso.
 * Tap = ir a ese día (permite volver a días anteriores y editarlos).
 * Mantener presionado = muestra un cartel con el estado.
 */
@Composable
private fun WeekStrip(
    dayStates: List<DayStatus>,
    selectedIndex: Int,
    onDayClick: (Int) -> Unit
) {
    val iniciales = listOf("D", "L", "M", "X", "J", "V", "S")
    val hoy = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayStates.forEachIndexed { index, status ->
                Box(contentAlignment = Alignment.TopCenter) {
                    val esHoy = index == hoy
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            // Solo el día de hoy va relleno; los demás únicamente con borde
                            .background(if (esHoy) status.color else Color.Transparent)
                            .border(
                                width = if (index == selectedIndex) 2.5.dp else 1.5.dp,
                                color = if (index == selectedIndex) Color.White else status.color,
                                shape = CircleShape
                            )
                            .pointerInput(index) {
                                detectTapGestures(
                                    onTap = { onDayClick(index) },
                                    onLongPress = { tooltipIndex = index },
                                    onPress = {
                                        // Al soltar el dedo se oculta el cartel
                                        tryAwaitRelease()
                                        tooltipIndex = null
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iniciales[index],
                            color = if (esHoy) NegroFondo else status.color,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cartel al mantener presionado
                    if (tooltipIndex == index) {
                        Popup(
                            alignment = Alignment.TopCenter,
                            offset = androidx.compose.ui.unit.IntOffset(0, -110),
                            onDismissRequest = { tooltipIndex = null }
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SuperficieElevada)
                                    .border(1.5.dp, status.color, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = status.label,
                                    color = TextoPrincipal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mensaje breve que aparece arriba y se desvanece solo (1,4 s).
 * Verde al activar los recordatorios, gris al desactivarlos.
 */
@Composable
private fun AvisoFlotante(
    texto: String?,
    activado: Boolean,
    onOculto: () -> Unit
) {
    var visible by remember(texto) { mutableStateOf(texto != null) }

    LaunchedEffect(texto) {
        if (texto != null) {
            visible = true
            kotlinx.coroutines.delay(1400L)
            visible = false
            kotlinx.coroutines.delay(250L)
            onOculto()
        }
    }

    if (texto == null) return

    val color = if (activado) VerdeTN else Color(0xFF9E9E9E)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(180)
            ) + androidx.compose.animation.slideInVertically(
                initialOffsetY = { -it },
                animationSpec = androidx.compose.animation.core.tween(220)
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(220)
            ) + androidx.compose.animation.slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = androidx.compose.animation.core.tween(220)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SuperficieElevada)
                    .border(1.5.dp, color, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = texto,
                    color = TextoPrincipal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Selector de la hora del recordatorio de entrenamiento.
 * Muestra la hora elegida en grande y permite ajustarla con atajos rápidos
 * o con los controles de hora/minuto.
 */
@Composable
private fun RecordatorioHoraDialog(
    hora: Int,
    minuto: Int,
    diaEnEdicion: RoutineDayView? = null,
    onHoraCambio: (Int) -> Unit,
    onMinutoCambio: (Int) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    val horaValida = hora.coerceIn(0, 23)
    val hora12 = when (horaValida) {
        0 -> 12
        in 1..12 -> horaValida
        else -> horaValida - 12
    }
    val sufijo = if (horaValida >= 12) "PM" else "AM"
    val descripcion = when (horaValida) {
        in 5..11 -> "Entrenamiento por la mañana"
        in 12..17 -> "Entrenamiento por la tarde"
        in 18..21 -> "Entrenamiento por la noche"
        else -> "Recordatorio de madrugada"
    }

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = GrisFondo,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = if (diaEnEdicion != null) "RECORDATORIO · ${diaEnEdicion.dayLabel.uppercase()}" else "RECORDATORIO",
                    color = VerdeTN,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (diaEnEdicion != null) {
                        "¿A qué hora entrenas el ${diaEnEdicion.dayLabel.lowercase()}?"
                    } else {
                        "¿A qué hora entrenas?"
                    },
                    color = TextoPrincipal,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ===== Hora elegida, en grande, con AM/PM =====
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SuperficieElevada)
                        .border(1.5.dp, VerdeTN, RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "%02d:%02d".format(horaValida, minuto),
                        color = VerdeTN,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "%d:%02d %s".format(hora12, minuto, sufijo),
                        color = TextoPrincipal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = descripcion,
                        color = GrisTexto,
                        fontSize = 12.sp
                    )
                }

                if (diaEnEdicion != null) {
                    Text(
                        text = "Cada día puede tener su propia hora. Cambia de día en la franja de arriba y mantén presionada la campana para configurarlo.",
                        color = GrisTexto,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                // ===== Ajuste de hora y minutos =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectorNumerico(
                        etiqueta = "Hora",
                        valor = "%02d".format(horaValida),
                        onMenos = { onHoraCambio(if (horaValida == 0) 23 else horaValida - 1) },
                        onMas = { onHoraCambio(if (horaValida == 23) 0 else horaValida + 1) },
                        modifier = Modifier.weight(1f)
                    )
                    SelectorNumerico(
                        etiqueta = "Minutos",
                        valor = "%02d".format(minuto),
                        onMenos = { onMinutoCambio(if (minuto == 0) 45 else minuto - 15) },
                        onMas = { onMinutoCambio(if (minuto >= 45) 0 else minuto + 15) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGuardar,
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("GUARDAR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = GrisTexto, fontSize = 13.sp)
            }
        }
    )
}

/** Control - / valor / + usado para ajustar hora y minutos. */
@Composable
private fun SelectorNumerico(
    etiqueta: String,
    valor: String,
    onMenos: () -> Unit,
    onMas: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SuperficieElevada)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(etiqueta, color = GrisTexto, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonCircular("−", onMenos)
            Text(
                text = valor,
                color = TextoPrincipal,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            BotonCircular("+", onMas)
        }
    }
}

@Composable
private fun BotonCircular(simbolo: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(VerdeTN.copy(alpha = 0.18f))
            .border(1.dp, VerdeTN.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(simbolo, color = VerdeTN, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
