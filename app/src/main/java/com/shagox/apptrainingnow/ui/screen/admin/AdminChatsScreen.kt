package com.shagox.apptrainingnow.ui.screen.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal

/**
 * Chat del admin: lista todos los usuarios para iniciar conversación.
 */
@Composable
fun AdminChatsScreen(
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository,
    chatRepository: com.shagox.apptrainingnow.data.repository.ChatRepository,
    currentUserId: Int,
    onNavigateToChat: (Int) -> Unit
) {
    val users by userRepository.getAllUsers().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        val list = users.filter { it.id != currentUserId }
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) list
        else list.filter {
            it.name.lowercase().contains(q) ||
                it.lastName.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.role.lowercase().contains(q) ||
                it.id.toString() == q
        }
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
            onActionClick = { }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por nombre, email o rol...", color = GrisTexto) },
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

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (users.isEmpty() || users.all { it.id == currentUserId })
                        "No hay otros usuarios"
                    else
                        "No hay resultados",
                    color = GrisTexto,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredUsers, key = { it.id }) { user ->
                    AdminChatUserCard(
                        user = user,
                        onClick = { onNavigateToChat(user.id) }
                    )
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
                Text(text = "ID: ${user.id} • ${user.role}", color = GrisTexto, fontSize = 12.sp)
            }
        }
    }
}
