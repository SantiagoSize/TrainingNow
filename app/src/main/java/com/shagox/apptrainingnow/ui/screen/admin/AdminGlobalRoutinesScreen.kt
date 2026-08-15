package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lista las rutinas globales (ownerId = null, visibles para todos los usuarios como
 * "recomendadas") para que el admin pueda renombrarlas o eliminarlas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGlobalRoutinesScreen(
    routineRepository: RoutineRepository,
    onBack: () -> Unit,
    actorId: Int = 0,
    actorName: String = "",
    actorRole: String = "ADMIN"
) {
    val auditLogRepository = remember { com.shagox.apptrainingnow.data.repository.AuditLogRepository() }
    val scope = rememberCoroutineScope()
    val rutinas by routineRepository.getGlobalRoutines().collectAsState(initial = emptyList())
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy", Locale.Builder().setLanguage("es").setRegion("CL").build()) }

    var rutinaAEditar by remember { mutableStateOf<RoutineEntity?>(null) }
    var rutinaAEliminar by remember { mutableStateOf<RoutineEntity?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rutinas Globales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VerdeTN,
                    titleContentColor = NegroFondo,
                    navigationIconContentColor = NegroFondo
                )
            )
        },
        containerColor = NegroFondo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Rutinas recomendadas visibles para todos los usuarios (creadas desde \"Entrenamiento Global\").",
                color = GrisTexto,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))

            if (rutinas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no hay rutinas globales publicadas", color = GrisTexto)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(rutinas, key = { it.id }) { rutina ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GrisFondo, RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = VerdeTN)
                            Column(Modifier.weight(1f)) {
                                Text(rutina.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
                            IconButton(onClick = { rutinaAEditar = rutina }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Renombrar", tint = VerdeTN)
                            }
                            IconButton(onClick = { rutinaAEliminar = rutina }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF6B6B))
                            }
                        }
                    }
                }
            }
        }
    }

    // Renombrar
    val editando = rutinaAEditar
    if (editando != null) {
        var nuevoNombre by remember(editando.id) { mutableStateOf(editando.name) }
        AlertDialog(
            onDismissRequest = { if (!isSaving) rutinaAEditar = null },
            containerColor = GrisFondo,
            title = { Text("Renombrar rutina", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    singleLine = true,
                    label = { Text("Nombre de la rutina") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VerdeTN,
                        unfocusedBorderColor = GrisTexto,
                        focusedLabelColor = VerdeTN,
                        cursorColor = VerdeTN
                    )
                )
            },
            confirmButton = {
                Button(
                    enabled = !isSaving && nuevoNombre.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde),
                    onClick = {
                        isSaving = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                routineRepository.updateRoutine(editando.copy(name = nuevoNombre.trim()))
                                auditLogRepository.log(
                                    actorId = actorId,
                                    actorName = actorName,
                                    actorRole = actorRole,
                                    action = "ROUTINE_RENAMED",
                                    targetType = "ROUTINE",
                                    targetId = editando.id,
                                    targetName = nuevoNombre.trim(),
                                    details = "\"${editando.name}\" → \"${nuevoNombre.trim()}\""
                                )
                            }
                            isSaving = false
                            rutinaAEditar = null
                        }
                    }
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = TextoSobreVerde)
                    else Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSaving) rutinaAEditar = null }) {
                    Text("Cancelar", color = GrisTexto)
                }
            }
        )
    }

    // Eliminar
    val aEliminar = rutinaAEliminar
    if (aEliminar != null) {
        AlertDialog(
            onDismissRequest = { rutinaAEliminar = null },
            containerColor = GrisFondo,
            title = { Text("Eliminar rutina global", color = Color.White) },
            text = {
                Text(
                    "\"${aEliminar.name}\" dejará de estar disponible para los usuarios. Esta acción no se puede deshacer.",
                    color = GrisTexto
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B), contentColor = Color.White),
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
                    Text("Cancelar", color = GrisTexto)
                }
            }
        )
    }
}
