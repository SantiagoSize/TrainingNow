package com.shagox.apptrainingnow.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.R
import com.shagox.apptrainingnow.data.repository.PasswordResetRepository
import kotlinx.coroutines.launch
import com.shagox.apptrainingnow.ui.components.IconoOjoContrasena
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.SuperficieElevada
import com.shagox.apptrainingnow.ui.theme.LocalTemaClaro
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModel
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.utils.ComposeFileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla Perfil: pestaña para iniciar sesión y registrarse en la misma página.
 * Si el usuario está logueado, muestra sus datos (cerrar sesión vive en Ajustes).
 */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val loginState by authViewModel.loginState.collectAsState()
    val registerState by authViewModel.register.collectAsState()
    val justRegistered by authViewModel.justRegistered.collectAsState()
    val loggedUser = loginState.loggedUser
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Iniciar sesión, 1 = Registrarse
    var showEditProfile by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

    if (justRegistered && loggedUser != null) {
        WelcomeOnboardingDialog(
            user = loggedUser,
            authViewModel = authViewModel,
            onFinish = { authViewModel.consumeJustRegistered() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Tu",
            title = "PERFIL",
            actionIcon = Icons.Filled.Person,
            onActionClick = {}  // Botón verde con icono siempre visible (diseño)
        )

        if (loggedUser != null) {
            ProfileLoggedContent(
                user = loggedUser,
                authViewModel = authViewModel,
                onEdit = { showEditProfile = true }
            )

            if (showEditProfile) {
                EditProfileDialog(
                    user = loggedUser,
                    onDismiss = { showEditProfile = false },
                    onSave = { updated ->
                        authViewModel.updateUser(updated)
                        showEditProfile = false
                    }
                )
            }
        } else {
            // Logo Training Now! en la zona superior (pantalla de login/registro)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        if (LocalTemaClaro.current) R.drawable.logo_claro else R.drawable.logo_2
                    ),
                    contentDescription = "Training Now!",
                    modifier = Modifier.fillMaxWidth(0.62f)
                )
            }

            // No logueado: control segmentado (bordes redondeados en los extremos, integrado con fondo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NegroFondo)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selectedTab == 0) VerdeTN else NegroFondo)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "INICIAR SESIÓN",
                        color = if (selectedTab == 0) androidx.compose.ui.graphics.Color.White else GrisTexto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selectedTab == 1) VerdeTN else NegroFondo)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "REGISTRARSE",
                        color = if (selectedTab == 1) androidx.compose.ui.graphics.Color.White else GrisTexto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (selectedTab == 0) {
                LoginTabContent(
                    loginState = loginState,
                    onEmailChange = { authViewModel.onLoginEmailChange(it) },
                    onPassChange = { authViewModel.onLoginPassChange(it) },
                    onSubmit = { authViewModel.submitLogin() },
                    onForgotPassword = { showForgotPassword = true }
                )

                if (showForgotPassword) {
                    ForgotPasswordDialog(
                        initialEmail = loginState.email,
                        onDismiss = { showForgotPassword = false }
                    )
                }
            } else {
                RegisterTabContent(
                    registerState = registerState,
                    onNameChange = { authViewModel.onNameChange(it) },
                    onLastNameChange = { authViewModel.onLastNameChange(it) },
                    onEmailChange = { authViewModel.onRegisterEmailChange(it) },
                    onPhoneChange = { authViewModel.onPhoneChange(it) },
                    onPassChange = { authViewModel.onRegisterPassChange(it) },
                    onConfirmChange = { authViewModel.onConfirmChange(it) },
                    onTermsAcceptedChange = { authViewModel.onTermsAcceptedChange(it) },
                    onSubmit = { authViewModel.submitRegister() }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "TrainingNow v1.0",
                color = GrisTexto,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            // 3 botones invisibles: Usuario normal | Entrenador (@coach.tn) | Admin (@admin.tn)
            if (selectedTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Usuario normal (usuario@gmail.com - seed de la BD)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .alpha(0f)
                            .clickable {
                                authViewModel.onLoginEmailChange("usuario@gmail.com")
                                authViewModel.onLoginPassChange("user123")
                            }
                    )
                    // Entrenador (@trainingnow.com)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .alpha(0f)
                            .clickable {
                                authViewModel.onLoginEmailChange("entrenador@trainingnow.com")
                                authViewModel.onLoginPassChange("entrenador123")
                            }
                    )
                    // Admin (@trainingnow.com)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .alpha(0f)
                            .clickable {
                                authViewModel.onLoginEmailChange("admin@trainingnow.com")
                                authViewModel.onLoginPassChange("admin123")
                            }
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatBirthDate(birthDate: Long?): String {
    if (birthDate == null) return "No registrado"
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(birthDate))
    } catch (_: Exception) {
        "No registrado"
    }
}

/** Muestra la altura (guardada siempre en cm) en la unidad elegida por el usuario. */
private fun formatearAltura(context: android.content.Context, cm: Float): String {
    val imperial = com.shagox.apptrainingnow.utils.UnitsPreference.esImperial(context)
    return if (imperial) {
        val pulgadas = com.shagox.apptrainingnow.utils.UnitsPreference.cmAPulgadas(cm.toDouble())
        "${"%.1f".format(pulgadas)} in"
    } else {
        "${cm.toInt()} cm"
    }
}

