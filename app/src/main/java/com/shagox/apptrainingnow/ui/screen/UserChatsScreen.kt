package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.UserRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun UserChatsScreen(
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    currentUserId: Int,
    onNavigateToChat: (Int) -> Unit // trainerId
) {
    var searchQuery by remember { mutableStateOf("") }
    var trainers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
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
            actionIcon = Icons.Default.Search,
            onActionClick = { /* búsqueda */ }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por nombre, ID o especialidad...", color = Color.Gray) },
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
                unfocusedBorderColor = Color.Gray,
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
                    color = Color.Gray,
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
                        onMessageClick = { onNavigateToChat(trainer.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrainerCard(
    trainer: UserEntity,
    onMessageClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMessageClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GrisFondo
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de perfil
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
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
                    // Placeholder si no hay foto
                    Text(
                        text = "${trainer.name.firstOrNull() ?: 'U'}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del entrenador
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${trainer.name} ${trainer.lastName}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${trainer.id}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                if (trainer.specializations != null && trainer.specializations.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trainer.specializations,
                        color = VerdeTN,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Botón de mensaje
            IconButton(
                onClick = onMessageClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = VerdeTN.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar mensaje",
                    tint = VerdeTN,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
