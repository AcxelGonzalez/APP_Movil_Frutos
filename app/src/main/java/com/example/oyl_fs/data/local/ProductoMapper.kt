package com.example.oyl_fs.data.local

import com.example.oyl_fs.model.ProductoInfo

/**
 * Convierte un producto de la aplicación
 * en una entidad compatible con Room.
 *
 * Conserva el ID utilizado por el carrito
 * y los códigos QR.
 *
 * El nombre del drawable se proporciona
 * por separado, porque los IDs de R.drawable
 * no deben guardarse directamente en SQLite.
 */
fun ProductoInfo.aEntidad(
    imagenDrawableNombre: String? = null
): ProductoEntity {

    return ProductoEntity(
        id = id,
        nombre = nombre,
        codigo = codigo,
        categoria = categoria,
        descripcion = descripcion,
        presentacion = presentacion,
        precio = precio,
        stock = stock,
        stockMinimo = stockMinimo,
        disponible = disponible,
        imagenDrawableNombre = imagenDrawableNombre,
        imagenUri = imagenUri
    )
}

/**
 * Convierte una entidad de Room al modelo
 * utilizado por las pantallas y ViewModels.
 *
 * El ID del drawable se resolverá desde los
 * recursos Android cuando conectemos Room
 * con el repositorio de productos.
 */
fun ProductoEntity.aProducto(
    imagen: Int = 0
): ProductoInfo {

    return ProductoInfo(
        id = id,
        nombre = nombre,
        codigo = codigo,
        categoria = categoria,
        descripcion = descripcion,
        presentacion = presentacion,
        precio = precio,
        stock = stock,
        stockMinimo = stockMinimo,
        disponible = disponible,
        imagen = imagen,
        imagenUri = imagenUri
    )
}
