package com.shagox.apptrainingnow.ui.viewmodel

import com.shagox.apptrainingnow.data.local.trainer.TrainerStats
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.repository.INotificationRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios de CoachViewModel: creación de rutinas (individual y global),
 * gestión de solicitudes de clientes (aceptar/rechazar/pausar) y búsqueda.
 * Repositorios (clases concretas) mockeados con MockK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var trainerRepository: TrainerRepository
    private lateinit var progressRepository: ProgressRepository
    private lateinit var userRepository: IUserRepository
    private lateinit var notificationRepository: INotificationRepository
    private lateinit var viewModel: CoachViewModel

    private val trainerId = 2
    private val trainer = UserEntity(
        id = trainerId, role = "TRAINER", name = "Carlos", lastName = "Mendoza Silva",
        email = "entrenador@trainingnow.com", phone = "56900000000", password = "hash"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        trainerRepository = mockk(relaxed = true)
        progressRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        notificationRepository = mockk(relaxed = true)

        // Flows leídos en el constructor (stateIn con SharingStarted.Lazily): stub explícito
        // para que no queden en null si algún test decide observarlos.
        every { trainerRepository.getActiveClients(trainerId) } returns flowOf(emptyList())
        every { trainerRepository.getClientsByStatus(trainerId, any()) } returns flowOf(emptyList())
        every { trainerRepository.observePendingRequestCount(trainerId) } returns flowOf(0)
        every { trainerRepository.observeActiveClientCount(trainerId) } returns flowOf(0)
        every { trainerRepository.getMyCreatedRoutines(trainerId) } returns flowOf(emptyList())
        coEvery { trainerRepository.getTrainerStats(trainerId) } returns TrainerStats(0, 0, 0, 0, 0)
        coEvery { trainerRepository.countMyRoutines(trainerId) } returns 0

        viewModel = CoachViewModel(trainerRepository, progressRepository, userRepository, trainerId, notificationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== CREACIÓN DE RUTINAS ====================

    @Test
    fun createRoutineForClient_exitosa_actualizaEstadoYNotificaCliente() = runTest(dispatcher) {
        coEvery { trainerRepository.createRoutineForClient(trainerId, 3, "Full Body", "Lunes", listOf(1, 2), null) } returns Result.success(10L)
        coEvery { userRepository.getUserById(trainerId) } returns trainer

        viewModel.createRoutineForClient(clientId = 3, name = "Full Body", dayInfo = "Lunes", exerciseIds = listOf(1, 2))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Rutina creada exitosamente", state.successMessage)
        assertNull(state.error)
        // La notificación al cliente corre en un withContext(Dispatchers.IO) aparte: se
        // espera con timeout en vez de confiar solo en advanceUntilIdle().
        coVerify(timeout = 2000) { notificationRepository.saveNotification(match { it.userId == 3 && it.type == "ROUTINE_ASSIGNED" }) }
    }

    @Test
    fun createRoutineForClient_fallo_muestraErrorYNoNotifica() = runTest(dispatcher) {
        coEvery { trainerRepository.createRoutineForClient(trainerId, 3, any(), any(), any(), any()) } returns
                Result.failure(Exception("No se pudo guardar la rutina"))

        viewModel.createRoutineForClient(clientId = 3, name = "Full Body", dayInfo = "Lunes", exerciseIds = listOf(1))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("No se pudo guardar la rutina", state.error)
        assertNull(state.successMessage)
        coVerify(exactly = 0) { notificationRepository.saveNotification(any()) }
    }

    @Test
    fun createGlobalRoutine_exitosa_actualizaEstado() = runTest(dispatcher) {
        coEvery { trainerRepository.createGlobalRoutine(trainerId, "Hipertrofia", "Lunes a Viernes", listOf(1, 2, 3)) } returns Result.success(20L)

        viewModel.createGlobalRoutine(name = "Hipertrofia", dayInfo = "Lunes a Viernes", exerciseIds = listOf(1, 2, 3))
        advanceUntilIdle()

        assertEquals("Rutina global creada", viewModel.uiState.value.successMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun createGlobalRoutine_fallo_muestraError() = runTest(dispatcher) {
        coEvery { trainerRepository.createGlobalRoutine(trainerId, any(), any(), any()) } returns
                Result.failure(Exception("Error de base de datos"))

        viewModel.createGlobalRoutine(name = "Hipertrofia", dayInfo = "Lunes", exerciseIds = emptyList())
        advanceUntilIdle()

        assertEquals("Error de base de datos", viewModel.uiState.value.error)
    }

    @Test
    fun deleteRoutine_exitosa_muestraMensaje() = runTest(dispatcher) {
        coEvery { trainerRepository.deleteRoutine(10) } returns Result.success(Unit)

        viewModel.deleteRoutine(10)
        advanceUntilIdle()

        assertEquals("Rutina eliminada", viewModel.uiState.value.successMessage)
    }

    // ==================== SOLICITUDES DE CLIENTES ====================

    @Test
    fun acceptClientRequest_exitosa_muestraMensajeYRecargaStats() = runTest(dispatcher) {
        coEvery { trainerRepository.acceptClientRequest(trainerId, 5) } returns Result.success(Unit)

        viewModel.acceptClientRequest(5)
        advanceUntilIdle()

        assertEquals("Cliente aceptado", viewModel.uiState.value.successMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(atLeast = 2) { trainerRepository.getTrainerStats(trainerId) } // 1 en init + 1 al aceptar
    }

    @Test
    fun rejectClientRequest_exitosa_muestraMensaje() = runTest(dispatcher) {
        coEvery { trainerRepository.rejectClientRequest(trainerId, 5) } returns Result.success(Unit)

        viewModel.rejectClientRequest(5)
        advanceUntilIdle()

        assertEquals("Solicitud rechazada", viewModel.uiState.value.successMessage)
    }

    @Test
    fun pauseClient_exitosa_muestraMensaje() = runTest(dispatcher) {
        coEvery { trainerRepository.pauseClient(trainerId, 5) } returns Result.success(Unit)

        viewModel.pauseClient(5)
        advanceUntilIdle()

        assertEquals("Cliente pausado", viewModel.uiState.value.successMessage)
    }

    @Test
    fun acceptClientRequest_fallo_muestraError() = runTest(dispatcher) {
        coEvery { trainerRepository.acceptClientRequest(trainerId, 5) } returns
                Result.failure(Exception("Cliente no encontrado"))

        viewModel.acceptClientRequest(5)
        advanceUntilIdle()

        assertEquals("Cliente no encontrado", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.successMessage)
    }

    // ==================== BÚSQUEDA ====================

    @Test
    fun searchClients_conQueryVacio_noConsultaRepositorioYLimpiaResultados() = runTest(dispatcher) {
        viewModel.searchClients("")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
        coVerify(exactly = 0) { trainerRepository.searchClients(any(), any()) }
    }

    @Test
    fun searchClients_conQuery_devuelveResultados() = runTest(dispatcher) {
        val resultados = listOf(
            UserEntity(id = 3, role = "USER", name = "Santiago", lastName = "Vargas Reyes",
                email = "usuario@gmail.com", phone = "56911111111", password = "hash")
        )
        coEvery { trainerRepository.searchClients(trainerId, "santiago") } returns resultados

        viewModel.searchClients("santiago")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.searchResults.size)
        assertEquals("Santiago", state.searchResults[0].name)
        assertFalse(state.isSearching)
    }
}
