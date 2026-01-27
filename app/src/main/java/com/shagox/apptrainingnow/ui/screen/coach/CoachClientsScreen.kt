package com.shagox.apptrainingnow.ui.screen.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.ui.viewmodel.CoachUiState
import com.shagox.apptrainingnow.ui.viewmodel.CoachViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Pantalla principal de clientes para el entrenador.
 * Muestra lista de clientes activos, pendientes y permite búsqueda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachClientsScreen(
    viewModel: CoachViewModel,
    onClientClick: (Int) -> Unit,
    onChatClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeClients by viewModel.activeClients.collectAsState()
    val pendingClients by viewModel.pendingClients.collectAsState()
    val pendingCount by viewModel.pendingRequestCount.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mis Clientes",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Badge de solicitudes pendientes
                    if (pendingCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text(pendingCount.toString()) }
                            }
                        ) {
                            IconButton(onClick = { selectedTab = 1 }) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "Solicitudes",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchClients(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar cliente...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            viewModel.clearSearch()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Tabs: Activos / Pendientes
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Activos (${activeClients.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pendientes")
                            if (pendingCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge { Text(pendingCount.toString()) }
                            }
                        }
                    }
                )
            }

            // Contenido según tab seleccionado
            when {
                searchQuery.isNotEmpty() -> {
                    // Mostrar resultados de búsqueda
                    SearchResultsList(
                        results = uiState.searchResults,
                        isSearching = uiState.isSearching,
                        onClientClick = onClientClick,
                        onChatClick = onChatClick
                    )
                }
                selectedTab == 0 -> {
                    // Lista de clientes activos
                    ClientsList(
                        clients = activeClients,
                        emptyMessage = "No tienes clientes activos",
                        onClientClick = onClientClick,
                        onChatClick = onChatClick
                    )
                }
                selectedTab == 1 -> {
                    // Lista de solicitudes pendientes
                    PendingClientsList(
                        clients = pendingClients,
                        onAccept = { viewModel.acceptClientRequest(it) },
                        onReject = { viewModel.rejectClientRequest(it) }
                    )
                }
            }
        }
    }

    // Mostrar mensajes de éxito/error
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Mostrar snackbar de error
        }
    }
    
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            // Mostrar snackbar de éxito
            viewModel.clearSuccessMessage()
        }
    }
}

@Composable
private fun ClientsList(
    clients: List<UserEntity>,
    emptyMessage: String,
    onClientClick: (Int) -> Unit,
    onChatClick: (Int) -> Unit
) {
    if (clients.isEmpty()) {
        EmptyState(message = emptyMessage, icon = Icons.Default.People)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(clients, key = { it.id }) { client ->
                ClientCard(
                    client = client,
                    onClick = { onClientClick(client.id) },
                    onChatClick = { onChatClick(client.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<UserEntity>,
    isSearching: Boolean,
    onClientClick: (Int) -> Unit,
    onChatClick: (Int) -> Unit
) {
    when {
        isSearching -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        results.isEmpty() -> {
            EmptyState(
                message = "No se encontraron clientes",
                icon = Icons.Default.SearchOff
            )
        }
        else -> {
            ClientsList(
                clients = results,
                emptyMessage = "",
                onClientClick = onClientClick,
                onChatClick = onChatClick
            )
        }
    }
}

@Composable
private fun PendingClientsList(
    clients: List<UserEntity>,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit
) {
    if (clients.isEmpty()) {
        EmptyState(
            message = "No hay solicitudes pendientes",
            icon = Icons.Default.CheckCircle
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(clients, key = { it.id }) { client ->
                PendingClientCard(
                    client = client,
                    onAccept = { onAccept(client.id) },
                    onReject = { onReject(client.id) }
                )
            }
        }
    }
}

@Composable
private fun ClientCard(
    client: UserEntity,
    onClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info del cliente
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${client.name} ${client.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = client.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (client.weight != null && client.height != null) {
                    Text(
                        text = "${client.weight}kg • ${client.height}cm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Botón de chat
            IconButton(onClick = onChatClick) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Flecha de navegación
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PendingClientCard(
    client: UserEntity,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${client.name.first()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${client.name} ${client.lastName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = client.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badge de nuevo
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "NUEVO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechazar")
                }
                
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aceptar")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
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
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
