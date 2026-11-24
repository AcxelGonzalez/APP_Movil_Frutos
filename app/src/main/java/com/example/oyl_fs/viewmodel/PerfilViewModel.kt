package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.repository.DatosLocalesRemotosRepository
import com.example.oyl_fs.repository.SesionRemotaRepository
import com.example.oyl_fs.network.ActualizarPerfilRequest
import com.example.oyl_fs.network.UsuarioDto
import com.example.oyl_fs.network.mensajeApi
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.util.ArchivosAvatar
import com.example.oyl_fs.util.AvatarInvalidoException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.model.ErroresPerfil
import com.example.oyl_fs.model.ValidacionPerfil
import com.example.oyl_fs.repository.UsuarioRoomRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.oyl_fs.data.local.UsuarioEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PerfilViewModel @JvmOverloads constructor(
    application: Application,
    private val usuarios: UsuarioRoomRepository = UsuarioRoomRepository(application),
    private val preferencias: SharedPreferences = application.getSharedPreferences("perfiles_oyl", Context.MODE_PRIVATE),
    private val complementos: DatosLocalesRemotosRepository = DatosLocalesRemotosRepository(application),
    private val archivosAvatar: ArchivosAvatar = ArchivosAvatar(application)
) : AndroidViewModel(application) {
    // Las cuentas demo conservan sus preferencias; los clientes usan Room.
    private var remoto = false
    private var generacionPerfil = 0L
    private var revisionAvatar = 0L

    suspend fun activarRemoto(usuario: UsuarioDto) {
        remoto = true
        val generacion = ++generacionPerfil
        limpiarAvisosAvatar()
        _perfiles.value = emptyList()
        val perfil = complementos.perfil(usuario)
        if (generacion != generacionPerfil) return
        _perfiles.value = listOf(perfil)
        limpiarError()
    }

    private val _perfiles = MutableStateFlow(
        listOf(cargarPerfil("admin-demo", true), cargarPerfil("cliente-demo", false))
    )
    val perfiles = _perfiles.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _guardando = MutableStateFlow(false)
    val guardando = _guardando.asStateFlow()

    private val _errores = MutableStateFlow(ErroresPerfil())
    val errores = _errores.asStateFlow()

    private val _errorAvatar = MutableStateFlow<String?>(null)
    val errorAvatar = _errorAvatar.asStateFlow()
    private val _mensajeAvatar = MutableStateFlow<String?>(null)
    val mensajeAvatar = _mensajeAvatar.asStateFlow()

    private fun limpiarAvisosAvatar() { _errorAvatar.value = null; _mensajeAvatar.value = null }
    private fun perfilActivo(id: String, generacion: Long) =
        generacionPerfil == generacion && _perfiles.value.any { it.id == id }
    private fun claveAvatar(id: String) = if (remoto) "remoto:$id" else "local:$id"
    private fun mostrarAvatar(id: String, archivo: String?) {
        _perfiles.value = _perfiles.value.map { if (it.id == id) it.copy(avatarArchivo = archivo) else it }
    }

    fun cargarAvatar(usuarioId: String) {
        if (_guardando.value || _perfiles.value.none { it.id == usuarioId }) return
        val generacion = generacionPerfil
        val revision = ++revisionAvatar
        val clave = claveAvatar(usuarioId)
        viewModelScope.launch {
            try {
                val archivo = complementos.obtenerAvatar(clave)
                if (perfilActivo(usuarioId, generacion) && revision == revisionAvatar) mostrarAvatar(usuarioId, archivo)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                if (perfilActivo(usuarioId, generacion) && revision == revisionAvatar) {
                    _errorAvatar.value = "No se pudo recuperar el avatar guardado. Intenta nuevamente."
                }
            }
        }
    }

    // Captura la cuenta antes de abrir la galería; un resultado tardío no pasa a otra sesión.
    fun prepararSeleccionAvatar(usuarioId: String): Long? {
        if (_guardando.value || _perfiles.value.none { it.id == usuarioId }) return null
        limpiarAvisosAvatar()
        return generacionPerfil
    }

    fun errorSeleccionAvatar(usuarioId: String, generacion: Long) {
        if (perfilActivo(usuarioId, generacion)) _errorAvatar.value = "No se pudo abrir la galería. Intenta nuevamente."
    }

    fun cambiarAvatar(usuarioId: String, generacion: Long, origen: Uri) {
        if (_guardando.value || !perfilActivo(usuarioId, generacion)) return
        _guardando.value = true
        limpiarAvisosAvatar()
        ++revisionAvatar
        val clave = claveAvatar(usuarioId)
        // Conocer el nombre antes del IO permite limpiarlo incluso si se cancela el retorno.
        val nuevo = archivosAvatar.nuevoNombre()
        viewModelScope.launch {
            var referenciado = false
            try {
                archivosAvatar.importar(origen, nuevo)
                if (!perfilActivo(usuarioId, generacion)) return@launch
                withContext(NonCancellable) {
                    val anterior = complementos.reemplazarAvatar(clave, nuevo)
                    referenciado = true
                    archivosAvatar.eliminar(anterior)
                }
                if (perfilActivo(usuarioId, generacion)) {
                    mostrarAvatar(usuarioId, nuevo)
                    _mensajeAvatar.value = "Avatar actualizado en este dispositivo."
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                if (perfilActivo(usuarioId, generacion)) _errorAvatar.value =
                    if (e is AvatarInvalidoException) e.message else "No se pudo guardar el avatar. Tu imagen anterior se conserva."
            } finally {
                withContext(NonCancellable) { if (!referenciado) archivosAvatar.eliminar(nuevo) }
                _guardando.value = false
            }
        }
    }

    fun quitarAvatar(usuarioId: String) {
        if (_guardando.value || _perfiles.value.none { it.id == usuarioId }) return
        val generacion = generacionPerfil
        val clave = claveAvatar(usuarioId)
        ++revisionAvatar
        _guardando.value = true
        limpiarAvisosAvatar()
        viewModelScope.launch {
            try {
                withContext(NonCancellable) {
                    val anterior = complementos.reemplazarAvatar(clave, null)
                    archivosAvatar.eliminar(anterior)
                }
                if (perfilActivo(usuarioId, generacion)) {
                    mostrarAvatar(usuarioId, null)
                    _mensajeAvatar.value = "Avatar quitado."
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                if (perfilActivo(usuarioId, generacion)) _errorAvatar.value = "No se pudo quitar el avatar. Intenta nuevamente."
            } finally { _guardando.value = false }
        }
    }

    fun limpiarError() { _error.value = null; _errores.value = ErroresPerfil() }

    private fun cargarPerfil(id: String, esAdmin: Boolean): PerfilUsuario {
        return PerfilUsuario(
            id = id,
            esAdmin = esAdmin,
            nombre = preferencias.getString("$id.nombre", if (esAdmin) "Administrador" else "Cliente") ?: "",
            apellido = preferencias.getString("$id.apellido", "") ?: "",
            rut = preferencias.getString("$id.rut", "") ?: "",
            correo = preferencias.getString("$id.correo", if (esAdmin) "admin@example.test" else "cliente@example.test") ?: "",
            telefono = preferencias.getString("$id.telefono", "") ?: "",
            direccion = preferencias.getString("$id.direccion", "") ?: ""
        )
    }

    suspend fun guardarPerfil(perfil: PerfilUsuario): Boolean {
        if (_guardando.value) return false
        val generacion = generacionPerfil
        val actual = _perfiles.value.find { it.id == perfil.id }
        if (actual == null || actual.esAdmin != perfil.esAdmin) {
            _errores.value = ErroresPerfil(general = "El perfil no es válido")
            _error.value = _errores.value.primerError()
            return false
        }
        val validacion = ValidacionPerfil.validar(perfil, requerirTelefono = remoto)
        _errores.value = validacion
        _error.value = validacion.primerError()
        if (_error.value != null) return false
        val guardado = perfil.copy(
            nombre = perfil.nombre.trim(), apellido = perfil.apellido.trim(),
            rut = perfil.rut.trim(), correo = perfil.correo.trim(),
            telefono = perfil.telefono.trim(), direccion = perfil.direccion.trim()
        )
        _guardando.value = true
        return try {
            if (remoto) {
                val actualizado = SesionRemotaRepository.actualizarPerfil(
                    ActualizarPerfilRequest(guardado.nombre, guardado.correo, guardado.telefono, guardado.direccion)
                ).getOrThrow()
                check(actualizado.id.toString() == perfil.id && (actualizado.rol == "ADMIN") == perfil.esAdmin) {
                    "La respuesta no corresponde al perfil activo"
                }
                val guardadoLocal = complementos.guardarPerfil(actualizado, guardado.rut, guardado.apellido)
                if (generacion != generacionPerfil) return false
                _perfiles.value = listOf(guardadoLocal.copy(avatarArchivo = _perfiles.value.firstOrNull { it.id == perfil.id }?.avatarArchivo))
                limpiarError()
                return true
            }
            if (perfil.id in setOf("admin-demo", "cliente-demo")) {
                withContext(Dispatchers.IO) {
                    check(preferencias.edit()
                        .putString("${perfil.id}.nombre", guardado.nombre)
                        .putString("${perfil.id}.apellido", guardado.apellido)
                        .putString("${perfil.id}.rut", guardado.rut)
                        .putString("${perfil.id}.correo", guardado.correo)
                        .putString("${perfil.id}.telefono", guardado.telefono)
                        .putString("${perfil.id}.direccion", guardado.direccion).commit())
                }
            } else {
                val erroresRoom = usuarios.actualizarPerfil(guardado)
                if (erroresRoom != null) {
                    _errores.value = erroresRoom
                    _error.value = erroresRoom.primerError()
                    return false
                }
            }
            _perfiles.value = _perfiles.value.map { if (it.id == perfil.id) guardado else it }
            limpiarError()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (generacion != generacionPerfil) return false
            _errores.value = ErroresPerfil(general = if (remoto) e.mensajeApi() else "No se pudo guardar el perfil")
            _error.value = _errores.value.general
            false
        } finally {
            _guardando.value = false
        }
    }

    /**
     * Incorpora el perfil de un cliente
     * autenticado mediante Room.
     *
     * El identificador es exclusivo de ese cliente.
     *
     * Incorpora preferencias antiguas válidas una vez;
     * después, Room es la única fuente de datos.
     */
    suspend fun activarClienteRegistrado(usuario: UsuarioEntity) {
        if (usuario.id.isBlank()) return
        remoto = false
        generacionPerfil++
        limpiarAvisosAvatar()
        var actual = usuario
        // Se migra una sola vez. El marcador vive en la misma cuenta de Room,
        // para no volver a sobrescribirla aunque fallen las preferencias antiguas.
        if (!usuario.perfilPreferenciasMigrado && preferencias.contains("${usuario.id}.nombre")) {
            val anterior = PerfilUsuario(
                usuario.id, false,
                preferencias.getString("${usuario.id}.nombre", usuario.nombre).orEmpty(),
                preferencias.getString("${usuario.id}.apellido", usuario.apellido).orEmpty(),
                preferencias.getString("${usuario.id}.rut", usuario.rut).orEmpty(),
                preferencias.getString("${usuario.id}.correo", usuario.correo).orEmpty(),
                preferencias.getString("${usuario.id}.telefono", usuario.telefono).orEmpty(),
                preferencias.getString("${usuario.id}.direccion", usuario.direccion).orEmpty()
            )
            val erroresMigracion = usuarios.actualizarPerfil(anterior)
            if (erroresMigracion == null) {
                actual = usuarios.obtenerUsuario(usuario.id) ?: error("La cuenta ya no existe")
            } else {
                _errores.value = erroresMigracion.copy(general = "Revisa los datos anteriores del perfil: no se pudieron incorporar a la cuenta")
                _error.value = _errores.value.general
            }
        }
        val perfil = PerfilUsuario(actual.id, false, actual.nombre, actual.apellido,
            actual.rut, actual.correo, actual.telefono, actual.direccion)
        _perfiles.value = _perfiles.value.filter { it.id in setOf("admin-demo", "cliente-demo") } + perfil
    }

    /**
     * Elimina de la memoria los perfiles
     * de clientes registrados al cerrar sesión.
     *
     * No borra sus datos guardados.
     */
    fun limpiarSesionRegistrada() {
        generacionPerfil++
        limpiarAvisosAvatar()

        if (remoto) {
            _perfiles.value = emptyList()
            limpiarError()
            return
        }
        _perfiles.value = _perfiles.value.filter {
            it.id == "admin-demo" ||
                    it.id == "cliente-demo"
        }

        limpiarError()
    }

}
