package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.model.ValidacionPerfil
import com.example.oyl_fs.util.ValidacionContacto
import org.junit.Assert.*
import org.junit.Test

class ValidacionContactoTest {
    @Test fun telefonoAdmiteFormatoChilenoPeroRechazaLetrasYExcesoDeDigitos() {
        assertTrue(ValidacionContacto.telefonoValido("+56 9 1234 5678"))
        assertTrue(ValidacionContacto.telefonoValido(""))
        assertFalse(ValidacionContacto.telefonoValido("abcdefgh12345678"))
        assertFalse(ValidacionContacto.telefonoValido("1234567"))
        assertFalse(ValidacionContacto.telefonoValido("123456789012"))
    }

    @Test fun rutSeNormalizaSinOmitirLaValidacionDelDigito() {
        assertTrue(ValidacionContacto.rutValido("12.345.678-5"))
        assertTrue(ValidacionContacto.rutValido("123456785"))
        assertFalse(ValidacionContacto.rutValido("12.345.678-9"))
        assertFalse(ValidacionContacto.rutValido(""))
    }

    @Test fun perfilDevuelveErroresPorCampoYAdmiteCuentasAntiguasSinRut() {
        val perfil = PerfilUsuario("cliente", false, "Cliente", correo = "cliente@example.test")
        assertNull(ValidacionPerfil.validar(perfil).primerError())
        val errores = ValidacionPerfil.validar(perfil.copy(nombre = " ", correo = "invalido", rut = "123", telefono = "abc12345678"))
        assertNotNull(errores.nombre)
        assertNotNull(errores.correo)
        assertNotNull(errores.rut)
        assertNotNull(errores.telefono)
    }
}
