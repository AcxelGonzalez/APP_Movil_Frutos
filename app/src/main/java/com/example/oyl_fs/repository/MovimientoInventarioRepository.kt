package com.example.oyl_fs.repository

import android.content.Context
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.network.*
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MovimientoInventarioGuardado(
    val actorId: String, val clave: String, val producto: ProductoInfo, val cambio: Int,
    val estado: String, val stockAnterior: Int?, val stockResultante: Int?, val mensaje: String?
) {
    val finalizado: Boolean get() = estado in setOf("APLICADA", "RECHAZADA")
}

class MovimientoInventarioRepository(
    private val database: PerfilRemotoDatabase,
    private val api: MovimientosStockApi = RetrofitClient.movimientosStock,
    private val sesion: () -> SesionActiva? = { SesionRemotaRepository.sesionActual() }
) {
    constructor(context: Context) : this(PerfilRemotoDatabase.obtener(context))
    companion object { private val mutex = Mutex() }
    private val dao = database.movimientoStockDao()
    private val gson = Gson()

    suspend fun obtener(actorId: String): MovimientoInventarioGuardado? = mutex.withLock {
        verificar(actorId)
        dao.obtener(actorId)?.aMovimiento()
    }

    suspend fun iniciar(actorId: String, producto: ProductoInfo, cambio: Int): MovimientoInventarioGuardado = mutex.withLock {
        val inicio = verificar(actorId)
        require(producto.id > 0 && cambio != 0 && cambio != Int.MIN_VALUE) { "Movimiento de inventario inválido" }
        // Si ya existe uno, jamás crear otra clave ni reenviarlo como una intención nueva.
        check(dao.obtener(actorId) == null) { "Resuelve el movimiento de inventario guardado antes de registrar otro." }
        val fila = MovimientoStockLocalEntity(actorId, UUID.randomUUID().toString(), producto.id, cambio,
            gson.toJson(producto), "EN_REVISION", null, null, "Falta confirmar el movimiento con el servidor")
        dao.insertar(fila) // Confirmar SQLite antes del primer HTTP.
        verificar(actorId, inicio.idSesion)
        enviar(fila, continuar = true)
    }

    suspend fun recuperar(actorId: String, clave: String, continuar: Boolean): MovimientoInventarioGuardado = mutex.withLock {
        val inicio = verificar(actorId)
        val fila = requireNotNull(dao.obtener(actorId)) { "No hay un movimiento de inventario guardado" }
        check(fila.clave == clave) { "El movimiento guardado cambió. Vuelve a consultarlo antes de continuar." }
        verificar(actorId, inicio.idSesion)
        if (fila.aMovimiento().finalizado) fila.aMovimiento() else enviar(fila, continuar)
    }

    private suspend fun enviar(fila: MovimientoStockLocalEntity, continuar: Boolean): MovimientoInventarioGuardado {
        verificar(fila.actorId)
        val respuesta = if (continuar) api.continuar(fila.clave, MovimientoStockRequest(fila.productoId.toLong(), fila.cambio))
            else api.consultar(fila.clave)
        check(respuesta.actorId.toString() == fila.actorId && respuesta.clave == fila.clave &&
            respuesta.estado in setOf("ABIERTA", "NO_REGISTRADA", "APLICADA", "RECHAZADA")) {
            "La respuesta del movimiento de inventario requiere revisión. Se conserva el registro original."
        }
        if (respuesta.estado == "NO_REGISTRADA") {
            check(respuesta.productoId == null && respuesta.cambio == null && respuesta.stockAnterior == null && respuesta.stockResultante == null)
        } else {
            check(respuesta.productoId == fila.productoId.toLong() && respuesta.cambio == fila.cambio) {
                "La respuesta del movimiento corresponde a otro producto o cantidad"
            }
        }
        if (respuesta.estado == "APLICADA") {
            val antes = requireNotNull(respuesta.stockAnterior)
            val despues = requireNotNull(respuesta.stockResultante)
            check(antes >= 0 && despues >= 0 && antes.toLong() + fila.cambio == despues.toLong()) {
                "Las existencias del comprobante del movimiento requieren revisión"
            }
        } else check(respuesta.stockAnterior == null && respuesta.stockResultante == null)
        val estado = if (respuesta.estado in setOf("ABIERTA", "NO_REGISTRADA")) "EN_REVISION" else respuesta.estado
        dao.actualizar(fila.actorId, fila.clave, estado, respuesta.stockAnterior, respuesta.stockResultante, respuesta.mensaje)
        return fila.copy(estado = estado, stockAnterior = respuesta.stockAnterior, stockResultante = respuesta.stockResultante,
            mensaje = respuesta.mensaje).aMovimiento()
    }

    suspend fun cerrar(actorId: String, clave: String) = mutex.withLock {
        verificar(actorId)
        dao.cerrar(actorId, clave) // SQL solo permite cerrar estados terminales.
    }

    private fun verificar(actorId: String, idSesion: String? = null): SesionActiva {
        val actual = requireNotNull(sesion()) { "Inicia sesión con la cuenta del movimiento de inventario" }
        check(actual.identidad.esAdmin && actual.identidad.usuarioId == actorId && (idSesion == null || actual.idSesion == idSesion)) {
            "Inicia sesión con la cuenta del movimiento de inventario"
        }
        return actual
    }
    private fun MovimientoStockLocalEntity.aMovimiento(): MovimientoInventarioGuardado {
        check(clave == UUID.fromString(clave).toString() && cambio != 0 && cambio != Int.MIN_VALUE)
        val producto = requireNotNull(gson.fromJson(contenido, ProductoInfo::class.java))
        check(producto.id == productoId && productoId > 0 && estado in setOf("EN_REVISION", "APLICADA", "RECHAZADA")) {
            "El registro local del movimiento requiere revisión"
        }
        if (estado == "APLICADA") {
            check(stockAnterior != null && stockResultante != null && stockAnterior >= 0 && stockResultante >= 0 &&
                stockAnterior.toLong() + cambio == stockResultante.toLong()) { "El comprobante local de inventario requiere revisión" }
        } else check(stockAnterior == null && stockResultante == null)
        return MovimientoInventarioGuardado(actorId, clave, producto, cambio, estado, stockAnterior, stockResultante, mensaje)
    }
}
