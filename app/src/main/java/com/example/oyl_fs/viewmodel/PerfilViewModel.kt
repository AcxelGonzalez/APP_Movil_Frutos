package com.example.oyl_fs.viewmodel

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
    private val preferencias: SharedPreferences = application.getSharedPreferences("perfiles_oyl", Context.MODE_PRIVATE)
) : AndroidViewModel(application) {
    // Las cuentas demo conservan sus preferencias; los clientes usan Room.
    private val _perfiles = MutableStateFlow(
        listOf(cargarPerfil("admin-demo", true), cargarPerfil("cliente-demo", false))
    )
    val perfiles = _perfiles.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _errores = MutableStateFlow(ErroresPerfil())
    val errores = _errores.asStateFlow()

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
        val actual = _perfiles.value.find { it.id == perfil.id }
        if (actual == null || actual.esAdmin != perfil.esAdmin) {
            _errores.value = ErroresPerfil(general = "El perfil no es válido")
            _error.value = _errores.value.primerError()
            return false
        }
        val validacion = ValidacionPerfil.validar(perfil)
        _errores.value = validacion
        _error.value = validacion.primerError()
        if (_error.value != null) return false
        val guardado = perfil.copy(
            nombre = perfil.nombre.trim(), apellido = perfil.apellido.trim(),
            rut = perfil.rut.trim(), correo = perfil.correo.trim(),
            telefono = perfil.telefono.trim(), direccion = perfil.direccion.trim()
        )
        return try {
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
        } catch (_: Exception) {
            _errores.value = ErroresPerfil(general = "No se pudo guardar el perfil")
            _error.value = _errores.value.general
            false
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

        _perfiles.value = _perfiles.value.filter {
            it.id == "admin-demo" ||
                    it.id == "cliente-demo"
        }

        limpiarError()
    }

}
