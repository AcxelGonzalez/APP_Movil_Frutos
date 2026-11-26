package com.example.oyl_fs.data.local

import androidx.room.Entity

// La identidad incluye al cliente: un producto puede estar en varios carritos.
// No se elimina al borrar el producto; así se informa su falta de disponibilidad.
@Entity(tableName = "carrito", primaryKeys = ["usuarioId", "productoId"])
data class CarritoEntity(
    val usuarioId: String,
    val productoId: Int,
    val nombre: String,
    val formato: String,
    val precio: Int,
    val cantidad: Int,
    val imagenDrawableNombre: String?,
    val imagenUri: String?
)
