package com.example.oyl_fs.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa un producto almacenado en SQLite.
 *
 * Es independiente de ProductoInfo, utilizado
 * actualmente por las pantallas y ViewModels.
 *
 * Conservamos los identificadores numéricos
 * porque también se utilizan en los códigos QR.
 */
@Entity(
    tableName = "productos",
    indices = [
        Index(value = ["codigo"], unique = true)
    ]
)
data class ProductoEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,
    val codigo: String,
    val categoria: String,
    val descripcion: String,
    val presentacion: String,

    val precio: Int,
    val stock: Int,
    val stockMinimo: Int,
    val disponible: Boolean,

    // Nombre estable del recurso drawable.
    // Ejemplo: "almendras".
    val imagenDrawableNombre: String? = null,

    // URI de una imagen elegida por el usuario.
    val imagenUri: String? = null
)
