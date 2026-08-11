package com.shagox.apptrainingnow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shagox.apptrainingnow.data.local.user.UserEntity
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
    private val repository: IUserRepository
) : ViewModel() {

    // ---------- LOGIN ----------
    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()
    
    /** Estado del login para acceso desde navegación */
    val loginState: StateFlow<LoginUiState> = _login.asStateFlow()

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
     */
    fun logout() {
        com.shagox.apptrainingnow.data.remote.RemoteModule.authToken = null
        _login.update { 
            LoginUiState() // Reset completo del estado
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
     */
    fun updateProfilePhoto(userId: Int, photoUrl: String?) {
        viewModelScope.launch {
            val updated = repository.updateProfilePhoto(userId, photoUrl)
            if (updated != null) {
                _login.update { it.copy(loggedUser = updated) }
            }
        }
    }

    /**
     * Actualiza los datos del usuario en la BD y refresca el usuario logueado.
     */
    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
            _login.update { it.copy(loggedUser = user) }
        }
    }

    // ================= REGISTER LOGIC =================

    fun onNameChange(value: String) {
        val trimmedValue = value.trim()
        _register.update {
            it.copy(
                name = trimmedValue,
                nameError = validateNameLettersOnly(trimmedValue)
            )
        }
        recomputeRegisterCanSubmit()
    }

    fun onLastNameChange(value: String) {
        val trimmedValue = value.trim()
        _register.update {
            it.copy(
                lastName = trimmedValue,
                lastNameError = validateNameLettersOnly(trimmedValue)
            )
        }
        recomputeRegisterCanSubmit()
    }

    fun onTermsAcceptedChange(value: Boolean) {
        _register.update { it.copy(termsAccepted = value) }
        recomputeRegisterCanSubmit()
    }

    fun onRegisterEmailChange(value: String) {
        val trimmedValue = value.trim()
        _register.update {
            it.copy(
                email = trimmedValue,
                emailError = validateEmail(trimmedValue)
            )
        }
        recomputeRegisterCanSubmit()
    }

    fun onPhoneChange(value: String) {
        val trimmedValue = value.trim()
        _register.update {
            it.copy(
                phone = trimmedValue,
                phoneError = validatePhoneDigitsOnly(trimmedValue)
            )
        }
        recomputeRegisterCanSubmit()
    }

    fun onRegisterPassChange(value: String) {
        // No hacer trim a la contraseña mientras se escribe (para permitir espacios si el usuario los quiere)
        _register.update {
            it.copy(
                pass = value,
                passError = validateStringPassword(value)
            )
        }
        recomputeRegisterCanSubmit()
    }

    fun onConfirmChange(value: String) {
        // No hacer trim a la confirmación mientras se escribe
        val pass = _register.value.pass
        _register.update {
            it.copy(
                confirm = value,
                confirmError = validateConfirm(pass, value)
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
