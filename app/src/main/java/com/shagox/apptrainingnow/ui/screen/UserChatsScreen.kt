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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pestaña activa de la pantalla: Foro (publicaciones de entrenadores) o Mis Chats (contactos). */
private enum class TabChats { FORO, MIS_CHATS }

@Composable
fun UserChatsScreen(
    userRepository: IUserRepository,
    chatRepository: ChatRepository,
    currentUserId: Int,
    onNavigateToChat: (Int) -> Unit // trainerId
) {
    var tab by remember { mutableStateOf(TabChats.FORO) }
    var searchQuery by remember { mutableStateOf("") }
    var trainers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var trainerDetailDialog by remember { mutableStateOf<UserEntity?>(null) }
    // Publicación del entrenador seleccionada en el Foro (vista completa con botón Enviar mensaje).
    var publicacionDetalle by remember { mutableStateOf<UserEntity?>(null) }
    // "Mis chats" del usuario solo muestra entrenadores + un contacto de soporte (un admin
    // real, mostrado como "Soporte TrainingNow!"). El id real es el de ese admin, así que
    // los mensajes le llegan igual que a cualquier otro contacto.
    var soporte by remember { mutableStateOf<UserEntity?>(null) }
    val scope = rememberCoroutineScope()

    // Un entrenador solo aparece en "Mis chats" si ya se le escribió un mensaje o si el
    // usuario ya abrió su chat (por ejemplo desde el Foro). Ambos quedan en Room, que no se
    // borra al cerrar sesión (los mensajes además quedan guardados en el backend).
    var contactosConMensajes by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val contactosGuardados by chatRepository.observarContactosGuardados(currentUserId)
        .collectAsState(initial = emptyList())
    LaunchedEffect(currentUserId, tab) {
        // Se recalcula también al volver a la pestaña Mis Chats, por si se envió un mensaje
        // nuevo desde el chat abierto vía Foro.
        contactosConMensajes = chatRepository.obtenerContactosConMensajes(currentUserId).toSet()
    }
    val contactosConChat = remember(contactosConMensajes, contactosGuardados) {
        contactosConMensajes + contactosGuardados.toSet()
    }

    // Cargar todos los entrenadores al inicio
    LaunchedEffect(Unit) {
        try {
            userRepository.getAllTrainers().collect { trainerList ->
                trainers = if (searchQuery.isBlank()) trainerList else trainers
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("UserChatsScreen", "Error al cargar entrenadores", e)
            trainers = emptyList()
        }
    }

    // Contacto de soporte: el admin de menor id representa a "Soporte TrainingNow!".
    LaunchedEffect(Unit) {
        try {
            userRepository.getAllUsers().collect { all ->
                val admin = all.filter { it.role == "ADMIN" }.minByOrNull { it.id }
                soporte = admin?.copy(
                    name = "Soporte",
                    lastName = "TrainingNow!",
                    specializations = "Ayuda con cualquier duda de la app"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("UserChatsScreen", "Error al cargar soporte", e)
        }
    }

    // Buscar cuando cambia el query. IMPORTANTE: la llamada va directo en la corrutina de este
    // LaunchedEffect (NO en scope.launch aparte). Antes usaba scope.launch, que corre en el
    // rememberCoroutineScope general y no se cancela cuando el usuario sigue tecleando: si
    // buscabas "c" y después "ca", ambas búsquedas quedaban en vuelo y la que respondiera
    // último (no necesariamente la más nueva) era la que se mostraba, mezclando resultados de
    // consultas viejas con la actual. Al llamar directo aquí, Compose cancela automáticamente
    // la búsqueda anterior apenas cambia searchQuery, así solo puede "ganar" la más reciente.
    LaunchedEffect(searchQuery) {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isBlank()) {
            isLoading = false
            // Los entrenadores se actualizarán automáticamente desde el LaunchedEffect anterior
        } else {
            isLoading = true
            try {
                trainers = userRepository.searchTrainers(trimmedQuery)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("UserChatsScreen", "Error al buscar entrenadores", e)
                trainers = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    // Entrenadores que sí tienen "publicación" (imagen promocional configurada). Solo estos
    // aparecen en el Foro cuando no hay una búsqueda activa.
    val conPublicacion = remember(trainers) { trainers.filter { !it.promoImageUrl.isNullOrBlank() } }
    val buscando = searchQuery.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Mis",
            title = "CHATS",
            actionIcon = Icons.AutoMirrored.Filled.Chat,
            onActionClick = { /* nuevo chat / acción */ }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Selector Foro / Mis Chats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabChip(
                text = "Foro",
                selected = tab == TabChats.FORO,
                modifier = Modifier.weight(1f),
                onClick = { tab = TabChats.FORO }
            )
            TabChip(
                text = "Mis Chats",
                selected = tab == TabChats.MIS_CHATS,
                modifier = Modifier.weight(1f),
                onClick = { tab = TabChats.MIS_CHATS }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Barra de búsqueda (estilo app: GrisFondo, borde VerdeTN al foco)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = if (tab == TabChats.FORO) "Buscar entrenador por correo o nombre..."
                    else "Buscar por nombre, ID o especialidad...",
                    color = GrisTexto
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = VerdeTN
                )
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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VerdeTN)
            }
        } else if (tab == TabChats.FORO) {
            ForoContent(
                trainers = trainers,
                conPublicacion = conPublicacion,
                buscando = buscando,
                onVerPublicacion = { publicacionDetalle = it },
                onAbrirChat = { trainerId ->
                    scope.launch { chatRepository.marcarChatAbierto(currentUserId, trainerId) }
                    onNavigateToChat(trainerId)
                }
            )
        } else {
            val misChats = remember(trainers, contactosConChat) {
                trainers.filter { it.id in contactosConChat }
            }
            MisChatsContent(
                trainers = misChats,
                soporte = soporte,
                buscando = buscando,
                onMessageClick = onNavigateToChat,
                onLongPress = { trainerDetailDialog = it }
            )
        }
    }

    // Diálogo con el perfil del entrenador (al mantener 1,5 s en la tarjeta de Mis Chats)
    trainerDetailDialog?.let { trainer ->
        val preferencia by chatRepository.observarPreferencia(currentUserId, trainer.id)
            .collectAsState(initial = null)
        com.shagox.apptrainingnow.ui.components.PerfilContactoDialog(
            usuario = trainer,
            onDismiss = { trainerDetailDialog = null },
            bloqueado = preferencia?.bloqueado ?: false,
            silenciado = preferencia?.silenciado ?: false,
            onBloquear = { scope.launch { chatRepository.bloquearContacto(currentUserId, trainer.id) } },
            onDesbloquear = { scope.launch { chatRepository.desbloquearContacto(currentUserId, trainer.id) } },
            onSilenciar = { scope.launch { chatRepository.silenciarContacto(currentUserId, trainer.id) } },
            onDesilenciar = { scope.launch { chatRepository.desilenciarContacto(currentUserId, trainer.id) } },
            onEliminarConversacion = {
                scope.launch { chatRepository.eliminarConversacion(currentUserId, trainer.id) }
                trainerDetailDialog = null
            },
            onEliminarDeMisChats = {
                scope.launch { chatRepository.eliminarDeMisChats(currentUserId, trainer.id) }
                trainerDetailDialog = null
            }
        )
    }

    // Vista completa de la publicación del Foro: foto, título, descripción, entrenador y
    // experiencia. Único lugar desde el que el usuario puede decidir escribirle.
    publicacionDetalle?.let { trainer ->
        PublicacionDetalleDialog(
            trainer = trainer,
            onDismiss = { publicacionDetalle = null },
            onEnviarMensaje = {
                publicacionDetalle = null
                tab = TabChats.MIS_CHATS
                scope.launch { chatRepository.marcarChatAbierto(currentUserId, trainer.id) }
                onNavigateToChat(trainer.id)
            }
        )
    }
}

/** Contenido de la pestaña Foro. */
@Composable
private fun ForoContent(
    trainers: List<UserEntity>,
    conPublicacion: List<UserEntity>,
    buscando: Boolean,
    onVerPublicacion: (UserEntity) -> Unit,
    onAbrirChat: (Int) -> Unit
) {
    if (buscando) {
        // Con búsqueda activa se muestran TODOS los entrenadores (tengan o no publicación),
        // como cuenta normal sin el anuncio.
        if (trainers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se encontraron entrenadores", color = GrisTexto, fontSize = 16.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(trainers, key = { "buscar_${it.id}" }) { trainer ->
                    CuentaNormalRow(trainer = trainer, onClick = { onAbrirChat(trainer.id) })
                }
            }
        }
    } else {
        if (conPublicacion.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Todavía ningún entrenador tiene una publicación",
                    color = GrisTexto,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(conPublicacion, key = { "pub_${it.id}" }) { trainer ->
                    PublicacionCard(trainer = trainer, onVer = { onVerPublicacion(trainer) })
                }
            }
        }
    }
}

