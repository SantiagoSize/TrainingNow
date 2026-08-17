package com.shagox.apptrainingnow.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests unitarios de los validadores de formularios.
 * Robolectric provee android.util.Patterns para validateEmail.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ValidatorsTest {

    // ---------- Nombre ----------
    @Test
    fun nombreValido_conAcentos_pasa() {
        assertNull(validateNameLettersOnly("José Ñuñez"))
    }

    @Test
    fun nombreVacio_falla() {
        assertEquals("El nombre es obligatorio", validateNameLettersOnly(""))
    }

    @Test
    fun nombreConNumeros_falla() {
        assertNotNull(validateNameLettersOnly("Santiago123"))
    }

    // ---------- Email ----------
    @Test
    fun emailValido_pasa() {
        assertNull(validateEmail("usuario@gmail.com"))
    }

    @Test
    fun emailSinArroba_falla() {
        assertNotNull(validateEmail("usuariogmail.com"))
    }

    @Test
    fun emailVacio_falla() {
        assertEquals("El correo es obligatorio", validateEmail(""))
    }

    // ---------- Teléfono ----------
    @Test
    fun telefonoValido_pasa() {
        assertNull(validatePhoneDigitsOnly("56912345678"))
    }

    @Test
    fun telefonoConLetras_falla() {
        assertNotNull(validatePhoneDigitsOnly("91234abcd"))
    }

    @Test
    fun telefonoMuyCorto_falla() {
        assertNotNull(validatePhoneDigitsOnly("1234567"))
    }

    // ---------- Contraseña ----------
    @Test
    fun passwordFuerte_pasa() {
        assertNull(validateStringPassword("Entrena2026"))
    }

    @Test
    fun passwordSinMayuscula_falla() {
        assertEquals("Falta una mayúscula", validateStringPassword("entrena2026"))
    }

    @Test
    fun passwordSinNumero_falla() {
        assertEquals("Falta un número", validateStringPassword("Entrenadora"))
    }

    @Test
    fun passwordCorta_falla() {
        assertEquals("Mínimo 8 caracteres", validateStringPassword("Abc1"))
    }

    // ---------- Confirmación ----------
    @Test
    fun confirmacionIgual_pasa() {
        assertNull(validateConfirm("Entrena2026", "Entrena2026"))
    }

    @Test
    fun confirmacionDistinta_falla() {
        assertEquals("Las contraseñas deben ser iguales", validateConfirm("Entrena2026", "Otra2026"))
    }
}
