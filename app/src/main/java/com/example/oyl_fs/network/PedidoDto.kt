package com.example.oyl_fs.network

// Un pedido tal como lo devuelve ms-pedidos
data class PedidoDto(
    val id: Long,
    val usuarioId: Long,
    val fecha: String,
    val estado: String,          // PENDIENTE_VALIDACION, CONFIRMADO, PREPARADO, ENTREGADO
    val total: Long,
    val detalles: List<DetallePedidoDto>
)

// Cada línea del pedido (un producto con su cantidad)
data class DetallePedidoDto(
    val productoId: Long,
    val nombreProducto: String,
    val precioUnitario: Int,
    val cantidad: Int,
    val subtotal: Long
)