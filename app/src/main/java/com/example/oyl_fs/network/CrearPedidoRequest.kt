package com.example.oyl_fs.network

// Lo que la app ENVÍA para crear un pedido (igual al body de Postman)
data class CrearPedidoRequest(
    val usuarioId: Long,
    val items: List<ItemPedidoRequest>
)

// Un producto del carrito: solo id y cantidad.
// El precio NO se manda: el servidor lo toma de ms-productos, así nadie puede alterarlo.
data class ItemPedidoRequest(
    val productoId: Long,
    val cantidad: Int
)