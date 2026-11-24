package com.example.oyl_fs.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.example.oyl_fs.viewmodel.SesionIdentificada
import com.example.oyl_fs.network.*
import com.example.oyl_fs.util.CifradoSesion
import com.example.oyl_fs.util.CifradoSesionKeystore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class CredencialPeticion internal constructor(val token: String, val idSesion: String)
data class SesionActiva(val identidad: SesionIdentificada, val idSesion: String, val venceEnMillis: Long)
data class InvalidacionSesion(val idSesion: String, val mensaje: String)

object SesionRemotaRepository {
    private val candado = Any()
    private var preferencias: SharedPreferences? = null
    private var cifrado: CifradoSesion = CifradoSesionKeystore()
    private val gson = Gson()
    private var revision = 0L
    private class Persistida(val usuario: UsuarioDto, val accessToken: String,
        val expiresAtEpochSeconds: Long, val idSesion: String)
    private var actual: Persistida? = null
    @Volatile var mensajeRestauracion: String? = null
        private set
    private val _usuario = MutableStateFlow<UsuarioDto?>(null)
    val usuario: StateFlow<UsuarioDto?> = _usuario.asStateFlow()
    private val _invalidacion = MutableStateFlow<InvalidacionSesion?>(null)
    val invalidacion = _invalidacion.asStateFlow()

    fun configurar(context: Context, cifrado: CifradoSesion = CifradoSesionKeystore()) = synchronized(candado) {
        preferencias = context.applicationContext.getSharedPreferences("sesion_remota_oyl", Context.MODE_PRIVATE)
        this.cifrado = cifrado
    }

    fun UsuarioDto.identidad() = SesionIdentificada(id.toString(), nombre, rol == "ADMIN", false)
    val usuarioId: Long? get() = synchronized(candado) { actual?.usuario?.id }
    private fun vencida(sesion: Persistida) = sesion.expiresAtEpochSeconds <= System.currentTimeMillis() / 1000
    private fun validar(usuario: UsuarioDto) {
        require(usuario.id > 0 && usuario.rol in setOf("ADMIN", "CLIENTE")) { "Respuesta de usuario inválida" }
    }
    private fun validar(sesion: Persistida) {
        validar(sesion.usuario)
        require(sesion.idSesion.isNotBlank() && sesion.accessToken.length <= 16384 &&
            Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").matches(sesion.accessToken) &&
            sesion.expiresAtEpochSeconds in 1..Long.MAX_VALUE / 1000) { "Respuesta de sesión inválida" }
    }

    fun sesionActual(): SesionActiva? = synchronized(candado) {
        actual?.takeUnless { vencida(it) }?.let {
            SesionActiva(it.usuario.identidad(), it.idSesion, it.expiresAtEpochSeconds * 1000)
        }
    }

    suspend fun restaurar(): SesionIdentificada? = withContext(Dispatchers.IO) {
        synchronized(candado) {
            revision++
            actual = null; _usuario.value = null; mensajeRestauracion = null
            val preferencias = requireNotNull(preferencias)
            val guardada = preferencias.getString("sesion_cifrada", null)
            if (guardada == null) {
                if (preferencias.contains("usuario")) {
                    check(preferencias.edit().clear().commit())
                    mensajeRestauracion = "Inicia sesión nuevamente para activar la protección de tu cuenta."
                }
                return@synchronized null
            }
            try {
                val recuperada = requireNotNull(gson.fromJson(cifrado.descifrar(guardada), Persistida::class.java))
                validar(recuperada)
                if (vencida(recuperada)) {
                    check(preferencias.edit().clear().commit())
                    mensajeRestauracion = "Tu sesión venció. Vuelve a iniciar sesión. Si estabas confirmando una compra, revisa Mis pedidos antes de repetirla."
                    return@synchronized null
                }
                actual = recuperada; _usuario.value = recuperada.usuario
                recuperada.usuario.identidad()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                check(preferencias.edit().clear().commit())
                mensajeRestauracion = "No se pudo recuperar la sesión protegida. Inicia sesión nuevamente."
                null
            }
        }
    }

    private fun persistir(sesion: Persistida) {
        val texto = cifrado.cifrar(gson.toJson(sesion))
        check(requireNotNull(preferencias).edit().clear().putString("sesion_cifrada", texto).commit())
    }

