package com.example.oyl_fs.model

enum class DestinoNotificacion { PEDIDOS, INVENTARIO }

data class NotificacionAdmin(
    val id: String,
    val titulo: String,
    val mensaje: String,
    val destino: DestinoNotificacion
)

// Solo se crean avisos a partir de los datos reales cargados por la app.
fun crearNotificacionesAdmin(
    pedidos: List<Pedido>,
    productos: List<ProductoInfo>
): List<NotificacionAdmin> {
    val inventario = productos.filter { it.stock <= it.stockMinimo }
        .sortedBy { it.stock }
        .map { producto ->
            NotificacionAdmin(
                id = "stock:${producto.id}:${producto.stock}:${producto.stockMinimo}",
                titulo = if (producto.stock <= 0) "Producto agotado" else "Stock bajo",
                mensaje = "${producto.nombre}: ${producto.stock.coerceAtLeast(0)} unidades disponibles.",
                destino = DestinoNotificacion.INVENTARIO
            )
        }
    val pendientes = pedidos.filter { it.estado == EstadoPedido.PENDIENTE_VALIDACION }
        .sortedByDescending { it.fecha }
        .map { pedido ->
            NotificacionAdmin(
                id = "pedido:${pedido.id}:${pedido.fecha}",
                titulo = "Pedido pendiente",
                mensaje = "El pedido ${pedido.id} está esperando validación.",
                destino = DestinoNotificacion.PEDIDOS
            )
        }
    return inventario + pendientes
}
