package com.example.oyl_fs.model

data class ItemCarrito(
    val productoId: String,
    val nombre: String,
    val formato: String,
    val precio: Int,
    val cantidad: Int,
    val stockDisponible: Int
) {
    val subtotal: Int get() = precio * cantidad
    val stockSuficiente: Boolean get() = cantidad <= stockDisponible
}
