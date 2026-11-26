package com.example.oyl_fs.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import com.example.oyl_fs.data.local.AvatarPerfilEntity
import com.example.oyl_fs.data.local.PerfilRemotoDatabase
import com.example.oyl_fs.data.local.PerfilRemotoEntity
import com.example.oyl_fs.model.EntregaPedido
import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.network.UsuarioDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Perfiles en Room y entregas en preferencias; RUT y apellido permanecen locales. */
class DatosLocalesRemotosRepository(
    private val preferencias: SharedPreferences,
    private val perfiles: PerfilRemotoDatabase
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences("complementos_remotos_oyl", Context.MODE_PRIVATE),
        PerfilRemotoDatabase.obtener(context)
    )
    private val gson = Gson()
    private data class ComplementoPerfil(val rut: String? = "", val apellido: String? = "")
    private data class ComplementoPedido(val usuarioId: String, val entrega: EntregaPedido)

    suspend fun perfil(usuario: UsuarioDto): PerfilUsuario = perfiles.withTransaction {
        validarIdentidad(usuario)
        val anterior = obtenerOMigrar(usuario.id)
        val actualizado = anterior.conDatosRemotos(usuario)
        perfiles.perfilDao().guardar(actualizado)
        actualizado.aPerfil()
    }

    suspend fun guardarPerfil(usuarioId: String, rut: String, apellido: String = "") = perfiles.withTransaction {
        val id = requireNotNull(usuarioId.toLongOrNull()?.takeIf { it > 0 }) { "Identidad de perfil inválida" }
        perfiles.perfilDao().guardar(obtenerOMigrar(id).copy(rut = rut, apellido = apellido))
    }

    // Registro/edición: copia remota y complementos se confirman en una sola transacción local.
    suspend fun guardarPerfil(usuario: UsuarioDto, rut: String, apellido: String = ""): PerfilUsuario = perfiles.withTransaction {
        validarIdentidad(usuario)
        val actualizado = obtenerOMigrar(usuario.id).conDatosRemotos(usuario).copy(rut = rut, apellido = apellido)
        perfiles.perfilDao().guardar(actualizado)
        actualizado.aPerfil()
    }

    suspend fun obtenerAvatar(clave: String): String? = perfiles.avatarDao().obtener(clave)

    /** Devuelve la referencia anterior solo después de confirmar la nueva en SQLite. */
    suspend fun reemplazarAvatar(clave: String, archivo: String?): String? = perfiles.withTransaction {
        require(clave.startsWith("remoto:") || clave.startsWith("local:")) { "Identidad de avatar inválida" }
        val anterior = perfiles.avatarDao().obtener(clave)
        if (archivo == null) perfiles.avatarDao().quitar(clave)
        else perfiles.avatarDao().guardar(AvatarPerfilEntity(clave, archivo))
        anterior
    }

    private fun validarIdentidad(usuario: UsuarioDto) {
        require(usuario.id > 0 && usuario.rol in setOf("ADMIN", "CLIENTE")) { "Identidad de perfil inválida" }
    }

    private fun PerfilRemotoEntity.conDatosRemotos(usuario: UsuarioDto) = copy(
        esAdmin = usuario.rol == "ADMIN", nombre = usuario.nombre, correo = usuario.correo,
        telefono = usuario.telefono.orEmpty(), direccion = usuario.direccion.orEmpty()
    )

    private suspend fun obtenerOMigrar(id: Long): PerfilRemotoEntity {
        perfiles.perfilDao().obtener(id)?.let { return it }
        // La fila de Room identifica la migración completada. Las preferencias se conservan
        // como respaldo anterior, pero nunca vuelven a prevalecer sobre una fila existente.
        val texto = preferencias.getString("perfil.$id", null)
        val extra = if (texto == null) ComplementoPerfil() else {
            val migrado = requireNotNull(gson.fromJson(texto, ComplementoPerfil::class.java)) {
                "No se pudieron recuperar los datos anteriores del perfil"
            }
            migrado
        }
        return PerfilRemotoEntity(usuarioId = id,
            rut = requireNotNull(extra.rut) { "El RUT anterior del perfil no se pudo recuperar" },
            apellido = requireNotNull(extra.apellido) { "El apellido anterior del perfil no se pudo recuperar" })
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
