package com.shagox.apptrainingnow.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.R
import com.shagox.apptrainingnow.data.repository.PasswordResetRepository
import kotlinx.coroutines.launch
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
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
 * Si el usuario está logueado, muestra sus datos y el botón Cerrar sesión.
 */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    onVerAvanceMensual: () -> Unit = {}
) {
    val loginState by authViewModel.loginState.collectAsState()
    val registerState by authViewModel.register.collectAsState()
    val loggedUser = loginState.loggedUser
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Iniciar sesión, 1 = Registrarse
    var showEditProfile by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

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
                onEdit = { showEditProfile = true },
                onLogout = { authViewModel.logout() }
            )

            // Acceso al reporte mensual de entrenamiento
            Button(
                onClick = onVerAvanceMensual,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrisFondo,
                    contentColor = VerdeTN
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = VerdeTN,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("MI AVANCE MENSUAL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

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
                    painter = painterResource(R.drawable.logo_2),
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
                    onEmailChange = { authViewModel.onRegisterEmailChange(it) },
                    onPhoneChange = { authViewModel.onPhoneChange(it) },
                    onPassChange = { authViewModel.onRegisterPassChange(it) },
                    onConfirmChange = { authViewModel.onConfirmChange(it) },
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

private fun formatGender(gender: String?): String {
    if (gender.isNullOrBlank()) return "No registrado"
    return when (gender.uppercase()) {
        "M" -> "Masculino"
        "F" -> "Femenino"
        "MALE" -> "Masculino"
        "FEMALE" -> "Femenino"
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
                color = androidx.compose.ui.graphics.Color.White,
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
    onEdit: () -> Unit,
    onLogout: () -> Unit
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
            title = { Text("Foto de perfil", color = androidx.compose.ui.graphics.Color.White) },
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
                            tint = NegroFondo,
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
            value = user.email
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
            value = user.height?.let { "${it.toInt()} cm" } ?: "No registrado"
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileDataRow(
            icon = Icons.Filled.Person,
            label = "Peso",
            value = user.weight?.let { "${it.toInt()} kg" } ?: "No registrado"
        )

        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE53935)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("CERRAR SESIÓN", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold)
        }
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
    if (str.isNullOrBlank()) return null
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(str.trim())?.time
    } catch (_: Exception) { null }
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
    var gender by remember(user) { mutableStateOf(user.gender ?: "") }
    var heightStr by remember(user) { mutableStateOf(user.height?.toString() ?: "") }
    var weightStr by remember(user) { mutableStateOf(user.weight?.toString() ?: "") }
    var specializations by remember(user) { mutableStateOf(user.specializations ?: "") }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = androidx.compose.ui.graphics.Color.White,
        unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
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
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = birthDateStr, onValueChange = { birthDateStr = it }, label = { Text("Fecha nacimiento (dd/MM/yyyy)", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Género (M/F)", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = heightStr, onValueChange = { heightStr = it }, label = { Text("Altura (cm)", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = weightStr, onValueChange = { weightStr = it }, label = { Text("Peso (kg)", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            if (user.role == "TRAINER") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = specializations, onValueChange = { specializations = it }, label = { Text("Especializaciones", color = GrisTexto) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = RoundedCornerShape(12.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GrisFondo),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar", color = androidx.compose.ui.graphics.Color.White) }
                Button(
                    onClick = {
                        val updated = user.copy(
                            name = name.trim(),
                            lastName = lastName.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            birthDate = parseBirthDateToMillis(birthDateStr),
                            gender = gender.trim().takeIf { it.isNotBlank() },
                            height = heightStr.toFloatOrNull(),
                            weight = weightStr.toFloatOrNull(),
                            specializations = specializations.trim().takeIf { it.isNotBlank() }
                        )
                        onSave(updated)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Guardar", color = NegroFondo) }
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
        // Logo arriba (logo_2, sin texto)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logo_2),
                contentDescription = "Logo Training NOW!",
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(100.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = loginState.email,
            onValueChange = onEmailChange,
            label = { Text("Correo Electrónico", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = androidx.compose.ui.graphics.Color.White,
                unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
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

        OutlinedTextField(
            value = loginState.pass,
            onValueChange = onPassChange,
            label = { Text("Contraseña", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = androidx.compose.ui.graphics.Color.White,
                unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
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
            colors = ButtonDefaults.buttonColors(containerColor = GrisFondo, contentColor = androidx.compose.ui.graphics.Color.White),
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
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = androidx.compose.ui.graphics.Color.White,
        unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
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
        // Logo arriba (logo_2, sin texto)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logo_2),
                contentDescription = "Logo Training NOW!",
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(100.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = registerState.name,
            onValueChange = onNameChange,
            label = { Text("Nombre", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.nameError != null) Text(registerState.nameError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
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
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.phoneError != null) Text(registerState.phoneError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = registerState.pass,
            onValueChange = onPassChange,
            label = { Text("Contraseña", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.passError != null) Text(registerState.passError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = registerState.confirm,
            onValueChange = onConfirmChange,
            label = { Text("Confirmar contraseña", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.confirmError != null) Text(registerState.confirmError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = registerState.canSubmit && !registerState.isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = NegroFondo),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (registerState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), color = NegroFondo)
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
        focusedTextColor = androidx.compose.ui.graphics.Color.White,
        unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
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
                            color = androidx.compose.ui.graphics.Color.White
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
                        OutlinedTextField(
                            value = newPass, onValueChange = { newPass = it },
                            label = { Text("Nueva contraseña") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = fieldColors, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPass, onValueChange = { confirmPass = it },
                            label = { Text("Confirmar contraseña") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
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
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = NegroFondo)
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
                    colors = ButtonDefaults.buttonColors(containerColor = GrisBorde, contentColor = androidx.compose.ui.graphics.Color.White)
                ) { Text("Cancelar") }
            }
        }
    )
}
