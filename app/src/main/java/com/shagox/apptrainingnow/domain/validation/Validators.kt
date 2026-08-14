package com.shagox.apptrainingnow.domain.validation

import android.util.Patterns

// ============================================================
// VALIDACIONES DE FORMULARIOS
// Cada función revisa un solo campo y devuelve:
//   - null           → el dato está bien
//   - un String       → el mensaje de error para mostrar bajo el campo
// Son funciones simples a propósito: una entrada, una salida, sin efectos secundarios.
// ============================================================

/**
 * Validación: NOMBRE / APELLIDO (solo letras).
 * Qué revisa: que no esté vacío y que solo tenga letras (con tildes y ñ) y espacios.
 * Dónde se usa: campos "Nombres" y "Apellidos" del registro.
 */
fun validateNameLettersOnly(nombre: String): String? {
    if (nombre.isBlank()) return "El nombre es obligatorio"
    val soloLetrasYEspacios = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$")
    return if (!soloLetrasYEspacios.matches(nombre)) "Solo se aceptan letras y espacios" else null
}

/**
 * Validación: CORREO (formato de email).
 * Qué revisa: que no esté vacío y que tenga forma de correo válido (ej. usuario@dominio.com).
 * Dónde se usa: campo "Correo" del registro y del login.
 */
fun validateEmail(email: String): String? {
    if (email.isBlank()) return "El correo es obligatorio"
    val formatoValido = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    return if (!formatoValido) "Formato de correo inválido" else null
}

/**
 * Códigos telefónicos de países de América, SIN el símbolo "+" (ej. "56" para Chile).
 * Se usan para exigir que el teléfono incluya el código de su país al inicio.
 * Ordenados de mayor a menor cantidad de dígitos para no confundir, por ejemplo,
 * "59" con "595" (Paraguay) al buscar coincidencia.
 */
private val CODIGOS_PAIS_AMERICA = listOf(
    "56",  // Chile
    "54",  // Argentina
    "591", // Bolivia
    "55",  // Brasil
    "1",   // Canadá / EE.UU. / Puerto Rico / República Dominicana
    "57",  // Colombia
    "506", // Costa Rica
    "53",  // Cuba
    "593", // Ecuador
    "503", // El Salvador
    "502", // Guatemala
    "504", // Honduras
    "52",  // México
    "505", // Nicaragua
    "507", // Panamá
    "595", // Paraguay
    "51",  // Perú
    "598", // Uruguay
    "58"   // Venezuela
).sortedByDescending { it.length }

/**
 * Validación: TELÉFONO (código de país obligatorio, solo dígitos).
 * Qué revisa:
 *  1. que no esté vacío,
 *  2. que sean puros números (sin "+", espacios ni letras — el símbolo "+" ya no se pide),
 *  3. que el número, al empezar, tenga el código de un país de América reconocido
 *     (ej. "56" de Chile, "54" de Argentina, "1" de EE.UU./Canadá...). Esto evita el
 *     error típico de escribir solo el número local, por ejemplo "912341234" sin el
 *     "56" de Chile adelante,
 *  4. que después de ese código todavía queden dígitos suficientes para un número real.
 * Dónde se usa: campo "Teléfono" del registro.
 */
fun validatePhoneDigitsOnly(telefono: String): String? {
    if (telefono.isBlank()) return "El teléfono es obligatorio"
    if (!telefono.all { it.isDigit() }) return "Solo se aceptan números, sin espacios ni símbolos"
    if (telefono.length !in 8..15) return "Debe tener entre 8 y 15 números en total"

    // Nota: este mensaje ya NO se muestra en rojo bajo el campo (se veía como un error molesto).
    // La UI del registro explica esto mismo con un texto gris permanente debajo del campo,
    // pero acá se sigue exigiendo el código de país para que no se pueda enviar sin él.
    val codigoPais = CODIGOS_PAIS_AMERICA.firstOrNull { telefono.startsWith(it) }
        ?: return "Falta el código de país"

    val numeroLocal = telefono.removePrefix(codigoPais)
    if (numeroLocal.length < 7) return "Falta el número después del código de país"

    return null
}

/**
 * Validación: CONTRASEÑA (seguridad mínima).
 * Qué revisa: que no esté vacía, que tenga mínimo 8 caracteres, y que combine al
 * menos una mayúscula, una minúscula y un número.
 * Dónde se usa: campo "Contraseña" del registro.
 */
fun validateStringPassword(pass: String): String? {
    if (pass.isBlank()) return "Debe escribir una contraseña"
    if (pass.length < 8) return "Mínimo 8 caracteres"
    if (!pass.any { it.isUpperCase() }) return "Falta una mayúscula"
    if (!pass.any { it.isLowerCase() }) return "Falta una minúscula"
    if (!pass.any { it.isDigit() }) return "Falta un número"
    return null
}

/**
 * Validación: CONFIRMAR CONTRASEÑA (que coincida).
 * Qué revisa: que el campo no esté vacío y que sea exactamente igual a la contraseña
 * escrita en el campo "Contraseña".
 * Dónde se usa: campo "Confirmar contraseña" del registro.
 */
fun validateConfirm(pass: String, confirm: String): String? {
    if (confirm.isBlank()) return "Debe confirmar la contraseña"
    return if (pass != confirm) "Las contraseñas deben ser iguales" else null
}