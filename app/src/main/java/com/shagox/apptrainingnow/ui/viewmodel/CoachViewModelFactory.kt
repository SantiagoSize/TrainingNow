package com.shagox.apptrainingnow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.data.repository.UserRepository

/**
 * Factory para crear instancias de CoachViewModel con sus dependencias.
 */
class CoachViewModelFactory(
    private val trainerRepository: TrainerRepository,
    private val progressRepository: ProgressRepository,
    private val userRepository: UserRepository,
    private val trainerId: Int
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CoachViewModel::class.java)) {
            return CoachViewModel(
                trainerRepository = trainerRepository,
                progressRepository = progressRepository,
                userRepository = userRepository,
                trainerId = trainerId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
