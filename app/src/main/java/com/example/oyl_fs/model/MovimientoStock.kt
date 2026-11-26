package com.example.oyl_fs.model

enum class TipoMovimientoStock(val titulo: String) {
    ENTRADA("Entrada"), SALIDA("Salida");

    fun cambio(cantidad: Int): Int = if (this == ENTRADA) cantidad else -cantidad
}

object ValidacionMovimientoStock {
    fun errorCantidad(texto: String): String? {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return "Ingresa la cantidad de unidades."
        if (!limpio.all { it in '0'..'9' }) return "Ingresa una cantidad entera positiva."
        val cantidad = limpio.toIntOrNull()
            ?: return "La cantidad supera el máximo permitido (2.147.483.647)."
        return if (cantidad <= 0) "La cantidad debe ser mayor que cero." else null
    }
}
