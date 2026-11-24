package com.example.oyl_fs.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ErroresApiTest {
    private fun error(codigo: Int, cuerpo: String) = HttpException(
        Response.error<Unit>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    @Test fun muestraElMensajeDeValidacionDelServidor() {
        assertEquals("Correo ya registrado", error(409, "{\"message\":\"Correo ya registrado\"}").mensajeApi())
    }
    @Test fun cuerpoVacioOHtmlUsaUnMensajeLegible() {
        assertEquals("El servidor no pudo completar la operación. Inténtalo más tarde.", error(503, "<html>sin servicio</html>").mensajeApi())
        assertEquals("Revisa los datos del formulario e inténtalo nuevamente.", error(400, "{}").mensajeApi())
    }
    @Test fun desconexionNoSeConfundeConCredencialesIncorrectas() {
        assertEquals("La conexión se interrumpió. Revisa tu conexión e inténtalo nuevamente.", IOException().mensajeApi())
    }
}
