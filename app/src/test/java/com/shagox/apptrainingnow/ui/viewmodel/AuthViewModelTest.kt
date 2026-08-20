package com.shagox.apptrainingnow.ui.viewmodel

import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests unitarios de AuthViewModel con repositorio mockeado (MockK).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IUserRepository
    private lateinit var viewModel: AuthViewModel

    private val usuario = UserEntity(
        id = 3, role = "USER", name = "Santiago", lastName = "Usuario",
        email = "usuario@gmail.com", phone = "912345678", password = "hash"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk(relaxed = true)
        viewModel = AuthViewModel(repository, RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun completarLogin(email: String = "usuario@gmail.com", pass: String = "Entrena2026") {
        viewModel.onLoginEmailChange(email)
        viewModel.onLoginPassChange(pass)
    }

    // ==================== LOGIN ====================

    @Test
    fun loginExitoso_guardaUsuarioLogueado() = runTest(dispatcher) {
        coEvery { repository.login(any(), any()) } returns Result.success(usuario)

        completarLogin()
        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.loginState.value
        assertTrue(state.success)
        assertEquals("Santiago", state.loggedUser?.name)
        assertNull(state.errorMsg)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun loginFallido_muestraMensajeDelBackend() = runTest(dispatcher) {
        // El backend responde con el motivo del bloqueo (baneo/suspensión)
        coEvery { repository.login(any(), any()) } returns
                Result.failure(Exception("Tu cuenta fue baneada permanentemente. Motivo: spam"))

        completarLogin()
        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.loginState.value
        assertFalse(state.success)
        assertNull(state.loggedUser)
        assertEquals("Tu cuenta fue baneada permanentemente. Motivo: spam", state.errorMsg)
    }

    @Test
    fun loginSinCampos_noEjecuta() = runTest(dispatcher) {
        viewModel.submitLogin()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun logout_limpiaSesion() = runTest(dispatcher) {
        coEvery { repository.login(any(), any()) } returns Result.success(usuario)
        completarLogin()
        viewModel.submitLogin()
        advanceUntilIdle()
        assertTrue(viewModel.isLoggedIn())

        viewModel.logout()
        advanceUntilIdle()

        assertFalse(viewModel.isLoggedIn())
        assertNull(viewModel.loginState.value.loggedUser)
    }

    // ==================== REGISTRO ====================

    private fun completarRegistro(email: String) {
        viewModel.onNameChange("Santiago")
        viewModel.onLastNameChange("Usuario")
        viewModel.onRegisterEmailChange(email)
        viewModel.onPhoneChange("56912345678")
        viewModel.onRegisterPassChange("Entrena2026")
        viewModel.onConfirmChange("Entrena2026")
        viewModel.onTermsAcceptedChange(true)
    }

    @Test
    fun registroValido_insertaUsuario() = runTest(dispatcher) {
        every { repository.determineRoleByEmail(any()) } returns "USER"
        completarRegistro("nuevo@gmail.com")
        viewModel.submitRegister()
        advanceUntilIdle()

        assertTrue(viewModel.register.value.success)
        coVerify(exactly = 1) { repository.insertUser(any()) }
    }

    @Test
    fun registroConDominioCorporativo_bloqueado() = runTest(dispatcher) {
        // El dominio @trainingnow.com no está en la lista de dominios permitidos para el
        // registro público (ver validateEmailRegistro), así que queda bloqueado como error
        // de campo antes de poder enviar el formulario (canSubmit=false), sin llegar a
        // invocar al repositorio ni al chequeo interno de submitRegister().
        completarRegistro("falso@trainingnow.com")
        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.register.value
        assertFalse(state.success)
        assertNotNull(state.emailError)
        assertFalse(state.canSubmit)
        coVerify(exactly = 0) { repository.insertUser(any()) }
    }

    @Test
    fun registroConEmailDuplicado_muestraError() = runTest(dispatcher) {
        every { repository.determineRoleByEmail(any()) } returns "USER"
        coEvery { repository.insertUser(any()) } throws Exception("El email ya existe")

        completarRegistro("dup@gmail.com")
        viewModel.submitRegister()
        advanceUntilIdle()

        assertEquals("El email ya existe", viewModel.register.value.errorMsg)
        assertFalse(viewModel.register.value.success)
    }
}