/** Contenido de la pestaña Mis Chats (comportamiento existente, sin cambios). */
@Composable
private fun MisChatsContent(
    trainers: List<UserEntity>,
    soporte: UserEntity?,
    buscando: Boolean,
    onMessageClick: (Int) -> Unit,
    onLongPress: (UserEntity) -> Unit
) {
    if (trainers.isEmpty() && (buscando || soporte == null)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (!buscando) "Todavía no tienes chats guardados. Escríbele a un entrenador desde el Foro."
                else "No se encontraron resultados",
                color = GrisTexto,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!buscando) {
                soporte?.let { admin ->
                    item(key = "soporte") {
                        TrainerCard(
                            trainer = admin,
                            esSoporte = true,
                            onMessageClick = { onMessageClick(admin.id) },
                            onLongPress = { onLongPress(admin) }
                        )
                    }
                }
            }
            items(trainers, key = { "chat_${it.id}" }) { trainer ->
                TrainerCard(
                    trainer = trainer,
                    onMessageClick = { onMessageClick(trainer.id) },
                    onLongPress = { onLongPress(trainer) }
                )
            }
        }
    }
}

/** Chip de selección Foro / Mis Chats. */
@Composable
private fun TabChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) VerdeTN else GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) TextoSobreVerde else VerdeTN,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Fila de cuenta normal (sin el diseño de anuncio): se muestra al buscar en el Foro para que
 * aparezcan todos los entrenadores, tengan o no publicación. Toca para abrir el chat directo.
 */
