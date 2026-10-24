package com.example.oyl_fs.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.data.local.UsuarioEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PerfilViewModel(application: Application) : AndroidViewModel(application) {
    // Guardado local sencillo; cada perfil utiliza su identificador.
    private val preferencias = application.getSharedPreferences("perfiles_oyl", Context.MODE_PRIVATE)
    private val _perfiles = MutableStateFlow(
        listOf(cargarPerfil("admin-demo", true), cargarPerfil("cliente-demo", false))
    )
    val perfiles = _perfiles.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun limpiarError() { _error.value = null }

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

    fun guardarPerfil(perfil: PerfilUsuario): Boolean {
        val actual = _perfiles.value.find { it.id == perfil.id }
        _error.value = when {
            actual == null || actual.esAdmin != perfil.esAdmin -> "El perfil no es válido"
            perfil.nombre.isBlank() -> "Escribe el nombre"
            !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(perfil.correo.trim()) -> "Escribe un correo válido"
            perfil.telefono.isNotBlank() &&
                (perfil.telefono.count { it.isDigit() } !in 8..15 ||
                    perfil.telefono.any { !it.isDigit() && it !in "+ -()" }) -> "Revisa el teléfono"
            else -> null
        }
        if (_error.value != null) return false
        val guardado = perfil.copy(
            nombre = perfil.nombre.trim(), apellido = perfil.apellido.trim(),
            rut = perfil.rut.trim(), correo = perfil.correo.trim(),
            telefono = perfil.telefono.trim(), direccion = perfil.direccion.trim()
        )
        // No guarda contraseñas ni cambia el funcionamiento del login.
        preferencias.edit()
            .putString("${perfil.id}.nombre", guardado.nombre)
            .putString("${perfil.id}.apellido", guardado.apellido)
            .putString("${perfil.id}.rut", guardado.rut)
            .putString("${perfil.id}.correo", guardado.correo)
            .putString("${perfil.id}.telefono", guardado.telefono)
            .putString("${perfil.id}.direccion", guardado.direccion)
            .apply()
        _perfiles.value = _perfiles.value.map { if (it.id == perfil.id) guardado else it }
        return true
    }

    /**
     * Incorpora el perfil de un cliente
     * autenticado mediante Room.
     *
     * El identificador es exclusivo de ese cliente.
     *
     * Si anteriormente editó sus datos, recupera
     * las preferencias guardadas para ese ID.
     */
    fun activarClienteRegistrado(usuario: UsuarioEntity) {

        val id = usuario.id

        if (id.isBlank()) return

        val perfil = PerfilUsuario(
            id = id,
            esAdmin = false,

            nombre = preferencias.getString(
                "$id.nombre",
                usuario.nombre
            ) ?: usuario.nombre,

            apellido = preferencias.getString(
                "$id.apellido",
                usuario.apellido
            ) ?: usuario.apellido,

            rut = preferencias.getString(
                "$id.rut",
                usuario.rut
            ) ?: usuario.rut,

            correo = preferencias.getString(
                "$id.correo",
                usuario.correo
            ) ?: usuario.correo,

            telefono = preferencias.getString(
                "$id.telefono",
                usuario.telefono
            ) ?: usuario.telefono,

            direccion = preferencias.getString(
                "$id.direccion",
                usuario.direccion
            ) ?: usuario.direccion
        )

        // Conservar los perfiles demo y únicamente
        // el perfil registrado que inició sesión.
        val perfilesDemo = _perfiles.value.filter {
            it.id == "admin-demo" ||
                    it.id == "cliente-demo"
        }

        _perfiles.value = perfilesDemo + perfil
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

        _error.value = null
    }

}
