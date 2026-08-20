package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
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

private enum class TabAdminChats(val etiqueta: String) {
    ABIERTOS("Chats abiertos"),
    ADMINS("Admins"),
    ENTRENADORES("Entrenadores")
}

/**
 * Chat del admin, en 3 pestañas:
 * - "Chats abiertos": conversaciones reales ya existentes (backend), incluyendo los mensajes
 *   que los usuarios le mandan al contacto "Soporte TrainingNow!" (que en realidad es el admin
 *   de menor id, ver UserChatsScreen). Así el admin ve y responde el soporte técnico desde acá.
 * - "Admins": iniciar chat con otro administrador.
 * - "Entrenadores": iniciar chat con un entrenador.
 */
@Composable
fun AdminChatsScreen(
    userRepository: IUserRepository,
    chatRepository: ChatRepository,
    currentUserId: Int,
    onNavigateToChat: (Int) -> Unit
) {
    val users by userRepository.getAllUsers().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(TabAdminChats.ABIERTOS) }
    var resumenes by remember { mutableStateOf<List<ConversationSummaryDto>>(emptyList()) }
    var cargandoAbiertos by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun cargarAbiertos() {
        cargandoAbiertos = true
        resumenes = withContext(Dispatchers.IO) { chatRepository.obtenerResumenConversaciones(currentUserId) }
        cargandoAbiertos = false
    }

    LaunchedEffect(currentUserId) { cargarAbiertos() }

    val formatoHora = remember { SimpleDateFormat("dd/MM HH:mm", Locale.Builder().setLanguage("es").setRegion("CL").build()) }

    fun filtrar(lista: List<UserEntity>): List<UserEntity> {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) return lista
        return lista.filter {
            it.name.lowercase().contains(q) ||
                it.lastName.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.id.toString() == q
        }
    }

    val admins = remember(users, searchQuery) {
        filtrar(users.filter { it.role == "ADMIN" && it.id != currentUserId })
    }
    val entrenadores = remember(users, searchQuery) {
        filtrar(users.filter { it.role == "TRAINER" })
    }
    val abiertos = remember(resumenes, users, searchQuery) {
        val q = searchQuery.trim().lowercase()
        resumenes.mapNotNull { resumen ->
            val usuario = users.firstOrNull { it.id == resumen.contactId } ?: return@mapNotNull null
            usuario to resumen
        }.filter { (usuario, _) ->
            q.isBlank() || usuario.name.lowercase().contains(q) || usuario.lastName.lowercase().contains(q) ||
                usuario.email.lowercase().contains(q)
        }.sortedByDescending { it.second.lastTimestamp ?: 0L }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Chat",
            title = "ADMIN",
            actionIcon = Icons.AutoMirrored.Filled.Chat,
            onActionClick = { scope.launch { cargarAbiertos() } }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Carrusel horizontal: con 3 pestañas de ancho fijo, "Chats abiertos" se comprimía y el
        // texto se partía en pantallas angostas (mismo bug ya corregido en Actividad).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabAdminChats.entries.forEach { opcion ->
                // Se calcula sobre "abiertos" (la lista ya cruzada con usuarios) y no sobre
                // "resumenes" en crudo, para que el número nunca contradiga lo que se ve abajo
                // (si el cruce con la lista de usuarios todavía no cargó, ambos muestran 0).
                val badge = if (opcion == TabAdminChats.ABIERTOS) abiertos.sumOf { it.second.unreadCount } else 0
                TabChipAdmin(
                    text = opcion.etiqueta,
                    selected = tab == opcion,
                    badge = badge,
                    onClick = { tab = opcion }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por nombre o correo...", color = GrisTexto) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = VerdeTN)
            },
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

        when (tab) {
            TabAdminChats.ABIERTOS -> {
                if (cargandoAbiertos) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VerdeTN)
                    }
                } else if (abiertos.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.HeadsetMic,
                                contentDescription = null,
                                tint = GrisTexto.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isBlank())
                                    "Todavía no hay conversaciones abiertas"
                                else "No hay resultados",
                                color = GrisTexto,
                                fontSize = 15.sp
                            )
                            if (searchQuery.isBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Aquí aparecen los mensajes de soporte técnico\nque te escriban los usuarios",
                                    color = GrisTexto,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(abiertos, key = { "abierto_${it.first.id}" }) { (usuario, resumen) ->
                            ConversacionAbiertaCard(
                                usuario = usuario,
                                resumen = resumen,
                                formatoHora = formatoHora,
                                onClick = { onNavigateToChat(usuario.id) }
                            )
                        }
                    }
                }
            }
            TabAdminChats.ADMINS -> ListaContactosAdmin(admins, searchQuery, "No hay otros administradores", onNavigateToChat)
            TabAdminChats.ENTRENADORES -> ListaContactosAdmin(entrenadores, searchQuery, "No hay entrenadores registrados", onNavigateToChat)
        }
    }
}

@Composable
private fun ListaContactosAdmin(
    lista: List<UserEntity>,
    searchQuery: String,
    mensajeVacio: String,
    onNavigateToChat: (Int) -> Unit
) {
    if (lista.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (searchQuery.isBlank()) mensajeVacio else "No hay resultados",
                color = GrisTexto,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(lista, key = { it.id }) { user ->
                AdminChatUserCard(
                    user = user,
                    onClick = { onNavigateToChat(user.id) }
                )
            }
        }
    }
}

@Composable
private fun TabChipAdmin(
    text: String,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) VerdeTN else GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) TextoSobreVerde else VerdeTN,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) TextoSobreVerde else VerdeTN)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.toString(),
                    color = if (selected) VerdeTN else TextoSobreVerde,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ConversacionAbiertaCard(
    usuario: UserEntity,
    resumen: ConversationSummaryDto,
    formatoHora: SimpleDateFormat,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, if (resumen.unreadCount > 0) VerdeTN else VerdeTN.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VerdeTN.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                val foto = usuario.profilePhotoUrl?.takeIf { it.isNotBlank() }
                if (foto != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(foto).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "${usuario.name.firstOrNull() ?: '?'}${usuario.lastName.firstOrNull() ?: '?'}",
                        color = VerdeTN,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${usuario.name} ${usuario.lastName}",
                        color = TextoPrincipal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "· ${usuario.role}",
                        color = GrisTexto,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = resumen.lastMessage?.takeIf { it.isNotBlank() }
                        ?: if (resumen.lastAttachmentType != null) "Adjunto" else "Sin mensajes aún",
                    color = GrisTexto,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                resumen.lastTimestamp?.let {
                    Text(text = formatoHora.format(Date(it)), color = GrisTexto, fontSize = 11.sp)
                }
                if (resumen.unreadCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(VerdeTN)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = resumen.unreadCount.toString(),
                            color = TextoSobreVerde,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminChatUserCard(
    user: UserEntity,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(GrisTexto.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profilePhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(user.profilePhotoUrl).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "${user.name.firstOrNull() ?: '?'}${user.lastName.firstOrNull() ?: '?'}",
                        color = VerdeTN,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.name} ${user.lastName}",
                    color = TextoPrincipal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = user.email, color = GrisTexto, fontSize = 13.sp)
                Text(text = user.role, color = GrisTexto, fontSize = 12.sp)
            }
        }
    }
}
