package com.shagox.apptrainingnow.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shagox.apptrainingnow.data.repository.IUserRepository

class AuthViewModelFactory(
    private val repository: IUserRepository,
    context: Context
) : ViewModelProvider.Factory {
    // Se guarda el Application context (no el de la Activity) para evitar fugas de memoria.
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}