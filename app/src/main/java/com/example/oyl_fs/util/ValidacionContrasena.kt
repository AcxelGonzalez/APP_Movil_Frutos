package com.example.oyl_fs.util

object ValidacionContrasena {
    // BCrypt limita bytes UTF-8, no caracteres. No normalizar ni recortar la clave.
    fun errorMaximo(clave: String): String? =
        if (clave.toByteArray(Charsets.UTF_8).size > 72) {
            "La contraseña supera el máximo de 72 bytes UTF-8. Las tildes y los emojis pueden ocupar varios bytes."
        } else null

    fun errorRegistro(clave: String): String? = when {
        clave.isBlank() || clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres y no puede estar en blanco."
        else -> errorMaximo(clave)
    }
}
