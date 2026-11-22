package com.example.oyl_fs.network

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

// No muestra excepciones, direcciones del servidor ni detalles de la base de datos.
fun Throwable.mensajeApi(esLogin: Boolean = false): String = when (this) {
    is SocketTimeoutException -> "El servidor está tardando en responder. Inténtalo nuevamente."
    is UnknownHostException, is ConnectException -> "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo nuevamente."
    is SSLException -> "No pudimos establecer una conexión segura con el servidor. Inténtalo más tarde."
    is HttpException -> {
        val cuerpo = runCatching { response()?.errorBody()?.string() }.getOrNull().orEmpty()
        val esAcceso = esLogin || response()?.raw()?.request?.url?.encodedPath?.endsWith("/login") == true
        mensajeErrorHttp(code(), cuerpo, esAcceso)
    }
    is IOException -> "La conexión se interrumpió. Revisa tu conexión e inténtalo nuevamente."
    is IllegalArgumentException -> mensajeUsuarioSeguro(message)
        ?: "Revisa los datos ingresados e inténtalo nuevamente."
    else -> "No pudimos completar la operación. Inténtalo nuevamente."
}

internal fun mensajeErrorHttp(codigo: Int, cuerpo: String, esLogin: Boolean = false): String {
    val validacion = if (codigo in listOf(400, 409, 422)) {
        runCatching {
            val json = JsonParser().parse(cuerpo.take(8000)).asJsonObject
            listOf("message", "mensaje", "detail").firstNotNullOfOrNull { campo ->
                json.get(campo)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                    ?.asString?.let(::mensajeUsuarioSeguro)
            }
        }.getOrNull()
    } else null
    return when (codigo) {
        400, 422 -> validacion ?: "Revisa los datos del formulario e inténtalo nuevamente."
        401 -> if (esLogin) "Usuario o contraseña incorrectos." else "Tu sesión no es válida. Vuelve a iniciar sesión."
        403 -> "Tu cuenta no tiene permiso para realizar esta acción."
        404 -> "No encontramos la información solicitada. Actualiza la pantalla."
        408, 504 -> "El servidor está tardando en responder. Inténtalo nuevamente."
        409 -> validacion ?: "Los datos ya existen o cambiaron. Revisa la información antes de guardar."
        413 -> "El archivo es demasiado grande. Selecciona uno más pequeño."
        429 -> "Se hicieron demasiados intentos. Espera un momento y vuelve a intentarlo."
        in 500..599 -> "El servidor no pudo completar la operación. Inténtalo más tarde."
        else -> "No pudimos completar la solicitud. Inténtalo nuevamente."
    }
}

private fun mensajeUsuarioSeguro(mensaje: String?): String? {
    val texto = mensaje?.trim()?.takeIf { it.length in 3..240 } ?: return null
    val tecnico = Regex("exception|stacktrace|jdbc|hibernate|java\\.|org\\.|sql|https?://|<[^>]+>|\\n", RegexOption.IGNORE_CASE)
    // Conserva las validaciones dirigidas al usuario, sin reutilizar errores técnicos.
    val validacion = Regex("usuario|contrase|correo|producto|stock|precio|nombre|código|codigo|cantidad|datos|campo|pedido|teléfono|telefono|dirección|direccion|sesión|sesion|imagen|fotograf", RegexOption.IGNORE_CASE)
    return texto.takeIf { !tecnico.containsMatchIn(it) && validacion.containsMatchIn(it) }
}