/** Muestra el peso (guardado siempre en kg) en la unidad elegida por el usuario. */
private fun formatearPeso(context: android.content.Context, kg: Float): String {
    val imperial = com.shagox.apptrainingnow.utils.UnitsPreference.esImperial(context)
    return if (imperial) {
        val libras = com.shagox.apptrainingnow.utils.UnitsPreference.kgALibras(kg.toDouble())
        "${"%.1f".format(libras)} lb"
    } else {
        "${kg.toInt()} kg"
    }
}

private fun formatGender(gender: String?): String {
    if (gender.isNullOrBlank()) return "No registrado"
    return when (gender.uppercase()) {
        "M" -> "Masculino"
        "F" -> "Femenino"
        "MALE" -> "Masculino"
        "FEMALE" -> "Femenino"
        "N" -> "No especificar"
        else -> gender
    }
}

@Composable
private fun ProfileDataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GrisFondo)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VerdeTN,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = GrisTexto, fontSize = 12.sp)
            Text(
                value,
                color = TextoPrincipal,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProfileLoggedContent(
    user: UserEntity,
    authViewModel: AuthViewModel,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
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
        ActivityResultContracts.RequestPermission()
    ) { }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val path = ComposeFileProvider.getProfilePhotoFile(context, user.id).absolutePath
            authViewModel.updateProfilePhoto(user.id, path)
        }
    }
    val getContentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val file = ComposeFileProvider.getProfilePhotoFile(context, user.id)
                    file.outputStream().use { output -> input.copyTo(output) }
                    authViewModel.updateProfilePhoto(user.id, file.absolutePath)
                }
            } catch (_: Exception) { }
        }
    }

    if (showPhotoOptions) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Foto de perfil", color = TextoPrincipal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Elige una opción", color = GrisTexto)
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showPhotoOptions = false
                            if (!hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                takePictureLauncher.launch(ComposeFileProvider.getProfilePhotoUri(context, user.id))
                            }
                        }
                    ) { Text("Cámara", color = VerdeTN) }
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showPhotoOptions = false
                            if (!hasGalleryPermission) {
                                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
                                galleryPermissionLauncher.launch(perm)
                            } else {
                                getContentLauncher.launch("image/*")
                            }
                        }
                    ) { Text("Galería", color = VerdeTN) }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showPhotoOptions = false }) {
                    Text("Cancelar", color = GrisTexto)
                }
            },
            containerColor = GrisFondo
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp)
    ) {
        // Foto de perfil (circular, tappable para cambiar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { showPhotoOptions = true }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(GrisFondo)
                    ) {
                        val photoUrl = user.profilePhotoUrl
                        if (!photoUrl.isNullOrBlank()) {
                            val path = if (photoUrl.startsWith("/")) File(photoUrl) else photoUrl
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(path).build(),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${user.name.firstOrNull()?.uppercaseChar() ?: '?'}",
                                    color = VerdeTN,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    // Icono de cámara (overlay) para indicar que se puede cambiar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(VerdeTN),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Cambiar foto",
                            tint = TextoSobreVerde,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Toca para cambiar foto",
                    color = GrisTexto,
                    fontSize = 12.sp
                )
            }
        }

        // Header: DATOS PERSONALES + EDITAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DATOS PERSONALES",
                color = VerdeTN,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(GrisFondo)
                    .clickable(onClick = { showEditDialog = true })
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = VerdeTN,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("EDITAR", color = VerdeTN, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        ProfileDataRow(
            icon = Icons.Filled.Person,
            label = "Nombre",
            value = "${user.name} ${user.lastName}".trim().ifBlank { user.name }
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileDataRow(
            icon = Icons.Filled.Email,
            label = "Email",
            value = maskEmail(user.email)
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileDataRow(
            icon = Icons.Filled.Phone,
            label = "Teléfono",
            value = user.phone.ifBlank { "No registrado" }
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileDataRow(
            icon = Icons.Filled.CalendarToday,
            label = "Fecha de nacimiento",
            value = formatBirthDate(user.birthDate)
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileDataRow(
            icon = Icons.Filled.Person,
            label = "Género",
            value = formatGender(user.gender)
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileDataRow(
            icon = Icons.Filled.Person,
            label = "Altura",
            value = user.height?.let { formatearAltura(context, it) } ?: "No registrado"
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileDataRow(
            icon = Icons.Filled.Person,
            label = "Peso",
            value = user.weight?.let { formatearPeso(context, it) } ?: "No registrado"
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showEditDialog) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                authViewModel.updateUser(updated)
                showEditDialog = false
            }
        )
    }
}

private fun parseBirthDateToMillis(str: String?): Long? {
    if (str.isNullOrBlank() || str.trim().length != 10) return null
    return try {
        // isLenient = false: por defecto SimpleDateFormat "corrige" fechas imposibles en vez
        // de rechazarlas (ej. mes 40 se convertía silenciosamente en una fecha futura años
        // después, sin avisar). Con isLenient = false, día/mes fuera de rango lanza excepción.
        val millis = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .apply { isLenient = false }
            .parse(str.trim())?.time ?: return null
        if (!esFechaNacimientoRazonable(millis)) return null
        millis
    } catch (_: Exception) { null }
}

/** Rechaza fechas futuras y edades fuera de un rango humano razonable (0 a 120 años). */
private fun esFechaNacimientoRazonable(millis: Long): Boolean {
    val ahora = System.currentTimeMillis()
    if (millis > ahora) return false
    val edadMs = ahora - millis
    val edadAnios = edadMs / (365.25 * 24 * 60 * 60 * 1000)
    return edadAnios in 0.0..120.0
}

/**
 * Autoformatea la fecha de nacimiento mientras se escribe: el usuario solo escribe números
 * (día, mes, año seguidos) y las "/" se agregan solas cada 2 dígitos, quedando "dd/MM/yyyy".
 * Antes había que escribir las barras a mano, lo que era incómodo y fácil de escribir mal.
 */
private fun autoFormatearFecha(texto: String): String {
    val digitos = texto.filter { it.isDigit() }.take(8) // ddMMyyyy = 8 dígitos como máximo
    val sb = StringBuilder()
    for (i in digitos.indices) {
        if (i == 2 || i == 4) sb.append('/')
        sb.append(digitos[i])
    }
    return sb.toString()
}

/** Enmascara el correo mostrando solo los primeros 3 caracteres, ej: "usu•••@gmail.com". */
private fun maskEmail(email: String): String {
    val atIndex = email.indexOf('@')
    if (atIndex <= 0) return email
    val local = email.substring(0, atIndex)
    val domain = email.substring(atIndex)
    val visible = local.take(3)
    val hiddenLength = (local.length - visible.length).coerceAtLeast(3)
    return "$visible${"•".repeat(hiddenLength)}$domain"
}

/** Código de país telefónico (solo América), para que el usuario no tenga que escribirlo a mano. */
private data class PaisTelefono(val nombre: String, val bandera: String, val codigo: String)

private val PAISES_AMERICA = listOf(
    PaisTelefono("Chile", "🇨🇱", "+56"),
    PaisTelefono("Argentina", "🇦🇷", "+54"),
    PaisTelefono("Bolivia", "🇧🇴", "+591"),
    PaisTelefono("Brasil", "🇧🇷", "+55"),
    PaisTelefono("Canadá", "🇨🇦", "+1"),
    PaisTelefono("Colombia", "🇨🇴", "+57"),
    PaisTelefono("Costa Rica", "🇨🇷", "+506"),
    PaisTelefono("Cuba", "🇨🇺", "+53"),
    PaisTelefono("Ecuador", "🇪🇨", "+593"),
    PaisTelefono("Estados Unidos", "🇺🇸", "+1"),
    PaisTelefono("El Salvador", "🇸🇻", "+503"),
    PaisTelefono("Guatemala", "🇬🇹", "+502"),
    PaisTelefono("Honduras", "🇭🇳", "+504"),
    PaisTelefono("México", "🇲🇽", "+52"),
    PaisTelefono("Nicaragua", "🇳🇮", "+505"),
    PaisTelefono("Panamá", "🇵🇦", "+507"),
    PaisTelefono("Paraguay", "🇵🇾", "+595"),
    PaisTelefono("Perú", "🇵🇪", "+51"),
    PaisTelefono("Puerto Rico", "🇵🇷", "+1"),
    PaisTelefono("República Dominicana", "🇩🇴", "+1"),
    PaisTelefono("Uruguay", "🇺🇾", "+598"),
    PaisTelefono("Venezuela", "🇻🇪", "+58")
)

/**
 * Detecta la bandera de América según el código con el que empieza el teléfono que el usuario
 * va escribiendo (ej. "56912345678" → 🇨🇱, sin "+" porque el campo ahora es solo dígitos).
 * Mientras no escriba un código reconocido, muestra un ícono neutro. Puramente visual:
 * no modifica lo que el usuario escribió.
 */
private fun detectarBanderaTelefono(telefono: String): String {
    val texto = telefono.trim()
    if (texto.isBlank()) return "📱"
    return PAISES_AMERICA
        .sortedByDescending { it.codigo.length }
        .firstOrNull { texto.startsWith(it.codigo.removePrefix("+")) }
        ?.bandera
        ?: "📱"
}

/**
 * Selector de género con tres cuadrados (Mujer / Hombre / No especificar). Al elegir uno se
 * resalta en verde y los demás se ven atenuados en gris. [seleccionado] usa "F", "M" o "N"
 * (mismo formato que ya entiende [formatGender]); null = todavía no eligió ninguno (todos
 * normales). "No especificar" existe para respetar a quienes no se identifican como hombre
 * o mujer: mejor tener la opción disponible desde el inicio que no tenerla.
 */
@Composable
private fun GenderSelector(
    seleccionado: String?,
    onSeleccionar: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GenderOption(
            emoji = "👩",
            label = "Mujer",
            isSelected = seleccionado == "F",
            haySeleccion = seleccionado != null,
            modifier = Modifier.weight(1f),
            onClick = { onSeleccionar("F") }
        )
        GenderOption(
            emoji = "👨",
            label = "Hombre",
            isSelected = seleccionado == "M",
            haySeleccion = seleccionado != null,
            modifier = Modifier.weight(1f),
            onClick = { onSeleccionar("M") }
        )
        GenderOption(
            emoji = "✳️",
            label = "No especificar",
            isSelected = seleccionado == "N",
            haySeleccion = seleccionado != null,
            modifier = Modifier.weight(1f),
            onClick = { onSeleccionar("N") }
        )
    }
}

@Composable
private fun GenderOption(
    emoji: String,
    label: String,
    isSelected: Boolean,
    haySeleccion: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Atenuado = hay una opción elegida y no es esta (la contraria queda en gris)
    val atenuado = haySeleccion && !isSelected
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) VerdeTN.copy(alpha = 0.15f) else GrisFondo)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) VerdeTN else GrisBorde,
                shape = RoundedCornerShape(16.dp)
            )
            .alpha(if (atenuado) 0.4f else 1f)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            color = if (isSelected) VerdeTN else TextoPrincipal,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}

