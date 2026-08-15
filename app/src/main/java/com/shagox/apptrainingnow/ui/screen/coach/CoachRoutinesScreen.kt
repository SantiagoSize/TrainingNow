package com.shagox.apptrainingnow.ui.screen.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.viewmodel.CoachViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de gestión de rutinas para el entrenador.
 * Estilo app: NegroFondo, ScreenHeaderTN, tarjetas GrisFondo + borde VerdeTN.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachRoutinesScreen(
    viewModel: CoachViewModel,
    onCreateRoutine: () -> Unit,
    onRoutineClick: (Int) -> Unit,
    routineRepository: com.shagox.apptrainingnow.data.repository.RoutineRepository? = null,
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository? = null,
    trainerId: Int = 0
) {
    val myRoutines by viewModel.myRoutines.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<RoutineEntity?>(null) }
    var shareDialogFor by remember { mutableStateOf<RoutineEntity?>(null) }
    var selectedFilter by remember { mutableStateOf(RoutineFilter.ALL) }
    val scope = rememberCoroutineScope()

    val filteredRoutines = remember(myRoutines, selectedFilter) {
        when (selectedFilter) {
            RoutineFilter.ALL -> myRoutines.filter { !it.isTemplate }
            RoutineFilter.GLOBAL -> myRoutines.filter { it.ownerId == null && !it.isTemplate }
            RoutineFilter.ASSIGNED -> myRoutines.filter { it.ownerId != null }
            RoutineFilter.TEMPLATE -> myRoutines.filter { it.isTemplate }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Gestión de",
            title = "RUTINAS",
            actionIcon = Icons.Default.Add,
            onActionClick = onCreateRoutine,
            actionTint = Color.White,
            actionBackgroundColor = VerdeTN
        )

        Spacer(Modifier.height(12.dp))

        // Estadísticas (GrisFondo + borde VerdeTN)
        StatsRow(
            totalRoutines = myRoutines.size,
            globalRoutines = myRoutines.count { it.ownerId == null },
            assignedRoutines = myRoutines.count { it.ownerId != null }
        )

        Spacer(Modifier.height(16.dp))

        // Filtros (estilo app: texto VerdeTN + línea cuando activo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            RoutineFilter.entries.forEach { filter ->
                val selected = selectedFilter == filter
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilter = filter }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        filter.label,
                        color = if (selected) VerdeTN else GrisTexto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .fillMaxWidth(0.5f)
                            .background(
                                if (selected) VerdeTN else Color.Transparent,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (filteredRoutines.isEmpty()) {
            EmptyRoutinesState(selectedFilter, onCreateRoutine)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRoutines, key = { it.id }) { routine ->
                    RoutineCard(
                        routine = routine,
                        onClick = { onRoutineClick(routine.id) },
                        onDelete = { showDeleteDialog = routine },
                        onShare = if (routine.isTemplate) { { shareDialogFor = routine } } else null
                    )
                }
            }
        }
    }

    shareDialogFor?.let { plantilla ->
        var input by remember(plantilla) { mutableStateOf("") }
        var enviando by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!enviando) shareDialogFor = null },
            containerColor = GrisFondo,
            title = { Text("Compartir \"${plantilla.name}\"", color = TextoPrincipal) },
            text = {
                Column {
                    Text(
                        "El usuario recibirá una notificación para aceptar o rechazar esta rutina.",
                        color = GrisTexto,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = input,
                        onValueChange = { input = it; error = null },
                        singleLine = true,
                        label = { Text("Correo o ID del usuario") },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextoPrincipal,
                            unfocusedTextColor = TextoPrincipal,
                            focusedBorderColor = VerdeTN,
                            unfocusedBorderColor = GrisTexto,
                            cursorColor = VerdeTN
                        )
                    )
                    error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = Color(0xFFE57373), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !enviando,
                    onClick = {
                        if (input.isBlank()) { error = "Ingresa el correo o ID del usuario"; return@TextButton }
                        enviando = true
                        scope.launch {
                            val targetId = input.trim().toIntOrNull()
                                ?: userRepository?.getAllUsers()?.first()
                                    ?.firstOrNull { it.email.equals(input.trim(), ignoreCase = true) }?.id
                            if (targetId == null || targetId <= 0) {
                                error = "No se encontró un usuario con ese correo o ID"
                                enviando = false
                            } else {
                                routineRepository?.shareTemplateWithUser(plantilla.id, trainerId, targetId)
                                enviando = false
                                shareDialogFor = null
                            }
                        }
                    }
                ) { Text("Compartir", color = VerdeTN) }
            },
            dismissButton = {
                TextButton(onClick = { if (!enviando) shareDialogFor = null }) {
                    Text("Cancelar", color = GrisTexto)
                }
            }
        )
    }

    showDeleteDialog?.let { routine ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Rutina", color = TextoPrincipal) },
            text = {
                Text(
                    "¿Eliminar \"${routine.name}\"? No se puede deshacer.",
                    color = GrisTexto
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRoutine(routine.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar", color = VerdeTN)
                }
            },
            containerColor = GrisFondo
        )
    }
}

@Composable
private fun StatsRow(
    totalRoutines: Int,
    globalRoutines: Int,
    assignedRoutines: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = totalRoutines.toString(),
                label = "Total",
                icon = Icons.Default.FitnessCenter
            )
            StatItem(
                value = globalRoutines.toString(),
                label = "Globales",
                icon = Icons.Default.Public
            )
            StatItem(
                value = assignedRoutines.toString(),
                label = "Asignadas",
                icon = Icons.Default.PersonPin
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = VerdeTN,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GrisTexto
        )
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val isGlobal = routine.ownerId == null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isGlobal) Icons.Default.Public else Icons.Default.PersonPin,
                        contentDescription = null,
                        tint = VerdeTN,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VerdeTN.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                routine.isTemplate -> "PLANTILLA"
                                routine.pendingShare -> "PENDIENTE DE ACEPTAR"
                                isGlobal -> "GLOBAL"
                                else -> "ASIGNADA"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = VerdeTN
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onShare != null) {
                        IconButton(onClick = onShare) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Compartir",
                                tint = VerdeTN
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFE57373)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = routine.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = routine.dayInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = GrisTexto
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = GrisTexto
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Creada: ${dateFormat.format(Date(routine.creationDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrisTexto
                )
            }

            if (routine.scheduledTime > routine.creationDate) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = VerdeTN
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Programada: ${dateFormat.format(Date(routine.scheduledTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VerdeTN
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRoutinesState(filter: RoutineFilter, onCreateRoutine: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = GrisTexto.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (filter) {
                    RoutineFilter.ALL -> "No has creado rutinas"
                    RoutineFilter.GLOBAL -> "No tienes rutinas globales"
                    RoutineFilter.ASSIGNED -> "No tienes rutinas asignadas"
                    RoutineFilter.TEMPLATE -> "No tienes plantillas. Crea una rutina y elige \"Plantilla\" para reutilizarla."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = GrisTexto
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCreateRoutine,
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nueva Rutina")
            }
        }
    }
}

enum class RoutineFilter(val label: String) {
    ALL("Todas"),
    GLOBAL("Globales"),
    ASSIGNED("Asignadas"),
    TEMPLATE("Plantillas")
}
