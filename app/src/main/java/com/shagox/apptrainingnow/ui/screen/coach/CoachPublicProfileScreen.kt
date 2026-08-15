package com.shagox.apptrainingnow.ui.screen.coach

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.utils.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Límite de caracteres de la descripción corta (la que se ve en la tarjeta de "Mis chats"). */
private const val LIMITE_BIO = 150

/**
 * El entrenador edita cómo lo ven los usuarios en "Mis chats": nombre (fijo, viene de su
 * cuenta), imagen promocional (distinta de su foto de perfil) y una descripción corta.
 * La imagen se guarda en el microservicio de usuarios (promoImageUrl), igual que la foto
 * de perfil, no solo en el dispositivo.
 */
@Composable
fun CoachPublicProfileScreen(
    userRepository: IUserRepository,
    currentUserId: Int,
    onBack: () -> Unit
) {
    var usuario by remember { mutableStateOf<UserEntity?>(null) }
    var bio by remember { mutableStateOf("") }
    var promoImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(currentUserId) {
        val u = userRepository.getUserById(currentUserId)
        usuario = u
        bio = u?.bio.orEmpty()
        promoImageUrl = u?.promoImageUrl
        isLoading = false
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val dataUri = withContext(Dispatchers.IO) {
                    ImageCompressor.compressToDataUri(context, uri)
                }
                if (dataUri != null) promoImageUrl = dataUri
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        BackButtonTN(text = "Atrás", onClick = onBack)
        ScreenHeaderTN(subtitle = "Mi", title = "PERFIL PÚBLICO")

        if (isLoading || usuario == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VerdeTN)
            }
        } else {
            val u = usuario!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Así te ven los usuarios en su lista de \"Mis chats\". Mantén presionada tu propia tarjeta para ver la vista completa.",
                    color = GrisTexto,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // ==================== VISTA PREVIA (tarjeta compacta) ====================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GrisFondo)
                        .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${u.name} ${u.lastName}",
                            color = TextoPrincipal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        PromoImage(
                            url = promoImageUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { galleryLauncher.launch("image/*") }
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = bio.ifBlank { "Escribe una descripción para que los usuarios te conozcan" },
                            color = if (bio.isBlank()) GrisTexto else TextoPrincipal,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "Toca la imagen para cambiarla",
                    color = GrisTexto,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text("Descripción corta", color = GrisTexto, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= LIMITE_BIO) bio = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: Entrenador certificado, foco en fuerza e hipertrofia. ¡Escríbeme!") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal,
                        focusedBorderColor = VerdeTN,
                        unfocusedBorderColor = GrisTexto,
                        cursorColor = VerdeTN
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "${bio.length}/$LIMITE_BIO",
                    color = GrisTexto,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )

                Spacer(Modifier.height(20.dp))

                // ==================== DATOS QUE SE VEN AL MANTENER PRESIONADO ====================
                Text("En la vista completa también se muestran:", color = GrisTexto, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = VerdeTN, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(u.phone.ifBlank { "Sin teléfono registrado" }, color = TextoPrincipal, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Email, contentDescription = null, tint = VerdeTN, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(u.email, color = TextoPrincipal, fontSize = 13.sp)
                }

                message?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = VerdeTN, fontSize = 13.sp)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        isSaving = true
                        message = null
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    userRepository.updateUser(
                                        u.copy(bio = bio.trim(), promoImageUrl = promoImageUrl)
                                    )
                                }
                                usuario = u.copy(bio = bio.trim(), promoImageUrl = promoImageUrl)
                                message = "Perfil actualizado"
                            } catch (e: Exception) {
                                message = e.message ?: "No se pudo guardar"
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde)
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(22.dp), color = TextoSobreVerde)
                    else Text("Guardar cambios")
                }
            }
        }
    }
}

@Composable
private fun PromoImage(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(GrisTexto.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "Imagen promocional",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Campaign, contentDescription = null, tint = GrisTexto, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(4.dp))
                Text("Sin imagen todavía", color = GrisTexto, fontSize = 12.sp)
            }
        }
    }
}
