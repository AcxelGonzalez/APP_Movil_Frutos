package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.oyl_fs.data.local.CarritoEntity
import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.ProductoInfo
import kotlinx.coroutines.flow.combine

class CarritoRoomRepository(
    context: Context,
    private val database: OylDatabase = OylDatabase.obtener(context.applicationContext)
) {
    private val contexto = context.applicationContext
    private val dao = database.carritoDao()

    // Stock e imágenes se leen siempre del catálogo actual, también al restaurar.
    // Se conserva el precio aceptado al agregar para detectar cambios al comprar.
    fun observar(usuarioId: String) = combine(
        dao.observar(usuarioId), database.productoDao().observarProductos()
    ) { items, productos ->
        val porId = productos.associateBy { it.id }
        items.map { item ->
            val producto = porId[item.productoId]
            val nombreImagen = if (producto != null) producto.imagenDrawableNombre else item.imagenDrawableNombre
            val imagen = nombreImagen?.let {
                contexto.resources.getIdentifier(it, "drawable", contexto.packageName)
            } ?: 0
            ItemCarrito(
                productoId = item.productoId.toString(), nombre = item.nombre,
                formato = item.formato, precio = item.precio, cantidad = item.cantidad,
                stockDisponible = if (producto?.disponible == true) producto.stock else 0,
                imagen = imagen,
                imagenUri = if (producto != null) producto.imagenUri else item.imagenUri
            )
        }
    }

    suspend fun agregar(usuarioId: String, producto: ProductoInfo, cantidad: Int): Boolean {
        if (usuarioId.isBlank() || cantidad <= 0) return false
        return database.withTransaction {
            val actual = database.productoDao().obtenerPorId(producto.id)
                ?: return@withTransaction false
            if (!actual.disponible || actual.precio != producto.precio) return@withTransaction false
            val anterior = dao.obtener(usuarioId, producto.id)
            val nueva = (anterior?.cantidad?.toLong() ?: 0L) + cantidad
            if (nueva > actual.stock || nueva > Int.MAX_VALUE) return@withTransaction false
            dao.guardar(CarritoEntity(
                usuarioId, actual.id, actual.nombre, actual.presentacion,
                anterior?.precio ?: actual.precio, nueva.toInt(),
                actual.imagenDrawableNombre, actual.imagenUri
            ))
            true
        }
    }

    suspend fun ajustar(usuarioId: String, productoId: Int, delta: Int): Boolean {
        if (usuarioId.isBlank() || delta !in setOf(-1, 1)) return false
        return database.withTransaction {
            val item = dao.obtener(usuarioId, productoId) ?: return@withTransaction false
            val nueva = item.cantidad.toLong() + delta
            if (nueva <= 0) {
                dao.quitar(usuarioId, productoId)
            } else {
                if (delta > 0) {
                    val producto = database.productoDao().obtenerPorId(productoId)
                    if (producto?.disponible != true || nueva > producto.stock) return@withTransaction false
                }
                dao.guardar(item.copy(cantidad = nueva.toInt()))
            }
            true
        }
    }

    suspend fun quitar(usuarioId: String, productoId: Int) {
        if (usuarioId.isNotBlank()) dao.quitar(usuarioId, productoId)
    }
}
