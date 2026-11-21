package com.example.oyl_fs.data.local

import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido

/**
 * Convierte un pedido de la aplicación
 * en una entidad para SQLite.
 */
fun Pedido.aEntidad(): PedidoEntity {

    return PedidoEntity(
        id = id,
        usuarioId = usuarioId,
        fecha = fecha,
        total = total,
        estado = estado.name
    )
}

/**
 * Convierte los artículos del pedido
 * en registros para detalle_pedidos.
 */
fun Pedido.aEntidadesDetalle():
        List<DetallePedidoEntity> {

    return productos.map { detalle ->

        DetallePedidoEntity(
            pedidoId = id,
            productoId = detalle.productoId,
            nombreProducto = detalle.nombreProducto,
            cantidad = detalle.cantidad,
            precioUnitario = detalle.precioUnitario
        )
    }
}

/**
 * Reconstruye un pedido completo
 * desde las entidades de Room.
 */
fun PedidoConDetalles.aModeloPedido(): Pedido {

    return Pedido(
        id = pedido.id,
        usuarioId = pedido.usuarioId,
        fecha = pedido.fecha,

        productos = detalles.map { detalle ->

            DetallePedido(
                productoId = detalle.productoId,
                nombreProducto = detalle.nombreProducto,
                cantidad = detalle.cantidad,
                precioUnitario = detalle.precioUnitario
            )
        },

        total = pedido.total,

        estado = EstadoPedido.valueOf(
            pedido.estado
        )
    )
}
