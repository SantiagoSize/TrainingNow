package com.shagox.apptrainingnow.ui.screen.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.remote.dto.UserDto
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Vista del entrenador: todos los usuarios normales (rol USER) con su información,
 * incluida la fecha de creación de la cuenta y su estado. Solo lectura.
 */
@Composable
fun CoachUsersScreen(
    userRepository: IUserRepository,
    onBack: () -> Unit
) {
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            users = userRepository.getAllClientsInfo()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val filtered = if (query.isBlank()) users else users.filter {
        it.name.contains(query, true) || it.lastName.contains(query, true) ||
                it.email.contains(query, true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        BackButtonTN(text = "Volver", onClick = onBack)
        ScreenHeaderTN(
            subtitle = "Todos los",
            title = "USUARIOS",
            actionIcon = Icons.Filled.People,
            onActionClick = {}
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar por nombre o email...", color = GrisTexto) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = VerdeTN,
                unfocusedBorderColor = GrisTexto,
                cursorColor = VerdeTN
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        when {
            loading -> CircularProgressIndicator(color = VerdeTN, modifier = Modifier.padding(24.dp))
            error != null -> Text(error!!, color = Color(0xFFE53935), modifier = Modifier.padding(16.dp))
            filtered.isEmpty() -> Text("Sin usuarios", color = GrisTexto, modifier = Modifier.padding(16.dp))
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { user -> UserInfoCard(user) }
            }
        }
    }
}

@Composable
private fun UserInfoCard(user: UserDto) {
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val estado = when {
        user.isBanned -> "BANEADO" to Color(0xFFE53935)
        (user.suspendedUntil ?: 0L) > System.currentTimeMillis() -> "SUSPENDIDO" to Color(0xFFFFB300)
        else -> "ACTIVO" to VerdeTN
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = GrisFondo),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${user.name} ${user.lastName}".trim(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(estado.first, color = estado.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(user.email, color = GrisTexto, fontSize = 13.sp)
            if (user.phone.isNotBlank()) {
                Text("Tel: ${user.phone}", color = GrisTexto, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                user.createdAt?.let {
                    Text("Cuenta creada: ${dateFormat.format(Date(it))}", color = GrisTexto, fontSize = 12.sp)
                }
                user.weight?.let { Text("Peso: ${it} kg", color = GrisTexto, fontSize = 12.sp) }
                user.height?.let { Text("Altura: ${it} cm", color = GrisTexto, fontSize = 12.sp) }
            }
        }
    }
}
