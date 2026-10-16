package com.example.oyl_fs.model

// Datos de perfil separados de las credenciales del login.
data class PerfilUsuario(
    val id: String,
    val esAdmin: Boolean,
    val nombre: String,
    val apellido: String = "",
    val rut: String = "",
    val correo: String = "",
    val telefono: String = "",
    val direccion: String = ""
)
