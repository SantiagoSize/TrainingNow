package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.UserApi
import retrofit2.Response

/**
 * Repositorio del flujo "Olvidé mi contraseña" contra TrainNow-Usuarios (8081).
 * Pasos: solicitar código → verificar código → confirmar nueva contraseña.
 */
class PasswordResetRepository(
    private val api: UserApi = RemoteModule.userApi()
) {

    suspend fun requestCode(email: String): Result<Unit> = call {
        api.requestPasswordReset(mapOf("email" to email.trim()))
    }

    suspend fun verifyCode(email: String, code: String): Result<Unit> = call {
        api.verifyPasswordReset(mapOf("email" to email.trim(), "code" to code.trim()))
    }

    suspend fun confirmReset(email: String, code: String, newPassword: String): Result<Unit> = call {
        api.confirmPasswordReset(
            mapOf(
                "email" to email.trim(),
                "code" to code.trim(),
                "newPassword" to newPassword
            )
        )
    }

    private suspend fun call(block: suspend () -> Response<Map<String, String>>): Result<Unit> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val msg = when (response.code()) {
                    404 -> "No existe una cuenta con ese email"
                    400 -> "Código incorrecto o expirado"
                    503 -> "No se pudo enviar el correo. Intenta más tarde"
                    else -> "Error del servidor (${response.code()})"
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión con el servidor"))
        }
    }
}
