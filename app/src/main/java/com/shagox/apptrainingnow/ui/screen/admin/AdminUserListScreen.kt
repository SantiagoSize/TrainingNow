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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import java.text.SimpleDateFormat
import java.util.*

/**
 * Lista de todos los usuarios (admin).
 */

/** Filtro de rol para la lista de usuarios. */
private enum class RoleFilter { TODOS, ADMIN, USUARIOS, ENTRENADOR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserListScreen(
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository,
    onBack: () -> Unit
) {
    val users by userRepository.getAllUsers().collectAsState(initial = emptyList())
    var roleFilter by remember { mutableStateOf(RoleFilter.TODOS) }
    val filteredUsers = remember(users, roleFilter) {
        when (roleFilter) {
            RoleFilter.TODOS -> users
            RoleFilter.ADMIN -> users.filter { it.role == "ADMIN" }
            RoleFilter.USUARIOS -> users.filter { it.role == "USER" }
            RoleFilter.ENTRENADOR -> users.filter { it.role == "TRAINER" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todos los usuarios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VerdeTN,
                    titleContentColor = NegroFondo,
                    navigationIconContentColor = NegroFondo
                )
            )
        },
        containerColor = NegroFondo
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    RoleFilter.TODOS to "Todos",
                    RoleFilter.ADMIN to "Admin",
                    RoleFilter.USUARIOS to "Usuarios",
                    RoleFilter.ENTRENADOR to "Entrenador"
                ).forEach { (filter, label) ->
                    FilterChip(
                        selected = roleFilter == filter,
                        onClick = { roleFilter = filter },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerdeTN,
                            selectedLabelColor = TextoSobreVerde,
                            containerColor = GrisFondo,
                            labelColor = Color.White
                        )
                    )
                }
            }
            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (users.isEmpty()) "No hay usuarios" else "No hay usuarios con este filtro",
                        color = GrisTexto
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredUsers, key = { it.id }) { user ->
                        UserListCard(user = user)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserListCard(user: UserEntity) {
    val suspendedUntil = user.suspendedUntil
    val isSuspended = suspendedUntil != null && suspendedUntil > System.currentTimeMillis()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(VerdeTN.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${user.name.first()}${user.lastName.first()}",
                    color = VerdeTN,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.name} ${user.lastName}",
                    color = TextoPrincipal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = user.email, color = GrisTexto, fontSize = 13.sp)
                Text(
                    text = "ID: ${user.id} • ${user.role}",
                    color = GrisTexto,
                    fontSize = 12.sp
                )
                if (user.isBanned) {
                    Text(text = "Baneado: ${user.banReason ?: ""}", color = Color(0xFFE57373), fontSize = 12.sp)
                } else if (isSuspended) {
                    Text(
                        text = "Suspendido hasta ${dateFormat.format(Date(suspendedUntil))}: ${user.suspendReason ?: ""}",
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
