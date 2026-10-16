package com.example.oyl_fs.model

data class ItemCarrito(
    val productoId: String,
    val nombre: String,
    val formato: String,
    val precio: Int,
    val cantidad: Int,
    val stockDisponible: Int
) {

    /**
     * Calcula el subtotal utilizando Long.
     *
     * Convertimos antes de multiplicar para evitar
     * desbordamientos cuando el precio o la cantidad
     * son elevados.
     */
    val subtotal: Long
        get() = precio.toLong() * cantidad.toLong()

    val stockSuficiente: Boolean get() = cantidad <= stockDisponible
}
