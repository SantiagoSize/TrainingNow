package com.shagox.apptrainingnow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shagox.apptrainingnow.data.local.progress.GoalCategory
import com.shagox.apptrainingnow.data.local.progress.GoalEntity
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.local.trainer.TrainerClientStatus
import com.shagox.apptrainingnow.data.local.trainer.TrainerStats
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.local.notification.NotificationAction
import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import com.shagox.apptrainingnow.data.local.notification.NotificationType
import com.shagox.apptrainingnow.data.repository.INotificationRepository
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel para la interfaz del Entrenador/Coach.
 * 
 * Gestiona:
 * - Lista y búsqueda de clientes
 * - Estado de solicitudes
 * - Creación de rutinas
 * - Asignación de objetivos
 * - Estadísticas del entrenador
 */
class CoachViewModel(
    private val trainerRepository: TrainerRepository,
    private val progressRepository: ProgressRepository,
    private val userRepository: IUserRepository,
    private val trainerId: Int,
    private val notificationRepository: INotificationRepository? = null
) : ViewModel() {

    // ==================== UI STATE ====================

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    private val _selectedClient = MutableStateFlow<UserEntity?>(null)
    val selectedClient: StateFlow<UserEntity?> = _selectedClient.asStateFlow()

    // ==================== FLOWS ====================

    /** Lista de clientes activos */
    val activeClients: StateFlow<List<UserEntity>> = trainerRepository
        .getActiveClients(trainerId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Lista de clientes pendientes */
    val pendingClients: StateFlow<List<UserEntity>> = trainerRepository
        .getClientsByStatus(trainerId, TrainerClientStatus.PENDING)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Conteo de solicitudes pendientes */
    val pendingRequestCount: StateFlow<Int> = trainerRepository
        .observePendingRequestCount(trainerId)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /** Conteo de clientes activos */
    val activeClientCount: StateFlow<Int> = trainerRepository
        .observeActiveClientCount(trainerId)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /** Rutinas creadas por el entrenador */
    val myRoutines: StateFlow<List<RoutineEntity>> = trainerRepository
        .getMyCreatedRoutines(trainerId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadTrainerStats()
    }

    // ==================== GESTIÓN DE CLIENTES ====================

    /**
     * Busca clientes por nombre, apellido o email.
     */
    fun searchClients(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = if (query.isBlank()) {
                    emptyList()
                } else {
                    trainerRepository.searchClients(trainerId, query)
                }
                _uiState.update { 
                    it.copy(
                        searchResults = results,
                        isSearching = false,
                        searchQuery = query
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = e.message
                    )
                }
            }
        }
    }

    /**
     * Selecciona un cliente para ver detalles.
     */
    fun selectClient(client: UserEntity) {
        _selectedClient.value = client
        loadClientDetails(client.id)
    }

    /**
     * Carga un cliente por ID (para cuando se navega directo al detalle, p. ej. otra instancia del ViewModel).
     */
    fun loadClientById(clientId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, clientNotFound = false) }
            val client = userRepository.getUserById(clientId)
            _selectedClient.value = client
            if (client != null) {
                loadClientDetails(clientId)
            } else {
                _uiState.update { it.copy(clientNotFound = true) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Limpia la selección de cliente.
     */
    fun clearSelectedClient() {
        _selectedClient.value = null
        _uiState.update { it.copy(selectedClientRoutines = emptyList(), selectedClientGoals = emptyList()) }
    }

    /**
     * Carga los detalles de un cliente (rutinas y objetivos).
     */
    private fun loadClientDetails(clientId: Int) {
        viewModelScope.launch {
            // Cargar rutinas del cliente
            trainerRepository.getClientRoutines(trainerId, clientId)
                .collect { routines ->
                    _uiState.update { it.copy(selectedClientRoutines = routines) }
                }
        }
        viewModelScope.launch {
            // Cargar objetivos del cliente
            progressRepository.getClientGoals(clientId, trainerId)
                .collect { goals ->
                    _uiState.update { it.copy(selectedClientGoals = goals) }
                }
        }
    }

    // ==================== SOLICITUDES ====================

    /**
     * Acepta una solicitud de cliente.
     */
    fun acceptClientRequest(clientId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            trainerRepository.acceptClientRequest(trainerId, clientId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Cliente aceptado") }
                    loadTrainerStats()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * Rechaza una solicitud de cliente.
     */
    fun rejectClientRequest(clientId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            trainerRepository.rejectClientRequest(trainerId, clientId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Solicitud rechazada") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * Pausa la relación con un cliente.
     */
    fun pauseClient(clientId: Int) {
        viewModelScope.launch {
            trainerRepository.pauseClient(trainerId, clientId)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Cliente pausado") }
                    loadTrainerStats()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ==================== RUTINAS ====================

    /**
     * Crea una nueva rutina para un cliente.
     */
    fun createRoutineForClient(
        clientId: Int,
        name: String,
        dayInfo: String,
        exerciseIds: List<Int>,
        scheduledTime: Long? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            trainerRepository.createRoutineForClient(
                trainerId = trainerId,
                clientId = clientId,
                name = name,
                dayInfo = dayInfo,
                exerciseIds = exerciseIds,
                scheduledTime = scheduledTime
            ).onSuccess { routineId ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        successMessage = "Rutina creada exitosamente"
                    )
                }
                // Notificar al cliente cuando le llega la rutina
                notificationRepository?.let { repo ->
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            val trainer = userRepository.getUserById(trainerId)
                            val trainerName = trainer?.name?.let { "$it ${trainer.lastName}" } ?: "Tu entrenador"
                            repo.saveNotification(
                                NotificationEntity(
                                    userId = clientId,
                                    title = "Nueva rutina asignada",
                                    message = "$trainerName te ha asignado la rutina \"$name\".",
                                    type = NotificationType.ROUTINE_ASSIGNED.name,
                                    senderId = trainerId,
                                    actionType = NotificationAction.OPEN_ROUTINE.name,
                                    actionData = routineId.toString()
                                )
                            )
                        }
                    }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Crea una rutina global (pública).
     */
    fun createGlobalRoutine(
        name: String,
        dayInfo: String,
        exerciseIds: List<Int>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            trainerRepository.createGlobalRoutine(
                trainerId = trainerId,
                name = name,
                dayInfo = dayInfo,
                exerciseIds = exerciseIds
            ).onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        successMessage = "Rutina global creada"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Elimina una rutina.
     */
    fun deleteRoutine(routineId: Int) {
        viewModelScope.launch {
            trainerRepository.deleteRoutine(routineId)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Rutina eliminada") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ==================== OBJETIVOS ====================

    /**
     * Crea un objetivo para un cliente.
     */
    fun createGoalForClient(
        clientId: Int,
        title: String,
        description: String?,
        category: GoalCategory,
        targetValue: Double?,
        currentValue: Double?,
        unit: String?,
        targetDate: Long?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val goal = GoalEntity(
                userId = clientId,
                createdByTrainerId = trainerId,
                title = title,
                description = description,
                category = category.name,
                targetValue = targetValue,
                currentValue = currentValue,
                startValue = currentValue,
                unit = unit,
                targetDate = targetDate
            )
            progressRepository.createGoal(goal)
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Objetivo creado"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * Añade feedback a un objetivo.
     */
    fun addGoalFeedback(goalId: Int, feedback: String) {
        viewModelScope.launch {
            progressRepository.addTrainerFeedback(goalId, feedback)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Feedback añadido") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ==================== NOTAS ====================

    /**
     * Actualiza las notas sobre un cliente.
     */
    fun updateClientNotes(clientId: Int, notes: String) {
        viewModelScope.launch {
            trainerRepository.updateClientNotes(trainerId, clientId, notes)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Notas actualizadas") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Carga las estadísticas del entrenador.
     */
    private fun loadTrainerStats() {
        viewModelScope.launch {
            try {
                val stats = trainerRepository.getTrainerStats(trainerId)
                val routineCount = trainerRepository.countMyRoutines(trainerId)
                _uiState.update { 
                    it.copy(
                        trainerStats = stats,
                        totalRoutinesCreated = routineCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Obtiene clientes inactivos (sin interacción reciente).
     */
    fun loadInactiveClients(daysThreshold: Int = 7) {
        viewModelScope.launch {
            try {
                val inactiveClients = trainerRepository.getInactiveClients(trainerId, daysThreshold)
                _uiState.update { it.copy(inactiveClients = inactiveClients) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ==================== UTILIDADES ====================

    /**
     * Limpia el mensaje de error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Limpia el mensaje de éxito.
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Limpia los resultados de búsqueda.
     */
    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }
}

/**
 * Estado de la UI del Coach.
 */
data class CoachUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    
    // Búsqueda
    val searchQuery: String = "",
    val searchResults: List<UserEntity> = emptyList(),
    
    // Cliente seleccionado
    val selectedClientRoutines: List<RoutineEntity> = emptyList(),
    val selectedClientGoals: List<GoalEntity> = emptyList(),
    val clientNotFound: Boolean = false,
    
    // Estadísticas
    val trainerStats: TrainerStats? = null,
    val totalRoutinesCreated: Int = 0,
    val inactiveClients: List<UserEntity> = emptyList()
)
