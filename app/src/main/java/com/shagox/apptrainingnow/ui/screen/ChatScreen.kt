package com.shagox.apptrainingnow.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shagox.apptrainingnow.R
import com.shagox.apptrainingnow.data.local.chat.MessageEntity
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.ui.components.MenuOpcionesContactoDialog
import com.shagox.apptrainingnow.ui.components.PerfilContactoDialog
import com.shagox.apptrainingnow.ui.components.VideoPlayerDialog
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.LocalTemaClaro
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.utils.ComposeFileProvider
import com.shagox.apptrainingnow.utils.ImageCompressor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    currentUserId: Int,
    trainerId: Int,
    userRepository: IUserRepository,
    chatRepository: ChatRepository,
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    var trainer by remember { mutableStateOf<UserEntity?>(null) }
    var messageText by remember { mutableStateOf("") }
    var mostrarPerfilContacto by remember { mutableStateOf(false) }
    var mostrarMenuOpciones by remember { mutableStateOf(false) }
    var bloqueado by remember { mutableStateOf(false) }
    var silenciado by remember { mutableStateOf(false) }
    var lastCameraUri by remember { mutableStateOf<Uri?>(null) }
    var lastVideoUri by remember { mutableStateOf<Uri?>(null) }
    var subiendoAdjunto by remember { mutableStateOf(false) }
    var errorAdjunto by remember { mutableStateOf<String?>(null) }
    var mostrarMenuAdjuntar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    /** Límite del backend (MAX_ATTACHMENT_BYTES en MensajeService); se valida antes de subir. */
    val limiteAdjuntoBytes = 20L * 1024 * 1024

    /**
     * Comprime (si es imagen) o valida el tamaño (si es video) y sube el adjunto al backend;
     * si todo sale bien, envía el mensaje con attachmentUrl/attachmentType. La captura de
     * video ya viene comprimida por la app de cámara del sistema; acá solo se limita el peso.
     */
    fun enviarAdjunto(uri: Uri, esVideo: Boolean) {
        scope.launch {
            subiendoAdjunto = true
            errorAdjunto = null
            try {
                val bytes: ByteArray?
                val mimeType: String
                val nombreArchivo: String
                if (esVideo) {
                    bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                    nombreArchivo = "chat_${System.currentTimeMillis()}.mp4"
                } else {
                    bytes = ImageCompressor.compressToBytes(context, uri, maxDimension = 1280, targetBytes = 300 * 1024)
                    mimeType = "image/jpeg"
                    nombreArchivo = "chat_${System.currentTimeMillis()}.jpg"
                }
                if (bytes == null) {
                    errorAdjunto = "No se pudo leer el archivo."
                    return@launch
                }
                if (bytes.size > limiteAdjuntoBytes) {
                    errorAdjunto = if (esVideo) "El video es muy pesado (máx. 20 MB). Grábalo más corto."
                                    else "La imagen es muy pesada."
                    return@launch
                }
                val subida = chatRepository.subirAdjunto(bytes, mimeType, nombreArchivo)
                if (subida == null) {
                    errorAdjunto = "No se pudo subir el archivo. Revisa tu conexión."
                    return@launch
                }
                chatRepository.sendMessage(
                    MessageEntity(
                        senderId = currentUserId,
                        receiverId = trainerId,
                        content = if (esVideo) "🎥 Video" else "📷 Foto",
                        attachmentUrl = subida.url,
                        attachmentType = subida.attachmentType
                    )
                )
            } catch (e: Exception) {
                errorAdjunto = "Error al enviar el adjunto."
            } finally {
                subiendoAdjunto = false
            }
        }
    }

    // Permisos para cámara y galería (sin remember para que se actualicen al volver del diálogo)
    val hasCameraPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasGalleryPermission = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        else -> true
    }
    val hasVideoPermission = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        else -> true
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            lastCameraUri?.let { uri -> enviarAdjunto(uri, esVideo = false) }
            lastCameraUri = null
        }
    }
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { enviarAdjunto(it, esVideo = false) }
    }
    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            lastVideoUri?.let { uri -> enviarAdjunto(uri, esVideo = true) }
            lastVideoUri = null
        }
    }
    val getVideoContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { enviarAdjunto(it, esVideo = true) }
    }

    fun sendImageFromCamera() {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val uri = ComposeFileProvider.getImageUri(context)
        lastCameraUri = uri
        takePictureLauncher.launch(uri)
    }
    fun sendImageFromGallery() {
        if (!hasGalleryPermission) {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            galleryPermissionLauncher.launch(perm)
            return
        }
        getContentLauncher.launch("image/*")
    }
    fun sendVideoFromCamera() {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val uri = ComposeFileProvider.getVideoUri(context)
        lastVideoUri = uri
        captureVideoLauncher.launch(uri)
    }
    fun sendVideoFromGallery() {
        if (!hasVideoPermission) {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            galleryPermissionLauncher.launch(perm)
            return
        }
        getVideoContentLauncher.launch("video/*")
    }

    // Cargar información del entrenador/usuario, y refrescarla cada 15s para poder mostrar
    // si está "Conectado" o "Desconectado" (según su último heartbeat, ver lastActiveAt).
    // También se cachea localmente en Room (asegurarUsuarioLocal): las cuentas reales vienen
    // del backend (login vía API) y nunca quedaban guardadas en la tabla local "users", así
    // que el primer mensaje entre dos cuentas nuevas reventaba la FK de MessageEntity y
    // crasheaba la app. Se sincroniza ANTES de que el usuario alcance a escribir.
    LaunchedEffect(trainerId) {
        while (true) {
            try {
                trainer = userRepository.getUserById(trainerId)
                trainer?.let { chatRepository.asegurarUsuarioLocal(it) }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Error al cargar entrenador", e)
            }
            kotlinx.coroutines.delay(15_000L)
        }
    }

    LaunchedEffect(currentUserId) {
        try {
            userRepository.getUserById(currentUserId)?.let { chatRepository.asegurarUsuarioLocal(it) }
        } catch (e: Exception) {
            android.util.Log.e("ChatScreen", "Error al sincronizar mi usuario local", e)
        }
    }

    // Cargar mensajes
    LaunchedEffect(currentUserId, trainerId) {
        try {
            chatRepository.getConversation(currentUserId, trainerId).collect { messageList ->
                messages = messageList
                // Scroll al final cuando hay nuevos mensajes
                if (messageList.isNotEmpty()) {
                    scope.launch {
                        try {
                            listState.animateScrollToItem(messageList.size - 1)
                        } catch (e: Exception) {
                            // Ignorar errores de scroll
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatScreen", "Error al cargar mensajes", e)
        }
    }

    // Preferencias locales sobre este contacto (bloqueado/silenciado), reactivas.
    LaunchedEffect(currentUserId, trainerId) {
        chatRepository.observarPreferencia(currentUserId, trainerId).collect { pref ->
            bloqueado = pref?.bloqueado ?: false
            silenciado = pref?.silenciado ?: false
        }
    }

    // Fondo con el logo repetido (mosaico), como el wallpaper de chat de WhatsApp. Una sola
    // imagen por tema (clara/oscura, generadas con transparencia real), pintada como shader
    // en TileMode.Repeated para que cubra toda la pantalla sin importar el tamaño del teléfono.
    val temaClaro = LocalTemaClaro.current
    val fondoChatBitmap = ImageBitmap.imageResource(
        id = if (temaClaro) R.drawable.fondo_chat_claro else R.drawable.fondo_chat_oscuro
    )
    val fondoChatBrush = remember(temaClaro) {
        ShaderBrush(ImageShader(fondoChatBitmap, TileMode.Repeated, TileMode.Repeated))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .background(fondoChatBrush)
    ) {
        // Header con información del entrenador y botón para salir del chat. shadowElevation
        // lo separa visualmente del fondo con mosaico que corre detrás de los mensajes.
        Surface(
            color = GrisFondo,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Salir del chat",
                        tint = TextoPrincipal
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Foto + nombre: un toque abre el perfil completo del contacto; mantener
                // presionado 2 s abre el menú de opciones (silenciar/bloquear/eliminar chat).
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(trainer != null) {
                            if (trainer == null) return@pointerInput
                            detectTapGestures(
                                onPress = {
                                    var abrioMenu = false
                                    val job = scope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        abrioMenu = true
                                        mostrarMenuOpciones = true
                                    }
                                    try {
                                        awaitRelease()
                                    } finally {
                                        job.cancel()
                                        if (!abrioMenu) mostrarPerfilContacto = true
                                    }
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(elevation = 4.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentTrainer = trainer
                        val photoUrl = currentTrainer?.profilePhotoUrl
                        if (photoUrl != null && photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoUrl)
                                    .build(),
                                contentDescription = "Foto de ${currentTrainer.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "${currentTrainer?.name?.firstOrNull() ?: 'T'}",
                                color = TextoPrincipal,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        val currentTrainer = trainer
                        Text(
                            text = "${currentTrainer?.name ?: "Entrenador"} ${currentTrainer?.lastName ?: ""}",
                            color = TextoPrincipal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        EstadoConexion(usuario = currentTrainer)
                    }
                }
            }
        }

        // Lista de mensajes
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val currentTrainer = trainer
                MessageBubble(
                    message = message,
                    isFromCurrentUser = message.senderId == currentUserId,
                    trainerName = currentTrainer?.name ?: "Entrenador",
                    chatRepository = chatRepository
                )
            }
        }

        // Input de mensaje. shadowElevation lo separa del fondo con mosaico, igual que el header.
        Surface(
            color = GrisFondo,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (bloqueado) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bloqueaste a este contacto. No puedes enviarle mensajes.",
                            color = Color(0xFFE53935),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            scope.launch { chatRepository.desbloquearContacto(currentUserId, trainerId) }
                        }) {
                            Text("Desbloquear", color = VerdeTN)
                        }
                    }
                    return@Column
                }
                if (errorAdjunto != null) {
                    Text(
                        text = errorAdjunto ?: "",
                        color = Color(0xFFE53935),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Adjuntar (foto/video, cámara o galería)
                    Box {
                        IconButton(
                            onClick = { mostrarMenuAdjuntar = true },
                            enabled = !subiendoAdjunto,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (subiendoAdjunto) {
                                CircularProgressIndicator(color = VerdeTN, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Adjuntar",
                                    tint = VerdeTN,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = mostrarMenuAdjuntar,
                            onDismissRequest = { mostrarMenuAdjuntar = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tomar foto") },
                                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = VerdeTN) },
                                onClick = { mostrarMenuAdjuntar = false; sendImageFromCamera() }
                            )
                            DropdownMenuItem(
                                text = { Text("Foto de galería") },
                                leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = VerdeTN) },
                                onClick = { mostrarMenuAdjuntar = false; sendImageFromGallery() }
                            )
                            DropdownMenuItem(
                                text = { Text("Grabar video") },
                                leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, tint = VerdeTN) },
                                onClick = { mostrarMenuAdjuntar = false; sendVideoFromCamera() }
                            )
                            DropdownMenuItem(
                                text = { Text("Video de galería") },
                                leadingIcon = { Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = VerdeTN) },
                                onClick = { mostrarMenuAdjuntar = false; sendVideoFromGallery() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextoPrincipal,
                            unfocusedTextColor = TextoPrincipal,
                            focusedBorderColor = VerdeTN,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = NegroFondo,
                            unfocusedContainerColor = NegroFondo
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val trimmedMessage = messageText.trim()
                            if (trimmedMessage.isNotBlank()) {
                                scope.launch {
                                    chatRepository.sendMessage(
                                        MessageEntity(
                                            senderId = currentUserId,
                                            receiverId = trainerId,
                                            content = trimmedMessage
                                        )
                                    )
                                    messageText = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = VerdeTN,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = TextoSobreVerde,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    if (mostrarPerfilContacto) {
        trainer?.let { contacto ->
            PerfilContactoDialog(
                usuario = contacto,
                onDismiss = { mostrarPerfilContacto = false },
                bloqueado = bloqueado,
                silenciado = silenciado,
                onBloquear = { scope.launch { chatRepository.bloquearContacto(currentUserId, trainerId) } },
                onDesbloquear = { scope.launch { chatRepository.desbloquearContacto(currentUserId, trainerId) } },
                onSilenciar = { scope.launch { chatRepository.silenciarContacto(currentUserId, trainerId) } },
                onDesilenciar = { scope.launch { chatRepository.desilenciarContacto(currentUserId, trainerId) } },
                onEliminarConversacion = {
                    scope.launch { chatRepository.eliminarConversacion(currentUserId, trainerId) }
                    onBack()
                }
            )
        }
    }

    if (mostrarMenuOpciones) {
        MenuOpcionesContactoDialog(
            onDismiss = { mostrarMenuOpciones = false },
            bloqueado = bloqueado,
            silenciado = silenciado,
            onVerPerfil = { mostrarPerfilContacto = true },
            onBloquear = { scope.launch { chatRepository.bloquearContacto(currentUserId, trainerId) } },
            onDesbloquear = { scope.launch { chatRepository.desbloquearContacto(currentUserId, trainerId) } },
            onSilenciar = { scope.launch { chatRepository.silenciarContacto(currentUserId, trainerId) } },
            onDesilenciar = { scope.launch { chatRepository.desilenciarContacto(currentUserId, trainerId) } },
            onEliminarConversacion = {
                scope.launch { chatRepository.eliminarConversacion(currentUserId, trainerId) }
                onBack()
            }
        )
    }
}

/** Umbral para considerar "conectado": si su último heartbeat fue hace menos de 1 minuto. */
private const val UMBRAL_CONECTADO_MS = 60_000L

/** true si [usuario] mandó un heartbeat (ping de presencia) hace menos de [UMBRAL_CONECTADO_MS]. */
private fun estaConectado(usuario: UserEntity?): Boolean {
    val ultimoActivo = usuario?.lastActiveAt ?: return false
    return System.currentTimeMillis() - ultimoActivo < UMBRAL_CONECTADO_MS
}

/** Punto verde/gris + texto "Conectado"/"Desconectado", para el header del chat. */
@Composable
private fun EstadoConexion(usuario: UserEntity?) {
    if (usuario == null) return
    val conectado = estaConectado(usuario)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (conectado) VerdeTN else Color.Gray)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (conectado) "Conectado" else "Desconectado",
            color = if (conectado) VerdeTN else Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isFromCurrentUser: Boolean,
    trainerName: String,
    chatRepository: ChatRepository? = null
) {
    val context = LocalContext.current
    // Compatibilidad con mensajes viejos que mandaban la URI local cruda como texto
    // (antes de que existiera attachmentUrl/attachmentType).
    val esImagenLegacy = message.attachmentType == null &&
        (message.content.startsWith("content://") || message.content.startsWith("file://"))

    val urlAdjunto = message.attachmentUrl?.let { chatRepository?.urlCompletaDeAdjunto(it) }
    var mostrarVideo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            // Avatar del entrenador (solo en mensajes recibidos)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trainerName.firstOrNull()?.toString() ?: "T",
                    color = TextoPrincipal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFromCurrentUser) VerdeTN else GrisFondo
                )
            ) {
                when {
                    message.attachmentType == "IMAGE" && urlAdjunto != null -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(urlAdjunto).crossfade(true).build(),
                            contentDescription = "Imagen enviada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    message.attachmentType == "VIDEO" && urlAdjunto != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(8.dp)
                                .background(Color.Black, RoundedCornerShape(12.dp))
                                .clickable { mostrarVideo = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Reproducir video",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Toca para ver el video", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                    esImagenLegacy -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(message.content).crossfade(true).build(),
                            contentDescription = "Imagen enviada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    else -> {
                        Text(
                            text = message.content,
                            color = if (isFromCurrentUser) NegroFondo else Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatTimestamp(message.timestamp),
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (isFromCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }

    if (mostrarVideo && urlAdjunto != null) {
        VideoPlayerDialog(videoUrl = urlAdjunto, onDismiss = { mostrarVideo = false })
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
