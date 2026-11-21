package com.example.oyl_fs.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.oyl_fs.model.EntregaPedido
import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.network.UsuarioDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Complementos locales: el servidor no almacena estos campos. */
class DatosLocalesRemotosRepository(private val preferencias: SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences("complementos_remotos_oyl", Context.MODE_PRIVATE))
    private val gson = Gson()
    private data class ComplementoPerfil(val rut: String = "", val apellido: String = "")
    private data class ComplementoPedido(val usuarioId: String, val entrega: EntregaPedido)

    fun perfil(usuario: UsuarioDto): PerfilUsuario {
        val extra = leer("perfil.${usuario.id}", ComplementoPerfil::class.java) ?: ComplementoPerfil()
        return PerfilUsuario(usuario.id.toString(), usuario.rol == "ADMIN", usuario.nombre,
            extra.apellido, extra.rut, usuario.correo, usuario.telefono.orEmpty(), usuario.direccion.orEmpty())
    }

    suspend fun guardarPerfil(usuarioId: String, rut: String, apellido: String = "") = withContext(Dispatchers.IO) {
        require(usuarioId.toLongOrNull()?.let { it > 0 } == true)
        check(preferencias.edit().putString("perfil.$usuarioId", gson.toJson(ComplementoPerfil(rut, apellido))).commit())
    }

    suspend fun guardarEntrega(pedido: Pedido) = withContext(Dispatchers.IO) {
        pedido.entrega?.let { entrega ->
            check(preferencias.edit().putString("pedido.${pedido.id}", gson.toJson(ComplementoPedido(pedido.usuarioId, entrega))).commit())
        }
    }

    fun completarPedido(pedido: Pedido): Pedido {
        val extra = leer("pedido.${pedido.id}", ComplementoPedido::class.java)
        return if (extra?.usuarioId == pedido.usuarioId) pedido.copy(entrega = extra.entrega) else pedido
    }

    private fun <T> leer(clave: String, tipo: Class<T>): T? = runCatching {
        preferencias.getString(clave, null)?.let { gson.fromJson(it, tipo) }
    }.getOrNull()
}
