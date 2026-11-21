package com.example.oyl_fs.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.example.oyl_fs.viewmodel.SesionIdentificada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
object SesionRemotaRepository {

    private var preferencias: SharedPreferences? = null

    fun configurar(context: Context) {
        preferencias = context.applicationContext.getSharedPreferences("sesion_remota_oyl", Context.MODE_PRIVATE)
    }

    suspend fun restaurar(): SesionIdentificada? = withContext(Dispatchers.IO) {
        _usuario.value = null
        val json = preferencias?.getString("usuario", null) ?: return@withContext null
        val usuario = runCatching { Gson().fromJson(json, UsuarioDto::class.java) }.getOrNull()
        if (usuario == null || usuario.id <= 0 || usuario.rol !in setOf("ADMIN", "CLIENTE")) {
            cerrar()
            return@withContext null
        }
        _usuario.value = usuario
        usuario.identidad()
    }

    suspend fun guardar(sesion: SesionIdentificada) = withContext(Dispatchers.IO) {
        val usuario = requireNotNull(_usuario.value) { "Inicia sesión con el servidor" }
        require(usuario.identidad() == sesion)
        check(requireNotNull(preferencias).edit().putString("usuario", Gson().toJson(usuario)).commit())
    }

    suspend fun cerrar() = withContext(Dispatchers.IO) {
        check(requireNotNull(preferencias).edit().clear().commit())
        _usuario.value = null
    }

    fun UsuarioDto.identidad() = SesionIdentificada(id.toString(), nombre, rol == "ADMIN", false)

    private val _usuario = MutableStateFlow<UsuarioDto?>(null)
    val usuario: StateFlow<UsuarioDto?> = _usuario.asStateFlow()

    // Id del usuario en el backend, o null si no hay sesión
    val usuarioId: Long? get() = _usuario.value?.id

    // Pide el login a ms-usuarios. Si sale bien, guarda al usuario.
    suspend fun iniciarSesion(alias: String, contrasena: String, api: com.example.oyl_fs.network.UsuariosApi = RetrofitClient.usuarios): Result<UsuarioDto> {
        return try {
            val usuario = api.login(LoginRequest(alias.trim(), contrasena))
            require(usuario.id > 0 && usuario.rol in setOf("ADMIN", "CLIENTE")) { "Respuesta de usuario inválida" }
            _usuario.value = usuario
            Result.success(usuario)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Crea la cuenta en ms-usuarios. No inicia sesión: después se vuelve al login.
    suspend fun registrar(solicitud: RegistroRequest, api: com.example.oyl_fs.network.UsuariosApi = RetrofitClient.usuarios): Result<UsuarioDto> {
        return try {
            val creado = api.registrar(solicitud)
            require(creado.id > 0 && creado.rol == "CLIENTE") { "Respuesta de registro inválida" }
            Result.success(creado)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Guarda los cambios del perfil en ms-usuarios y actualiza al usuario de la sesión
    suspend fun actualizarPerfil(solicitud: ActualizarPerfilRequest, api: com.example.oyl_fs.network.UsuariosApi = RetrofitClient.usuarios): Result<UsuarioDto> {
        val id = usuarioId
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada"))
        return try {
            val actualizado = api.actualizar(id, solicitud)
            require(actualizado.id == id) { "Respuesta de usuario inválida" }
            if (_usuario.value?.id != id) error("La sesión cambió")
            guardarUsuario(actualizado)
            _usuario.value = actualizado
            Result.success(actualizado)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun guardarUsuario(usuario: UsuarioDto) = withContext(Dispatchers.IO) {
        check(requireNotNull(preferencias).edit().putString("usuario", Gson().toJson(usuario)).commit())
    }
}
