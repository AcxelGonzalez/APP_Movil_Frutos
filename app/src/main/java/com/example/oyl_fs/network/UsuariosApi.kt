package com.example.oyl_fs.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UsuariosApi {

    // Devuelve el usuario si alias y contraseña son correctos; si no, responde 401
    @POST("api/usuarios/login")
    suspend fun login(@Body request: LoginRequest): LoginResponseDto

    // Crea una cuenta de cliente (201). Responde 409 si el alias o el correo ya existen
    @POST("api/usuarios/registro")
    suspend fun registrar(@Body request: RegistroRequest): UsuarioDto

    // Edita nombre, correo, teléfono y dirección del usuario
    @PUT("api/usuarios/{id}")
    suspend fun actualizar(
        @Path("id") id: Long,
        @Body request: ActualizarPerfilRequest
    ): UsuarioDto
}
