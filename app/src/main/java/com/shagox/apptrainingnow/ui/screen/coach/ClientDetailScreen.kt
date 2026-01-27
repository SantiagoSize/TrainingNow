package com.shagox.apptrainingnow.ui.screen.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shagox.apptrainingnow.data.local.progress.GoalEntity
import com.shagox.apptrainingnow.data.local.progress.GoalStatus
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.ui.viewmodel.CoachViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de detalle de un cliente para el entrenador.
 * Muestra información del cliente, sus rutinas y objetivos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    viewModel: CoachViewModel,
    clientId: Int,
    onBack: () -> Unit,
    onChatClick: () -> Unit,
    onCreateRoutine: () -> Unit,
    onCreateGoal: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedClient by viewModel.selectedClient.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    // Cargar cliente si no está seleccionado
    LaunchedEffect(clientId) {
        // El cliente debería haberse seleccionado antes de navegar aquí
    }

    selectedClient?.let { client ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalle del Cliente") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = onChatClick) {
                            Icon(Icons.AutoMirrored.Filled.Chat, "Chat")
                        }
                        IconButton(onClick = { showNotesDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Notes, "Notas")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header con info del cliente
                item {
                    ClientHeaderCard(client = client)
                }

                // Tabs de contenido
                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Rutinas (${uiState.selectedClientRoutines.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Objetivos (${uiState.selectedClientGoals.size})") }
                        )
                    }
                }

                // Contenido según tab
                when (selectedTab) {
                    0 -> {
                        // Rutinas del cliente
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rutinas Asignadas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onCreateRoutine) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Nueva")
                                }
                            }
                        }
                        
                        if (uiState.selectedClientRoutines.isEmpty()) {
                            item {
                                EmptySection(
                                    message = "No hay rutinas asignadas",
                                    icon = Icons.Default.FitnessCenter
                                )
                            }
                        } else {
                            items(uiState.selectedClientRoutines) { routine ->
                                RoutineCard(routine = routine)
                            }
                        }
                    }
                    1 -> {
                        // Objetivos del cliente
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Objetivos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onCreateGoal) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Nuevo")
                                }
                            }
                        }
                        
                        if (uiState.selectedClientGoals.isEmpty()) {
                            item {
                                EmptySection(
                                    message = "No hay objetivos definidos",
                                    icon = Icons.Default.Flag
                                )
                            }
                        } else {
                            items(uiState.selectedClientGoals) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onAddFeedback = { feedback ->
                                        viewModel.addGoalFeedback(goal.id, feedback)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de notas
        if (showNotesDialog) {
            AlertDialog(
                onDismissRequest = { showNotesDialog = false },
                title = { Text("Notas sobre ${client.name}") },
                text = {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("Escribe notas privadas sobre el cliente...") }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateClientNotes(client.id, notes)
                            showNotesDialog = false
                        }
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotesDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    } ?: run {
        // Cliente no encontrado
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ClientHeaderCard(client: UserEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (client.profilePhotoUrl != null) {
                    AsyncImage(
                        model = client.profilePhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "${client.name.first()}${client.lastName.first()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nombre
            Text(
                text = "${client.name} ${client.lastName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Email
            Text(
                text = client.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats del cliente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                client.weight?.let {
                    StatChip(
                        label = "Peso",
                        value = "${it.toInt()} kg",
                        icon = Icons.Default.Scale
                    )
                }
                client.height?.let {
                    StatChip(
                        label = "Altura",
                        value = "${it.toInt()} cm",
                        icon = Icons.Default.Height
                    )
                }
                client.gender?.let {
                    StatChip(
                        label = "Género",
                        value = if (it == "M") "Masculino" else "Femenino",
                        icon = Icons.Default.Person
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoutineCard(routine: RoutineEntity) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = routine.dayInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Programada: ${dateFormat.format(Date(routine.scheduledTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    onAddFeedback: (String) -> Unit
) {
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }

    val statusColor = when (goal.status) {
        GoalStatus.ACTIVE.name -> MaterialTheme.colorScheme.primary
        GoalStatus.COMPLETED.name -> MaterialTheme.colorScheme.tertiary
        GoalStatus.PAUSED.name -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono y título
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Badge de estado
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = goal.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            goal.description?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Barra de progreso
            if (goal.targetValue != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { (goal.progressPercentage / 100).toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${goal.progressPercentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                
                // Valores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Actual: ${goal.currentValue ?: 0} ${goal.unit ?: ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Meta: ${goal.targetValue} ${goal.unit ?: ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Feedback existente
            goal.trainerFeedback?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Comment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Botón de feedback
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { showFeedbackDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.AutoMirrored.Filled.Comment, null)
                Spacer(Modifier.width(4.dp))
                Text(if (goal.trainerFeedback == null) "Añadir Feedback" else "Editar Feedback")
            }
        }
    }

    // Diálogo de feedback
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text("Feedback para el objetivo") },
            text = {
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Escribe tu feedback...") },
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddFeedback(feedback)
                        showFeedbackDialog = false
                        feedback = ""
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EmptySection(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
