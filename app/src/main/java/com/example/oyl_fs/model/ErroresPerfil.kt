package com.example.oyl_fs.model

import com.example.oyl_fs.util.ValidacionContacto

data class ErroresPerfil(
    val nombre: String? = null,
    val correo: String? = null,
    val rut: String? = null,
    val telefono: String? = null,
    val general: String? = null
) {
    fun primerError() = listOfNotNull(nombre, correo, rut, telefono, general).firstOrNull()
}

object ValidacionPerfil {
    fun validar(perfil: PerfilUsuario, requerirTelefono: Boolean = false) = ErroresPerfil(
        nombre = if (perfil.nombre.isBlank()) "Escribe el nombre"
            else com.example.oyl_fs.util.ValidacionLongitud.errorNombrePerfil(perfil.nombre),
        correo = if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(perfil.correo.trim())) "Escribe un correo válido" else null,
        // Las cuentas antiguas y demo pueden no tener RUT; si se ingresa debe ser válido.
        rut = if (perfil.rut.isNotBlank() && !ValidacionContacto.rutValido(perfil.rut)) "Ingresa un RUT válido" else null,
        telefono = when {
            requerirTelefono && perfil.telefono.isBlank() -> "Escribe el teléfono"
            !ValidacionContacto.telefonoValido(perfil.telefono) -> "Revisa el teléfono"
            else -> null
        }
    )
}
