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
    repository: AttendanceRepository = remember { AttendanceRepository() }
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<MonthlyReportDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val monthKey = remember(monthOffset) { AttendanceRepository.monthKeyOffset(monthOffset) }

    LaunchedEffect(monthKey, userId) {
        loading = true
        error = null
        repository.getMonthlyReport(userId, monthKey).fold(
            onSuccess = { report = it; loading = false },
            onFailure = { error = it.message; loading = false }
        )
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
                color = Color.White,
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

            report != null -> ReportContent(report!!)
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
        MetricCard("ENTRENÓ", "${report.daysTrained}", "días", VerdeTN, Modifier.weight(1f))
        MetricCard("LO DEJÓ", "${report.daysMissed}", "días", RojoTN, Modifier.weight(1f))
        MetricCard("DESCANSO", "${report.daysRest}", "días", GrisTexto, Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard("RACHA ACTUAL", "${report.currentStreak}", "días seguidos", AmarilloTN, Modifier.weight(1f))
        MetricCard("MEJOR RACHA", "${report.longestStreak}", "días seguidos", VerdeTN, Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard("EJERCICIOS", "${report.totalExercises}", "completados", VerdeTN, Modifier.weight(1f))
        MetricCard("TIEMPO", "${report.totalMinutes}", "minutos", VerdeTN, Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Detalle día a día
    Text(
        text = "DETALLE DEL MES",
        color = VerdeTN,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )

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
                "Aún no hay entrenamientos registrados este mes.\n¡Completa una rutina para empezar!",
                color = GrisTexto,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            report.days.forEach { day ->
                val (color, etiqueta) = when (day.status) {
                    "TRAINED" -> VerdeTN to "Entrenó"
                    "MISSED" -> RojoTN to "No entrenó"
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
                            color = Color.White,
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
        Text(title, color = GrisTexto, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(unit, color = GrisTexto, fontSize = 10.sp)
    }
}
