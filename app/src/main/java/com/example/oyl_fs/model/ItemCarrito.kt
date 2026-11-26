package com.example.oyl_fs.model

data class ItemCarrito(
    val productoId: String,
    val nombre: String,
    val formato: String,
    val precio: Int,
    val cantidad: Int,
    val stockDisponible: Int,
    val imagen: Int = 0,
    val imagenUri: String? = null
) {

    val subtotal: Long
        get() = precio.toLong() * cantidad.toLong()

    val stockSuficiente: Boolean get() = cantidad <= stockDisponible
}
