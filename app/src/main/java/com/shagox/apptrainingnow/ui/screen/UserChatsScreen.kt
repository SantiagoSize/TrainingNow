package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserChatsScreen(
    userRepository: IUserRepository,
    chatRepository: ChatRepository,
    currentUserId: Int,
    onNavigateToChat: (Int) -> Unit // trainerId
) {
    var searchQuery by remember { mutableStateOf("") }
    var trainers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var trainerDetailDialog by remember { mutableStateOf<UserEntity?>(null) }
    // "Mis chats" del usuario solo muestra entrenadores + un contacto de soporte (un admin
    // real, mostrado como "Soporte TrainingNow!"). El id real es el de ese admin, así que
    // los mensajes le llegan igual que a cualquier otro contacto.
    var soporte by remember { mutableStateOf<UserEntity?>(null) }
    val scope = rememberCoroutineScope()

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

    // Buscar cuando cambia el query
    LaunchedEffect(searchQuery) {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isBlank()) {
            isLoading = false
            // Los entrenadores se actualizarán automáticamente desde el LaunchedEffect anterior
        } else {
            isLoading = true
            scope.launch {
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
    }

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

        // Barra de búsqueda (estilo app: GrisFondo, borde VerdeTN al foco)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por nombre, ID o especialidad...", color = GrisTexto) },
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

        // Lista: Soporte TrainingNow! (solo sin búsqueda activa) + entrenadores
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VerdeTN)
            }
        } else if (trainers.isEmpty() && (searchQuery.isNotBlank() || soporte == null)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No hay entrenadores disponibles" else "No se encontraron resultados",
                    color = GrisTexto,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (searchQuery.isBlank()) {
                    soporte?.let { admin ->
                        item(key = "soporte") {
                            TrainerCard(
                                trainer = admin,
                                onMessageClick = { onNavigateToChat(admin.id) },
                                onLongPress = { trainerDetailDialog = admin }
                            )
                        }
                    }
                }
                items(trainers) { trainer ->
                    TrainerCard(
                        trainer = trainer,
                        onMessageClick = { onNavigateToChat(trainer.id) },
                        onLongPress = { trainerDetailDialog = trainer }
                    )
                }
            }
        }
    }

    // Diálogo con el perfil del entrenador (al mantener 3 s en la tarjeta)
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
            }
        )
    }
}

/**
 * Tarjeta de entrenador (o soporte) en "Mis chats": arriba el nombre, debajo su imagen
 * promocional (o la foto de perfil si no ha subido una) y debajo una descripción corta.
 * Mantener presionado 1,5 s abre la vista completa (imagen, descripción larga, teléfono
 * y correo); un toque corto abre el chat.
 */
@Composable
fun TrainerCard(
    trainer: UserEntity,
    onMessageClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var didLongPress by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        didLongPress = false
                        val job = scope.launch {
                            delay(1500)
                            didLongPress = true
                            onLongPress()
                        }
                        try {
                            awaitRelease()
                        } finally {
                            job.cancel()
                            if (!didLongPress) onMessageClick()
                        }
                    }
                )
            }
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
                        .background(VerdeTN),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar mensaje",
                        tint = TextoSobreVerde,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val imagenPromo = trainer.promoImageUrl?.takeIf { it.isNotBlank() }
                ?: trainer.profilePhotoUrl?.takeIf { it.isNotBlank() }
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
                ?: "Toca para escribirle"
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
