package com.shagox.apptrainingnow.ui.screen.coach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.viewmodel.CoachUiState
import com.shagox.apptrainingnow.ui.viewmodel.CoachViewModel

/**
 * Pantalla principal de clientes para el entrenador.
 * Estilo TN: NegroFondo, ScreenHeaderTN, tarjetas GrisFondo con borde VerdeTN, avatares VerdeTN.
 */
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Mis",
            title = "CLIENTES",
            actionIcon = Icons.Default.PersonAdd,
            onActionClick = { selectedTab = 1 },
            actionTint = Color.White,
            actionBackgroundColor = VerdeTN,
            actionBadgeCount = if (pendingCount > 0) pendingCount else null
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Barra de búsqueda (estilo TN)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.searchClients(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar cliente...", color = GrisTexto) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = VerdeTN)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.clearSearch()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = GrisTexto)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = VerdeTN,
                unfocusedBorderColor = GrisTexto,
                cursorColor = VerdeTN,
                focusedContainerColor = GrisFondo,
                unfocusedContainerColor = GrisFondo
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs: Activos / Pendientes (estilo usuario: texto verde + línea debajo cuando activo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Activos (${activeClients.size})",
                    color = if (selectedTab == 0) VerdeTN else GrisTexto,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth(0.4f)
                        .background(
                            if (selectedTab == 0) VerdeTN else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pendientes",
                        color = if (selectedTab == 1) VerdeTN else GrisTexto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (pendingCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "($pendingCount)",
                            color = if (selectedTab == 1) VerdeTN else GrisTexto,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth(0.4f)
                        .background(
                            if (selectedTab == 1) VerdeTN else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            searchQuery.isNotEmpty() -> SearchResultsList(
                results = uiState.searchResults,
                isSearching = uiState.isSearching,
                onClientClick = onClientClick,
                onChatClick = onChatClick
            )
            selectedTab == 0 -> ClientsList(
                clients = activeClients,
                emptyMessage = "No tienes clientes activos",
                onClientClick = onClientClick,
                onChatClick = onChatClick
            )
            else -> PendingClientsList(
                clients = pendingClients,
                onAccept = { viewModel.acceptClientRequest(it) },
                onReject = { viewModel.rejectClientRequest(it) }
            )
        }
    }

    uiState.error?.let { /* snackbar si se implementa */ }
    uiState.successMessage?.let { msg ->
        LaunchedEffect(msg) {
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
            contentPadding = PaddingValues(bottom = 24.dp),
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
        isSearching -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = VerdeTN)
        }
        results.isEmpty() -> EmptyState(
            message = "No se encontraron clientes",
            icon = Icons.Default.SearchOff
        )
        else -> ClientsList(
            clients = results,
            emptyMessage = "",
            onClientClick = onClientClick,
            onChatClick = onChatClick
        )
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
            icon = Icons.Default.PersonAdd
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
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
        colors = CardDefaults.cardColors(containerColor = GrisFondo),
        border = BorderStroke(1.dp, VerdeTN)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar: VerdeTN (sin púrpura)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VerdeTN.copy(alpha = 0.2f)),
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
                        color = VerdeTN,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${client.name} ${client.lastName}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = client.email,
                    color = GrisTexto,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (client.weight != null && client.height != null) {
                    Text(
                        text = "${client.weight}kg • ${client.height}cm",
                        color = VerdeTN,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = onChatClick) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chat",
                    tint = VerdeTN
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GrisTexto,
                modifier = Modifier.size(24.dp)
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
        colors = CardDefaults.cardColors(containerColor = GrisFondo),
        border = BorderStroke(1.dp, VerdeTN)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(VerdeTN.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${client.name.first()}",
                        color = VerdeTN,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${client.name} ${client.lastName}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = client.email,
                        color = GrisTexto,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(VerdeTN, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "NUEVO",
                        color = NegroFondo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE57373)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE57373))
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechazar", fontSize = 14.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VerdeTN)
                        .clickable(onClick = onAccept)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = NegroFondo)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aceptar", color = NegroFondo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = GrisTexto.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = GrisTexto,
                fontSize = 16.sp
            )
        }
    }
}
