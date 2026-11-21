package com.example.oyl_fs.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Representa un pedido junto a todos
 * los productos que contiene.
 *
 * Room utiliza esta relación para
 * recuperar la información desde
 * las tablas pedidos y detalle_pedidos.
 */
data class PedidoConDetalles(

    @Embedded
    val pedido: PedidoEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "pedidoId"
    )
    val detalles: List<DetallePedidoEntity>
)
