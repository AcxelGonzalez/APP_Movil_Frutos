package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.model.ModalidadEntrega
import com.example.oyl_fs.model.EntregaPedido
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.model.ValidacionEntregaPedido
import kotlinx.coroutines.CancellationException

/** Confirma en el servidor; Room conserva el carrito y la entrega es un complemento local. */
class CompraRemotaRepository(
    context: Context,
    private val database: OylDatabase = OylDatabase.obtener(context),
    private val productos: ProductoRemoteRepository = ProductoRemoteRepository(context),
    private val pedidos: PedidoRemoteRepository = PedidoRemoteRepository(),
    private val complementos: DatosLocalesRemotosRepository = DatosLocalesRemotosRepository(context)
) {
    private val puntosRetiro = PuntoRetiroRepository(database)

    data class Confirmada(val pedido: Pedido, val advertencia: String? = null)

    suspend fun confirmar(usuarioId: String, items: List<ItemCarrito>, entrega: EntregaPedido): Confirmada {
        val id = requireNotNull(usuarioId.toLongOrNull()) { "Inicia sesión nuevamente" }
        require(id > 0 && items.isNotEmpty()) { "El carrito está vacío" }
        require(items.map { it.productoId }.distinct().size == items.size) { "Carrito inválido" }
        val puntos = if (entrega.modalidad == ModalidadEntrega.RETIRO) puntosRetiro.obtenerPuntos() else emptyList()
        val entregaValidada = ValidacionEntregaPedido.normalizar(entrega, puntos)
        // Releer el servidor antes de enviar, para detectar cambios de precio o stock.
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
        val resultado = pedidos.crearPedido(id, items)
        val dto = when (resultado) {
            is ResultadoPedido.Error -> error(resultado.mensaje)
            is ResultadoPedido.Exito -> resultado.pedido
        }
        val pedido = try {
            with(pedidos) { dto.aPedido() }.copy(entrega = entregaValidada)
        } catch (_: Exception) {
            error("El servidor respondió al envío, pero el comprobante no se pudo leer. Revisa Mis pedidos antes de volver a comprar.")
        }
        // Una respuesta exitosa no se convierte en un error de compra si falla el guardado local.
        var advertencia: String? = null
        try {
            complementos.guardarEntrega(pedido)
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            advertencia = "El pedido fue registrado, pero no se pudieron guardar sus datos de entrega en este dispositivo."
        }
        try {
            database.withTransaction {
                for (item in items) database.carritoDao().descontarComprados(usuarioId, item.productoId.toInt(), item.cantidad)
                database.carritoDao().quitarVacios(usuarioId)
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            advertencia = listOfNotNull(advertencia, "El pedido fue registrado. Quita del carrito los artículos comprados antes de volver a comprar.").joinToString(" ")
        }
        return Confirmada(pedido, advertencia)
    }
}
