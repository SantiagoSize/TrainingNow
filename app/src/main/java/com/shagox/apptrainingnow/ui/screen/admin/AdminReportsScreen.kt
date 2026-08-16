package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.remote.dto.ReportDto
import com.shagox.apptrainingnow.data.repository.ReportRepository
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class FiltroReporte(val etiqueta: String, val status: String?) {
    PENDIENTES("Pendientes", "PENDING"),
    TODOS("Todos", null)
}

/**
 * Reportes de usuarios hechos por otros usuarios (ej: desde el menú de opciones de un
 * contacto en el chat). El admin puede descartarlo (sin fundamento) o ir a Sanciones a
 * banear/suspender con el mismo motivo. Datos vienen de TrainNow-Usuarios (/api/reports).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(onBack: () -> Unit, onIrASancionar: (Int) -> Unit) {
    val reportRepository = remember { ReportRepository() }
    var filtro by remember { mutableStateOf(FiltroReporte.PENDIENTES) }
    var reportes by remember { mutableStateOf<List<ReportDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun cargar() {
        isLoading = true
        reportes = withContext(Dispatchers.IO) { reportRepository.getAll(filtro.status) }
        isLoading = false
    }

    LaunchedEffect(filtro) { cargar() }

    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.Builder().setLanguage("es").setRegion("CL").build()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { cargar() } }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = NegroFondo)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VerdeTN,
                    titleContentColor = NegroFondo,
                    navigationIconContentColor = NegroFondo,
                    actionIconContentColor = NegroFondo
                )
            )
        },
        containerColor = NegroFondo
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroReporte.entries.forEach { opcion ->
                    FilterChip(
                        selected = filtro == opcion,
                        onClick = { filtro = opcion },
                        label = { Text(opcion.etiqueta, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerdeTN,
                            selectedLabelColor = TextoSobreVerde,
                            containerColor = GrisFondo,
                            labelColor = Color.White
                        )
                    )
                }
            }

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VerdeTN)
                    }
                }
                reportes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin reportes por ahora", color = GrisTexto)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reportes, key = { it.id ?: it.timestamp ?: 0 }) { reporte ->
                            ReportCard(
                                reporte = reporte,
                                formatoFecha = formatoFecha,
                                onDescartar = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            reportRepository.resolver(reporte.id ?: return@withContext, "DISMISSED")
                                        }
                                        cargar()
                                    }
                                },
                                onSancionar = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            reportRepository.resolver(reporte.id ?: return@withContext, "REVIEWED")
                                        }
                                        onIrASancionar(reporte.reportedId.toInt())
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    reporte: ReportDto,
    formatoFecha: SimpleDateFormat,
    onDescartar: () -> Unit,
    onSancionar: () -> Unit
) {
    val pendiente = reporte.status == "PENDING"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrisFondo, RoundedCornerShape(14.dp))
            .border(1.dp, (if (pendiente) Color(0xFFFFB74D) else VerdeTN).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            "Reportado: ${reporte.reportedName} (ID ${reporte.reportedId})",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text("Motivo: ${reporte.reason}", color = TextoPrincipal, fontSize = 13.sp)
        if (!reporte.details.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(reporte.details, color = GrisTexto, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Reportado por ${reporte.reporterName} (ID ${reporte.reporterId})" +
                (reporte.timestamp?.let { " · ${formatoFecha.format(Date(it))}" } ?: ""),
            color = GrisTexto,
            fontSize = 11.sp
        )
        if (pendiente) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDescartar) {
                    Text("Descartar", color = GrisTexto)
                }
                Button(
                    onClick = onSancionar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ir a sancionar")
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (reporte.status == "REVIEWED") Icons.Filled.Block else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (reporte.status == "REVIEWED") Color(0xFFE57373) else VerdeTN,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (reporte.status == "REVIEWED") "Se sancionó al usuario" else "Descartado",
                    color = GrisTexto,
                    fontSize = 12.sp
                )
            }
        }
    }
}
