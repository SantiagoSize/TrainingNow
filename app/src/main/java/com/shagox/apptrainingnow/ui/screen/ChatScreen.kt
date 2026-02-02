package com.shagox.apptrainingnow.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shagox.apptrainingnow.data.local.chat.MessageEntity
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.utils.ComposeFileProvider
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
    var lastCameraUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

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
            lastCameraUri?.let { uri ->
                scope.launch {
                    chatRepository.sendMessage(
                        MessageEntity(
                            senderId = currentUserId,
                            receiverId = trainerId,
                            content = uri.toString()
                        )
                    )
                }
                lastCameraUri = null
            }
        }
    }
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                chatRepository.sendMessage(
                    MessageEntity(
                        senderId = currentUserId,
                        receiverId = trainerId,
                        content = it.toString()
                    )
                )
            }
        }
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

    // Cargar información del entrenador
    LaunchedEffect(trainerId) {
        scope.launch {
            try {
                trainer = userRepository.getUserById(trainerId)
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Error al cargar entrenador", e)
            }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
    ) {
        // Header con información del entrenador y botón para salir del chat
        Surface(
            color = GrisFondo,
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
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Foto del entrenador
                Box(
                    modifier = Modifier
                        .size(50.dp)
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
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val currentTrainer = trainer
                    Text(
                        text = "${currentTrainer?.name ?: "Entrenador"} ${currentTrainer?.lastName ?: ""}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val specializations = currentTrainer?.specializations
                    if (specializations != null && specializations.isNotBlank()) {
                        Text(
                            text = specializations,
                            color = VerdeTN,
                            fontSize = 12.sp
                        )
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
                    trainerName = currentTrainer?.name ?: "Entrenador"
                )
            }
        }

        // Input de mensaje
        Surface(
            color = GrisFondo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cámara
                IconButton(
                    onClick = { sendImageFromCamera() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Tomar foto",
                        tint = VerdeTN,
                        modifier = Modifier.size(26.dp)
                    )
                }
                // Galería / biblioteca
                IconButton(
                    onClick = { sendImageFromGallery() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Elegir de galería",
                        tint = VerdeTN,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
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
                        tint = NegroFondo,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isFromCurrentUser: Boolean,
    trainerName: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            // Avatar del entrenador (solo en mensajes recibidos)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trainerName.firstOrNull()?.toString() ?: "T",
                    color = Color.White,
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
            val isImageMessage = message.content.startsWith("content://") || message.content.startsWith("file://")
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
                if (isImageMessage) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(message.content)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagen enviada",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = message.content,
                        color = if (isFromCurrentUser) NegroFondo else Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
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
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
