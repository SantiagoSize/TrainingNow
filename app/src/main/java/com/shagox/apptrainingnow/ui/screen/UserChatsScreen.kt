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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

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

        // Lista de entrenadores
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VerdeTN)
            }
        } else if (trainers.isEmpty()) {
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

    // Diálogo con datos del entrenador (al mantener 3 s en la tarjeta)
    trainerDetailDialog?.let { trainer ->
        TrainerDetailDialog(
            trainer = trainer,
            onDismiss = { trainerDetailDialog = null }
        )
    }
}

private fun ageFromBirthDate(birthDate: Long?): String {
    if (birthDate == null) return "No registrado"
    val cal = Calendar.getInstance()
    val now = cal.get(Calendar.YEAR)
    cal.timeInMillis = birthDate
    val year = cal.get(Calendar.YEAR)
    return (now - year).toString()
}

@Composable
private fun TrainerDetailDialog(
    trainer: UserEntity,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${trainer.name} ${trainer.lastName}",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ID: ${trainer.id}", color = GrisTexto)
                Text("Edad: ${ageFromBirthDate(trainer.birthDate)} años", color = GrisTexto)
                Text(
                    "Peso: ${trainer.weight?.toInt()?.toString()?.plus(" kg") ?: "No registrado"}",
                    color = GrisTexto
                )
                Text(
                    "Altura: ${trainer.height?.toInt()?.toString()?.plus(" cm") ?: "No registrado"}",
                    color = GrisTexto
                )
                if (trainer.role == "TRAINER" && !trainer.specializations.isNullOrBlank()) {
                    Text("Especialización: ${trainer.specializations}", color = VerdeTN)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cerrar", color = VerdeTN)
            }
        },
        containerColor = GrisFondo
    )
}

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
                            delay(3000)
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
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Foto de perfil
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GrisTexto.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (trainer.profilePhotoUrl != null && trainer.profilePhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(trainer.profilePhotoUrl)
                            .build(),
                        contentDescription = "Foto de ${trainer.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "${trainer.name.firstOrNull() ?: 'U'}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Información del entrenador
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${trainer.name} ${trainer.lastName}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${trainer.id}",
                    color = GrisTexto,
                    fontSize = 12.sp
                )
                if (trainer.specializations != null && trainer.specializations.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trainer.specializations,
                        color = VerdeTN,
                        fontSize = 13.sp
                    )
                }
            }

            // Botón de mensaje (círculo verde, icono avión)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VerdeTN),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar mensaje",
                    tint = NegroFondo,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
