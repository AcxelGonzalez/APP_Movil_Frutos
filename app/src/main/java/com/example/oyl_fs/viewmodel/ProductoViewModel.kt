package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.screens.ProductosDatos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProductoViewModel : ViewModel() {


    private val _productos = MutableStateFlow<List<ProductoInfo>>(
        ProductosDatos.productos
    )

    val productos: StateFlow<List<ProductoInfo>> =
        _productos.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)

    val error: StateFlow<String?> = _error.asStateFlow()

    // Busca un producto
    fun obtenerProducto(id: Int): ProductoInfo? {
        return _productos.value.find { it.id == id }
    }

    fun limpiarError() {
        _error.value = null
    }

    // Comprueba los datos antes de agregar o editar.
    private fun validarProducto(producto: ProductoInfo): Boolean {
        _error.value = when {
            producto.nombre.isBlank() ->
                "Escribe el nombre del producto"

            producto.codigo.isBlank() ->
                "Escribe el código del producto"

            producto.categoria !in listOf(
                "Frutos secos", "Semillas", "Deshidratados"
            ) ->
                "Selecciona una categoría válida"

            producto.precio <= 0 ->
                "El precio debe ser mayor que cero"

            producto.stock < 0 || producto.stockMinimo < 0 ->
                "El stock no puede ser negativo"

            else -> null
        }

        return _error.value == null
    }

    fun agregarProducto(producto: ProductoInfo): Boolean {
        if (!validarProducto(producto)) return false

        val codigoExiste = _productos.value.any {
            it.codigo.trim().equals(
                producto.codigo.trim(),
                ignoreCase = true
            )
        }

        if (codigoExiste) {
            _error.value = "Ya existe un producto con ese código"
            return false
        }

        // despues bd
        val nuevoId = (_productos.value.maxOfOrNull { it.id } ?: 0) + 1

        val nuevoProducto = producto.copy(
            id = nuevoId,
            nombre = producto.nombre.trim(),
            codigo = producto.codigo.trim()
        )

        // Después guardaremos en SQLite y actualizaremos la lista.
        _productos.value = _productos.value + nuevoProducto
        return true
    }

    fun editarProducto(producto: ProductoInfo): Boolean {
        limpiarError()

        if (obtenerProducto(producto.id) == null) {
            _error.value = "El producto ya no existe"
            return false
        }

        if (!validarProducto(producto)) return false

        val codigoExiste = _productos.value.any {
            it.id != producto.id &&
                    it.codigo.trim().equals(
                        producto.codigo.trim(),
                        ignoreCase = true
                    )
        }

        if (codigoExiste) {
            _error.value = "Otro producto ya utiliza ese código"
            return false
        }

        val productoEditado = producto.copy(
            nombre = producto.nombre.trim(),
            codigo = producto.codigo.trim()
        )

         //  bd
        _productos.value = _productos.value.map {
            if (it.id == producto.id) productoEditado else it
        }

        return true
    }

    // elimina producto

    fun eliminarProducto(id: Int): Boolean {
        limpiarError()

        if (obtenerProducto(id) == null) {
            _error.value = "El producto ya no existe"
            return false
        }

        // bd
        _productos.value = _productos.value.filter {
            it.id != id
        }

        return true
    }
}