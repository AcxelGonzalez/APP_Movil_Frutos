package com.example.oyl_fs.model

// Información de cada producto
data class ProductoInfo(
    val id: Int,
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val descripcion: String,
    val presentacion: String,
    val precio: Int,
    val stock: Int,
    val stockMinimo: Int,
    val disponible: Boolean,

    // Img  drawable.
    val imagen: Int = 0,

    // img agregar prod
    val imagenUri: String? = null
)