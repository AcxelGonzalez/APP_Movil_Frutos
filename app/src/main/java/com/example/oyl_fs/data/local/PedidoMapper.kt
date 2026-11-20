package com.example.oyl_fs.data.local

import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.model.EntregaPedido
import com.example.oyl_fs.model.ModalidadEntrega

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
        estado = estado.name,
        modalidadEntrega = entrega?.modalidad?.name,
        nombreContacto = entrega?.nombreContacto,
        telefonoContacto = entrega?.telefonoContacto,
        direccionEntrega = entrega?.direccion,
        puntoRetiroId = entrega?.puntoRetiroId,
        puntoRetiroNombre = entrega?.puntoRetiroNombre
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
        ),
        entrega = pedido.modalidadEntrega?.let { modalidad ->
            EntregaPedido(ModalidadEntrega.valueOf(modalidad),
                pedido.nombreContacto.orEmpty(), pedido.telefonoContacto.orEmpty(),
                pedido.direccionEntrega.orEmpty(), pedido.puntoRetiroId.orEmpty(), pedido.puntoRetiroNombre.orEmpty())
        }
    )
}
