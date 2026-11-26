package com.example.oyl_fs.repository

import android.content.SharedPreferences
import com.example.oyl_fs.viewmodel.SesionIdentificada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Solo persiste la identidad local; nunca contraseña, hash o un rol editable.
class SesionRepository(
    private val preferencias: SharedPreferences,
    private val usuarios: UsuarioRoomRepository
) {
    suspend fun restaurar(): SesionIdentificada? = withContext(Dispatchers.IO) {
        val id = preferencias.getString("usuarioId", null) ?: return@withContext null
        when (id) {
            "admin-demo" -> SesionIdentificada(id, "Administrador", true, true)
            "cliente-demo" -> SesionIdentificada(id, "Cliente", false, true)
            else -> usuarios.obtenerUsuario(id)?.let {
                SesionIdentificada(it.id, it.nombre, false, false)
            } ?: run {
                cerrar()
                null
            }
        }
    }

    suspend fun guardar(sesion: SesionIdentificada) = withContext(Dispatchers.IO) {
        require(sesion.usuarioId.isNotBlank())
        check(preferencias.edit().putString("usuarioId", sesion.usuarioId).commit()) {
            "No se pudo guardar la sesión"
        }
    }

    suspend fun cerrar() = withContext(Dispatchers.IO) {
        check(preferencias.edit().remove("usuarioId").commit()) {
            "No se pudo cerrar la sesión"
        }
    }
}
