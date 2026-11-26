package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.*
import com.example.oyl_fs.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** El intento se confirma en SQLite antes de efectuar cualquier POST. */
class CompraRemotaRepository(
    context: Context,
    private val database: OylDatabase = OylDatabase.obtener(context),
    private val productos: ProductoRemoteRepository = ProductoRemoteRepository(context),
    private val pedidos: PedidoRemoteRepository = PedidoRemoteRepository(),
    private val complementos: DatosLocalesRemotosRepository = DatosLocalesRemotosRepository(context),
    private val intentos: IntentoCompraRepository = IntentoCompraRepository(context),
    private val api: PedidosApi = RetrofitClient.pedidos,
    private val usuarioActivo: () -> Long? = { SesionRemotaRepository.usuarioId }
) {
    companion object { private val exclusividad = Mutex() }
    private val puntosRetiro = PuntoRetiroRepository(database)
    data class Confirmada(val pedido: Pedido, val advertencia: String? = null)
    data class Recuperacion(val confirmada: Confirmada? = null, val mensaje: String? = null)
    suspend fun pendiente(usuarioId: String): IntentoCompra? = intentos.obtener(usuarioId)

    suspend fun confirmar(usuarioId: String, items: List<ItemCarrito>, entrega: EntregaPedido): Confirmada = exclusividad.withLock {
        verificarUsuario(usuarioId)
        check(intentos.obtener(usuarioId) == null) { "Hay una compra en revisión. Consulta o continúa ese intento antes de crear otro." }
        val id = requireNotNull(usuarioId.toLongOrNull()) { "Inicia sesión nuevamente" }
        require(id > 0 && items.isNotEmpty() && items.size <= 200) { "El carrito está vacío o tiene demasiados productos" }
        require(items.map { it.productoId }.distinct().size == items.size) { "Carrito inválido" }
        val puntos = if (entrega.modalidad == ModalidadEntrega.RETIRO) puntosRetiro.obtenerPuntos() else emptyList()
        val entregaValidada = ValidacionEntregaPedido.normalizar(entrega, puntos)
        val catalogo = productos.listar().associateBy { it.id.toString() }
        var total = 0L
        for (item in items) {
            val producto = requireNotNull(catalogo[item.productoId]) { "${item.nombre} ya no está disponible" }
            require(item.cantidad > 0 && producto.disponible && producto.stock >= item.cantidad) { "Revisa el stock de ${item.nombre}" }
            require(item.precio > 0 && producto.precio == item.precio) { "El precio de ${item.nombre} cambió. Quítalo y agrégalo nuevamente." }
            total += item.subtotal
            require(total <= Int.MAX_VALUE) { "El monto supera el máximo permitido" }
            val guardado = database.carritoDao().obtener(usuarioId, producto.id)
            require(guardado != null && guardado.cantidad == item.cantidad && guardado.precio == item.precio) { "El carrito cambió. Revísalo antes de confirmar." }
        }
        verificarUsuario(usuarioId)
        val intento = IntentoCompra(usuarioId, UUID.randomUUID().toString(), items.toList(), entregaValidada)
        intentos.guardar(intento) // Si falla, no enviar. Si se cancela después, el intento ya existe.
        val resultado = resolver(intento, enviar(intento))
        resultado.confirmada ?: error(resultado.mensaje ?: "La compra sigue en revisión. Consulta el mismo intento.")
    }

    /** Únicamente GET. Abrir la pantalla/restaurar la app nunca envía una compra. */
    suspend fun consultar(usuarioId: String): Recuperacion = exclusividad.withLock {
        verificarUsuario(usuarioId)
        val intento = requireNotNull(intentos.obtener(usuarioId)) { "No hay una compra pendiente" }
        resolver(intento, api.consultarSolicitud(intento.clave))
    }
    /** Acción explícita del usuario: conserva clave, cantidades, precios y entrega originales. */
    suspend fun continuar(usuarioId: String): Recuperacion = exclusividad.withLock {
        verificarUsuario(usuarioId)
        val intento = requireNotNull(intentos.obtener(usuarioId)) { "No hay una compra pendiente" }
        // No validar contra el stock actual: este intento podría haberlo descontado ya.
        resolver(intento, enviar(intento))
    }
    private fun verificarUsuario(usuarioId: String) {
        check(usuarioActivo()?.toString() == usuarioId) { "Inicia sesión con la cuenta de esta compra para recuperarla" }
    }
    private suspend fun enviar(intento: IntentoCompra): SolicitudCompraDto = api.crear(intento.clave,
        CrearPedidoRequest(intento.usuarioId.toLong(), intento.items.map {
            ItemPedidoRequest(it.productoId.toLong(), it.cantidad, it.precio)
        }))

    private suspend fun resolver(intento: IntentoCompra, resultado: SolicitudCompraDto): Recuperacion {
        return when (resultado.estado) {
            "COMPLETADA" -> {
                val dto = requireNotNull(resultado.pedido) { "No se pudo leer el comprobante. Conservamos el intento para consultarlo." }
                check(dto.usuarioId.toString() == intento.usuarioId && dto.id > 0) { "Comprobante de otra cuenta: se conserva el intento para revisión" }
                val pedido = with(pedidos) { dto.aPedido() }.copy(entrega = intento.entrega)
                check(pedido.productos.size == intento.items.size &&
                    pedido.productos.map { it.productoId }.distinct().size == intento.items.size && pedido.productos.all { d ->
                    intento.items.any { it.productoId == d.productoId && it.cantidad == d.cantidad && it.precio == d.precioUnitario }
                } && pedido.total.toLong() == intento.items.sumOf { it.subtotal }) { "El comprobante no coincide con el intento. Se conserva para revisión." }
                var advertencia: String? = null
                try {
                    complementos.guardarEntrega(pedido)
                    aplicarCarrito(intento)
                    intentos.quitar(intento)
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) {
                    advertencia = "El pedido está registrado. Queda pendiente completar el guardado local; consulta esta compra nuevamente antes de crear otra."
                }
                Recuperacion(Confirmada(pedido, advertencia))
            }
            "RECHAZADA" -> {
                // Solo el rechazo durable del inventario permite abandonar el intento.
                intentos.quitar(intento)
                Recuperacion(mensaje = resultado.mensaje ?: "Compra rechazada. Revisa el carrito antes de volver a confirmar.")
            }
            "FINALIZADA_SIN_PEDIDO" -> {
                aplicarCarrito(intento)
                intentos.quitar(intento)
                Recuperacion(mensaje = resultado.mensaje ?: "La compra terminó y su pedido ya no está disponible. No se reenviará.")
            }
            "EN_REVISION", "NO_RECIBIDA" -> Recuperacion(mensaje = resultado.mensaje ?: "La compra sigue en revisión. Continuar usará el mismo intento, sin duplicar el descuento.")
            else -> error("Estado de compra desconocido. Se conserva el intento para revisión.")
        }
    }
    private suspend fun aplicarCarrito(intento: IntentoCompra) = database.withTransaction {
        if (!database.compraAplicadaDao().existe(intento.clave, intento.usuarioId)) {
            for (item in intento.items) {
                val id = item.productoId.toInt()
                val actual = database.carritoDao().obtener(intento.usuarioId, id)
                // Una línea reemplazada a otro precio es una selección posterior.
                if (actual != null && actual.precio == item.precio)
                    database.carritoDao().descontarComprados(intento.usuarioId, id, minOf(item.cantidad, actual.cantidad))
            }
            database.carritoDao().quitarVacios(intento.usuarioId)
            database.compraAplicadaDao().guardar(CompraAplicadaEntity(intento.clave, intento.usuarioId))
        }
    }
}
