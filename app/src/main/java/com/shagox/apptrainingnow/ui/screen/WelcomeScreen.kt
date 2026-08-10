package com.shagox.apptrainingnow.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shagox.apptrainingnow.R
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.GrisFondo

private val VerdeOscuro = Color(0xFF0D3D1A)
private val VerdeNeon = Color(0xFF22FF5F)
private val GrisCard = GrisFondo

/**
 * Pantalla de bienvenida que se muestra solo la primera vez.
 * Diseño impactante: activación de permisos (cámara, galería, notificaciones).
 * Al pulsar "Entrar" se guarda que ya se vio y se navega a Rutinas; esta pantalla no vuelve a salir.
 */
@Composable
fun WelcomeScreen(
    onComenzar: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Permisos como estado reactivo: al aceptar, la tarjeta se actualiza al instante
    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun checkGallery(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            hasPermission(Manifest.permission.READ_MEDIA_IMAGES) ||
                    hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
        else -> hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var cameraPermission by remember { mutableStateOf(hasPermission(Manifest.permission.CAMERA)) }
    var galleryPermission by remember { mutableStateOf(checkGallery()) }
    var notificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermission = granted }
    // Galería: en Android 14+ el sistema puede abrir el selector de fotos (acceso parcial);
    // se piden ambos permisos y con cualquiera concedido la tarjeta queda activada.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> galleryPermission = checkGallery() }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> notificationPermission = granted }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VerdeOscuro, NegroFondo, NegroFondo)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logo_2),
                contentDescription = "Training NOW!",
                modifier = Modifier
                    .height(72.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logoiconotn),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Activa lo esencial",
                color = TextoPrincipal,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Para una mejor experiencia en TrainingNow",
                color = TextoPrincipal.copy(alpha = 0.85f),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tarjeta: Cámara
            PermissionCard(
                icon = Icons.Filled.CameraAlt,
                title = "Cámara",
                description = "Fotos de progreso y perfil",
                isGranted = cameraPermission,
                onActivate = {
                    if (!cameraPermission) {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta: Galería
            PermissionCard(
                icon = Icons.Filled.PhotoLibrary,
                title = "Galería",
                description = "Subir imágenes y guardar fotos",
                isGranted = galleryPermission,
                onActivate = {
                    if (!galleryPermission) {
                        val perms = when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                            )
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        galleryLauncher.launch(perms)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta: Notificaciones
            PermissionCard(
                icon = Icons.Filled.Notifications,
                title = "Notificaciones",
                description = "Recordatorios de entrenamiento",
                isGranted = notificationPermission,
                onActivate = {
                    if (!notificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                hideIfNotNeeded = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón principal: Entrar
            Button(
                onClick = onComenzar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = "ENTRAR A TRAININGNOW",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Pie
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "© 2025 - 2026 shagox",
                color = TextoPrincipal.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
            Text(
                text = "Versión 0.1.0",
                color = TextoPrincipal.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onActivate: () -> Unit,
    hideIfNotNeeded: Boolean = false
) {
    if (hideIfNotNeeded) return

    val borderColor by animateFloatAsState(
        targetValue = if (isGranted) 1f else 0.4f,
        animationSpec = tween(300),
        label = "border"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isGranted) GrisCard.copy(alpha = 0.45f) else GrisCard.copy(alpha = 0.9f))
            .border(
                width = 1.5.dp,
                color = if (isGranted) Color.Gray.copy(alpha = 0.5f) else VerdeTN.copy(alpha = borderColor),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(VerdeTN.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color.Gray else TextoPrincipal.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = title,
                    color = if (isGranted) Color.Gray else Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = if (isGranted) Color.Gray.copy(alpha = 0.7f) else TextoPrincipal.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Activado",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Button(
                    onClick = onActivate,
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Activar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewWelcomeScreen() {
    MaterialTheme {
        WelcomeScreen(onComenzar = { })
    }
}
