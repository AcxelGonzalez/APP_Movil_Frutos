package com.example.oyl_fs.network

// Lo que la app ENVÍA para iniciar sesión (igual al body de Postman)
data class LoginRequest(
    val alias: String,
    val contrasena: String
)

// Lo que la app ENVÍA para crear una cuenta (igual al body de Postman)
data class RegistroRequest(
    val nombre: String,
    val alias: String,
    val correo: String,
    val telefono: String? = null,
    val direccion: String? = null,
    val contrasena: String
)

// Lo que la app ENVÍA para editar el perfil (igual al PUT de Postman).
// El alias y el rol no se pueden cambiar.
data class ActualizarPerfilRequest(
    val nombre: String,
    val correo: String,
    val telefono: String? = null,
    val direccion: String? = null
)

// El usuario tal como lo devuelve ms-usuarios (nunca incluye la contraseña)
data class UsuarioDto(
    val id: Long,
    val nombre: String,
    val alias: String,
    val correo: String,
    val telefono: String? = null,
    val direccion: String? = null,
    val rol: String            // "ADMIN" o "CLIENTE"
)

