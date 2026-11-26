package com.example.oyl_fs.network

// Representa un producto tal como lo manda y lo recibe el backend (ms-productos)
data class ProductoDto(
    val id: Long? = null,
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val descripcion: String? = null,
    val presentacion: String,
    val precio: Int,
    val stock: Int,
    val stockMinimo: Int = 0,
    val disponible: Boolean = true,
    val imagenUrl: String? = null
)