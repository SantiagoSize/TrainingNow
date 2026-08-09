package com.shagox.apptrainingnow.ui.screen.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.viewmodel.CoachViewModel
import java.util.*

/**
 * Pantalla compacta con los datos básicos del cliente: imagen, ID, género, edad, estatura y peso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    viewModel: CoachViewModel,
    clientId: Int,
    onBack: () -> Unit,
    onChatClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedClient by viewModel.selectedClient.collectAsState()

    LaunchedEffect(clientId) {
        if (selectedClient?.id != clientId) {
            viewModel.loadClientById(clientId)
        }
    }

    selectedClient?.let { client ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Cliente", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = onChatClick) {
                            Icon(Icons.AutoMirrored.Filled.Chat, "Chat")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = VerdeTN,
                        titleContentColor = NegroFondo,
                        navigationIconContentColor = NegroFondo,
                        actionIconContentColor = NegroFondo
                    )
                )
            },
            containerColor = NegroFondo
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {
                // Tarjeta compacta: imagen + datos (estilo app: GrisFondo + borde VerdeTN)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VerdeTN, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrisFondo)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar / imagen
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(VerdeTN.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (client.profilePhotoUrl != null) {
                                AsyncImage(
                                    model = client.profilePhotoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = "${client.name.first()}${client.lastName.first()}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VerdeTN
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "${client.name} ${client.lastName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "ID: ${client.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrisTexto
                        )

                        Spacer(Modifier.height(16.dp))

                        // Datos: género, edad, estatura, peso
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            client.gender?.let { DataChip("Género", formatGender(it), Icons.Default.Person) }
                            client.birthDate?.let { DataChip("Edad", "${ageFromBirthDate(it)} años", Icons.Default.CalendarToday) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            client.height?.let { DataChip("Estatura", "${it.toInt()} cm", Icons.Default.Height) }
                            client.weight?.let { DataChip("Peso", "${it.toInt()} kg", Icons.Default.Scale) }
                        }
                    }
                }
            }
        }
    } ?: run {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NegroFondo),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = VerdeTN)
            } else if (uiState.clientNotFound) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Cliente no encontrado", color = GrisTexto)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onBack) { Text("Volver", color = VerdeTN) }
                }
            } else {
                CircularProgressIndicator(color = VerdeTN)
            }
        }
    }
}

@Composable
private fun DataChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(20.dp), tint = VerdeTN)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(label, fontSize = 12.sp, color = GrisTexto)
    }
}

private fun formatGender(g: String): String = when (g.uppercase()) {
    "M" -> "Masculino"
    "F" -> "Femenino"
    else -> g
}

private fun ageFromBirthDate(birthDateMillis: Long): Int {
    val cal = Calendar.getInstance()
    val now = cal.get(Calendar.YEAR)
    cal.timeInMillis = birthDateMillis
    return now - cal.get(Calendar.YEAR)
}
