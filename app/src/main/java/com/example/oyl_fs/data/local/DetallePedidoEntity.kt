
package com.example.oyl_fs.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa un producto incluido en un pedido.
 *
 * Se guardan el nombre y precio al momento
 * de la compra para preservar el historial
 * aunque cambie el catálogo posteriormente.
 */
@Entity(
    tableName = "detalle_pedidos",

    foreignKeys = [
        ForeignKey(
            entity = PedidoEntity::class,
            parentColumns = ["id"],
            childColumns = ["pedidoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index(value = ["pedidoId"])
    ]
)
data class DetallePedidoEntity(

    // Identificador interno del detalle.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Pedido al cual pertenece.
    val pedidoId: String,

    // ID del producto en el momento de compra.
    val productoId: String,

    // Nombre histórico del producto.
    val nombreProducto: String,

    // Cantidad comprada.
    val cantidad: Int,

    // Precio registrado durante la compra.
    val precioUnitario: Int
)
