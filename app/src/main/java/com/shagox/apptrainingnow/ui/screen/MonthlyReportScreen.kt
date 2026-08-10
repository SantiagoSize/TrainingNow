package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.remote.dto.MonthlyReportDto
import com.shagox.apptrainingnow.data.repository.AttendanceRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal

private val AmarilloTN = Color(0xFFFFC107)
private val RojoTN = Color(0xFFE53935)

/**
 * Reporte mensual de entrenamiento del usuario.
 * Muestra adherencia, días entrenados/perdidos/descanso, rachas y el detalle del mes.
 */
@Composable
fun MonthlyReportScreen(
    userId: Int,
    onBack: () -> Unit,
    /** Sin cuenta: el reporte se calcula desde la base local en vez del servidor. */
    esInvitado: Boolean = false,
    workoutRepository: com.shagox.apptrainingnow.data.repository.WorkoutRepository? = null,
    repository: AttendanceRepository = remember { AttendanceRepository() }
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<MonthlyReportDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val monthKey = remember(monthOffset) { AttendanceRepository.monthKeyOffset(monthOffset) }

    LaunchedEffect(monthKey, userId, esInvitado) {
        loading = true
        error = null
        if (esInvitado && workoutRepository != null) {
            // Invitado: se usa el historial guardado en el teléfono
            report = workoutRepository.reporteMensualLocal(userId, monthKey)
            loading = false
        } else {
            repository.getMonthlyReport(userId, monthKey).fold(
                onSuccess = { report = it; loading = false },
                onFailure = { error = it.message; loading = false }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackButtonTN(text = "Volver", onClick = onBack)
        ScreenHeaderTN(
            subtitle = "Tu avance",
            title = "MENSUAL",
            actionIcon = Icons.Filled.BarChart,
            onActionClick = {}
        )

        // Selector de mes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthOffset -= 1 }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mes anterior", tint = VerdeTN)
            }
            Text(
                text = AttendanceRepository.monthLabel(monthKey),
                color = TextoPrincipal,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { if (monthOffset < 0) monthOffset += 1 },
                enabled = monthOffset < 0
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    "Mes siguiente",
                    tint = if (monthOffset < 0) VerdeTN else GrisBorde
                )
            }
        }

        when {
            loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = VerdeTN) }

            error != null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(error!!, color = GrisTexto, fontSize = 14.sp, textAlign = TextAlign.Center)
            }

            report != null -> {
                if (esInvitado) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmarilloTN.copy(alpha = 0.12f))
                            .border(1.dp, AmarilloTN.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Estás sin cuenta: este avance solo se guarda en este teléfono. Crea tu cuenta para conservarlo.",
                            color = AmarilloTN,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                ReportContent(report!!)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ReportContent(report: MonthlyReportDto) {
    // Anillo de adherencia
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.5.dp, VerdeTN, RoundedCornerShape(16.dp))
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${report.adherencePercent}%",
                color = VerdeTN,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            )
            Text("ADHERENCIA DEL MES", color = GrisTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            // Barra de progreso
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(GrisBorde)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(report.adherencePercent / 100f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(VerdeTN)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Tarjetas de métricas
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard("Entrenaste", "${report.daysTrained}", "días", VerdeTN, Modifier.weight(1f))
        MetricCard("No entrenaste", "${report.daysMissed}", "días", RojoTN, Modifier.weight(1f))
        MetricCard("Descansaste", "${report.daysRest}", "días", GrisTexto, Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            "Racha",
            "${report.currentStreak}",
            if (report.currentStreak == 1) "día seguido" else "días seguidos",
            AmarilloTN,
            Modifier.weight(1f)
        )
        MetricCard("Ejercicios", "${report.totalExercises}", "completados", VerdeTN, Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(22.dp))

    // ===== Calendario del mes =====
    Text(
        text = "CALENDARIO DEL MES",
        color = VerdeTN,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
    CalendarioMensual(report)

    Spacer(modifier = Modifier.height(22.dp))

    // Detalle día a día
    Text(
        text = "DETALLE DEL MES",
        color = VerdeTN,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )

    // La evaluación acompaña al detalle del mes
    EvaluacionDelMes(report)
    Spacer(modifier = Modifier.height(12.dp))

    if (report.days.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GrisFondo)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Todavía no registras entrenamientos este mes.\nCompleta una rutina y aparecerá aquí.",
                color = GrisTexto,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            report.days.forEach { day ->
                val (color, etiqueta) = when (day.status) {
                    "TRAINED" -> VerdeTN to "Entrenaste"
                    "MISSED" -> RojoTN to "No entrenaste"
                    else -> GrisTexto to "Descanso"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GrisFondo)
                        .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = day.date.takeLast(5).replace("-", "/"),
                            color = TextoPrincipal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (day.exercisesCompleted > 0) {
                            Text(
                                "${day.exercisesCompleted} ej.",
                                color = GrisTexto,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(etiqueta, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GrisFondo)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = GrisTexto, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(unit, color = GrisTexto, fontSize = 10.sp)
    }
}

/**
 * Cuadrícula del mes: una casilla por día, coloreada según lo que ocurrió.
 * Verde = entrenó · Rojo = no entrenó · Gris = descanso · Vacío = sin registro.
 */
@Composable
private fun CalendarioMensual(report: MonthlyReportDto) {
    val calendario = remember(report.month) {
        java.util.Calendar.getInstance().apply {
            val partes = report.month.split("-")
            if (partes.size == 2) {
                set(java.util.Calendar.YEAR, partes[0].toIntOrNull() ?: get(java.util.Calendar.YEAR))
                set(java.util.Calendar.MONTH, (partes[1].toIntOrNull() ?: 1) - 1)
            }
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
    }
    val diasDelMes = calendario.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    // Desplazamiento para que la primera fila empiece en el día correcto (0 = lunes)
    val primerDiaSemana = (calendario.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7

    val estadoPorDia = remember(report) {
        report.days.associate { dia ->
            (dia.date.takeLast(2).toIntOrNull() ?: 0) to dia.status
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, GrisBorde, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Cabecera de días
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { letra ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(letra, color = GrisTexto, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Casillas
        val totalCeldas = primerDiaSemana + diasDelMes
        val filas = (totalCeldas + 6) / 7
        repeat(filas) { fila ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                repeat(7) { columna ->
                    val indice = fila * 7 + columna
                    val numeroDia = indice - primerDiaSemana + 1
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (numeroDia in 1..diasDelMes) {
                            val estado = estadoPorDia[numeroDia]
                            val color = when (estado) {
                                "TRAINED" -> VerdeTN
                                "MISSED" -> RojoTN
                                "REST" -> GrisTexto
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (color == Color.Transparent) Color.Transparent else color)
                                    .border(
                                        1.dp,
                                        if (color == Color.Transparent) GrisBorde else color,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$numeroDia",
                                    color = if (color == Color.Transparent) GrisTexto else NegroFondo,
                                    fontSize = 11.sp,
                                    fontWeight = if (color == Color.Transparent) FontWeight.Normal else FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LeyendaCalendario(VerdeTN, "Entrenaste")
            LeyendaCalendario(RojoTN, "No entrenaste")
            LeyendaCalendario(GrisTexto, "Descanso")
        }
    }
}

@Composable
private fun LeyendaCalendario(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(texto, color = GrisTexto, fontSize = 10.sp)
    }
}

/**
 * Evaluación del mes: dice al usuario cómo va y si necesita mejorar,
 * en función de su adherencia y de los días entrenados.
 */
@Composable
private fun EvaluacionDelMes(report: MonthlyReportDto) {
    // Cara de texto según el desempeño del mes
    data class Evaluacion(val cara: String, val titulo: String, val mensaje: String, val color: Color)

    val evaluacion = when {
        report.daysTrained == 0 -> Evaluacion(
            ":,(",
            "Aún sin entrenamientos",
            "Este mes no hay sesiones registradas. Empieza con dos o tres días a la semana: lo importante es volver a la rutina.",
            RojoTN
        )
        report.adherencePercent >= 90 -> Evaluacion(
            ":D",
            "¡Excelente constancia!",
            "Cumpliste el ${report.adherencePercent}% de tus entrenamientos con ${report.daysTrained} días este mes. Mantén este ritmo y considera subir la carga.",
            VerdeTN
        )
        report.adherencePercent >= 70 -> Evaluacion(
            ":)",
            "Vas por buen camino",
            "Con ${report.adherencePercent}% de cumplimiento y ${report.daysTrained} días entrenados estás sobre el promedio. Recuperar ${report.daysMissed} sesión(es) perdida(s) te acercaría a la excelencia.",
            VerdeTN
        )
        report.adherencePercent >= 50 -> Evaluacion(
            ":/",
            "Puedes mejorar",
            "Cumpliste el ${report.adherencePercent}% de lo planificado y dejaste ${report.daysMissed} sesión(es) sin hacer. Prueba a fijar una hora de recordatorio para cada día.",
            AmarilloTN
        )
        else -> Evaluacion(
            ":(",
            "Necesitas retomar el ritmo",
            "Solo cumpliste el ${report.adherencePercent}% del plan. Reduce el número de días de la rutina para que sea sostenible y ve subiendo poco a poco.",
            RojoTN
        )
    }
    val (cara, titulo, mensaje, color) = evaluacion

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cara,
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = titulo,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = mensaje,
            color = TextoPrincipal.copy(alpha = 0.88f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        if (report.currentStreak >= 3) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ":D  Llevas ${report.currentStreak} días seguidos entrenando.",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
