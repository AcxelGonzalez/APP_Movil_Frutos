package com.example.oyl_fs.repository

import com.example.oyl_fs.network.ActualizarPerfilRequest
import com.example.oyl_fs.network.LoginRequest
import com.example.oyl_fs.network.RegistroRequest
import com.example.oyl_fs.network.RetrofitClient
import com.example.oyl_fs.network.UsuarioDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Guarda quién tiene la sesión iniciada. Lo usan el login, la compra, el perfil y el historial.
object SesionRepository {

    private val _usuario = MutableStateFlow<UsuarioDto?>(null)
    val usuario: StateFlow<UsuarioDto?> = _usuario.asStateFlow()

    // Id del usuario en el backend, o null si no hay sesión
    val usuarioId: Long? get() = _usuario.value?.id

    // Pide el login a ms-usuarios. Si sale bien, guarda al usuario.
    suspend fun iniciarSesion(alias: String, contrasena: String): Result<UsuarioDto> {
        return try {
            val usuario = RetrofitClient.usuarios.login(LoginRequest(alias.trim(), contrasena))
            _usuario.value = usuario
            Result.success(usuario)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Crea la cuenta en ms-usuarios. No inicia sesión: después se vuelve al login.
    suspend fun registrar(solicitud: RegistroRequest): Result<UsuarioDto> {
        return try {
            Result.success(RetrofitClient.usuarios.registrar(solicitud))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Guarda los cambios del perfil en ms-usuarios y actualiza al usuario de la sesión
    suspend fun actualizarPerfil(solicitud: ActualizarPerfilRequest): Result<UsuarioDto> {
        val id = usuarioId
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada"))
        return try {
            val actualizado = RetrofitClient.usuarios.actualizar(id, solicitud)
            _usuario.value = actualizado
            Result.success(actualizado)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cerrarSesion() {
        _usuario.value = null
    }
}
