package com.example.oyl_fs.network

import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErroresApiDesarrolloTest {
    @Test fun credencialesYSesionTienenMensajesDiferentes() {
        assertEquals("Usuario o contraseña incorrectos.", mensajeErrorHttp(401, "", true))
        assertTrue(mensajeErrorHttp(401, "", false).contains("sesión"))
    }
    @Test fun erroresDelServidorNoMuestranDetallesTecnicos() {
        val mensaje = mensajeErrorHttp(500, "{\"message\":\"SQLException jdbc password=secret\"}")
        assertFalse(mensaje.contains("secret"))
        assertFalse(mensaje.contains("jdbc"))
        assertTrue(mensaje.contains("más tarde"))
    }
    @Test fun validacionDelBackendSeConservaSiEsLegible() {
        assertEquals("El correo ya está registrado.",
            mensajeErrorHttp(409, "{\"message\":\"El correo ya está registrado.\"}"))
    }
    @Test fun cuerposInvalidosOVaciosTienenAlternativaClara() {
        assertTrue(mensajeErrorHttp(400, "<html>Error</html>").startsWith("Revisa"))
        assertTrue(mensajeErrorHttp(400, "{\"message\":\"\"}").startsWith("Revisa"))
    }
    @Test fun validacionTecnicaNoSeFiltraAlFormulario() {
        assertFalse(mensajeErrorHttp(400, "{\"message\":\"producto java.lang.Exception\"}")
            .contains("java.lang"))
    }
    @Test fun tiempoDeEsperaYFaltaDeConexionExplicanQueHacer() {
        assertTrue(SocketTimeoutException().mensajeApi().contains("tardando"))
        assertTrue(UnknownHostException().mensajeApi().contains("conexión"))
    }
    @Test fun erroresInesperadosNoMuestranElMensajeInterno() {
        assertEquals("No pudimos completar la operación. Inténtalo nuevamente.",
            RuntimeException("Error secreto interno").mensajeApi())
    }
}
