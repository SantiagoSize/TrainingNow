package com.shagox.apptrainingnow.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shagox.apptrainingnow.data.local.user.SessionManager
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.domain.validation.validateConfirm
import com.shagox.apptrainingnow.domain.validation.validateEmail
import com.shagox.apptrainingnow.domain.validation.validateNameLettersOnly
import com.shagox.apptrainingnow.domain.validation.validatePhoneDigitsOnly
import com.shagox.apptrainingnow.domain.validation.validateStringPassword
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// -------------------- LOGIN UI STATE --------------------
data class LoginUiState(
    val email: String = "",
    val pass: String = "",
    val emailError: String? = null,
    val errorMsg: String? = null,
    val isSubmitting: Boolean = false,
    val canSubmit: Boolean = false,
    val success: Boolean = false,
    val loggedUser: UserEntity? = null // Usuario logueado
)

// -------------------- REGISTER UI STATE --------------------
data class RegisterUiState(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val pass: String = "",
    val confirm: String = "",
    val termsAccepted: Boolean = false,
    val nameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passError: String? = null,
    val confirmError: String? = null,
    val errorMsg: String? = null,
    val isSubmitting: Boolean = false,
    val canSubmit: Boolean = false,
    val success: Boolean = false
)

// -------------------- AUTH VIEWMODEL --------------------
/**
 * ViewModel para gestionar autenticación de usuarios.
 * 
 * Maneja:
 * - Login de usuarios
 * - Registro de nuevos usuarios
 * - Estado del usuario logueado
 * - Validación de formularios
 */
