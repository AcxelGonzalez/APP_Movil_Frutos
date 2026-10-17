
package com.example.oyl_fs.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa un pedido almacenado en SQLite.
 *
 * Conserva el identificador utilizado por
 * Historial, administración y preparación
 * de pedidos, por ejemplo: PED-001.
 *
 * Los productos del pedido se almacenarán
 * en una tabla independiente.
 */
@Entity(
    tableName = "pedidos",
    indices = [
        Index(value = ["usuarioId"]),
        Index(value = ["fecha"])
    ]
)
data class PedidoEntity(

    @PrimaryKey
    val id: String,

    // Identificador del cliente.
    val usuarioId: String,

    // Fecha expresada en milisegundos.
    val fecha: Long,

    // Total confirmado del pedido.
    val total: Int,

    // Nombre del enum EstadoPedido.
    // Ejemplo: PENDIENTE_VALIDACION.
    val estado: String
)
