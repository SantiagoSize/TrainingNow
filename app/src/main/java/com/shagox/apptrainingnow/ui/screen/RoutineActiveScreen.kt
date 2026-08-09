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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.domain.RoutineDayView
import com.shagox.apptrainingnow.data.domain.RoutineWithDays
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.utils.ReminderHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Variante TN verde más suave para tab Notificaciones seleccionado (mismo tono TN). */
private val VerdeTNMuted = VerdeTN.copy(alpha = 0.85f)

/** Nombres de días en español para coincidir con el día actual. */
private val DIAS_SEMANA_ES = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

private fun nombreDiaHoy(): String {
    val cal = Calendar.getInstance()
    val index = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Lunes, 6=Domingo
    return DIAS_SEMANA_ES[index]
}

/** Índice del día actual en la semana (0 = Lunes, 6 = Domingo). */
private fun indiceDiaHoy(): Int {
    return (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
}

/**
 * Pantalla de detalle de rutina (diseño Full Body: tarjeta verde fecha + Actividad, 2 tabs, Seguimiento).
 */
@Composable
fun RoutineActiveScreen(
    routineRepository: RoutineRepository,
    exerciseRepository: IExerciseRepository? = null,
    userId: Int,
    routineId: Int,
    initialRoutineName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(ReminderHelper.isEnabled(context)) }
    val routineWithDays by routineRepository.getRoutineWithDays(routineId, userId).collectAsState(initial = null)
    val days = routineWithDays?.days ?: emptyList()
    val todayDayName = nombreDiaHoy()
    val todayDayIndex = indiceDiaHoy()
    var selectedDayRoutineId by remember(routineId) { mutableStateOf<Int?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
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
    if (selectedDayRoutineId == null && days.isNotEmpty()) {
        selectedDayRoutineId = days.first().routineId
    }
    val selectedDay: RoutineDayView? = days.find { it.routineId == selectedDayRoutineId } ?: days.firstOrNull()
    val routineName = routineWithDays?.routineName ?: initialRoutineName
    val todayDateLabel = run {
        val cal = Calendar.getInstance()
        val locale = Locale.forLanguageTag("es")
        SimpleDateFormat("EEEE d", locale).format(cal.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }

    var showNotificationTimeDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var notificationHour by remember { mutableStateOf(ReminderHelper.getHour(context)) }
    var notificationMinute by remember { mutableStateOf(ReminderHelper.getMinute(context)) }
    var checkedExerciseIds by remember(selectedDayRoutineId) { mutableStateOf(setOf<Int>()) }
    val allExercisesChecked = selectedDay?.exercises?.isNotEmpty() == true &&
            selectedDay.exercises.all { it.id in checkedExerciseIds }
    val seguimientoStatus = if (allExercisesChecked) "Terminado" else "Pendiente"

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
                            color = NegroFondo,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .padding(horizontal = 16.dp)
                                .background(NegroFondo.copy(alpha = 0.5f))
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(
                                text = "Actividad de hoy:",
                                color = NegroFondo,
                                fontSize = 14.sp
                            )
                            Text(
                                text = selectedDay?.displayActivity ?: "-",
                                color = NegroFondo,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2) Contenedor con borde verde: Calendario | Notificaciones (arriba) + línea verde + Seguimiento + botón amarillo (abajo)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, VerdeTN, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1A1A)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable { showCalendarDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarToday,
                                        contentDescription = "Calendario",
                                        tint = VerdeTN,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Calendario", color = VerdeTN, fontSize = 14.sp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(VerdeTN)
                            )
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
                                                    } else {
                                                        ReminderHelper.cancel(context)
                                                    }
                                                },
                                                onLongPress = {
                                                    notificationHour = ReminderHelper.getHour(context)
                                                    notificationMinute = ReminderHelper.getMinute(context)
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
                                    .background(
                                        VerdeTN
                                    )
                                    .padding(vertical = 14.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = seguimientoStatus,
                                    color = NegroFondo,
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
                            onAddClick = { showAddExerciseDialog = true }
                        )
                    }
                } ?: ExerciseListPlaceholder()
            }
        }
    }

    if (showAddExerciseDialog) {
        val currentDay = selectedDay
        val existingIds = currentDay?.exercises?.map { it.id }?.toSet() ?: emptySet()
        val available = allExercisesForPicker.filter { it.id !in existingIds }
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            containerColor = GrisFondo,
            title = { Text("Agregar ejercicio", color = Color.White, fontWeight = FontWeight.Bold) },
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
                                    Text(exercise.name, color = Color.White, fontSize = 15.sp)
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
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showNotificationTimeDialog) {
        val hour12 = when (val h = notificationHour.coerceIn(0, 23)) {
            0 -> 12
            in 1..12 -> h
            else -> h - 12
        }
        val isPM = notificationHour >= 12
        val minuteOptions = listOf(0, 15, 30, 45)
        AlertDialog(
            onDismissRequest = { showNotificationTimeDialog = false },
            title = { Text("Hora del recordatorio", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Elige a qué hora recordar tu entrenamiento.", color = Color.Gray, fontSize = 14.sp)
                    Text("Hora", color = Color.Gray, fontSize = 12.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1..4, 5..8, 9..12).forEach { range ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                range.forEach { h ->
                                    val selected = hour12 == h
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selected) VerdeTN else Color(0xFF2C2C2C))
                                            .border(1.dp, if (selected) VerdeTN else Color.Gray, RoundedCornerShape(10.dp))
                                            .clickable {
                                                notificationHour = if (isPM) {
                                                    if (h == 12) 12 else h + 12
                                                } else {
                                                    if (h == 12) 0 else h
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = h.toString(),
                                            color = if (selected) Color.Black else Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text("Minutos", color = Color.Gray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        minuteOptions.forEach { m ->
                            val selected = notificationMinute == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) VerdeTN else Color(0xFF2C2C2C))
                                    .border(1.dp, if (selected) VerdeTN else Color.Gray, RoundedCornerShape(12.dp))
                                    .clickable { notificationMinute = m }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = m.toString().padStart(2, '0'),
                                    color = if (selected) Color.Black else Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Text("AM o PM", color = Color.Gray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isPM) VerdeTN else Color(0xFF2C2C2C))
                                .border(1.dp, if (!isPM) VerdeTN else Color.Gray, RoundedCornerShape(12.dp))
                                .clickable { notificationHour = if (hour12 == 12) 0 else hour12 }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AM", color = if (!isPM) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPM) VerdeTN else Color(0xFF2C2C2C))
                                .border(1.dp, if (isPM) VerdeTN else Color.Gray, RoundedCornerShape(12.dp))
                                .clickable { notificationHour = if (hour12 == 12) 12 else hour12 + 12 }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PM", color = if (isPM) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ReminderHelper.saveTime(context, notificationHour, notificationMinute)
                        if (notificationsEnabled) {
                            ReminderHelper.saveUserId(context, userId)
                            ReminderHelper.schedule(context, userId)
                        }
                        showNotificationTimeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = Color.Black)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationTimeDialog = false }) {
                    Text("Cancelar", color = VerdeTN)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showCalendarDialog) {
        CalendarWeekDialog(
            days = days,
            selectedDayRoutineId = selectedDayRoutineId,
            checkedExerciseIds = checkedExerciseIds,
            onDismiss = { showCalendarDialog = false }
        )
    }
}

@Composable
private fun CalendarWeekDialog(
    days: List<RoutineDayView>,
    selectedDayRoutineId: Int?,
    checkedExerciseIds: Set<Int>,
    onDismiss: () -> Unit
) {
    val weekdays = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Semana de entrenamiento", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Estado por día (Lunes a Viernes):",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                weekdays.forEachIndexed { index, dayName ->
                    val dayView = days.getOrNull(index)
                    val status = when {
                        dayView == null -> "Sin entrenamiento"
                        dayView.routineId == selectedDayRoutineId -> {
                            val allChecked = dayView.exercises.isNotEmpty() &&
                                dayView.exercises.all { it.id in checkedExerciseIds }
                            if (allChecked) "Completado" else "Pendiente"
                        }
                        else -> "Pendiente"
                    }
                    val (bgColor, textColor) = when (status) {
                        "Completado" -> VerdeTN to Color.Black
                        "Pendiente" -> VerdeTN to Color.Black
                        else -> Color(0xFF555555) to Color.White
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(bgColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayName,
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.widthIn(min = 100.dp)
                        )
                        Text(
                            text = status,
                            color = textColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = VerdeTN)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
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
        color = if (isSelected) VerdeTNMuted else Color(0xFF2C2C2C)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
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
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        exercises.forEachIndexed { index, ex ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF252525)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VerdeTN)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}. ${ex.name}",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = ex.id in checkedIds,
                        onCheckedChange = { onCheckChange(ex.id, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = VerdeTN,
                            uncheckedColor = Color.Gray
                        )
                    )
                }
            }
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
        color = if (isSelected) VerdeTN else Color(0xFF1E1E1E)
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