class AuthViewModel(
    private val repository: IUserRepository,
    private val context: Context
) : ViewModel() {

    // ---------- LOGIN ----------
    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    /** Estado del login para acceso desde navegación */
    val loginState: StateFlow<LoginUiState> = _login.asStateFlow()

    /**
     * Restaura la sesión guardada (si había una) apenas se crea el ViewModel, para que la
     * app no muestre el login de nuevo solo porque se cerró/mató el proceso. Se restaura
     * primero desde la caché local (instantáneo) y después se refresca contra el backend
     * por si los datos cambiaron mientras tanto (ej. una sanción aplicada); si no hay
     * conexión, se queda con la copia en caché sin romper nada.
     */
    init {
        val usuarioGuardado = SessionManager.cargarUsuario(context)
        if (usuarioGuardado != null) {
            _login.update { it.copy(loggedUser = usuarioGuardado, success = true) }
            viewModelScope.launch {
                try {
                    val actualizado = repository.getUserById(usuarioGuardado.id)
                    if (actualizado != null) {
                        _login.update { it.copy(loggedUser = actualizado) }
                        SessionManager.actualizarUsuario(context, actualizado)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AuthViewModel", "No se pudo refrescar la sesión guardada: ${e.message}")
                }
            }
        }
    }

    // ---------- REGISTER ----------
    private val _register = MutableStateFlow(RegisterUiState())
    val register: StateFlow<RegisterUiState> = _register.asStateFlow()

    /** Se pone en true justo después de un registro exitoso (con auto-login), para
     *  disparar el carousel de bienvenida una sola vez. Se limpia con [consumeJustRegistered]. */
    private val _justRegistered = MutableStateFlow(false)
    val justRegistered: StateFlow<Boolean> = _justRegistered.asStateFlow()

    fun consumeJustRegistered() {
        _justRegistered.value = false
    }

    /** Usuario actualmente logueado (null si no hay sesión) */
    val currentUser: UserEntity?
        get() = _login.value.loggedUser

    // ================= LOGIN LOGIC =================

    fun onLoginEmailChange(value: String) {
        val trimmedValue = value.trim()
        _login.update {
            it.copy(
                email = trimmedValue,
                emailError = validateEmail(trimmedValue)
            )
        }
        recomputeLoginCanSubmit()
    }

    fun onLoginPassChange(value: String) {
        val trimmedValue = value.trim()
        _login.update { it.copy(pass = trimmedValue) }
        recomputeLoginCanSubmit()
    }

    private fun recomputeLoginCanSubmit() {
        val s = _login.value
        val canSubmit =
            s.email.isNotBlank() &&
                    s.pass.isNotBlank() &&
                    s.emailError == null

        _login.update { it.copy(canSubmit = canSubmit) }
    }

    fun submitLogin() {
        val s = _login.value
        if (!s.canSubmit || s.isSubmitting) return

        viewModelScope.launch {
            _login.update { it.copy(isSubmitting = true, errorMsg = null) }

            delay(1200) // fake loading

            // Aplicar trim a ambos campos antes de hacer login
            val trimmedEmail = s.email.trim()
            val trimmedPass = s.pass.trim()

            val result = repository.login(
                trimmedEmail,
                trimmedPass
            )

            _login.update {
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    // Sesión persistida: sobrevive a cerrar la app o apagar el teléfono.
                    if (user != null) SessionManager.guardar(context, RemoteModule.authToken, user)
                    it.copy(
                        isSubmitting = false,
                        success = true,
                        loggedUser = user // Guardar el usuario logueado
                    )
                } else {
                    it.copy(
                        isSubmitting = false,
                        // Mensaje real del backend (baneo/suspensión) o genérico
                        errorMsg = result.exceptionOrNull()?.message ?: "Credenciales incorrectas"
                    )
                }
            }
        }
    }
    
    /**
     * Cierra la sesión del usuario actual.
     *
     * También reinicia el modo invitado a un estado limpio (sin los entrenamientos
     * personalizados que se acaban de transferir a la cuenta, ver [GuestSession.migrarRutinasA])
     * ANTES de marcar la sesión como cerrada, para que no haya una ventana en la que la
     * pantalla ya está en modo invitado pero todavía viendo al invitado viejo.
     */
    fun logout() {
        RemoteModule.authToken = null
        SessionManager.limpiar(context)
        viewModelScope.launch {
            com.shagox.apptrainingnow.data.local.user.GuestSession.reiniciar(context)
            _login.update {
                LoginUiState() // Reset completo del estado
            }
        }
    }
    
    /**
     * Verifica si hay un usuario logueado.
     */
    fun isLoggedIn(): Boolean = _login.value.loggedUser != null

    /**
     * Elimina permanentemente la cuenta del usuario logueado, previa verificación de contraseña
     * (se reintenta el login con la contraseña ingresada como forma de confirmar identidad).
     * Si todo sale bien, cierra la sesión y notifica el resultado por [onResult].
     */
    fun deleteAccount(password: String, onResult: (success: Boolean, error: String?) -> Unit) {
        val user = _login.value.loggedUser
        if (user == null) {
            onResult(false, "No hay una sesión activa")
            return
        }
        viewModelScope.launch {
            val verificacion = repository.login(user.email, password.trim())
            if (verificacion.isFailure) {
                onResult(false, "Contraseña incorrecta")
                return@launch
            }
            try {
                repository.deleteUserById(user.id)
                logout()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message ?: "No se pudo eliminar la cuenta")
            }
        }
    }

    /**
     * Actualiza la foto de perfil del usuario en la BD y refresca el usuario logueado.
     * Blindado con try/catch: si falla la llamada al backend (ej. sin conexión, servidor
     * caído), antes esto tumbaba toda la app con un crash silencioso. Ahora solo se
     * ignora el error y el usuario puede reintentar más tarde desde su perfil.
     */
    fun updateProfilePhoto(userId: Int, photoUrl: String?) {
        viewModelScope.launch {
            try {
                val updated = repository.updateProfilePhoto(userId, photoUrl)
                if (updated != null) {
                    _login.update { it.copy(loggedUser = updated) }
                    SessionManager.actualizarUsuario(context, updated)
                }
            } catch (e: Exception) {
                android.util.Log.w("AuthViewModel", "No se pudo actualizar la foto de perfil: ${e.message}")
            }
        }
    }

    /**
     * Actualiza los datos del usuario en la BD y refresca el usuario logueado.
     * Mismo blindaje que [updateProfilePhoto]: una falla de red aquí ya no debe tumbar la app.
     *
     * [onDone] se llama SIEMPRE al terminar (haya ido bien o mal), para que quien llama pueda
     * esperar a que el guardado termine antes de seguir (ej. cerrar el carrusel de bienvenida
     * recién después de guardar, no antes).
     *
     * Después de guardar, se vuelve a pedir el usuario al backend en vez de confiar en el
     * objeto [user] que se mandó a guardar: así el perfil queda reflejando EXACTAMENTE lo que
     * quedó guardado en el servidor, no una copia local que podría haber quedado desactualizada.
     */
    fun updateUser(user: UserEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateUser(user)
                val actualizado = repository.getUserById(user.id) ?: user
                _login.update { it.copy(loggedUser = actualizado) }
                SessionManager.actualizarUsuario(context, actualizado)
            } catch (e: Exception) {
                android.util.Log.w("AuthViewModel", "No se pudo actualizar el usuario: ${e.message}")
            } finally {
                onDone()
            }
        }
    }

    // ================= REGISTER LOGIC =================

    /**
     * Límite máximo de caracteres (tope) de cada campo del registro.
     * Cada número se eligió según el tipo de dato: un nombre real no necesita más de 40
     * caracteres, un teléfono con código de país no pasa de 16, etc. Sirven para que el
     * usuario no pueda escribir textos absurdamente largos.
     */
    companion object {
        private const val MAX_NAME_LENGTH = 40
        private const val MAX_EMAIL_LENGTH = 60
        private const val MAX_PHONE_LENGTH = 16
        private const val MAX_PASSWORD_LENGTH = 40
    }

    /**
     * Saneo: ESPACIOS DOBLES → UN ESPACIO.
     * Qué hace: si el usuario escribe 2 o más espacios seguidos, los deja en 1 solo.
     * Ojo: a propósito NO usa trim() aquí, porque trim() borra el espacio final apenas
     * se escribe y no dejaría escribir un nombre compuesto como "Juan Carlos" (el espacio
     * desaparecía antes de poder escribir la siguiente palabra). El trim() final se hace
     * recién al enviar el formulario, en [submitRegister].
     */
    private fun colapsarEspacios(value: String): String = value.replace(Regex(" {2,}"), " ")

    /** Campo NOMBRES: colapsa espacios dobles + tope de [MAX_NAME_LENGTH] caracteres. */
    fun onNameChange(value: String) {
        val limpio = colapsarEspacios(value).take(MAX_NAME_LENGTH)
        _register.update {
            it.copy(
                name = limpio,
                nameError = validateNameLettersOnly(limpio.trim())
            )
        }
        recomputeRegisterCanSubmit()
    }

    /** Campo APELLIDOS: mismo saneo que Nombres (espacios colapsados + tope de caracteres). */
    fun onLastNameChange(value: String) {
        val limpio = colapsarEspacios(value).take(MAX_NAME_LENGTH)
        _register.update {
            it.copy(
                lastName = limpio,
                lastNameError = validateNameLettersOnly(limpio.trim())
            )
        }
        recomputeRegisterCanSubmit()
    }

    fun onTermsAcceptedChange(value: Boolean) {
        _register.update { it.copy(termsAccepted = value) }
        recomputeRegisterCanSubmit()
    }

    /** Campo CORREO: quita todos los espacios (un email nunca lleva) + tope de [MAX_EMAIL_LENGTH]. */
    fun onRegisterEmailChange(value: String) {
        val trimmedValue = value.replace(" ", "").take(MAX_EMAIL_LENGTH)
        _register.update {
            it.copy(
                email = trimmedValue,
                emailError = validateEmail(trimmedValue)
            )
        }
        recomputeRegisterCanSubmit()
    }

    /**
     * Campo TELÉFONO: deja pasar solo dígitos (filtra "+", espacios y cualquier otro
     * símbolo apenas se escriben) + tope de [MAX_PHONE_LENGTH] (código de país + número).
     */
    fun onPhoneChange(value: String) {
        val trimmedValue = value.filter { it.isDigit() }.take(MAX_PHONE_LENGTH)
        _register.update {
            it.copy(
                phone = trimmedValue,
                phoneError = validatePhoneDigitsOnly(trimmedValue)
            )
        }
        recomputeRegisterCanSubmit()
    }

    /**
     * Campo CONTRASEÑA: tope de [MAX_PASSWORD_LENGTH] caracteres.
     * A propósito NO se le hace trim ni se le tocan los espacios mientras se escribe,
     * para permitir espacios internos si el usuario los quiere en su contraseña.
     */
    fun onRegisterPassChange(value: String) {
        val limitado = value.take(MAX_PASSWORD_LENGTH)
        _register.update {
            it.copy(
                pass = limitado,
                passError = validateStringPassword(limitado)
            )
        }
        recomputeRegisterCanSubmit()
    }

    /** Campo CONFIRMAR CONTRASEÑA: mismo tope que Contraseña, sin tocar espacios. */
    fun onConfirmChange(value: String) {
        val limitado = value.take(MAX_PASSWORD_LENGTH)
        val pass = _register.value.pass
        _register.update {
            it.copy(
                confirm = limitado,
                confirmError = validateConfirm(pass, limitado)
            )
        }
        recomputeRegisterCanSubmit()
    }

    private fun recomputeRegisterCanSubmit() {
        val s = _register.value

        val noErrors = listOf(
            s.nameError,
            s.lastNameError,
            s.emailError,
            s.phoneError,
            s.passError,
            s.confirmError
        ).all { it == null }

        val filled =
            s.name.isNotBlank() &&
                    s.lastName.isNotBlank() &&
                    s.email.isNotBlank() &&
                    s.pass.isNotBlank()

        _register.update {
            it.copy(canSubmit = noErrors && filled && s.termsAccepted)
        }
    }

    fun submitRegister() {
        val s = _register.value
        if (!s.canSubmit || s.isSubmitting) return

        viewModelScope.launch {
            _register.update {
                it.copy(
                    isSubmitting = true,
                    errorMsg = null
                )
            }

            // Aplicar trim a todos los campos antes de guardar
            val trimmedName = s.name.trim()
            val trimmedLastName = s.lastName.trim()
            val trimmedEmail = s.email.trim()
            val trimmedPhone = s.phone.trim()
            val trimmedPass = s.pass.trim()

            if (trimmedEmail.lowercase().endsWith("@trainingnow.com")) {
                _register.update {
                    it.copy(
                        isSubmitting = false,
                        errorMsg = "El dominio @trainingnow.com es exclusivo del personal. Usa tu correo personal."
                    )
                }
                return@launch
            }

            val role = repository.determineRoleByEmail(trimmedEmail)

            val newUser = UserEntity(
                role = role,
                name = trimmedName,
                lastName = trimmedLastName,
                email = trimmedEmail,
                phone = trimmedPhone,
                password = trimmedPass
            )

            try {
                repository.insertUser(newUser)
                _register.update {
                    it.copy(
                        isSubmitting = false,
                        success = true
                    )
                }
                // Auto-login para continuar directo a la bienvenida (carousel) sin pedirle
                // de nuevo las credenciales que recién escribió.
                val loginResult = repository.login(trimmedEmail, trimmedPass)
                val loggedInUser = loginResult.getOrNull()
                if (loggedInUser != null) {
                    _login.update { it.copy(loggedUser = loggedInUser, success = true) }
                    SessionManager.guardar(context, RemoteModule.authToken, loggedInUser)
                    _justRegistered.value = true
                }
            } catch (e: Exception) {
                _register.update {
                    it.copy(
                        isSubmitting = false,
                        errorMsg = e.message
                    )
                }
            }
        }
    }
}
