package com.example.oyl_fs.network

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

fun Throwable.mensajeApi(): String = when (this) {
    is HttpException -> {
        val predeterminado = when (code()) {
            400 -> "Revisa los datos ingresados"
            401, 403 -> "Credenciales incorrectas o acceso no autorizado"
            404 -> "El registro ya no está disponible"
            409 -> "Los datos entran en conflicto con un registro existente"
            in 500..599 -> "El servidor no pudo completar la operación"
            else -> "Error ${code()}"
        }
        runCatching {
            val mensaje = JsonParser().parse(response()?.errorBody()?.string().orEmpty())
                .asJsonObject.get("message")?.asString
            mensaje?.takeIf { it.isNotBlank() } ?: predeterminado
        }.getOrDefault(predeterminado)
    }
    is IOException -> "No se pudo conectar con el servidor"
    else -> message ?: "No se pudo completar la operación"
}
