package com.example.oyl_fs.network

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

// Convierte un error de Retrofit en un mensaje legible para el usuario
fun Throwable.mensajeApi(): String = when (this) {
    is HttpException -> try {
        val cuerpo = response()?.errorBody()?.string().orEmpty()
        JSONObject(cuerpo).optString("message", "Error ${code()}")
    } catch (e: Exception) {
        "Error ${code()}"
    }
    is IOException -> "No se pudo conectar con el servidor"
    else -> message ?: "Error inesperado"
}