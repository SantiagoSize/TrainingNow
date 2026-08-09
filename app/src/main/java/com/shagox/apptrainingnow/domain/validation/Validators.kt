package com.shagox.apptrainingnow.domain.validation

import android.util.Patterns

// Archivo para las validaciones de los formularios
fun validateNameLettersOnly(nombre: String): String? {
    if (nombre.isBlank()) return "El nombre es obligatorio"
    val regex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$")
    return if (!regex.matches(nombre)) "Solo se aceptan letras y espacios" else null
}

fun validateEmail(email: String): String? {
    if (email.isBlank()) return "El correo es obligatorio"
    val ok = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    return if (!ok) "Formato de correo inválido" else null
}

fun validatePhoneDigitsOnly(telefono: String): String? {
    if (telefono.isBlank()) return "El teléfono es obligatorio"
    if (!telefono.all { it.isDigit() }) return "Solo deben ser números"
    if (telefono.length !in 8..15) return "Debe tener entre 8 y 15 números"
    return null
}

fun validateStringPassword(pass: String): String? {
    if (pass.isBlank()) return "Debe escribir una contraseña"
    if (pass.length < 8) return "Mínimo 8 caracteres"
    if (!pass.any { it.isUpperCase() }) return "Falta una mayúscula"
    if (!pass.any { it.isLowerCase() }) return "Falta una minúscula"
    if (!pass.any { it.isDigit() }) return "Falta un número"
    return null
}

fun validateConfirm(pass: String, confirm: String): String? {
    if (confirm.isBlank()) return "Debe confirmar la contraseña"
    return if (pass != confirm) "Las contraseñas deben ser iguales" else null
}