@Composable
private fun CuentaNormalRow(trainer: UserEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GrisFondo)
            .border(1.dp, GrisTexto.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(VerdeTN.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            val foto = trainer.profilePhotoUrl?.takeIf { it.isNotBlank() }
            if (foto != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(foto).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = trainer.name.firstOrNull()?.toString() ?: "E",
                    color = VerdeTN,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${trainer.name} ${trainer.lastName}",
                color = TextoPrincipal,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = trainer.email,
                color = GrisTexto,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Enviar mensaje",
            tint = VerdeTN,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Tarjeta de publicación en el Foro: NO abre el chat al tocarla ni al mantenerla presionada.
 * La única acción posible es el botón con el ojo, que abre la vista completa de la publicación.
 */
@Composable
private fun PublicacionCard(trainer: UserEntity, onVer: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${trainer.name} ${trainer.lastName}",
                    color = TextoPrincipal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VerdeTN)
                        .clickable(onClick = onVer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Ver publicación",
                        tint = TextoSobreVerde,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val imagenPromo = trainer.promoImageUrl?.takeIf { it.isNotBlank() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GrisTexto.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (imagenPromo != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(imagenPromo).build(),
                        contentDescription = "Imagen de ${trainer.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // La tarjeta es mucho más ancha que alta, así que el recorte central de una
                    // foto/anuncio horizontal siempre corta texto arriba y abajo. Un fundido a
                    // GrisFondo en los bordes disimula ese corte en vez de dejarlo a la mitad de
                    // una palabra; la vista completa (botón del ojo) muestra la imagen entera.
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(22.dp)
                            .background(Brush.verticalGradient(listOf(GrisFondo.copy(alpha = 0.85f), Color.Transparent)))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, GrisFondo.copy(alpha = 0.9f))))
                    )
                } else {
                    Text(
                        text = trainer.name.firstOrNull()?.toString() ?: "U",
                        color = TextoPrincipal,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val descripcionCorta = trainer.bio?.takeIf { it.isNotBlank() }
                ?: trainer.specializations?.takeIf { it.isNotBlank() }
                ?: "Toca el ojo para ver más"
            Text(
                text = descripcionCorta,
                color = if (trainer.bio.isNullOrBlank()) GrisTexto else TextoPrincipal,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Vista completa de una publicación del Foro: foto grande, título (nombre del entrenador),
 * descripción completa y experiencia (especialidades). Abajo, dos botones grandes pegados:
 * Cerrar y Enviar mensaje (este último abre el chat con el entrenador).
 */
@Composable
private fun PublicacionDetalleDialog(
    trainer: UserEntity,
    onDismiss: () -> Unit,
    onEnviarMensaje: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(18.dp))
                .background(NegroFondo)
                .border(1.5.dp, VerdeTN, RoundedCornerShape(18.dp))
        ) {
            val imagenPromo = trainer.promoImageUrl?.takeIf { it.isNotBlank() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(GrisTexto.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (imagenPromo != null) {
                    // Fit (no Crop): esta es la "vista completa" de la publicación, así que se
                    // ve la imagen entera. El aspecto real de la foto (~1.8:1) es casi igual al
                    // de este recuadro (~1.7:1), así que el margen que deja Fit es mínimo.
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(imagenPromo).build(),
                        contentDescription = "Imagen de ${trainer.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = trainer.name.firstOrNull()?.toString() ?: "U",
                        color = TextoPrincipal,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "${trainer.name} ${trainer.lastName}",
                    color = VerdeTN,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val descripcion = trainer.bio?.takeIf { it.isNotBlank() }
                    ?: "Este entrenador todavía no escribió una descripción."
                Text(
                    text = descripcion,
                    color = TextoPrincipal.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                val experiencia = trainer.specializations?.takeIf { it.isNotBlank() }
                if (experiencia != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "EXPERIENCIA",
                        color = VerdeTN,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = experiencia,
                        color = TextoPrincipal.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }

            // Botones grandes pegados: Cerrar / Enviar mensaje
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(GrisFondo)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CERRAR",
                        color = TextoPrincipal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(VerdeTN)
                        .clickable(onClick = onEnviarMensaje)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ENVIAR MENSAJE",
                        color = TextoSobreVerde,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Fila compacta de entrenador (o soporte) en "Mis chats": avatar circular pequeño, nombre y
 * una descripción corta de una línea. Un toque corto abre el chat; mantener presionado 1,5 s
 * abre la vista completa (imagen, descripción larga, teléfono y correo).
 *
 * Usa tryAwaitRelease() (no awaitRelease()) para distinguir un toque real de un gesto
 * cancelado por scroll: awaitRelease() lanza una excepción al hacer scroll, pero como el
 * onMessageClick estaba en un bloque finally, igual se disparaba con el mínimo arrastre y
 * no dejaba desplazar la lista.
 */
@Composable
fun TrainerCard(
    trainer: UserEntity,
    onMessageClick: () -> Unit,
    onLongPress: () -> Unit = {},
    esSoporte: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var didLongPress by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        didLongPress = false
                        val job = scope.launch {
                            delay(1500)
                            didLongPress = true
                            onLongPress()
                        }
                        val released = tryAwaitRelease()
                        job.cancel()
                        if (released && !didLongPress) onMessageClick()
                    }
                )
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mis Chats es un contacto real: se ve su foto de perfil, no el anuncio del Foro.
        val imagenPerfil = trainer.profilePhotoUrl?.takeIf { it.isNotBlank() }
            ?: trainer.promoImageUrl?.takeIf { it.isNotBlank() }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(VerdeTN.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (esSoporte) {
                Icon(
                    imageVector = Icons.Filled.HeadsetMic,
                    contentDescription = "Soporte técnico",
                    tint = VerdeTN,
                    modifier = Modifier.size(26.dp)
                )
            } else if (imagenPerfil != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imagenPerfil).build(),
                    contentDescription = "Imagen de ${trainer.name}",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = trainer.name.firstOrNull()?.toString() ?: "U",
                    color = VerdeTN,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${trainer.name} ${trainer.lastName}",
                color = TextoPrincipal,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val descripcionCorta = trainer.bio?.takeIf { it.isNotBlank() }
                ?: trainer.specializations?.takeIf { it.isNotBlank() }
                ?: "Toca para escribirle"
            Text(
                text = descripcionCorta,
                color = GrisTexto,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(VerdeTN),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar mensaje",
                tint = TextoSobreVerde,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
