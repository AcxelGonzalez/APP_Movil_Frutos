package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.network.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CancelacionGuardada(val pedido: Pedido, val estado: String, val mensaje: String?) {
    val finalizada: Boolean get() = estado == "COMPLETADA" || estado == "RECHAZADA"
    val pedidoId: Long get() = pedido.id.removePrefix("PED-").toLong()
}

/** Guarda la intención antes del POST. Consultar y restaurar nunca continúan la cancelación. */
class CancelacionPedidoRepository(
    private val database: PerfilRemotoDatabase,
    private val api: CancelacionesApi = RetrofitClient.cancelaciones,
    private val usuarioActivo: () -> Long? = { SesionRemotaRepository.usuarioId }
) {
    constructor(context: Context) : this(PerfilRemotoDatabase.obtener(context))
    companion object { private val exclusividad = Mutex() }
    private val dao = database.cancelacionDao()
    private val gson = Gson()

    fun observar(actorId: String): Flow<List<CancelacionGuardada>> = dao.observar(actorId).map { filas ->
        filas.map { fila ->
            val pedido = requireNotNull(gson.fromJson(fila.contenido, Pedido::class.java))
            check(pedido.id.removePrefix("PED-").toLong() == fila.pedidoId && pedido.usuarioId == fila.propietarioId) {
                "El registro de cancelación del pedido requiere revisión"
            }
            check(fila.estado in setOf("EN_REVISION", "COMPLETADA", "RECHAZADA"))
            CancelacionGuardada(pedido, fila.estado, fila.mensaje)
        }
    }

    suspend fun iniciar(actorId: String, pedido: Pedido, continuar: Boolean): CancelacionDto = exclusividad.withLock {
        verificarUsuario(actorId)
        val id = pedido.id.removePrefix("PED-").toLongOrNull()?.takeIf { it > 0 }
            ?: error("Pedido inválido")
        database.withTransaction {
            dao.insertar(CancelacionLocalEntity(actorId, id, pedido.usuarioId, gson.toJson(pedido),
                "EN_REVISION", "Falta consultar la cancelación de este pedido"))
        }
        recuperarInterno(actorId, id, continuar)
    }

    suspend fun recuperar(actorId: String, pedidoId: Long, continuar: Boolean): CancelacionDto = exclusividad.withLock {
        verificarUsuario(actorId)
        recuperarInterno(actorId, pedidoId, continuar)
    }

    private suspend fun recuperarInterno(actorId: String, pedidoId: Long, continuar: Boolean): CancelacionDto {
        val registro = requireNotNull(dao.obtener(actorId, pedidoId)) { "Falta el registro local del pedido" }
        if (registro.estado in setOf("COMPLETADA", "RECHAZADA")) {
            return CancelacionDto(pedidoId, registro.propietarioId.toLong(), registro.estado, registro.mensaje)
        }
        verificarUsuario(actorId)
        val respuesta = if (continuar) api.continuar(pedidoId) else api.consultar(pedidoId)
        check(respuesta.pedidoId == pedidoId && respuesta.usuarioId.toString() == registro.propietarioId) {
            "La respuesta de cancelación no corresponde al pedido. Se conserva para revisión."
        }
        check(respuesta.estado in setOf("EN_REVISION", "NO_SOLICITADA", "COMPLETADA", "RECHAZADA")) {
            "Estado de cancelación del pedido desconocido. Se conserva para revisión."
        }
        val estadoLocal = if (respuesta.estado == "NO_SOLICITADA") "EN_REVISION" else respuesta.estado
        dao.actualizar(actorId, pedidoId, estadoLocal, respuesta.mensaje)
        return respuesta
    }

    suspend fun cerrar(actorId: String, pedidoId: Long) {
        verificarUsuario(actorId)
        dao.cerrar(actorId, pedidoId) // El SQL impide descartar resultados todavía inciertos.
    }

    private fun verificarUsuario(actorId: String) {
        check(usuarioActivo()?.toString() == actorId) { "Inicia sesión con la cuenta de esta cancelación para recuperar el pedido" }
    }
}
