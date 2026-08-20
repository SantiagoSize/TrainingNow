package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lista las rutinas globales (ownerId = null, visibles para todos los usuarios como
 * "recomendadas"). Editar abre el mismo editor completo día a día que usa "Entrenamiento
 * Global" (CreateRoutineScreen en modo edición) en vez de un simple renombrado — antes solo
 * se podía cambiar el nombre, no los días ni los ejercicios.
 */
@Composable
fun AdminGlobalRoutinesScreen(
    routineRepository: RoutineRepository,
    onBack: () -> Unit,
    onEditRoutine: (Int) -> Unit,
    actorId: Int = 0,
    actorName: String = "",
    actorRole: String = "ADMIN"
) {
    val auditLogRepository = remember { com.shagox.apptrainingnow.data.repository.AuditLogRepository() }
    val scope = rememberCoroutineScope()
    val rutinas by routineRepository.getGlobalRoutines().collectAsState(initial = emptyList())
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy", Locale.Builder().setLanguage("es").setRegion("CL").build()) }

    var rutinaAEliminar by remember { mutableStateOf<RoutineEntity?>(null) }

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
                    BackButtonTN(text = "Volver", onClick = onBack)
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
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = VerdeTN,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Rutinas Globales",
                                color = TextoPrincipal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${rutinas.size} recomendada(s) visible(s) para todos",
                                color = GrisTexto,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(4.dp))

                if (rutinas.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = GrisTexto.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Aún no hay rutinas globales publicadas", color = GrisTexto, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Publica una desde \"Entrenamiento Global\" en el panel admin",
                                color = VerdeTN,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rutinas, key = { it.id }) { rutina ->
                            RutinaGlobalCard(
                                rutina = rutina,
                                formatoFecha = formatoFecha,
                                onEdit = { onEditRoutine(rutina.id) },
                                onDelete = { rutinaAEliminar = rutina }
                            )
                        }
                    }
                }
            }
        }
    }

    // Eliminar
    val aEliminar = rutinaAEliminar
    if (aEliminar != null) {
        AlertDialog(
            onDismissRequest = { rutinaAEliminar = null },
            containerColor = GrisFondo,
            title = { Text("Eliminar rutina global", color = TextoPrincipal) },
            text = {
                Text(
                    "\"${aEliminar.name}\" dejará de estar disponible para los usuarios. Esta acción no se puede deshacer.",
                    color = GrisTexto
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373), contentColor = Color.White),
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                routineRepository.deleteRoutine(aEliminar)
                                auditLogRepository.log(
                                    actorId = actorId,
                                    actorName = actorName,
                                    actorRole = actorRole,
                                    action = "ROUTINE_DELETED",
                                    targetType = "ROUTINE",
                                    targetId = aEliminar.id,
                                    targetName = aEliminar.name
                                )
                            }
                            rutinaAEliminar = null
                        }
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { rutinaAEliminar = null }) {
                    Text("Cancelar", color = VerdeTN)
                }
            }
        )
    }
}

@Composable
private fun RutinaGlobalCard(
    rutina: RoutineEntity,
    formatoFecha: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VerdeTN.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = VerdeTN, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(rutina.name, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                rutina.dayInfo.ifBlank { "Sin días asignados" },
                color = GrisTexto,
                fontSize = 12.sp,
                maxLines = 1
            )
            Text(
                "Publicada el ${formatoFecha.format(Date(rutina.creationDate))}",
                color = GrisTexto,
                fontSize = 11.sp
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Editar rutina", tint = VerdeTN)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar rutina", tint = Color(0xFFFF6B6B))
        }
    }
}
