package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.remote.dto.ConversationSummaryDto
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mensajes del entrenador: lista de SUS CLIENTES (no de otros entrenadores), con el
 * último mensaje y el contador de no leídos de cada conversación. Antes esta ruta
 * reutilizaba UserChatsScreen (que lista entrenadores), mostrándole al entrenador la
 * lista equivocada de contactos.
 */
@Composable
fun CoachChatsScreen(
    trainerRepository: TrainerRepository,
    chatRepository: ChatRepository,
    currentUserId: Int,
    onNavigateToChat: (Int) -> Unit // clientId
) {
    var searchQuery by remember { mutableStateOf("") }
    val clientes by trainerRepository.getActiveClients(currentUserId).collectAsState(initial = emptyList())
    var resumenPorContacto by remember { mutableStateOf<Map<Int, ConversationSummaryDto>>(emptyMap()) }

    // Último mensaje + no leídos por cliente; se refresca cada 15s (mismo patrón que el
    // estado "Conectado/Desconectado" del chat 1-a-1).
    LaunchedEffect(currentUserId) {
        while (true) {
            try {
                resumenPorContacto = chatRepository.obtenerResumenConversaciones(currentUserId)
                    .associateBy { it.contactId }
            } catch (_: Exception) {
                // Sin conexión: se conserva el último resumen conocido.
            }
            delay(15_000L)
        }
    }

    val clientesFiltrados = if (searchQuery.isBlank()) {
        clientes
    } else {
        clientes.filter { "${it.name} ${it.lastName}".contains(searchQuery, ignoreCase = true) }
    }
    // Conversaciones con mensajes recientes primero; sin mensajes, al final por nombre.
    val clientesOrdenados = clientesFiltrados.sortedWith(
        compareByDescending<UserEntity> { resumenPorContacto[it.id]?.lastTimestamp ?: 0L }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Mis",
            title = "MENSAJES",
            actionIcon = Icons.AutoMirrored.Filled.Chat,
            onActionClick = { /* sin acción por ahora */ }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar cliente...", color = GrisTexto) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = VerdeTN) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal,
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

        if (clientesOrdenados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isBlank()) "Aún no tienes clientes activos" else "No se encontraron resultados",
                    color = GrisTexto,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(clientesOrdenados) { cliente ->
                    ClienteChatCard(
                        cliente = cliente,
                        resumen = resumenPorContacto[cliente.id],
                        onClick = { onNavigateToChat(cliente.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClienteChatCard(
    cliente: UserEntity,
    resumen: ConversationSummaryDto?,
    onClick: () -> Unit
) {
    val sinLeer = resumen?.unreadCount ?: 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, if (sinLeer > 0) VerdeTN else GrisTexto.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(GrisTexto.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                val foto = cliente.profilePhotoUrl
                if (foto != null && foto.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(foto).build(),
                        contentDescription = "Foto de ${cliente.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = cliente.name.firstOrNull()?.toString() ?: "U",
                        color = TextoPrincipal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${cliente.name} ${cliente.lastName}",
                    color = TextoPrincipal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = resumen?.lastMessage?.takeIf { it.isNotBlank() } ?: "Toca para escribirle",
                    color = if (sinLeer > 0) TextoPrincipal else GrisTexto,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val ts = resumen?.lastTimestamp
                if (ts != null) {
                    Text(text = formatHora(ts), color = GrisTexto, fontSize = 11.sp)
                }
                if (sinLeer > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(VerdeTN),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (sinLeer > 9) "9+" else sinLeer.toString(),
                            color = TextoSobreVerde,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatHora(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
