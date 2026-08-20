package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.remote.dto.AuditLogDto
import com.shagox.apptrainingnow.data.repository.AuditLogRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
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

private enum class FiltroActividad(val etiqueta: String, val targetType: String?) {
    TODO("Todo", null),
    EJERCICIOS("Ejercicios", "EXERCISE"),
    CATEGORIAS("Categorías", "CATEGORY"),
    USUARIOS("Usuarios", "USER"),
    RUTINAS("Rutinas", "ROUTINE")
}

/**
 * Registro de actividad administrativa: quién creó/editó ejercicios y categorías,
 * quién sancionó a un usuario (y con qué motivo/duración), y quién publicó o editó
 * rutinas globales. Datos vienen de TrainNow-Usuarios (/api/audit-logs).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActivityLogScreen(onBack: () -> Unit) {
    val auditLogRepository = remember { AuditLogRepository() }
    var filtro by remember { mutableStateOf(FiltroActividad.TODO) }
    var logs by remember { mutableStateOf<List<AuditLogDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun cargar() {
        isLoading = true
        logs = withContext(Dispatchers.IO) { auditLogRepository.getAll(filtro.targetType) }
        isLoading = false
    }

    LaunchedEffect(filtro) { cargar() }

    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.Builder().setLanguage("es").setRegion("CL").build()) }

    Box(modifier = Modifier.fillMaxSize().background(NegroFondo)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== Cabecera con degradado (misma estética que el resto del panel admin) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(VerdeTN.copy(alpha = 0.20f), NegroFondo)))
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BackButtonTN(text = "Volver", onClick = onBack, modifier = Modifier.weight(1f))
                        IconButton(onClick = { scope.launch { cargar() } }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = VerdeTN)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(VerdeTN.copy(alpha = 0.2f))
                                .border(1.dp, VerdeTN.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = VerdeTN,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Actividad",
                                color = TextoPrincipal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Registro de acciones del panel admin",
                                color = GrisTexto,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Carrusel horizontal: con 5 filtros en una fila fija, el último ("Rutinas") se
            // comprimía y el texto quedaba partido letra por letra en pantallas angostas.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroActividad.entries.forEach { opcion ->
                    FilterChip(
                        selected = filtro == opcion,
                        onClick = { filtro = opcion },
                        label = { Text(opcion.etiqueta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerdeTN,
                            selectedLabelColor = TextoSobreVerde,
                            containerColor = GrisFondo,
                            labelColor = TextoPrincipal
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filtro == opcion,
                            borderColor = GrisBorde,
                            selectedBorderColor = VerdeTN
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
                logs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin actividad registrada todavía", color = GrisTexto)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(logs, key = { it.id ?: it.timestamp ?: 0 }) { log ->
                            ActivityLogCard(log = log, formatoFecha = formatoFecha)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLogCard(log: AuditLogDto, formatoFecha: SimpleDateFormat) {
    val (icono, colorIcono, etiquetaAccion) = descripcionAccion(log.action)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrisFondo, RoundedCornerShape(14.dp))
            .border(1.dp, colorIcono.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(colorIcono.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                buildString {
                    append(log.actorName.ifBlank { "Alguien" })
                    append(" · ")
                    append(etiquetaAccion)
                },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!log.targetName.isNullOrBlank()) {
                Text(log.targetName, color = GrisTexto, fontSize = 13.sp)
            }
            if (!log.details.isNullOrBlank()) {
                Text(log.details, color = GrisTexto, fontSize = 12.sp)
            }
            Text(
                buildString {
                    append(log.actorRole)
                    log.timestamp?.let {
                        append(" · ")
                        append(formatoFecha.format(Date(it)))
                    }
                },
                color = GrisTexto,
                fontSize = 11.sp
            )
        }
    }
}

/** Icono, color y etiqueta legible para cada tipo de acción registrada. */
@Composable
private fun descripcionAccion(action: String): Triple<ImageVector, Color, String> {
    return when (action) {
        "EXERCISE_CREATED" -> Triple(Icons.Filled.FitnessCenter, VerdeTN, "creó un ejercicio")
        "EXERCISE_UPDATED" -> Triple(Icons.Filled.Edit, VerdeTN, "editó un ejercicio")
        "EXERCISE_DELETED" -> Triple(Icons.Filled.Delete, Color(0xFFE57373), "eliminó un ejercicio")
        "CATEGORY_CREATED" -> Triple(Icons.Filled.FitnessCenter, VerdeTN, "creó una categoría")
        "CATEGORY_RENAMED" -> Triple(Icons.Filled.Edit, VerdeTN, "renombró una categoría")
        "USER_BANNED" -> Triple(Icons.Filled.Block, Color(0xFFE57373), "baneó a un usuario")
        "USER_SUSPENDED" -> Triple(Icons.Filled.Block, Color(0xFFFFB74D), "suspendió a un usuario")
        "USER_DELETED" -> Triple(Icons.Filled.Delete, Color(0xFFE57373), "eliminó a un usuario")
        "USER_RESTRICTION_LIFTED" -> Triple(Icons.Filled.LockOpen, VerdeTN, "levantó una restricción")
        "ROUTINE_GLOBAL_CREATED" -> Triple(Icons.Filled.CalendarMonth, VerdeTN, "publicó una rutina global")
        "ROUTINE_GLOBAL_UPDATED" -> Triple(Icons.Filled.Edit, VerdeTN, "editó una rutina global")
        "ROUTINE_RENAMED" -> Triple(Icons.Filled.Edit, VerdeTN, "renombró una rutina global")
        "ROUTINE_DELETED" -> Triple(Icons.Filled.Delete, Color(0xFFE57373), "eliminó una rutina global")
        else -> Triple(Icons.Filled.PersonAdd, GrisTexto, action)
    }
}
