package com.example.oyl_fs.model

// Estados disponibles para un pedido.
enum class EstadoPedido(val descripcion: String) {
    PENDIENTE_VALIDACION("Pendiente de Validación"),
    CONFIRMADO("Confirmado"),
    PREPARADO("Preparado"),
    ENTREGADO("Entregado")
}

// Representa un producto incluido en un pedido.
data class DetallePedido(
    val productoId: String,
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: Int
) {
    // Calcula el subtotal de este producto.
    val subtotal: Int
        get() = cantidad * precioUnitario
}

// Representa un pedido realizado por un cliente.
data class Pedido(
    val id: String,
    val usuarioId: String,
    val fecha: Long,
    val productos: List<DetallePedido>,
    val total: Int,
    val estado: EstadoPedido
)