    suspend fun guardar(sesion: SesionIdentificada) = withContext(Dispatchers.IO) {
        synchronized(candado) {
            val guardada = requireNotNull(actual) { "Inicia sesión con el servidor" }
            require(!vencida(guardada) && guardada.usuario.identidad() == sesion)
            persistir(guardada)
        }
    }

    suspend fun cerrar() = withContext(Dispatchers.IO) {
        synchronized(candado) {
            revision++; actual = null; _usuario.value = null
            check(requireNotNull(preferencias).edit().clear().commit())
        }
    }

    // Se llama desde IO (interceptor o temporizador). No contiene tokens en el aviso de UI.
    fun invalidarSiCoincide(idSesion: String, mensaje: String) = synchronized(candado) {
        val anterior = actual?.takeIf { it.idSesion == idSesion } ?: return@synchronized
        revision++; actual = null; _usuario.value = null
        runCatching { preferencias?.edit()?.clear()?.commit() }
        _invalidacion.value = InvalidacionSesion(anterior.idSesion, mensaje)
    }

    fun credencialParaPeticion(): CredencialPeticion? = synchronized(candado) {
        val sesion = actual ?: return@synchronized null
        if (vencida(sesion)) {
            invalidarSiCoincide(sesion.idSesion, "Tu sesión venció. Vuelve a iniciar sesión. Si estabas confirmando una compra, revisa Mis pedidos antes de repetirla.")
            throw SesionExpiradaException()
        }
        CredencialPeticion(sesion.accessToken, sesion.idSesion)
    }

    suspend fun expirarSiCoincide(idSesion: String) = withContext(Dispatchers.IO) {
        synchronized(candado) {
            actual?.takeIf { it.idSesion == idSesion && vencida(it) }?.let {
                invalidarSiCoincide(idSesion, "Tu sesión venció. Vuelve a iniciar sesión. Si estabas confirmando una compra, revisa Mis pedidos antes de repetirla.")
            }
        }
    }

    suspend fun iniciarSesion(alias: String, contrasena: String, api: UsuariosApi = RetrofitClient.usuarios): Result<UsuarioDto> {
        val inicio = synchronized(candado) { revision }
        return try {
            com.example.oyl_fs.util.ValidacionContrasena.errorMaximo(contrasena)?.let {
                throw IllegalArgumentException(it)
            }
            val respuesta = api.login(LoginRequest(alias.trim(), contrasena))
            require(respuesta.tokenType == "Bearer") { "Respuesta de sesión inválida" }
            val nueva = Persistida(respuesta.usuario, respuesta.accessToken, respuesta.expiresAtEpochSeconds, UUID.randomUUID().toString())
            validar(nueva)
            require(!vencida(nueva)) { "La sesión recibida ya venció" }
            synchronized(candado) {
                check(revision == inicio) { "La sesión cambió" }
                revision++; actual = nueva; _usuario.value = nueva.usuario
            }
            Result.success(nueva.usuario)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun registrar(solicitud: RegistroRequest, api: UsuariosApi = RetrofitClient.usuarios): Result<UsuarioDto> = try {
        com.example.oyl_fs.util.ValidacionContrasena.errorRegistro(solicitud.contrasena)?.let {
            throw IllegalArgumentException(it)
        }
        val creado = api.registrar(solicitud)
        validar(creado); require(creado.rol == "CLIENTE") { "Respuesta de registro inválida" }
        Result.success(creado)
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) { Result.failure(e) }

    suspend fun actualizarPerfil(solicitud: ActualizarPerfilRequest, api: UsuariosApi = RetrofitClient.usuarios): Result<UsuarioDto> {
        val inicio = synchronized(candado) { actual }
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada"))
        return try {
            val actualizado = api.actualizar(inicio.usuario.id, solicitud)
            validar(actualizado)
            require(actualizado.id == inicio.usuario.id && actualizado.rol == inicio.usuario.rol) { "Respuesta de usuario inválida" }
            withContext(Dispatchers.IO) {
                synchronized(candado) {
                    check(actual?.idSesion == inicio.idSesion) { "La sesión cambió" }
                    val nueva = Persistida(actualizado, inicio.accessToken, inicio.expiresAtEpochSeconds, inicio.idSesion)
                    persistir(nueva)
                    actual = nueva; _usuario.value = actualizado
                }
            }
            Result.success(actualizado)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e) }
    }
}