/**
 * Selector de unidades: Métrico (kg/cm) o Imperial (lb/in). Dos pastillas, la elegida se
 * resalta en verde. Se usa en el carrusel de bienvenida y en Ajustes (misma preferencia,
 * ver [com.shagox.apptrainingnow.utils.UnitsPreference]).
 */
@Composable
private fun UnitsSelector(
    imperial: Boolean,
    onCambiar: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        UnitOption(
            label = "Métrico (kg/cm)",
            isSelected = !imperial,
            modifier = Modifier.weight(1f),
            onClick = { onCambiar(false) }
        )
        UnitOption(
            label = "Imperial (lb/in)",
            isSelected = imperial,
            modifier = Modifier.weight(1f),
            onClick = { onCambiar(true) }
        )
    }
}

@Composable
private fun UnitOption(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) VerdeTN.copy(alpha = 0.15f) else GrisFondo)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) VerdeTN else GrisBorde,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isSelected) VerdeTN else TextoPrincipal,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EditProfileDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit
) {
    var name by remember(user) { mutableStateOf(user.name) }
    var lastName by remember(user) { mutableStateOf(user.lastName) }
    var email by remember(user) { mutableStateOf(user.email) }
    var phone by remember(user) { mutableStateOf(user.phone) }
    var birthDateStr by remember(user) {
        mutableStateOf(user.birthDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) } ?: "")
    }
    var gender by remember(user) { mutableStateOf(user.gender?.uppercase()?.takeIf { it == "M" || it == "F" || it == "N" }) }
    val context = LocalContext.current
    // La altura/peso se guardan siempre en cm/kg; acá solo se muestran/editan convertidos a
    // la unidad elegida por el usuario (ver Ajustes), y se reconvierten a cm/kg al guardar.
    val imperial = remember { com.shagox.apptrainingnow.utils.UnitsPreference.esImperial(context) }
    var heightStr by remember(user) {
        mutableStateOf(
            user.height?.let {
                if (imperial) "%.1f".format(com.shagox.apptrainingnow.utils.UnitsPreference.cmAPulgadas(it.toDouble()))
                else it.toString()
            } ?: ""
        )
    }
    var weightStr by remember(user) {
        mutableStateOf(
            user.weight?.let {
                if (imperial) "%.1f".format(com.shagox.apptrainingnow.utils.UnitsPreference.kgALibras(it.toDouble()))
                else it.toString()
            } ?: ""
        )
    }
    var specializations by remember(user) { mutableStateOf(user.specializations ?: "") }
    var bio by remember(user) { mutableStateOf(user.bio ?: "") }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        cursorColor = VerdeTN,
        focusedLabelColor = VerdeTN,
        unfocusedLabelColor = GrisTexto,
        focusedContainerColor = GrisFondo,
        unfocusedContainerColor = GrisFondo
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(NegroFondo)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Editar perfil", color = VerdeTN, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Apellido", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Email", color = GrisTexto) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = GrisTexto,
                    disabledBorderColor = GrisTexto.copy(alpha = 0.4f),
                    disabledLabelColor = GrisTexto,
                    focusedTextColor = TextoPrincipal,
                    unfocusedTextColor = TextoPrincipal,
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisTexto,
                    cursorColor = VerdeTN,
                    focusedLabelColor = VerdeTN,
                    unfocusedLabelColor = GrisTexto,
                    focusedContainerColor = GrisFondo,
                    unfocusedContainerColor = GrisFondo
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Text(
                "El correo no se puede cambiar. Contacta a un administrador si necesitas actualizarlo.",
                color = GrisTexto,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() }.take(16) },
                label = { Text("Teléfono", color = GrisTexto) },
                placeholder = { Text("Solo números", color = GrisTexto) },
                leadingIcon = { Text(detectarBanderaTelefono(phone), fontSize = 20.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = birthDateStr,
                onValueChange = { birthDateStr = autoFormatearFecha(it) },
                label = { Text("Fecha nacimiento (dd/MM/yyyy)", color = GrisTexto) },
                placeholder = { Text("Ej: 05061998", color = GrisTexto) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = birthDateStr.length == 10 && parseBirthDateToMillis(birthDateStr) == null,
                supportingText = {
                    if (birthDateStr.length == 10 && parseBirthDateToMillis(birthDateStr) == null) {
                        Text("Fecha inválida", color = androidx.compose.ui.graphics.Color(0xFFE53935))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Género", color = GrisTexto, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            GenderSelector(seleccionado = gender, onSeleccionar = { gender = it })
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = heightStr, onValueChange = { heightStr = it }, label = { Text(if (imperial) "Altura (in)" else "Altura (cm)", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = weightStr, onValueChange = { weightStr = it }, label = { Text(if (imperial) "Peso (lb)" else "Peso (kg)", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            if (user.role == "TRAINER") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = specializations, onValueChange = { specializations = it }, label = { Text("Especializaciones", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Descripción de tu perfil", color = GrisTexto) },
                    placeholder = { Text("Cuéntales a tus clientes sobre ti...", color = GrisTexto) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GrisFondo),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar", color = TextoPrincipal) }
                Button(
                    onClick = {
                        // Reconvertir a cm/kg (formato canónico) antes de guardar, si el
                        // usuario editó en pulgadas/libras.
                        val alturaCm = heightStr.toFloatOrNull()?.let {
                            if (imperial) com.shagox.apptrainingnow.utils.UnitsPreference.pulgadasACm(it.toDouble()).toFloat() else it
                        }
                        val pesoKg = weightStr.toFloatOrNull()?.let {
                            if (imperial) com.shagox.apptrainingnow.utils.UnitsPreference.librasAKg(it.toDouble()).toFloat() else it
                        }
                        val updated = user.copy(
                            name = name.trim(),
                            lastName = lastName.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            birthDate = parseBirthDateToMillis(birthDateStr),
                            gender = gender,
                            height = alturaCm,
                            weight = pesoKg,
                            specializations = specializations.trim().takeIf { it.isNotBlank() },
                            bio = bio.trim().takeIf { it.isNotBlank() }
                        )
                        onSave(updated)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Guardar", color = TextoSobreVerde) }
            }
        }
    }
}

@Composable
private fun LoginTabContent(
    loginState: com.shagox.apptrainingnow.ui.viewmodel.LoginUiState,
    onEmailChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onSubmit: () -> Unit
,
    onForgotPassword: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = loginState.email,
            onValueChange = onEmailChange,
            label = { Text("Correo Electrónico", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal,
                focusedBorderColor = VerdeTN,
                unfocusedBorderColor = GrisTexto,
                cursorColor = VerdeTN,
                focusedLabelColor = VerdeTN,
                unfocusedLabelColor = GrisTexto,
                focusedContainerColor = GrisFondo,
                unfocusedContainerColor = GrisFondo
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (loginState.emailError != null) {
            Text(loginState.emailError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))

        var loginPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = loginState.pass,
            onValueChange = onPassChange,
            label = { Text("Contraseña", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (loginPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconoOjoContrasena(visible = loginPassVisible, onToggle = { loginPassVisible = !loginPassVisible }, tint = GrisTexto)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal,
                focusedBorderColor = VerdeTN,
                unfocusedBorderColor = GrisTexto,
                cursorColor = VerdeTN,
                focusedLabelColor = VerdeTN,
                unfocusedLabelColor = GrisTexto,
                focusedContainerColor = GrisFondo,
                unfocusedContainerColor = GrisFondo
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState.canSubmit && !loginState.isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = GrisFondo, contentColor = TextoPrincipal),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (loginState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), color = VerdeTN)
            } else {
                Text("INICIAR SESIÓN", fontWeight = FontWeight.SemiBold)
            }
        }
        TextButton(
            onClick = onForgotPassword,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿Olvidaste tu contraseña?", color = VerdeTN, fontSize = 13.sp)
        }
        if (loginState.errorMsg != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF8B0000))  // Fondo rojo oscuro/granate
                    .padding(12.dp)
            ) {
                Text(
                    loginState.errorMsg,
                    color = androidx.compose.ui.graphics.Color(0xFFE53935),  // Texto rojo vibrante
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RegisterTabContent(
    registerState: com.shagox.apptrainingnow.ui.viewmodel.RegisterUiState,
    onNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    var showTerms by remember { mutableStateOf(false) }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        cursorColor = VerdeTN,
        focusedLabelColor = VerdeTN,
        unfocusedLabelColor = GrisTexto,
        focusedContainerColor = GrisFondo,
        unfocusedContainerColor = GrisFondo
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = registerState.name,
            onValueChange = onNameChange,
            label = { Text("Nombres", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.nameError != null) Text(registerState.nameError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = registerState.lastName,
            onValueChange = onLastNameChange,
            label = { Text("Apellidos", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.lastNameError != null) Text(registerState.lastNameError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = registerState.email,
            onValueChange = onEmailChange,
            label = { Text("Correo", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.emailError != null) Text(registerState.emailError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = registerState.phone,
            onValueChange = onPhoneChange,
            label = { Text("Teléfono", color = GrisTexto) },
            placeholder = { Text("56912345678", color = GrisTexto) },
            leadingIcon = { Text(detectarBanderaTelefono(registerState.phone), fontSize = 20.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        var registerPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = registerState.pass,
            onValueChange = onPassChange,
            label = { Text("Contraseña", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (registerPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconoOjoContrasena(visible = registerPassVisible, onToggle = { registerPassVisible = !registerPassVisible }, tint = GrisTexto)
            },
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.passError != null) Text(registerState.passError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        var registerConfirmVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = registerState.confirm,
            onValueChange = onConfirmChange,
            label = { Text("Confirmar contraseña", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (registerConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconoOjoContrasena(visible = registerConfirmVisible, onToggle = { registerConfirmVisible = !registerConfirmVisible }, tint = GrisTexto)
            },
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.confirmError != null) Text(registerState.confirmError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = registerState.termsAccepted,
                onCheckedChange = { marcar ->
                    // Al marcar, primero se muestra el cartel de T&C; recién ahí queda aceptado.
                    // Al desmarcar no hace falta volver a mostrarlo.
                    if (marcar) showTerms = true else onTermsAcceptedChange(false)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = VerdeTN,
                    uncheckedColor = GrisTexto,
                    checkmarkColor = TextoSobreVerde
                )
            )
            Text(
                text = "Acepto los Términos y Condiciones",
                color = TextoPrincipal,
                fontSize = 13.sp,
                modifier = Modifier.clickable { showTerms = true }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = registerState.canSubmit && !registerState.isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (registerState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), color = TextoSobreVerde)
            } else {
                Text("REGISTRARSE", fontWeight = FontWeight.SemiBold)
            }
        }
        if (registerState.errorMsg != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(registerState.errorMsg, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 14.sp)
        }
        if (registerState.success) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Registro exitoso. Inicia sesión.", color = VerdeTN, fontSize = 14.sp)
        }
    }

    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            containerColor = GrisFondo,
            title = { Text("Términos y Condiciones", color = TextoPrincipal, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = TERMINOS_Y_CONDICIONES_TN,
                        color = TextoPrincipal,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTermsAcceptedChange(true)
                    showTerms = false
                }) {
                    Text("ACEPTAR", color = VerdeTN, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTerms = false }) {
                    Text("CERRAR", color = GrisTexto)
                }
            }
        )
    }
}

/** Términos y condiciones de uso de TrainingNow (contenido de referencia para el proyecto académico). */
internal const val TERMINOS_Y_CONDICIONES_TN = """
1. ACEPTACIÓN DE LOS TÉRMINOS
Al registrarte en TrainingNow aceptas estos Términos y Condiciones y nuestra Política de Privacidad. Si no estás de acuerdo, no debes utilizar la aplicación.

2. USO DE LA APLICACIÓN
TrainingNow es una plataforma de entrenamiento físico que permite crear y seguir rutinas de ejercicio, registrar tu progreso y comunicarte con entrenadores. El uso de la app es responsabilidad exclusiva del usuario.

3. DATOS PERSONALES
Los datos ingresados (nombre, apellidos, correo, teléfono y datos físicos) se almacenan de forma segura y se utilizan únicamente para el funcionamiento de la app: gestión de tu cuenta, seguimiento de rutinas y comunicación con tu entrenador. No se comparten con terceros sin tu consentimiento.

4. RESPONSABILIDAD SOBRE LA ACTIVIDAD FÍSICA
TrainingNow no reemplaza la evaluación de un profesional de la salud. Consulta a un médico antes de iniciar cualquier rutina de ejercicio, especialmente si tienes alguna condición médica preexistente.

5. CUENTA DE USUARIO
Eres responsable de mantener la confidencialidad de tu contraseña y de toda actividad realizada desde tu cuenta. Debes notificar cualquier uso no autorizado.

6. CONDUCTA DEL USUARIO
Está prohibido usar la app con fines fraudulentos, difundir contenido ofensivo en el chat o suplantar la identidad de otros usuarios o entrenadores.

7. MODIFICACIONES
TrainingNow puede actualizar estos términos en cualquier momento. El uso continuado de la app tras una actualización implica la aceptación de los nuevos términos.

8. CONTACTO
Para dudas sobre estos términos, puedes escribir al equipo de soporte a través de la app.
"""

/**
 * Carousel de bienvenida a pantalla completa, mostrado justo después de crear la cuenta
 * (con auto-login). 4 diapositivas: bienvenida, chat, sincronización de rutinas y,
 * como último paso, datos opcionales (fecha de nacimiento, altura, peso, foto).
 */
@Composable
private fun WelcomeOnboardingDialog(
    user: UserEntity,
    authViewModel: AuthViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = 5

    var birthDateStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var unidadesImperiales by remember { mutableStateOf(com.shagox.apptrainingnow.utils.UnitsPreference.esImperial(context)) }
    var avatarDataUri by remember { mutableStateOf<String?>(null) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarDataUri = com.shagox.apptrainingnow.utils.ImageCompressor.compressToDataUri(
                context = context,
                uri = it,
                maxDimension = 400,
                targetBytes = 60 * 1024
            )
        }
    }

    // OJO: antes esto mandaba updateUser() y updateProfilePhoto() como 2 guardados separados
    // al mismo tiempo (2 PUT concurrentes). Como cada uno pisa el usuario completo, el que
    // terminaba último ganaba, y a veces la foto se perdía porque el guardado de datos
    // terminaba después con una copia vieja (sin la foto nueva). Por eso ahora todo se manda
    // en UN solo updateUser(), y recién se cierra el carrusel cuando terminó de guardar
    // (con onDone), para que el perfil quede al día apenas se cierra.
    fun finalizar() {
        com.shagox.apptrainingnow.utils.UnitsPreference.guardar(context, unidadesImperiales)
        val nuevaFecha = parseBirthDateToMillis(birthDateStr)
        // La altura/peso se guardan SIEMPRE en cm/kg (formato canónico); si el usuario eligió
        // imperial, se convierte lo que escribió (in/lb) antes de guardar.
        val nuevaAltura = heightStr.toFloatOrNull()?.let {
            if (unidadesImperiales) com.shagox.apptrainingnow.utils.UnitsPreference.pulgadasACm(it.toDouble()).toFloat() else it
        }
        val nuevoPeso = weightStr.toFloatOrNull()?.let {
            if (unidadesImperiales) com.shagox.apptrainingnow.utils.UnitsPreference.librasAKg(it.toDouble()).toFloat() else it
        }
        val hayDatosNuevos = nuevaFecha != null || nuevaAltura != null || nuevoPeso != null ||
                gender != null || avatarDataUri != null
        if (hayDatosNuevos) {
            authViewModel.updateUser(
                user.copy(
                    birthDate = nuevaFecha ?: user.birthDate,
                    height = nuevaAltura ?: user.height,
                    weight = nuevoPeso ?: user.weight,
                    gender = gender ?: user.gender,
                    profilePhotoUrl = avatarDataUri ?: user.profilePhotoUrl
                ),
                onDone = onFinish
            )
        } else {
            onFinish()
        }
    }

    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NegroFondo)
        ) {
            // Botón cerrar (arriba a la derecha), disponible en todas las diapositivas
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clip(CircleShape)
                    .background(SuperficieElevada)
                    .clickable { onFinish() }
                    .padding(10.dp)
            ) {
                Text("✕", color = TextoPrincipal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(top = 80.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentPage) {
                        0 -> OnboardingWelcomePage(userName = user.name)
                        1 -> OnboardingChatPage()
                        2 -> OnboardingSyncPage()
                        3 -> OnboardingProfileDataPage(
                            birthDateStr = birthDateStr,
                            onBirthDateChange = { birthDateStr = it },
                            heightStr = heightStr,
                            onHeightChange = { heightStr = it },
                            weightStr = weightStr,
                            onWeightChange = { weightStr = it },
                            gender = gender,
                            onGenderChange = { gender = it },
                            unidadesImperiales = unidadesImperiales,
                            onUnidadesChange = { unidadesImperiales = it },
                            avatarDataUri = avatarDataUri,
                            onPickAvatar = { avatarPickerLauncher.launch("image/*") }
                        )
                        else -> OnboardingThanksPage()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Indicador de diapositivas
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(totalPages) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentPage) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (index == currentPage) VerdeTN else GrisBorde)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) currentPage++ else finalizar()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (currentPage < totalPages - 1) "SIGUIENTE" else "FINALIZAR",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomePage(userName: String) {
    Spacer(modifier = Modifier.height(40.dp))
    Text(text = ":D", fontSize = 96.sp, fontWeight = FontWeight.Bold, color = VerdeTN)
    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = "¡Muchas gracias por crear tu cuenta!",
        color = TextoPrincipal,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Bienvenido(a) a TrainingNow, ${userName.trim().ifBlank { "" }}. Estamos felices de que te unas a nosotros para entrenar mejor cada día.",
        color = GrisTexto,
        fontSize = 15.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun OnboardingChatPage() {
    Spacer(modifier = Modifier.height(40.dp))
    Icon(
        imageVector = androidx.compose.material.icons.Icons.Filled.Person,
        contentDescription = null,
        tint = VerdeTN,
        modifier = Modifier.size(72.dp)
    )
    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = "Chatea con tu entrenador",
        color = TextoPrincipal,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Desde la sección de Chat puedes escribirte directamente con tu entrenador, resolver dudas sobre tus ejercicios y recibir seguimiento personalizado.",
        color = GrisTexto,
        fontSize = 15.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))

    // Mini vista previa del chat
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, GrisBorde, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(SuperficieElevada)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("¡Hola! ¿Cómo vas con la rutina de hoy?", color = TextoPrincipal, fontSize = 13.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(VerdeTN)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("¡Muy bien! Ya llevo 3 ejercicios", color = TextoSobreVerde, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun OnboardingSyncPage() {
    Spacer(modifier = Modifier.height(40.dp))
    Icon(
        imageVector = androidx.compose.material.icons.Icons.Filled.CalendarToday,
        contentDescription = null,
        tint = VerdeTN,
        modifier = Modifier.size(72.dp)
    )
    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = "Tus rutinas, siempre contigo",
        color = TextoPrincipal,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Tus rutinas y tu progreso se guardan en tu cuenta para siempre. Si inicias sesión desde otro dispositivo, ahí estarán esperándote, sin perder nada.",
        color = GrisTexto,
        fontSize = 15.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))

    // Mini vista previa de una rutina
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VerdeTN.copy(alpha = 0.12f))
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text("LUNES · Pecho y Tríceps", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))
        listOf("Press banca 4x10", "Fondos en paralelas 3x12", "Extensión de tríceps 3x15").forEach { ejercicio ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(VerdeTN)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(ejercicio, color = TextoPrincipal, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun OnboardingThanksPage() {
    Spacer(modifier = Modifier.height(60.dp))
    Text(text = ";)", fontSize = 96.sp, fontWeight = FontWeight.Bold, color = VerdeTN)
    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = "Muchas gracias ;)",
        color = TextoPrincipal,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Todo listo. Ahora sí, a entrenar.",
        color = GrisTexto,
        fontSize = 15.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun OnboardingProfileDataPage(
    birthDateStr: String,
    onBirthDateChange: (String) -> Unit,
    heightStr: String,
    onHeightChange: (String) -> Unit,
    weightStr: String,
    onWeightChange: (String) -> Unit,
    gender: String?,
    onGenderChange: (String) -> Unit,
    unidadesImperiales: Boolean,
    onUnidadesChange: (Boolean) -> Unit,
    avatarDataUri: String?,
    onPickAvatar: () -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        cursorColor = VerdeTN,
        focusedLabelColor = VerdeTN,
        unfocusedLabelColor = GrisTexto,
        focusedContainerColor = GrisFondo,
        unfocusedContainerColor = GrisFondo
    )

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Un último paso (opcional)",
        color = TextoPrincipal,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Puedes completar estos datos ahora o hacerlo más tarde desde tu perfil.",
        color = GrisTexto,
        fontSize = 14.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(GrisFondo)
            .clickable { onPickAvatar() },
        contentAlignment = Alignment.Center
    ) {
        if (avatarDataUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(avatarDataUri).build(),
                contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Elegir foto",
                tint = GrisTexto,
                modifier = Modifier.size(32.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = birthDateStr,
        onValueChange = { onBirthDateChange(autoFormatearFecha(it)) },
        label = { Text("Fecha de nacimiento (dd/MM/yyyy)", color = GrisTexto) },
        placeholder = { Text("Ej: 05061998", color = GrisTexto) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = birthDateStr.length == 10 && parseBirthDateToMillis(birthDateStr) == null,
        supportingText = {
            if (birthDateStr.length == 10 && parseBirthDateToMillis(birthDateStr) == null) {
                Text("Fecha inválida", color = androidx.compose.ui.graphics.Color(0xFFE53935))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = textFieldColors,
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text("Unidades de medida", color = GrisTexto, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))
    UnitsSelector(imperial = unidadesImperiales, onCambiar = onUnidadesChange)
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = heightStr,
        onValueChange = onHeightChange,
        label = { Text(if (unidadesImperiales) "Altura (in)" else "Altura (cm)", color = GrisTexto) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = textFieldColors,
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = weightStr,
        onValueChange = onWeightChange,
        label = { Text(if (unidadesImperiales) "Peso (lb)" else "Peso (kg)", color = GrisTexto) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = textFieldColors,
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text("Género", color = GrisTexto, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))
    GenderSelector(seleccionado = gender, onSeleccionar = onGenderChange)
}


/**
 * Diálogo "Olvidé mi contraseña" en 3 pasos:
 * 1. Email → envía código (EmailJS vía TrainNow-Usuarios)
 * 2. Código de 6 dígitos recibido por correo
 * 3. Nueva contraseña
 */
@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit
) {
    val repository = remember { PasswordResetRepository() }
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) }
    var email by remember { mutableStateOf(initialEmail) }
    var code by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        cursorColor = VerdeTN,
        focusedLabelColor = VerdeTN,
        unfocusedLabelColor = GrisTexto,
        focusedContainerColor = GrisFondo,
        unfocusedContainerColor = GrisFondo
    )

    fun run(block: suspend () -> Result<Unit>, onOk: () -> Unit) {
        loading = true
        error = null
        scope.launch {
            block().fold(
                onSuccess = {
                    loading = false
                    onOk()
                },
                onFailure = {
                    loading = false
                    error = it.message
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor = GrisFondo,
        title = {
            Text(
                text = when {
                    success -> "¡Listo!"
                    step == 1 -> "Recuperar cuenta"
                    step == 2 -> "Revisa tu correo"
                    else -> "Nueva contraseña"
                },
                color = VerdeTN,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    success -> {
                        Text(
                            "Tu contraseña fue actualizada. Ya puedes iniciar sesión.",
                            color = TextoPrincipal
                        )
                    }
                    step == 1 -> {
                        Text(
                            "Te enviaremos un código de 6 dígitos a tu correo.",
                            color = GrisTexto, fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            label = { Text("Email") }, singleLine = true,
                            colors = fieldColors, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    step == 2 -> {
                        Text(
                            "Ingresa el código enviado a $email (expira en 10 minutos).",
                            color = GrisTexto, fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
                            label = { Text("Código de 6 dígitos") }, singleLine = true,
                            colors = fieldColors, modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = {
                            run({ repository.requestCode(email) }) { error = null }
                        }, enabled = !loading) {
                            Text("Reenviar código", color = VerdeTN, fontSize = 12.sp)
                        }
                    }
                    else -> {
                        var newPassVisible by remember { mutableStateOf(false) }
                        var confirmPassVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = newPass, onValueChange = { newPass = it },
                            label = { Text("Nueva contraseña") }, singleLine = true,
                            visualTransformation = if (newPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconoOjoContrasena(visible = newPassVisible, onToggle = { newPassVisible = !newPassVisible }, tint = GrisTexto)
                            },
                            colors = fieldColors, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPass, onValueChange = { confirmPass = it },
                            label = { Text("Confirmar contraseña") }, singleLine = true,
                            visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconoOjoContrasena(visible = confirmPassVisible, onToggle = { confirmPassVisible = !confirmPassVisible }, tint = GrisTexto)
                            },
                            colors = fieldColors, modifier = Modifier.fillMaxWidth()
                        )
                        if (confirmPass.isNotEmpty() && newPass != confirmPass) {
                            Text("Las contraseñas no coinciden",
                                color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
                        }
                    }
                }
                if (error != null) {
                    Text(error!!, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 13.sp)
                }
                if (loading) {
                    CircularProgressIndicator(color = VerdeTN, modifier = Modifier.size(24.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        success -> onDismiss()
                        step == 1 -> run({ repository.requestCode(email) }) { step = 2 }
                        step == 2 -> run({ repository.verifyCode(email, code) }) { step = 3 }
                        else -> run({ repository.confirmReset(email, code, newPass) }) { success = true }
                    }
                },
                enabled = !loading && when {
                    success -> true
                    step == 1 -> email.contains("@")
                    step == 2 -> code.length == 6
                    else -> newPass.length >= 6 && newPass == confirmPass
                },
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde)
            ) {
                Text(
                    when {
                        success -> "ENTENDIDO"
                        step == 1 -> "ENVIAR CÓDIGO"
                        step == 2 -> "VERIFICAR"
                        else -> "CAMBIAR CONTRASEÑA"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!success) {
                Button(
                    onClick = onDismiss,
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = GrisBorde, contentColor = TextoPrincipal)
                ) { Text("Cancelar") }
            }
        }
    )
}
