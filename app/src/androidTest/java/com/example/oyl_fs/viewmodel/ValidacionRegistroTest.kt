
package com.example.oyl_fs.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas de validación del formulario.
 *
 * No requieren abrir SQLite ni iniciar
 * una pantalla Compose.
 */
class ValidacionRegistroTest {

    private val datosValidos = FormularioRegistro(
        alias = "cliente_nuevo",
        nombre = "Ana Prueba",
        correo = "ana@example.com",
        rut = "12.345.678-5",
        telefono = "+56 9 1234 5678",
        clave = "ClaveSegura123!",
        confirmacion = "ClaveSegura123!"
    )

    @Test
    fun aceptaFormularioValido() {
        val errores = ValidacionRegistro.validar(
            datosValidos
        )

        assertFalse(errores.hayErrores())
    }

    @Test
    fun rechazaAliasIncorrectoOReservado() {
        assertNotNull(
            ValidacionRegistro.validar(
                datosValidos.copy(alias = "ab")
            ).alias
        )

        assertNotNull(
            ValidacionRegistro.validar(
                datosValidos.copy(alias = "admin")
            ).alias
        )
    }

    @Test
    fun rechazaCorreoIncorrecto() {
        val errores = ValidacionRegistro.validar(
            datosValidos.copy(correo = "correo-invalido")
        )

        assertNotNull(errores.correo)
    }

    @Test
    fun verificaDigitoDelRut() {
        assertNull(
            ValidacionRegistro.validar(
                datosValidos
            ).rut
        )

        assertNotNull(
            ValidacionRegistro.validar(
                datosValidos.copy(rut = "12.345.678-9")
            ).rut
        )
    }

    @Test
    fun rechazaContrasenasDiferentes() {
        val errores = ValidacionRegistro.validar(
            datosValidos.copy(
                confirmacion = "OtraClave123!"
            )
        )

        assertNotNull(errores.confirmacion)
    }

    @Test
    fun rechazaContrasenaCorta() {
        val errores = ValidacionRegistro.validar(
            datosValidos.copy(
                clave = "1234",
                confirmacion = "1234"
            )
        )

        assertNotNull(errores.clave)
    }

    @Test
    fun aceptaTelefonoOpcional() {
        val errores = ValidacionRegistro.validar(
            datosValidos.copy(telefono = "")
        )

        assertNull(errores.telefono)
    }

    @Test
    fun rechazaCamposObligatoriosVacios() {
        val errores = ValidacionRegistro.validar(
            datosValidos.copy(
                nombre = "",
                rut = "",
                alias = ""
            )
        )

        assertNotNull(errores.nombre)
        assertNotNull(errores.rut)
        assertNotNull(errores.alias)
    }
}