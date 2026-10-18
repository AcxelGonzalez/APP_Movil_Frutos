package com.example.oyl_fs.repository

import android.content.Context

import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.ProductoEntity
import com.example.oyl_fs.data.local.aEntidad
import com.example.oyl_fs.data.local.ProductoDao
import com.example.oyl_fs.data.local.aProducto
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.data.local.DescuentoStock
import com.example.oyl_fs.data.local.StockNoDisponibleException
import com.example.oyl_fs.model.ItemCarrito

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio de productos almacenados con Room.
 *
 * Se encarga de:
 * - Consultar productos desde SQLite.
 * - Insertar nuevos productos.
 * - Actualizar productos existentes.
 * - Eliminar productos.
 * - Recuperar las imágenes del catálogo.
 *
 * Las operaciones de escritura son suspend
 * para no bloquear el hilo principal.
 */
class ProductoRoomRepository(
    context: Context,

    // Utiliza la base principal normalmente.
    // Durante las pruebas podemos proporcionar
    // un DAO de una base SQLite independiente.
    private val dao: ProductoDao = OylDatabase
        .obtener(context.applicationContext)
        .productoDao()
) {

    private val contexto = context.applicationContext


    /**
     * Observa los productos almacenados.
     *
     * Flow emite una nueva lista cuando Room
     * detecta modificaciones en la tabla.
     */
    val productos: Flow<List<ProductoInfo>> =
        dao.observarProductos().map { entidades ->
            entidades.map { entidad ->
                convertirAProducto(entidad)
            }
        }

    /**
     * Carga los productos guardados en Room.
     *
     * Se utiliza durante la inicialización.
     */
    suspend fun cargarProductos(): List<ProductoInfo> {

        return dao.listarProductos().map { entidad ->
            convertirAProducto(entidad)
        }
    }

    /**
     * Convierte una entidad Room a ProductoInfo
     * recuperando la imagen drawable por nombre.
     */
    private fun convertirAProducto(
        entidad: ProductoEntity
    ): ProductoInfo {

        val imagenId = entidad.imagenDrawableNombre
            ?.let { nombre ->
                contexto.resources.getIdentifier(
                    nombre,
                    "drawable",
                    contexto.packageName
                )
            } ?: 0

        return entidad.aProducto(
            imagen = imagenId
        )
    }

    /**
     * Obtiene el nombre estable de una imagen
     * incluida en los recursos de Android.
     *
     * No almacenamos directamente R.drawable.
     */
    private fun obtenerNombreImagen(
        producto: ProductoInfo
    ): String? {

        if (producto.imagen == 0) {
            return null
        }

        return runCatching {
            contexto.resources.getResourceEntryName(
                producto.imagen
            )
        }.getOrNull()
    }

    /**
     * Busca un producto por su ID.
     */
    suspend fun obtenerPorId(
        id: Int
    ): ProductoInfo? {

        return dao.obtenerPorId(id)
            ?.let { convertirAProducto(it) }
    }

    /**
     * Inserta un producto nuevo en SQLite.
     *
     * Room genera el identificador definitivo
     * cuando el ID es 0.
     *
     * Devuelve el nuevo ID.
     */
    suspend fun insertar(
        producto: ProductoInfo
    ): Long {

        val entidad = producto.aEntidad(
            imagenDrawableNombre =
                obtenerNombreImagen(producto)
        ).copy(id = 0)

        return dao.insertar(entidad)
    }

    /**
     * Inserta un producto y recupera su información
     * utilizando el identificador generado por Room.
     *
     * Esto evita generar IDs manualmente y mantiene
     * la compatibilidad con el carrito y los códigos QR.
     */
    suspend fun insertarYObtener(
        producto: ProductoInfo
    ): ProductoInfo {

        val nuevoId = insertar(producto).toInt()

        return obtenerPorId(nuevoId)
            ?: error(
                "No se pudo recuperar el producto insertado"
            )
    }

    /**
     * Actualiza un producto existente.
     *
     * Conserva su ID para no afectar los
     * pedidos, el carrito ni los códigos QR.
     */
    suspend fun actualizar(
        producto: ProductoInfo
    ): Boolean {

        val anterior = dao.obtenerPorId(producto.id)
            ?: return false

        val nombreImagen =
            obtenerNombreImagen(producto)
                ?: anterior.imagenDrawableNombre

        val entidad = producto.aEntidad(
            imagenDrawableNombre = nombreImagen
        )

        return dao.actualizar(entidad) > 0
    }

    /**
     * Elimina un producto mediante su ID.
     */
    suspend fun eliminar(
        id: Int
    ): Boolean {

        val entidad = dao.obtenerPorId(id)
            ?: return false

        return dao.eliminar(entidad) > 0
    }

    /**
     * Obtiene el número de productos
     * almacenados en SQLite.
     */
    suspend fun contar(): Int {
        return dao.contar()
    }

    /**
     * Descuenta el stock de una compra utilizando
     * una transacción de Room.
     *
     * Comprueba identificadores, cantidades,
     * precios y límites numéricos antes
     * de realizar las actualizaciones.
     *
     * Devuelve true si todos los descuentos
     * fueron confirmados por SQLite.
     */
    suspend fun descontarStockPorCompra(
        items: List<ItemCarrito>
    ): Boolean {

        if (items.isEmpty()) return false

        val descuentos = linkedMapOf<Int, DescuentoStock>()

        var totalCompra = 0L

        for (item in items) {

            val id = item.productoId.toIntOrNull()
                ?: return false

            if (
                id <= 0 ||
                item.cantidad <= 0 ||
                item.precio <= 0
            ) {
                return false
            }

            // Evita desbordamientos del total.
            val subtotal =
                item.precio.toLong() * item.cantidad.toLong()

            if (
                subtotal >
                Int.MAX_VALUE.toLong() - totalCompra
            ) {
                return false
            }

            totalCompra += subtotal

            // Si el mismo producto aparece varias
            // veces, agrupar todas sus cantidades.
            val anterior = descuentos[id]

            // No aceptar precios distintos
            // para un mismo identificador.
            if (
                anterior != null &&
                anterior.precioUnitario != item.precio
            ) {
                return false
            }

            val cantidadTotal =
                (anterior?.cantidad?.toLong() ?: 0L) +
                        item.cantidad.toLong()

            if (cantidadTotal > Int.MAX_VALUE) {
                return false
            }

            descuentos[id] = DescuentoStock(
                productoId = id,
                cantidad = cantidadTotal.toInt(),
                precioUnitario = item.precio
            )
        }

        return try {

            // Todos los descuentos se realizan
            // dentro de una sola transacción.
            dao.descontarStockLote(
                descuentos.values.toList()
            )

            true

        } catch (e: StockNoDisponibleException) {

            // La transacción se revierte.
            false
        }
    }

}
