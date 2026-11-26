package com.example.oyl_fs.repository

import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.ProductoInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map as mapFlow
import kotlinx.coroutines.flow.update

object CarritoRepository {

    private val carritos = MutableStateFlow<Map<String, List<ItemCarrito>>>(emptyMap())

    fun obtenerCarrito(clienteId: String): Flow<List<ItemCarrito>> =
        carritos.mapFlow { it[clienteId].orEmpty() }

    fun agregar(clienteId: String, item: ItemCarrito): Boolean {
        val actual = carritos.value[clienteId].orEmpty()
        val existente = actual.find { it.productoId == item.productoId }
        val nuevaCantidad = (existente?.cantidad ?: 0) + item.cantidad
        if (nuevaCantidad > item.stockDisponible) return false

        val nuevaLista = if (existente != null) {
            actual.map {
                if (it.productoId == item.productoId) it.copy(
                    cantidad = nuevaCantidad,
                    imagen = item.imagen,
                    imagenUri = item.imagenUri
                )
                else it
            }
        } else {
            actual + item
        }
        guardar(clienteId, nuevaLista)
        return true
    }

    fun cambiarCantidad(clienteId: String, productoId: String, cantidad: Int) {
        val actual = carritos.value[clienteId].orEmpty()
        val nuevaLista = if (cantidad <= 0) {
            actual.filterNot { it.productoId == productoId }
        } else {
            actual.map { if (it.productoId == productoId) it.copy(cantidad = cantidad) else it }
        }
        guardar(clienteId, nuevaLista)
    }

    fun quitar(clienteId: String, productoId: String) = cambiarCantidad(clienteId, productoId, 0)

    fun vaciar(clienteId: String) = guardar(clienteId, emptyList())

    /**
     * Sincroniza el stock del carrito con el catálogo.
     *
     * Si un producto fue eliminado o desactivado,
     * su stock disponible pasa a cero.
     *
     * Conserva la cantidad seleccionada y el precio
     * original para no modificar silenciosamente
     * los datos de una compra.
     */
    fun sincronizarConCatalogo(
        clienteId: String,
        catalogo: List<ProductoInfo>
    ) {
        if (clienteId.isBlank()) return

        val actual = carritos.value[clienteId].orEmpty()

        if (actual.isEmpty()) return

        // Permite localizar los productos por identificador.
        val productosPorId = catalogo.associateBy {
            it.id.toString()
        }

        val actualizados = actual.map { item ->

            val producto = productosPorId[item.productoId]

            // Si el producto no existe o fue desactivado,
            // no se considera disponible para comprar.
            val stockActual = if (
                producto != null && producto.disponible
            ) {
                producto.stock
            } else {
                0
            }

            item.copy(
                stockDisponible = stockActual,
                imagen = producto?.imagen ?: item.imagen,
                imagenUri = if (producto != null) producto.imagenUri else item.imagenUri
            )
        }

        if (actualizados != actual) {
            guardar(clienteId, actualizados)
        }
    }

    private fun guardar(clienteId: String, lista: List<ItemCarrito>) {
        carritos.update { it + (clienteId to lista) }
    }
}

