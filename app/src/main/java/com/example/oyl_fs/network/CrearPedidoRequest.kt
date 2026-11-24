package com.example.oyl_fs.network

// Lo que la app ENVÍA para crear un pedido (igual al body de Postman)
data class CrearPedidoRequest(
    val usuarioId: Long,
    val items: List<ItemPedidoRequest>
)

// El precio esperado expresa el importe aceptado por el cliente. El servidor exige
// que coincida con su catálogo; nunca lo usa para reemplazar el precio oficial.
data class ItemPedidoRequest(
    val productoId: Long,
    val cantidad: Int,
    val precioEsperado: Int
)