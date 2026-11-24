package com.example.oyl_fs.util

object ValidacionLongitud {
    fun errorNombrePerfil(nombre: String): String? =
        if (nombre.length > 255) "El nombre admite hasta 255 caracteres." else null

    fun errorPresentacion(presentacion: String): String? =
        if (presentacion.length > 255) "La presentación admite hasta 255 caracteres." else null
}
