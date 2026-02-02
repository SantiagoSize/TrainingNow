package com.shagox.apptrainingnow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shagox.apptrainingnow.data.repository.INotificationRepository
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository

/**
 * Factory para crear instancias de CoachViewModel con sus dependencias.
 * @param notificationRepository Opcional: si se proporciona, el cliente será notificado al asignarle una rutina.
 */
class CoachViewModelFactory(
    private val trainerRepository: TrainerRepository,
    private val progressRepository: ProgressRepository,
    private val userRepository: IUserRepository,
    private val trainerId: Int,
    private val notificationRepository: INotificationRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CoachViewModel::class.java)) {
            return CoachViewModel(
                trainerRepository = trainerRepository,
                progressRepository = progressRepository,
                userRepository = userRepository,
                trainerId = trainerId,
                notificationRepository = notificationRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
