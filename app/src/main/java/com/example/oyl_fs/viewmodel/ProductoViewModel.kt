package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.screens.ProductosDatos
import com.example.oyl_fs.model.ItemCarrito
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

    /**
     * Comprueba que los artículos de una compra
     * sigan disponibles en el catálogo.
     *
     * También detecta cambios de precio realizados
     * después de agregar un producto al carrito.
     *
     * Devuelve null si la compra es válida,
     * o un mensaje explicando el problema.
     */
    fun validarStockParaCompra(
        items: List<ItemCarrito>
    ): String? {

        if (items.isEmpty()) {
            return "El carrito está vacío"
        }

        // Validar los montos utilizando Long para evitar
        // desbordamientos antes de calcular el total.
        var totalSeguro = 0L

        for (item in items) {

            // Rechazar precios o cantidades inválidas.
            if (item.precio <= 0 || item.cantidad <= 0) {
                return "Precio o cantidad inválidos en ${item.nombre}"
            }

            // Convertir a Long ANTES de multiplicar.
            val subtotalSeguro =
                item.precio.toLong() * item.cantidad.toLong()

            // No permitir totales superiores al máximo
            // representable por los modelos actuales.
            if (subtotalSeguro > Int.MAX_VALUE.toLong() - totalSeguro) {
                return "El monto de la compra supera el máximo permitido"
            }

            totalSeguro += subtotalSeguro
        }

        // Agrupar cantidades por identificador.
        val cantidades = mutableMapOf<Int, Int>()

        for (item in items) {

            val id = item.productoId.toIntOrNull()
                ?: return "Identificador de producto inválido"

            if (item.cantidad <= 0) {
                return "Cantidad inválida para ${item.nombre}"
            }

            val acumulada =
                (cantidades[id] ?: 0).toLong() + item.cantidad

            if (acumulada > Int.MAX_VALUE) {
                return "Cantidad inválida para ${item.nombre}"
            }

            cantidades[id] = acumulada.toInt()

            val producto = obtenerProducto(id)
                ?: return "El producto ${item.nombre} ya no existe"

            if (producto.precio != item.precio) {
                return "El precio de ${item.nombre} cambió. Quítalo y agrégalo nuevamente."
            }
        }

        // Consultar el stock actual, no el que quedó
        // guardado cuando se agregó al carrito.
        for ((id, cantidad) in cantidades) {

            val producto = obtenerProducto(id)
                ?: return "El producto ya no existe"

            if (!producto.disponible) {
                return "${producto.nombre} no está disponible"
            }

            if (producto.stock < cantidad) {
                return "Stock insuficiente de ${producto.nombre}. Disponibles: ${producto.stock}"
            }
        }

        return null
    }

    /**
     * Descuenta las unidades vendidas del stock.
     *
     * Devuelve false si la compra no supera
     * la validación actual de existencias.
     */
    fun descontarStockPorCompra(
        items: List<ItemCarrito>
    ): Boolean {

        // No descontar si existen errores.
        if (validarStockParaCompra(items) != null) {
            return false
        }

        val cantidades = items
            .groupBy { it.productoId.toInt() }
            .mapValues { (_, lista) ->
                lista.sumOf { it.cantidad }
            }

        // Actualizar la lista observable de productos.
        _productos.value = _productos.value.map { producto ->

            val cantidadVendida =
                cantidades[producto.id] ?: 0

            if (cantidadVendida > 0) {
                producto.copy(
                    stock = producto.stock - cantidadVendida
                )
            } else {
                producto
            }
        }

        return true
    }

}